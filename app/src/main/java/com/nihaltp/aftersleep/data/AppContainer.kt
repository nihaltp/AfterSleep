package com.nihaltp.aftersleep.data

import android.content.Context
import com.nihaltp.aftersleep.media.ActiveMediaSessionReader
import com.nihaltp.aftersleep.media.MediaPlaybackOrchestrator
import com.nihaltp.aftersleep.service.WakeLockHelper

class AppContainer(context: Context) {
    val appContext = context.applicationContext

    val notificationHelper = NotificationChannelHelper(appContext)
    val settingsRepository = SettingsRepository(appContext)
    val timerStateRepository = TimerStateRepository(appContext)
    val reliabilityRepository = ReliabilityRepository(appContext)
    val activeSessionRepository = ActiveSessionRepository(appContext)
    val activeSessionReader = ActiveMediaSessionReader(appContext)
    val wakeLockHelper = WakeLockHelper(appContext)
    val mediaPlaybackOrchestrator =
        MediaPlaybackOrchestrator(
            context = appContext,
            activeSessionRepository = activeSessionRepository,
            activeSessionReader = activeSessionReader,
            settingsRepository = settingsRepository,
        )
    val batteryOptimizationHelper = BatteryOptimizationHelper(appContext)
}
