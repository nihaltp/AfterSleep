package com.nihaltp.aftersleep.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserSettingsTest {
    @Test
    fun `default values are user friendly`() {
        val settings = UserSettings()

        assertEquals(10 * 60_000L, settings.defaultDelayMillis)
        assertNull(settings.defaultStopAfterMillis)
        assertTrue(settings.fadeInVolumeEnabled)
        assertFalse(settings.fadeOutVolumeEnabled)
        assertTrue(settings.keepScreenDimEnabled)
        assertFalse(settings.autoOpenLastUsedMediaApp)
        assertFalse(settings.monochromeMode)
    }
}
