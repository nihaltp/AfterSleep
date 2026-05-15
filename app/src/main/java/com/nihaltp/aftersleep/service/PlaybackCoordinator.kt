package com.nihaltp.aftersleep.service

import com.nihaltp.aftersleep.data.ReliabilityRepository
import com.nihaltp.aftersleep.data.model.MediaSessionSnapshot
import com.nihaltp.aftersleep.media.MediaPlaybackOrchestrator
import kotlinx.coroutines.delay

class PlaybackCoordinator(
    private val orchestrator: MediaPlaybackOrchestrator,
    private val reliabilityRepository: ReliabilityRepository,
    private val volumeFadeController: VolumeFadeController,
) {
    suspend fun pause(
        session: MediaSessionSnapshot,
        fadeOut: Boolean,
    ): Boolean {
        if (fadeOut) {
            volumeFadeController.fadeOutVolume(session)
        }
        return orchestrator.pause(session)
    }

    suspend fun play(
        session: MediaSessionSnapshot,
        fadeIn: Boolean,
    ): Boolean {
        val resumed = orchestrator.play(session)
        if (resumed && fadeIn) {
            volumeFadeController.fadeInVolume(session)
        }
        return resumed
    }

    suspend fun verifyPlaybackResumed(session: MediaSessionSnapshot): Boolean {
        delay(3_500)
        val controller = orchestrator.getController(session) ?: return false
        val playbackState = controller.playbackState?.state
        val resumed =
            playbackState == android.media.session.PlaybackState.STATE_PLAYING ||
                playbackState == android.media.session.PlaybackState.STATE_BUFFERING
        if (resumed) {
            reliabilityRepository.recordSuccess(session.packageName)
        }
        return resumed
    }

    suspend fun handleResumeFailure(session: MediaSessionSnapshot): Boolean {
        reliabilityRepository.recordFailure(session.packageName)
        return orchestrator.maybeOpenLastUsedApp(session)
    }

    fun openApp(packageName: String): Boolean {
        return orchestrator.openPackage(packageName)
    }

    suspend fun getBestSession(lockedPackage: String? = null): MediaSessionSnapshot? {
        if (!lockedPackage.isNullOrBlank()) {
            val session = orchestrator.refreshActiveSessions().firstOrNull { it.packageName == lockedPackage }
            if (session != null) return session
        }
        return orchestrator.refreshActiveSessions().firstOrNull { it.isPlaying }
            ?: orchestrator.getBestSession()
    }

    suspend fun getActiveOrBestSession(): MediaSessionSnapshot? {
        return orchestrator.refreshActiveSessions().firstOrNull { it.isPlaying }
            ?: orchestrator.getBestSession()
    }
}
