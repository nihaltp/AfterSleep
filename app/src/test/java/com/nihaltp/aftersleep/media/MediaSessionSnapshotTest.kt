package com.nihaltp.aftersleep.media

import android.media.session.PlaybackState
import com.nihaltp.aftersleep.data.model.MediaSessionSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaSessionSnapshotTest {
    @Test
    fun `display helpers and playback labels are derived correctly`() {
        val playing =
            MediaSessionSnapshot(
                packageName = "com.example.music",
                appLabel = "Example Music",
                title = "",
                artist = null,
                playbackState = PlaybackState.STATE_PLAYING,
                isActive = true,
                lastUpdatedElapsedRealtime = 123L,
            )

        assertEquals("Playing", playing.playbackStateLabel)
        assertEquals("Unknown title", playing.displayTitle)
        assertEquals("com.example.music", playing.displaySubtitle)
        assertTrue(playing.isPlaying)

        val paused = playing.copy(playbackState = PlaybackState.STATE_PAUSED)
        assertEquals("Paused", paused.playbackStateLabel)
        assertFalse(paused.isPlaying)
    }
}
