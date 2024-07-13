package per.goweii.androidserver.runtime.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import per.goweii.androidserver.runtime.utils.NotificationUtils

class SystemLocaleChangeReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_LOCALE_CHANGED) {
            NotificationUtils.createHttpServiceNotificationChannel(context)
        }
    }
}