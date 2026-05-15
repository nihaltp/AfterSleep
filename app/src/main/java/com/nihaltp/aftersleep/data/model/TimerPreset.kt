package com.nihaltp.aftersleep.data.model

data class TimerPreset(
    val label: String,
    val minutes: Int,
) {
    val millis: Long = minutes * 60_000L
}
