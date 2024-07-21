package per.goweii.androidserver.runtime.ws

import com.koushikdutta.async.http.WebSocket
import com.koushikdutta.async.http.server.AsyncHttpServerRequest
import com.koushikdutta.async.http.server.AsyncHttpServerResponse
import per.goweii.androidserver.runtime.http.HttpPath
import per.goweii.androidserver.runtime.RestDelegate

internal class WebSocketDelegate(
    path: HttpPath,
    protocol: String,
    val instance: WebSocketController,
) : RestDelegate(
    path = path,
    protocol = protocol,
) {
    companion object {
        private const val TAG = "WebSocketDelegate"
    }

    fun onRequest(request: AsyncHttpServerRequest, response: AsyncHttpServerResponse) {
        instance.onRequest(request, response)
    }

    fun onConnect(webSocket: WebSocket) {
        instance.onConnect(webSocket)
    }

    fun onClose(e: Throwable) {
        instance.onClose(e)
    }

    fun onPing(message: String) {
        instance.onPing(message)
    }

    fun onPong(message: String) {
        instance.onPong(message)
    }

    fun onMessage(message: String) {
        instance.onMessage(message)
    }
}