package com.nihaltp.aftersleep.data.model

data class UserSettings(
    val defaultDelayMillis: Long = 10 * 60_000L,
    val defaultStopAfterMillis: Long? = null,
    val fadeInVolumeEnabled: Boolean = true,
    val fadeOutVolumeEnabled: Boolean = false,
    val keepScreenDimEnabled: Boolean = true,
    val autoOpenLastUsedMediaApp: Boolean = false,
    val monochromeMode: Boolean = false,
)
