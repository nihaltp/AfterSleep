package com.nihaltp.aftersleep.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import com.nihaltp.aftersleep.R

class NotificationChannelHelper(private val context: Context) {
    companion object {
        const val SERVICE_CHANNEL_ID = "aftersleep_service"
        const val FAILURE_CHANNEL_ID = "aftersleep_failures"
    }

    fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val serviceChannel =
            NotificationChannel(
                SERVICE_CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            }

        val failureChannel =
            NotificationChannel(
                FAILURE_CHANNEL_ID,
                context.getString(R.string.playback_failure_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.playback_failure_channel_description)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            }

        NotificationManagerCompat.from(context).createNotificationChannel(serviceChannel)
        NotificationManagerCompat.from(context).createNotificationChannel(failureChannel)
    }
}
