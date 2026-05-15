package com.nihaltp.aftersleep.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerPresetTest {
    @Test
    fun `preset converts minutes to millis`() {
        val preset = TimerPreset("10 min", 10)

        assertEquals(600_000L, preset.millis)
    }
}
