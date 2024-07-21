package per.goweii.androidserver.runtime.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import per.goweii.androidserver.runtime.R
import per.goweii.androidserver.runtime.HttpServer
import per.goweii.androidserver.runtime.utils.NotificationUtils
import per.goweii.androidserver.runtime.utils.interProcessHashCode
import java.lang.reflect.ParameterizedType

abstract class HttpService<S : HttpServer> : Service() {
    companion object {
        private const val EXTRA_SERVER_NAME = "server_name"

        private const val EXTRA_COMMAND = "command"

        inline fun <reified T : HttpService<*>> start(
            context: Context,
            serverName: String? = null,
        ) = start(
            context = context,
            serverClass = T::class.java,
            serverName = serverName,
        )

        fun start(
            context: Context,
            serverClass: Class<out HttpService<*>>,
            serverName: String? = null,
        ) {
            val intent = Intent(context, serverClass)
            serverName?.let { intent.putExtra(EXTRA_SERVER_NAME, it) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    enum class Command {
        RESTART,
        STOP,
    }

    @Suppress("UNCHECKED_CAST")
    private val server: S by lazy {
        val superClass = javaClass.genericSuperclass
        superClass as ParameterizedType
        val sType = superClass.actualTypeArguments.first()
        sType as Class<*>
        val constructor = sType.getConstructor()
        constructor.newInstance() as S
    }

    private val id: Int = server.interProcessHashCode

    private var serverName: String? = null
    private var serverHost: String? = null
    private var serverPort: Int? = null

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        Log.d(javaClass.simpleName, "onCreate: $id")

        showNotification(
            contentText = getString(R.string.android_server_http_server_state_preparing),
            btnText = getString(R.string.android_server_btn_stop),
            command = Command.STOP,
        )

        server.onStart = {
            showNotification(
                contentText = getString(R.string.android_server_http_server_state_starting),
                btnText = getString(R.string.android_server_btn_stop),
                command = Command.STOP,
            )
        }

        server.onRunning = {
            showNotification(
                contentText = getString(R.string.android_server_http_server_state_running),
                btnText = getString(R.string.android_server_btn_stop),
                command = Command.STOP,
            )
        }

        server.onFailed = {
            showNotification(
                contentText = getString(R.string.android_server_http_server_state_failed),
                btnText = getString(R.string.android_server_btn_Restart),
                command = Command.RESTART,
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serverName = intent?.getStringExtra(EXTRA_SERVER_NAME)

        val command = intent?.getStringExtra(EXTRA_COMMAND)?.let { Command.valueOf(it) }

        Log.d(javaClass.simpleName, "onStartCommand: $command")

        when (command) {
            Command.RESTART -> {
                server.start()
            }

            Command.STOP -> {
                stopSelf()
            }

            null -> {
                if (!server.isRunning) {
                    server.start()
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        Log.d(javaClass.simpleName, "onDestroy")

        server.stop()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        super.onDestroy()
    }

    private fun showNotification(
        contentText: String,
        btnText: String,
        command: Command,
    ) {
        Log.d(javaClass.simpleName, contentText)
        val notification = NotificationUtils.buildHttpServiceNotification(application)
            .setSmallIcon(application.applicationInfo.icon)
            .setContentTitle(serverName ?: getString(R.string.android_server_http_server_name))
            .setContentText(contentText)
            .apply {
                Intent(application, this@HttpService.javaClass).also {
                    it.putExtra(EXTRA_COMMAND, command.name)
                    Log.d(javaClass.simpleName, it.toString())
                }.let { intent ->
                    addAction(
                        NotificationCompat.Action.Builder(
                            null,
                            btnText,
                            PendingIntent.getService(
                                application,
                                intent.interProcessHashCode,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT,
                            )
                        ).build()
                    )
                }
            }
            .build()

        startForeground(id, notification)
    }
}