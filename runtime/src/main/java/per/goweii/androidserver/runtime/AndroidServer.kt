package per.goweii.androidserver.runtime

import android.app.Application

object AndroidServer {
    private lateinit var application: Application

    fun init(application: Application) {
        if (::application.isInitialized) {
            return
        }

        synchronized(this) {
            this.application = application
        }

        ServerRegistry.init(application)
    }
}