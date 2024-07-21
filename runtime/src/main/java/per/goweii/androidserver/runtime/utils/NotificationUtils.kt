package per.goweii.androidserver.runtime.utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import per.goweii.androidserver.runtime.R

internal object NotificationUtils {
    fun createHttpServiceNotificationChannel(context: Context): String {
        val channelId = context.getString(R.string.android_server_notification_channel_id)

        createNotificationChannel(
            context = context,
            importance = NotificationManagerCompat.IMPORTANCE_DEFAULT,
            id = channelId,
            name = context.getString(R.string.android_server_notification_channel_name),
            description = context.getString(R.string.android_server_notification_channel_desc),
        )

        return channelId
    }

    fun buildHttpServiceNotification(
        context: Context,
    ): NotificationCompat.Builder {
        val channelId = createHttpServiceNotificationChannel(context)

        val packageManager = context.packageManager
        val packageName = context.packageName

        return NotificationCompat.Builder(context, channelId)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSmallIcon(context.applicationInfo.icon)
            .apply {
                val launchIntent = packageManager.getLeanbackLaunchIntentForPackage(packageName)
                    ?: packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    val intent = Intent(Intent.ACTION_MAIN)
                    launchIntent.categories?.forEach { intent.addCategory(it) }
                    intent.component = launchIntent.component
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        intent.interProcessHashCode,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE,
                    )
                    setContentIntent(pendingIntent)
                }
            }
    }

    @Suppress("SameParameterValue")
    private fun createNotificationChannel(
        context: Context,
        importance: Int,
        id: String,
        name: String,
        description: String,
    ) {
        val notificationChannelCompat =
            NotificationChannelCompat.Builder(id, importance)
                .setName(name)
                .setDescription(description)
                .setLightsEnabled(false)
                .setShowBadge(false)
                .build()

        val notificationManagerCompat = NotificationManagerCompat.from(context)
        notificationManagerCompat.createNotificationChannel(notificationChannelCompat)
    }
}