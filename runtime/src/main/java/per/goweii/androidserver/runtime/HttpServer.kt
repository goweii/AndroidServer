package per.goweii.androidserver.runtime

import android.app.Application
import android.content.Intent
import android.os.Build
import per.goweii.androidserver.runtime.service.HttpService

class HttpServer(
    private val application: Application,
) {
    fun start() {
        val intent = Intent(application, HttpService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            application.startForegroundService(intent)
        } else {
            application.startService(intent)
        }
    }

    fun stop() {
        val intent = Intent(application, HttpService::class.java)
        application.stopService(intent)
    }
}