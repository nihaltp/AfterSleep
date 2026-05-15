package com.nihaltp.aftersleep.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.nihaltp.aftersleep.AfterSleepApplication
import com.nihaltp.aftersleep.BuildConfig
import com.nihaltp.aftersleep.MainActivity
import com.nihaltp.aftersleep.R
import com.nihaltp.aftersleep.data.NotificationChannelHelper
import com.nihaltp.aftersleep.data.model.MediaSessionSnapshot
import com.nihaltp.aftersleep.data.model.PlaybackStage
import com.nihaltp.aftersleep.data.model.TimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

class AfterSleepService : Service() {
    private val serviceScope = CoroutineScope(Job() + Dispatchers.Default)
    private val container by lazy { (application as AfterSleepApplication).container }
    private var runningJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        container.notificationHelper.createChannel()
        log("service created")
        startForeground(NOTIFICATION_ID, buildNotification("Ready", PlaybackStage.Idle, 0L, null))
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
        val session =
            container.mediaPlaybackOrchestrator.refreshActiveSessions().let { sessions ->
                sessions.firstOrNull { it.isPlaying } ?: sessions.firstOrNull()
            }

        if (session == null) {
            log("start requested without an active session")
            updateError("No active media session found")
            return
        }

        log(
            "start requested for ${session.packageName} delay=$delayMillis stopAfter=$stopAfterMillis",
        )

        runningJob?.cancel()
        runningJob =
            serviceScope.launch {
                val settings = container.settingsRepository.settingsFlow.first()
                container.wakeLockHelper.withWakeLock {
                    if (settings.fadeOutVolumeEnabled) {
                        fadeOutVolume(session)
                    }

                    val paused = container.mediaPlaybackOrchestrator.pause(session)
                    if (!paused) {
                        log("pause failed for ${session.packageName}")
                        updateError("Could not pause playback", session)
                        return@withWakeLock
                    }
                }

                container.timerStateRepository.saveStartedTimer(session, delayMillis, stopAfterMillis)
                startForeground(NOTIFICATION_ID, buildNotification("Waiting to resume", PlaybackStage.WaitingToStart, delayMillis, stopAfterMillis, session))

                var currentSession = session
                val delayEndsAt = SystemClock.elapsedRealtime() + delayMillis
                while (currentCoroutineContext().isActive && SystemClock.elapsedRealtime() < delayEndsAt) {
                    updateNotification("Waiting to resume", PlaybackStage.WaitingToStart, delayEndsAt - SystemClock.elapsedRealtime(), stopAfterMillis, currentSession)
                    delay(1000)
                }

                if (!currentCoroutineContext().isActive) return@launch

                val resumed =
                    container.wakeLockHelper.withWakeLock {
                        container.mediaPlaybackOrchestrator.play(currentSession)
                    }
                log("play requested for ${currentSession.packageName} success=$resumed")
                if (!resumed) {
                    handleResumeFailure(currentSession, settings)
                    return@launch
                }

                if (!verifyPlaybackResumed(currentSession)) {
                    handleResumeFailure(currentSession, settings)
                    return@launch
                }

                container.timerStateRepository.markPlaying(stopAfterMillis)
                if (settings.fadeInVolumeEnabled) {
                    container.wakeLockHelper.withWakeLock {
                        fadeInVolume(currentSession)
                    }
                }

                if (stopAfterMillis == null) {
                    container.timerStateRepository.markStopped("Playback resumed")
                    stopSelfSafely()
                    return@launch
                }

                val stopEndsAt = SystemClock.elapsedRealtime() + stopAfterMillis
                while (currentCoroutineContext().isActive && SystemClock.elapsedRealtime() < stopEndsAt) {
                    updateNotification("Waiting to stop", PlaybackStage.WaitingToStop, stopEndsAt - SystemClock.elapsedRealtime(), stopAfterMillis, currentSession)
                    delay(1000)
                }

                if (!currentCoroutineContext().isActive) return@launch
                container.wakeLockHelper.withWakeLock {
                    container.mediaPlaybackOrchestrator.pause(currentSession)
                }
                log("stop requested for ${currentSession.packageName}")
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
                    buildNotification("Ready", PlaybackStage.Idle, 0L, null),
                )
                stopSelfSafely()
                return@launch
            }

            log("restoring state=${state.stage} package=${state.sessionPackageName}")

            when (state.stage) {
                PlaybackStage.WaitingToStart ->
                    container.wakeLockHelper.withWakeLock {
                        resumeWaitingToStart(state)
                    }
                PlaybackStage.WaitingToStop, PlaybackStage.Playing ->
                    container.wakeLockHelper.withWakeLock {
                        resumeWaitingToStop(state)
                    }
                else -> {
                    startForeground(
                        NOTIFICATION_ID,
                        buildNotification("Ready", PlaybackStage.Idle, 0L, null),
                    )
                    stopSelfSafely()
                }
            }
        }
    }

    private suspend fun resumeWaitingToStart(state: TimerState) {
        val delayRemaining = state.delayRemainingMillis
        val session =
            container.mediaPlaybackOrchestrator.refreshActiveSessions().firstOrNull {
                it.packageName == state.sessionPackageName
            }
                ?: container.mediaPlaybackOrchestrator.getBestSession()

        if (session == null) {
            log("could not restore waiting-to-start because locked session was missing")
            updateError("No active media session to restore")
            return
        }

        startForeground(
            NOTIFICATION_ID,
            buildNotification(
                "Waiting to resume",
                PlaybackStage.WaitingToStart,
                delayRemaining,
                state.stopAfterMillis,
                session,
            ),
        )
        while (currentCoroutineContext().isActive && container.timerStateRepository.timerStateFlow.first().active && container.timerStateRepository.timerStateFlow.first().stage == PlaybackStage.WaitingToStart && (state.delayEndsAtElapsedRealtime - SystemClock.elapsedRealtime()) > 0) {
            updateNotification(
                "Waiting to resume",
                PlaybackStage.WaitingToStart,
                state.delayRemainingMillis,
                state.stopAfterMillis,
                session,
            )
            delay(1000)
        }

        if (!currentCoroutineContext().isActive) return

        if (container.mediaPlaybackOrchestrator.play(session)) {
            if (!verifyPlaybackResumed(session)) {
                handleResumeFailure(session, null)
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
        val mediaSession =
            session ?: container.mediaPlaybackOrchestrator.refreshActiveSessions().firstOrNull {
                it.packageName == state.sessionPackageName
            }
                ?: container.mediaPlaybackOrchestrator.getBestSession()

        if (mediaSession == null) {
            log("could not restore waiting-to-stop because locked session was missing")
            updateError("No active media session to stop")
            return
        }

        val stopEndsAt = state.stopEndsAtElapsedRealtime ?: (SystemClock.elapsedRealtime() + (state.stopAfterMillis ?: 0L))
        startForeground(
            NOTIFICATION_ID,
            buildNotification(
                "Waiting to stop",
                PlaybackStage.WaitingToStop,
                stopEndsAt - SystemClock.elapsedRealtime(),
                state.stopAfterMillis,
                mediaSession,
            ),
        )
        while (currentCoroutineContext().isActive && SystemClock.elapsedRealtime() < stopEndsAt) {
            updateNotification(
                "Waiting to stop",
                PlaybackStage.WaitingToStop,
                stopEndsAt - SystemClock.elapsedRealtime(),
                state.stopAfterMillis,
                mediaSession,
            )
            delay(1000)
        }

        if (!currentCoroutineContext().isActive) return
        container.mediaPlaybackOrchestrator.pause(mediaSession)
        container.timerStateRepository.markStopped("Playback paused after delay")
        stopSelfSafely()
    }

    private fun cancelTimer(message: String) {
        runningJob?.cancel()
        serviceScope.launch {
            container.timerStateRepository.markStopped(message)
        }
        log("timer cancelled")
        updateNotification(message, PlaybackStage.Completed, 0L, null, null)
        stopSelfSafely()
    }

    private fun pauseNow() {
        serviceScope.launch {
            val session =
                lockedSession()
                    ?: container.mediaPlaybackOrchestrator.refreshActiveSessions().firstOrNull { it.isPlaying }
                    ?: container.mediaPlaybackOrchestrator.getBestSession()
            if (session != null) {
                val settings = container.settingsRepository.settingsFlow.first()
                if (settings.fadeOutVolumeEnabled) {
                    fadeOutVolume(session)
                }
                log("pause-now requested for ${session.packageName}")
                container.mediaPlaybackOrchestrator.pause(session)
            }
            updateNotification("Paused now", PlaybackStage.Completed, 0L, null, session)
        }
    }

    private fun playNow() {
        serviceScope.launch {
            val session =
                lockedSession()
                    ?: container.mediaPlaybackOrchestrator.refreshActiveSessions().firstOrNull()
            if (session != null) {
                log("play-now requested for ${session.packageName}")
                container.mediaPlaybackOrchestrator.play(session)
            }
            updateNotification("Playing now", PlaybackStage.Playing, 0L, null, session)
        }
    }

    private fun openApp(intent: Intent?) {
        serviceScope.launch {
            val packageName =
                intent?.getStringExtra(EXTRA_PACKAGE_NAME)
                    ?: container.timerStateRepository.timerStateFlow.first().sessionPackageName
            if (container.mediaPlaybackOrchestrator.openPackage(packageName)) {
                log("open app requested for $packageName")
            }
        }
    }

    private fun updateError(
        message: String,
        session: MediaSessionSnapshot? = null,
    ) {
        log("error: $message")
        serviceScope.launch {
            container.timerStateRepository.markError(message)
        }
        updateNotification(message, PlaybackStage.Error, 0L, null, session)
        stopSelfSafely()
    }

    private fun updateNotification(
        title: String,
        stage: PlaybackStage,
        remainingMillis: Long,
        stopAfterMillis: Long?,
        session: MediaSessionSnapshot?,
    ) {
        val notification =
            buildNotification(title, stage, remainingMillis, stopAfterMillis, session)
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(
        title: String,
        stage: PlaybackStage,
        remainingMillis: Long,
        stopAfterMillis: Long?,
        session: MediaSessionSnapshot? = null,
    ): Notification {
        val contentIntent =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                },
                pendingIntentFlag(),
            )

        val cancelIntent = serviceIntent(ACTION_CANCEL)
        val pauseIntent = serviceIntent(ACTION_PAUSE_NOW)
        val playIntent = serviceIntent(ACTION_PLAY_NOW)
        val openAppIntent = session?.packageName?.let { serviceIntent(ACTION_OPEN_APP, it) }

        val contentText =
            when (stage) {
                PlaybackStage.WaitingToStart -> "${session?.appLabel ?: getString(R.string.app_name)} resumes in ${formatDuration(
                    remainingMillis,
                )}"
                PlaybackStage.WaitingToStop -> "${session?.appLabel ?: getString(R.string.app_name)} stops in ${formatDuration(
                    remainingMillis,
                )}"
                PlaybackStage.Playing -> "${session?.appLabel ?: getString(R.string.app_name)} is playing"
                PlaybackStage.Error -> title
                else -> title
            }

        val builder =
            NotificationCompat.Builder(
                this,
                if (stage == PlaybackStage.Error) NotificationChannelHelper.FAILURE_CHANNEL_ID else NotificationChannelHelper.SERVICE_CHANNEL_ID,
            )
                .setSmallIcon(android.R.drawable.ic_media_pause)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(contentText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                .setContentIntent(contentIntent)
                .setOngoing(
                    stage == PlaybackStage.WaitingToStart || stage == PlaybackStage.WaitingToStop,
                )
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
            Intent(this, AfterSleepService::class.java).apply {
                this.action = action
                if (packageName != null) {
                    putExtra(EXTRA_PACKAGE_NAME, packageName)
                }
            }
        return PendingIntent.getService(this, action.hashCode(), intent, pendingIntentFlag())
    }

    private fun pendingIntentFlag(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

    private fun stopSelfSafely() {
        runningJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun formatDuration(millis: Long): String {
        val totalSeconds = (millis / 1000).coerceAtLeast(0L)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private suspend fun fadeInVolume(session: MediaSessionSnapshot?) {
        val controller = container.mediaPlaybackOrchestrator.getController(session) ?: return
        val playbackInfo = controller.playbackInfo ?: return
        if (playbackInfo.volumeControl != android.media.VolumeProvider.VOLUME_CONTROL_ABSOLUTE) return

        val targetVolume = playbackInfo.currentVolume.coerceAtLeast(1)
        val startVolume = max(1, (targetVolume * 0.25f).roundToInt())
        controller.setVolumeTo(startVolume, 0)
        val stepCount = 6
        val stepDelay = 120L
        repeat(stepCount) { step ->
            val nextVolume = startVolume + ((targetVolume - startVolume) * (step + 1) / stepCount)
            controller.setVolumeTo(nextVolume.coerceAtMost(targetVolume), 0)
            delay(stepDelay)
        }
    }

    private suspend fun fadeOutVolume(session: MediaSessionSnapshot?) {
        val controller = container.mediaPlaybackOrchestrator.getController(session) ?: return
        val playbackInfo = controller.playbackInfo ?: return
        if (playbackInfo.volumeControl != android.media.VolumeProvider.VOLUME_CONTROL_ABSOLUTE) return

        val targetVolume = playbackInfo.currentVolume.coerceAtLeast(1)
        val endVolume = max(1, (targetVolume * 0.25f).roundToInt())
        val stepCount = 6
        val stepDelay = 100L
        repeat(stepCount) { step ->
            val nextVolume = targetVolume - ((targetVolume - endVolume) * (step + 1) / stepCount)
            controller.setVolumeTo(nextVolume.coerceAtLeast(endVolume), 0)
            delay(stepDelay)
        }
    }

    private suspend fun lockedSession(): MediaSessionSnapshot? {
        val lockedPackage = container.timerStateRepository.timerStateFlow.first().sessionPackageName
        if (lockedPackage.isNullOrBlank()) return null
        return container.mediaPlaybackOrchestrator.refreshActiveSessions().firstOrNull {
            it.packageName == lockedPackage
        }
    }

    private suspend fun verifyPlaybackResumed(session: MediaSessionSnapshot): Boolean {
        delay(3_500)
        val controller = container.mediaPlaybackOrchestrator.getController(session) ?: return false
        val playbackState = controller.playbackState?.state
        val resumed = playbackState == android.media.session.PlaybackState.STATE_PLAYING || playbackState == android.media.session.PlaybackState.STATE_BUFFERING
        log("verification for ${session.packageName}: state=$playbackState resumed=$resumed")
        if (resumed) {
            container.reliabilityRepository.recordSuccess(session.packageName)
        }
        return resumed
    }

    private suspend fun handleResumeFailure(
        session: MediaSessionSnapshot,
        settings: com.nihaltp.aftersleep.data.model.UserSettings?,
    ) {
        container.reliabilityRepository.recordFailure(session.packageName)
        val opened = container.mediaPlaybackOrchestrator.maybeOpenLastUsedApp(session)
        log("resume failure for ${session.packageName}, openApp=$opened")
        updateError("Could not resume ${session.appLabel}. Open ${session.appLabel}?", session)
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
