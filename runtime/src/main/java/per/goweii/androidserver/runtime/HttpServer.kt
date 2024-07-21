package per.goweii.androidserver.runtime

import android.os.Handler
import android.os.Looper
import android.util.ArrayMap
import android.util.Log
import com.koushikdutta.async.AsyncServer
import com.koushikdutta.async.AsyncServerSocket
import com.koushikdutta.async.http.server.AsyncHttpServer
import com.koushikdutta.async.http.server.AsyncHttpServerRequest
import com.koushikdutta.async.http.server.AsyncHttpServerResponse
import com.koushikdutta.async.http.server.AsyncHttpServerRouter
import com.koushikdutta.async.http.server.HttpServerRequestCallback
import per.goweii.androidserver.runtime.exception_handler.ExceptionHandler
import per.goweii.androidserver.runtime.http.HttpDelegate
import per.goweii.androidserver.runtime.http.HttpMethod
import per.goweii.androidserver.runtime.http.NoRequestBody
import per.goweii.androidserver.runtime.interceptor.RequestInterceptor
import per.goweii.androidserver.runtime.ws.WebSocketDelegate
import java.net.InetAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Suppress("MemberVisibilityCanBePrivate")
abstract class HttpServer {
    protected open val host: String = "localhost"

    protected abstract val port: Int

    protected abstract val pathMatcher: PathMatcher

    private val requestInterceptors = ArrayMap<PathMatcher, RequestInterceptor>()
    private val exceptionHandlers = ArrayMap<Class<out Throwable>, ExceptionHandler<out Throwable>>()

    private val asyncHttpServer = object : AsyncHttpServer() {
        override fun onRequest(request: AsyncHttpServerRequest, response: AsyncHttpServerResponse): Boolean {
            requestInterceptors.forEach {
                if (it.key.match(request.path) && it.value.intercept(request, response)) {
                    response.end()
                    return true
                }
            }
            return false
        }

        override fun onRequest(callback: HttpServerRequestCallback?, request: AsyncHttpServerRequest, response: AsyncHttpServerResponse) {
            if (callback != null) {
                try {
                    callback.onRequest(request, response)
                } catch (e: Throwable) {
                    val eClass = e.javaClass

                    val exceptionHandler = exceptionHandlers[eClass]
                    if (exceptionHandler == null) {
                        exceptionHandlers.entries
                            .find { it.key.isAssignableFrom(eClass) }
                            ?.value
                    }

                    if (exceptionHandler != null) {
                        @Suppress("UNCHECKED_CAST")
                        exceptionHandler as ExceptionHandler<Throwable>
                        exceptionHandler.handle(request, response, e)
                    } else {
                        response.code(500)
                        response.end()
                    }
                }
            } else {
                response.code(500)
                response.end()
            }
        }
    }

    private var executorService: ExecutorService? = null
    private var mainHandler: Handler? = null

    private var asyncServerSocket: AsyncServerSocket? = null

    val isRunning: Boolean get() = asyncServerSocket != null

    var onStart: (() -> Unit)? = null
    var onRunning: (() -> Unit)? = null
    var onFailed: ((Throwable) -> Unit)? = null

    init {
        asyncHttpServer.setErrorCallback {
            it.printStackTrace()
        }
    }

    fun <E : Throwable> addExceptionHandler(eClass: Class<E>, exceptionHandler: ExceptionHandler<E>) {
        exceptionHandlers[eClass] = exceptionHandler
    }

    fun addRequestInterceptor(pathMatcher: PathMatcher, requestInterceptor: RequestInterceptor) {
        requestInterceptors[pathMatcher] = requestInterceptor
    }

    fun start() {
        if (isRunning) {
            return
        }

        if (executorService == null || executorService!!.isTerminated || executorService!!.isShutdown) {
            executorService = Executors.newSingleThreadExecutor()
        }

        if (mainHandler == null) {
            mainHandler = Handler(Looper.getMainLooper())
        }

        executorService!!.submit {
            onStart()

            asyncServerSocket?.stop()
            asyncServerSocket = null

            asyncHttpServer.stop()

            try {
                onRegister(asyncHttpServer)

                val asyncServerSocket = AsyncServer.getDefault().listen(
                    InetAddress.getByName(host),
                    port,
                    asyncHttpServer.listenCallback
                )

                asyncServerSocket ?: throw Exception("Http server start failed")

                this.asyncServerSocket = asyncServerSocket

                onRunning()
            } catch (e: Throwable) {
                onFailed(e)
            }
        }
    }

    fun stop() {
        executorService?.shutdownNow()
        executorService = null

        mainHandler?.removeCallbacksAndMessages(null)
        mainHandler = null

        asyncServerSocket?.stop()
        asyncServerSocket = null

        asyncHttpServer.stop()
    }

    protected open fun onStart() {
        Log.d(javaClass.simpleName, "onStart")

        mainHandler?.post {
            onStart?.invoke()
        }
    }

    protected open fun onRegister(asyncHttpServer: AsyncHttpServer) {
        Log.d(javaClass.simpleName, "onRegister")

        val delegates = ServerRegistry.getDelegates(pathMatcher)
        delegates.forEach { delegate ->
            when (delegate) {
                is HttpDelegate -> {
                    delegate.requests.forEach { httpRequest ->
                        asyncHttpServer.addAction(httpRequest.method.name, httpRequest.path.pathPattern, httpRequest) { _ ->
                            when (httpRequest.method) {
                                HttpMethod.GET -> NoRequestBody
                                HttpMethod.POST -> null
                            }
                        }
                    }
                }

                is WebSocketDelegate -> {
                    asyncHttpServer.get(delegate.path.pathPattern) { request, response ->
                        delegate.onRequest(request, response)

                        val protocol = delegate.protocol.takeIf { it.isEmpty() }
                            ?: request.headers["Sec-WebSocket-Protocol"]
                            ?: ""

                        val webSocket = AsyncHttpServerRouter.checkWebSocketUpgrade(protocol, request, response)
                        if (webSocket == null) {
                            response.code(404)
                            response.end()
                            return@get
                        }

                        delegate.onConnect(webSocket)

                        webSocket.setPingCallback { delegate.onPing(it) }
                        webSocket.setPongCallback { delegate.onPong(it) }
                        webSocket.setStringCallback { delegate.onMessage(it) }
                        webSocket.setClosedCallback { delegate.onClose(it) }
                    }
                }

                else -> {}
            }
        }
    }

    protected open fun onRunning() {
        Log.d(javaClass.simpleName, "onRunning")

        mainHandler?.post {
            onRunning?.invoke()
        }
    }

    protected open fun onFailed(e: Throwable) {
        Log.d(javaClass.simpleName, "onFailed: $e")

        mainHandler?.post {
            onFailed?.invoke(e)
        }
    }
}