package com.nihaltp.aftersleep.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.nihaltp.aftersleep.AfterSleepApplication
import com.nihaltp.aftersleep.BuildConfig
import com.nihaltp.aftersleep.data.model.MediaSessionSnapshot
import com.nihaltp.aftersleep.data.model.PlaybackStage
import com.nihaltp.aftersleep.data.model.TimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AfterSleepService : Service() {
    private val serviceScope = CoroutineScope(Job() + Dispatchers.Default)
    private val container by lazy { (application as AfterSleepApplication).container }
    private var runningJob: Job? = null

    private val volumeFadeController by lazy { VolumeFadeController(container.mediaPlaybackOrchestrator) }
    private val notificationController by lazy { NotificationController(this) }
    private val playbackCoordinator by lazy {
        PlaybackCoordinator(
            container.mediaPlaybackOrchestrator,
            container.reliabilityRepository,
            volumeFadeController,
        )
    }
    private val timerRunner by lazy { TimerRunner() }

    override fun onCreate() {
        super.onCreate()
        container.notificationHelper.createChannel()
        log("service created")
        startForeground(
            NOTIFICATION_ID,
            notificationController.buildNotification("Ready", PlaybackStage.Idle, 0L),
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action ?: ACTION_RESTORE) {
            ACTION_START -> intent?.let { startTimer(it) }
            ACTION_CANCEL -> cancelTimer("Timer cancelled")
            ACTION_PAUSE_NOW -> pauseNow()
            ACTION_PLAY_NOW -> playNow()
            ACTION_OPEN_APP -> openApp(intent)
            ACTION_RESTORE -> restoreTimer()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        runningJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startTimer(intent: Intent) {
        val delayMillis = intent.getLongExtra(EXTRA_DELAY_MILLIS, 10 * 60_000L)
        val stopAfterMillis = intent.getLongExtra(EXTRA_STOP_AFTER_MILLIS, -1L).takeIf { it > 0L }

        runningJob?.cancel()
        runningJob =
            serviceScope.launch {
                val session = playbackCoordinator.getActiveOrBestSession()
                if (session == null) {
                    log("start requested without an active session")
                    updateError("No active media session found")
                    return@launch
                }

                log("start requested for ${session.packageName} delay=$delayMillis stopAfter=$stopAfterMillis")

                val settings = container.settingsRepository.settingsFlow.first()
                container.wakeLockHelper.withWakeLock {
                    val paused = playbackCoordinator.pause(session, settings.fadeOutVolumeEnabled)
                    if (!paused) {
                        log("pause failed for ${session.packageName}")
                        updateError("Could not pause playback", session)
                        return@withWakeLock
                    }
                }

                container.timerStateRepository.saveStartedTimer(session, delayMillis, stopAfterMillis)
                startForeground(
                    NOTIFICATION_ID,
                    notificationController.buildNotification(
                        "Waiting to resume",
                        PlaybackStage.WaitingToStart,
                        delayMillis,
                        session,
                    ),
                )

                val delayEndsAt = SystemClock.elapsedRealtime() + delayMillis
                val completed =
                    timerRunner.runCountdown(
                        endsAtElapsedRealtime = delayEndsAt,
                        onTick = { remaining ->
                            notificationController.notify(
                                NOTIFICATION_ID,
                                "Waiting to resume",
                                PlaybackStage.WaitingToStart,
                                remaining,
                                session,
                            )
                        },
                    )

                if (!completed) return@launch

                val resumed =
                    container.wakeLockHelper.withWakeLock {
                        playbackCoordinator.play(session, settings.fadeInVolumeEnabled)
                    }
                log("play requested for ${session.packageName} success=$resumed")
                if (!resumed || !playbackCoordinator.verifyPlaybackResumed(session)) {
                    handleResumeFailure(session)
                    return@launch
                }

                container.timerStateRepository.markPlaying(stopAfterMillis)

                if (stopAfterMillis == null) {
                    container.timerStateRepository.markStopped("Playback resumed")
                    stopSelfSafely()
                    return@launch
                }

                val stopEndsAt = SystemClock.elapsedRealtime() + stopAfterMillis
                val stopCompleted =
                    timerRunner.runCountdown(
                        endsAtElapsedRealtime = stopEndsAt,
                        onTick = { remaining ->
                            notificationController.notify(
                                NOTIFICATION_ID,
                                "Waiting to stop",
                                PlaybackStage.WaitingToStop,
                                remaining,
                                session,
                            )
                        },
                    )

                if (!stopCompleted) return@launch
                container.wakeLockHelper.withWakeLock {
                    playbackCoordinator.pause(session, settings.fadeOutVolumeEnabled)
                }
                log("stop requested for ${session.packageName}")
                container.timerStateRepository.markStopped("Playback paused after delay")
                stopSelfSafely()
            }
    }

    private fun restoreTimer() {
        serviceScope.launch {
            val state = container.timerStateRepository.timerStateFlow.first()
            if (!state.active) {
                log("restore requested but there is no active state")
                startForeground(
                    NOTIFICATION_ID,
                    notificationController.buildNotification("Ready", PlaybackStage.Idle, 0L),
                )
                stopSelfSafely()
                return@launch
            }

            log("restoring state=${state.stage} package=${state.sessionPackageName}")

            when (state.stage) {
                PlaybackStage.WaitingToStart ->
                    container.wakeLockHelper.withWakeLock { resumeWaitingToStart(state) }
                PlaybackStage.WaitingToStop, PlaybackStage.Playing ->
                    container.wakeLockHelper.withWakeLock { resumeWaitingToStop(state) }
                else -> {
                    startForeground(
                        NOTIFICATION_ID,
                        notificationController.buildNotification("Ready", PlaybackStage.Idle, 0L),
                    )
                    stopSelfSafely()
                }
            }
        }
    }

    private suspend fun resumeWaitingToStart(state: TimerState) {
        val delayRemaining = state.delayRemainingMillis
        val session = playbackCoordinator.getBestSession(state.sessionPackageName)

        if (session == null) {
            log("could not restore waiting-to-start because locked session was missing")
            updateError("No active media session to restore")
            return
        }

        startForeground(
            NOTIFICATION_ID,
            notificationController.buildNotification(
                "Waiting to resume",
                PlaybackStage.WaitingToStart,
                delayRemaining,
                session,
            ),
        )

        val completed =
            timerRunner.runCountdown(
                endsAtElapsedRealtime = state.delayEndsAtElapsedRealtime,
                onTick = { remaining ->
                    notificationController.notify(
                        NOTIFICATION_ID,
                        "Waiting to resume",
                        PlaybackStage.WaitingToStart,
                        remaining,
                        session,
                    )
                },
                shouldContinue = {
                    val currentState = container.timerStateRepository.timerStateFlow.first()
                    currentState.active && currentState.stage == PlaybackStage.WaitingToStart
                },
            )

        if (!completed) return

        val settings = container.settingsRepository.settingsFlow.first()
        val resumed = playbackCoordinator.play(session, settings.fadeInVolumeEnabled)
        if (resumed) {
            if (!playbackCoordinator.verifyPlaybackResumed(session)) {
                handleResumeFailure(session)
                return
            }
            container.timerStateRepository.markPlaying(state.stopAfterMillis)
            if (state.stopAfterMillis == null) {
                container.timerStateRepository.markStopped("Playback resumed")
                stopSelfSafely()
            } else {
                resumeWaitingToStop(
                    state.copy(
                        stage = PlaybackStage.WaitingToStop,
                        stopEndsAtElapsedRealtime = state.stopEndsAtElapsedRealtime,
                    ),
                    session,
                )
            }
        } else {
            updateError("Playback resume failed")
        }
    }

    private suspend fun resumeWaitingToStop(
        state: TimerState,
        session: MediaSessionSnapshot? = null,
    ) {
        val mediaSession = session ?: playbackCoordinator.getBestSession(state.sessionPackageName)

        if (mediaSession == null) {
            log("could not restore waiting-to-stop because locked session was missing")
            updateError("No active media session to stop")
            return
        }

        val stopEndsAt =
            state.stopEndsAtElapsedRealtime
                ?: (SystemClock.elapsedRealtime() + (state.stopAfterMillis ?: 0L))
        startForeground(
            NOTIFICATION_ID,
            notificationController.buildNotification(
                "Waiting to stop",
                PlaybackStage.WaitingToStop,
                stopEndsAt - SystemClock.elapsedRealtime(),
                mediaSession,
            ),
        )

        val completed =
            timerRunner.runCountdown(
                endsAtElapsedRealtime = stopEndsAt,
                onTick = { remaining ->
                    notificationController.notify(
                        NOTIFICATION_ID,
                        "Waiting to stop",
                        PlaybackStage.WaitingToStop,
                        remaining,
                        mediaSession,
                    )
                },
            )

        if (!completed) return

        val settings = container.settingsRepository.settingsFlow.first()
        playbackCoordinator.pause(mediaSession, settings.fadeOutVolumeEnabled)
        container.timerStateRepository.markStopped("Playback paused after delay")
        stopSelfSafely()
    }

    private fun cancelTimer(message: String) {
        runningJob?.cancel()
        serviceScope.launch { container.timerStateRepository.markStopped(message) }
        log("timer cancelled")
        notificationController.notify(NOTIFICATION_ID, message, PlaybackStage.Completed, 0L, null)
        stopSelfSafely()
    }

    private fun pauseNow() {
        serviceScope.launch {
            val session =
                playbackCoordinator.getBestSession(
                    container.timerStateRepository.timerStateFlow.first().sessionPackageName,
                )
            if (session != null) {
                val settings = container.settingsRepository.settingsFlow.first()
                log("pause-now requested for ${session.packageName}")
                playbackCoordinator.pause(session, settings.fadeOutVolumeEnabled)
            }
            notificationController.notify(NOTIFICATION_ID, "Paused now", PlaybackStage.Completed, 0L, session)
        }
    }

    private fun playNow() {
        serviceScope.launch {
            val session =
                playbackCoordinator.getBestSession(
                    container.timerStateRepository.timerStateFlow.first().sessionPackageName,
                )
            if (session != null) {
                log("play-now requested for ${session.packageName}")
                val settings = container.settingsRepository.settingsFlow.first()
                playbackCoordinator.play(session, settings.fadeInVolumeEnabled)
            }
            notificationController.notify(NOTIFICATION_ID, "Playing now", PlaybackStage.Playing, 0L, session)
        }
    }

    private fun openApp(intent: Intent?) {
        serviceScope.launch {
            val packageName =
                intent?.getStringExtra(EXTRA_PACKAGE_NAME)
                    ?: container.timerStateRepository.timerStateFlow.first().sessionPackageName
            if (packageName != null && playbackCoordinator.openApp(packageName)) {
                log("open app requested for $packageName")
            }
        }
    }

    private fun updateError(
        message: String,
        session: MediaSessionSnapshot? = null,
    ) {
        log("error: $message")
        serviceScope.launch { container.timerStateRepository.markError(message) }
        notificationController.notify(NOTIFICATION_ID, message, PlaybackStage.Error, 0L, session)
        stopSelfSafely()
    }

    private suspend fun handleResumeFailure(session: MediaSessionSnapshot) {
        val opened = playbackCoordinator.handleResumeFailure(session)
        log("resume failure for ${session.packageName}, openApp=$opened")
        updateError("Could not resume ${session.appLabel}. Open ${session.appLabel}?", session)
    }

    private fun stopSelfSafely() {
        runningJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun log(message: String) {
        if (BuildConfig.DEBUG) {
            android.util.Log.d(TAG, message)
        }
    }

    companion object {
        private const val TAG = "AfterSleepService"
        const val ACTION_START = "com.nihaltp.aftersleep.action.START"
        const val ACTION_CANCEL = "com.nihaltp.aftersleep.action.CANCEL"
        const val ACTION_PAUSE_NOW = "com.nihaltp.aftersleep.action.PAUSE_NOW"
        const val ACTION_PLAY_NOW = "com.nihaltp.aftersleep.action.PLAY_NOW"
        const val ACTION_RESTORE = "com.nihaltp.aftersleep.action.RESTORE"
        const val ACTION_OPEN_APP = "com.nihaltp.aftersleep.action.OPEN_APP"
        const val EXTRA_DELAY_MILLIS = "extra_delay_millis"
        const val EXTRA_STOP_AFTER_MILLIS = "extra_stop_after_millis"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        private const val NOTIFICATION_ID = 1001

        fun start(
            context: Context,
            delayMillis: Long,
            stopAfterMillis: Long?,
        ) {
            val intent =
                Intent(context, AfterSleepService::class.java).apply {
                    action = ACTION_START
                    putExtra(EXTRA_DELAY_MILLIS, delayMillis)
                    if (stopAfterMillis != null) {
                        putExtra(EXTRA_STOP_AFTER_MILLIS, stopAfterMillis)
                    }
                }
            ContextCompat.startForegroundService(context, intent)
        }

        fun sendCommand(
            context: Context,
            action: String,
        ) {
            val intent =
                Intent(
                    context,
                    AfterSleepService::class.java,
                ).apply { this.action = action }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
