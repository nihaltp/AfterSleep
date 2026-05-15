package com.nihaltp.aftersleep.service

import com.nihaltp.aftersleep.data.model.MediaSessionSnapshot
import com.nihaltp.aftersleep.media.MediaPlaybackOrchestrator
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.roundToInt

class VolumeFadeController(private val orchestrator: MediaPlaybackOrchestrator) {
    suspend fun fadeInVolume(session: MediaSessionSnapshot?) {
        val controller = orchestrator.getController(session) ?: return
        val playbackInfo = controller.playbackInfo
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

    suspend fun fadeOutVolume(session: MediaSessionSnapshot?) {
        val controller = orchestrator.getController(session) ?: return
        val playbackInfo = controller.playbackInfo
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
}
