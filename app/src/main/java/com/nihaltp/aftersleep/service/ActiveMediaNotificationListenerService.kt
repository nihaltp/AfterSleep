package com.nihaltp.aftersleep.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.nihaltp.aftersleep.AfterSleepApplication

class ActiveMediaNotificationListenerService : NotificationListenerService() {
    private val container by lazy { (application as AfterSleepApplication).container }

    override fun onListenerConnected() {
        super.onListenerConnected()
        refreshSessions()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        refreshSessions()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        refreshSessions()
    }

    private fun refreshSessions() {
        val snapshots = container.mediaPlaybackOrchestrator.refreshActiveSessions()
        container.activeSessionRepository.update(snapshots)
    }
}
