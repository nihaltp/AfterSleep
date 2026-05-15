package com.nihaltp.aftersleep.ui.components

import java.util.Locale

fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

fun formatMinutesLabel(millis: Long?): String =
    when (millis) {
        null -> "Off"
        else -> {
            val minutes = millis / 60_000L
            when {
                minutes >= 60 && minutes % 60 == 0L -> "${minutes / 60} h"
                minutes >= 60 -> "${minutes / 60} h ${minutes % 60} m"
                else -> "$minutes m"
            }
        }
    }
