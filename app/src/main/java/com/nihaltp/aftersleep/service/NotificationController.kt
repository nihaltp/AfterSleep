package com.nihaltp.aftersleep.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nihaltp.aftersleep.MainActivity
import com.nihaltp.aftersleep.R
import com.nihaltp.aftersleep.data.NotificationChannelHelper
import com.nihaltp.aftersleep.data.model.MediaSessionSnapshot
import com.nihaltp.aftersleep.data.model.PlaybackStage

class NotificationController(private val context: Context) {
    fun notify(
        notificationId: Int,
        title: String,
        stage: PlaybackStage,
        remainingMillis: Long,
        session: MediaSessionSnapshot?,
    ) {
        val notification = buildNotification(title, stage, remainingMillis, session)
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    fun buildNotification(
        title: String,
        stage: PlaybackStage,
        remainingMillis: Long,
        session: MediaSessionSnapshot? = null,
    ): Notification {
        val contentIntent =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                },
                pendingIntentFlag(),
            )

        val cancelIntent = serviceIntent(AfterSleepService.ACTION_CANCEL)
        val pauseIntent = serviceIntent(AfterSleepService.ACTION_PAUSE_NOW)
        val playIntent = serviceIntent(AfterSleepService.ACTION_PLAY_NOW)
        val openAppIntent = session?.packageName?.let { serviceIntent(AfterSleepService.ACTION_OPEN_APP, it) }

        val appName = session?.appLabel ?: context.getString(R.string.app_name)
        val contentText =
            when (stage) {
                PlaybackStage.WaitingToStart -> "$appName resumes in ${formatDuration(remainingMillis)}"
                PlaybackStage.WaitingToStop -> "$appName stops in ${formatDuration(remainingMillis)}"
                PlaybackStage.Playing -> "$appName is playing"
                PlaybackStage.Error -> title
                else -> title
            }

        val builder =
            NotificationCompat.Builder(
                context,
                if (stage == PlaybackStage.Error) {
                    NotificationChannelHelper.FAILURE_CHANNEL_ID
                } else {
                    NotificationChannelHelper.SERVICE_CHANNEL_ID
                },
            )
                .setSmallIcon(android.R.drawable.ic_media_pause)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(contentText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                .setContentIntent(contentIntent)
                .setOngoing(stage == PlaybackStage.WaitingToStart || stage == PlaybackStage.WaitingToStop)
                .setOnlyAlertOnce(true)
                .setSilent(stage != PlaybackStage.Error)

        builder.addAction(android.R.drawable.ic_media_pause, "Cancel", cancelIntent)
        builder.addAction(android.R.drawable.ic_media_previous, "Pause now", pauseIntent)
        builder.addAction(android.R.drawable.ic_media_play, "Play now", playIntent)
        if (stage == PlaybackStage.Error && openAppIntent != null) {
            builder.addAction(android.R.drawable.ic_menu_view, "Open app", openAppIntent)
        }

        return builder.build()
    }

    private fun serviceIntent(
        action: String,
        packageName: String? = null,
    ): PendingIntent {
        val intent =
            Intent(context, AfterSleepService::class.java).apply {
                this.action = action
                if (packageName != null) {
                    putExtra(AfterSleepService.EXTRA_PACKAGE_NAME, packageName)
                }
            }
        return PendingIntent.getService(context, action.hashCode(), intent, pendingIntentFlag())
    }

    private fun pendingIntentFlag(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

    private fun formatDuration(millis: Long): String {
        val totalSeconds = (millis / 1000).coerceAtLeast(0L)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}
