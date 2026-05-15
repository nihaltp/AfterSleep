package com.nihaltp.aftersleep.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattingTest {
    @Test
    fun `formatDuration shows minutes and seconds`() {
        assertEquals("00:10", formatDuration(10_000L))
        assertEquals("01:05", formatDuration(65_000L))
    }

    @Test
    fun `formatMinutesLabel handles off and hours`() {
        assertEquals("Off", formatMinutesLabel(null))
        assertEquals("15 m", formatMinutesLabel(15 * 60_000L))
        assertEquals("1 h", formatMinutesLabel(60 * 60_000L))
    }
}
