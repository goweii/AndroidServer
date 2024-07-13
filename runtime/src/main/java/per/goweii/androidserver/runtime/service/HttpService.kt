package per.goweii.androidserver.runtime.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.koushikdutta.async.http.server.AsyncHttpServer
import per.goweii.androidserver.runtime.HttpMethod
import per.goweii.androidserver.runtime.HttpRegistry
import per.goweii.androidserver.runtime.NoRequestBody
import per.goweii.androidserver.runtime.R
import per.goweii.androidserver.runtime.utils.NotificationUtils
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class HttpService : Service() {
    companion object {
        private const val TAG = "HttpService"

        private const val DEFAULT_PORT = 2476

        private const val EXTRA_SERVER_NAME = "server_name"
        private const val EXTRA_SERVER_PORT = "server_port"

        private const val EXTRA_COMMAND = "command"

        private const val COMMAND_RESTART = 1
        private const val COMMAND_STOP = 2
    }

    private val id = System.identityHashCode(this)

    private lateinit var executorService: ExecutorService
    private lateinit var mainHandler: Handler

    private val httpServer = AsyncHttpServer()

    private var serviceName: String? = null
    private var port: Int? = null

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        executorService = Executors.newSingleThreadExecutor()
        mainHandler = Handler(Looper.getMainLooper())
        httpServer.setErrorCallback {
            Log.e(TAG, it.toString())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceName = intent?.getStringExtra(EXTRA_SERVER_NAME)

        showNotification(
            contentText = getString(R.string.android_server_http_server_state_preparing),
            btnText = getString(R.string.android_server_btn_stop),
            command = COMMAND_STOP,
        )

        val command: Int? =
            if (intent?.hasExtra(EXTRA_COMMAND) == true) {
                intent.getIntExtra(EXTRA_COMMAND, 0)
            } else {
                null
            }

        when (command) {
            COMMAND_RESTART -> {
                port = intent!!.getIntExtra(EXTRA_SERVER_PORT, port ?: DEFAULT_PORT)
                startServer(port!!)
            }

            COMMAND_STOP -> {
                stopSelf()
            }

            null -> {
                if (port == null) {
                    port = intent?.getIntExtra(EXTRA_SERVER_PORT, DEFAULT_PORT) ?: DEFAULT_PORT
                    startServer(port!!)
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        httpServer.stop()
        executorService.shutdownNow()
        mainHandler.removeCallbacksAndMessages(null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        super.onDestroy()
    }

    private fun startServer(port: Int) {
        executorService.submit {
            mainHandler.post {
                showNotification(
                    contentText = getString(R.string.android_server_http_server_state_starting),
                    btnText = getString(R.string.android_server_btn_stop),
                    command = COMMAND_STOP,
                )
            }

            httpServer.stop()

            try {
                HttpRegistry.loadDelegate(application)
                    .forEach {
                        httpServer.addAction(it.method.name, it.pathRegex, it) { _ ->
                            when (it.method) {
                                HttpMethod.GET -> NoRequestBody
                                HttpMethod.POST -> null
                            }
                        }
                    }

                httpServer.listen(port) ?: throw Exception("Http server start failed")

                mainHandler.post {
                    showNotification(
                        contentText = getString(R.string.android_server_http_server_state_running),
                        btnText = getString(R.string.android_server_btn_stop),
                        command = COMMAND_STOP,
                    )
                }
            } catch (e: Throwable) {
                e.printStackTrace()

                mainHandler.post {
                    showNotification(
                        contentText = getString(R.string.android_server_http_server_state_failed),
                        btnText = getString(R.string.android_server_btn_Restart),
                        command = COMMAND_RESTART,
                    )
                }
            }
        }
    }

    private fun showNotification(
        contentText: String,
        btnText: String,
        command: Int,
    ) {
        val notification = NotificationUtils.buildHttpServiceNotification(application)
            .setSmallIcon(application.applicationInfo.icon)
            .setContentTitle(serviceName ?: getString(R.string.android_server_http_server_name))
            .setContentText(contentText)
            .apply {
                addAction(
                    NotificationCompat.Action.Builder(
                        null,
                        btnText,
                        PendingIntent.getService(
                            application,
                            command,
                            Intent(application, HttpService::class.java).also {
                                it.putExtra(EXTRA_COMMAND, command)
                            },
                            PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    ).build()
                )
            }
            .build()

        startForeground(id, notification)
    }
}