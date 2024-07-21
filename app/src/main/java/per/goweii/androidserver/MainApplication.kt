package per.goweii.androidserver

import android.app.Application
import android.content.Context
import per.goweii.androidserver.runtime.AndroidServer

class MainApplication: Application() {
    override fun getApplicationContext(): Context {
        return this
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        AndroidServer.init(this)
    }
}