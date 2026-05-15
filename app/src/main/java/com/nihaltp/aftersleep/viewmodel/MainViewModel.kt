package com.nihaltp.aftersleep.viewmodel

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nihaltp.aftersleep.BuildConfig
import com.nihaltp.aftersleep.data.AppContainer
import com.nihaltp.aftersleep.service.AfterSleepService
import com.nihaltp.aftersleep.ui.model.MainUiState
import com.nihaltp.aftersleep.ui.model.PermissionStateSnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

class MainViewModel(private val container: AppContainer) : ViewModel() {
    private val ticker: Flow<Long> =
        flow {
            while (true) {
                emit(SystemClock.elapsedRealtime())
                delay(1000)
            }
        }

    val uiState =
        combine(
            container.settingsRepository.settingsFlow,
            container.activeSessionRepository.activeSession,
            container.activeSessionRepository.sessions,
            container.timerStateRepository.timerStateFlow,
            ticker,
        ) { settings, activeSession, sessions, timerState, nowElapsedRealtime ->
            MainUiState(
                settings = settings,
                activeSession = activeSession,
                sessions = sessions,
                timerState = timerState,
                permissions = readPermissions(container.appContext, container),
                nowElapsedRealtime = nowElapsedRealtime,
                greeting = greetingForHour(),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MainUiState(),
        )

    fun refreshSessions() {
        val sessions = container.mediaPlaybackOrchestrator.refreshActiveSessions()
        log("refreshed ${sessions.size} sessions")
    }

    fun refreshPermissions() {
        val snapshot = readPermissions(container.appContext, container)
        if (!snapshot.listenerAccessGranted) {
            container.activeSessionRepository.clear()
        }
    }

    fun startDelayTimer(
        delayMillis: Long,
        stopAfterMillis: Long?,
    ) {
        val session = container.mediaPlaybackOrchestrator.getBestSession() ?: return
        log(
            "locking session ${session.packageName} for delay=$delayMillis stopAfter=$stopAfterMillis",
        )
        com.nihaltp.aftersleep.service.AfterSleepService.start(
            container.appContext,
            delayMillis,
            stopAfterMillis,
        )
    }

    fun startDebugResumeTest() {
        log("debug test requested")
        startDebugTest(container.appContext)
    }

    fun startDebugTest(context: Context) {
        AfterSleepService.start(
            context = context,
            delayMillis = 10_000L,
            stopAfterMillis = null,
        )
    }

    fun cancelTimer() {
        com.nihaltp.aftersleep.service.AfterSleepService.sendCommand(
            container.appContext,
            com.nihaltp.aftersleep.service.AfterSleepService.ACTION_CANCEL,
        )
    }

    fun pauseNow() {
        com.nihaltp.aftersleep.service.AfterSleepService.sendCommand(
            container.appContext,
            com.nihaltp.aftersleep.service.AfterSleepService.ACTION_PAUSE_NOW,
        )
    }

    fun playNow() {
        com.nihaltp.aftersleep.service.AfterSleepService.sendCommand(
            container.appContext,
            com.nihaltp.aftersleep.service.AfterSleepService.ACTION_PLAY_NOW,
        )
    }

    fun requestNotificationSettingsIntent(): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, container.appContext.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun requestListenerSettingsIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun requestBatteryOptimizationIntent(): Intent =
        container.batteryOptimizationHelper.createIgnoreOptimizationsIntent()

    fun requestNotificationPermission(): String = Manifest.permission.POST_NOTIFICATIONS

    fun requestSessionRefresh() {
        refreshSessions()
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(container) as T
                }
            }

        fun readPermissions(
            context: Context,
            container: AppContainer,
        ): PermissionStateSnapshot {
            val notificationsGranted =
                NotificationManagerCompat.from(
                    context,
                ).areNotificationsEnabled()
            val listenerGranted = context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)
            val batteryIgnored = container.batteryOptimizationHelper.isIgnoringOptimizations()
            return PermissionStateSnapshot(
                notificationPermissionGranted = notificationsGranted,
                listenerAccessGranted = listenerGranted,
                batteryOptimizationIgnored = batteryIgnored,
            )
        }

        private fun greetingForHour(): String {
            val hour = java.time.LocalTime.now().hour
            return when (hour) {
                in 5..11 -> "Good morning"
                in 12..17 -> "Good afternoon"
                else -> "Good evening"
            }
        }

        private fun log(message: String) {
            if (BuildConfig.DEBUG) {
                Log.d("AfterSleepVM", message)
            }
        }
    }
}
