package com.nihaltp.aftersleep.service

import com.nihaltp.aftersleep.data.ReliabilityRepository
import com.nihaltp.aftersleep.data.model.MediaSessionSnapshot
import com.nihaltp.aftersleep.media.MediaPlaybackOrchestrator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackCoordinatorTest {
    @Test
    fun `play resumes and triggers fadeIn when requested`() =
        runTest {
            val orchestrator = mockk<MediaPlaybackOrchestrator>()
            val reliabilityRepository = mockk<ReliabilityRepository>(relaxed = true)
            val volumeFadeController = mockk<VolumeFadeController>(relaxed = true)

            val session =
                MediaSessionSnapshot(
                    packageName = "com.example",
                    appLabel = "Example",
                    title = "Song",
                    artist = "Artist",
                    playbackState = 0,
                    isActive = true,
                    lastUpdatedElapsedRealtime = 1L,
                )

            coEvery { orchestrator.play(session) } returns true

            val coordinator = PlaybackCoordinator(orchestrator, reliabilityRepository, volumeFadeController)
            val resumed = coordinator.play(session, fadeIn = true)

            assertTrue(resumed)
            coVerify(exactly = 1) { orchestrator.play(session) }
            coVerify(exactly = 1) { volumeFadeController.fadeInVolume(session) }
        }
}
