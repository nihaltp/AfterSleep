package com.nihaltp.aftersleep.data.model

data class TimerState(
    val active: Boolean = false,
    val stage: PlaybackStage = PlaybackStage.Idle,
    val delayMillis: Long = 0L,
    val stopAfterMillis: Long? = null,
    val delayEndsAtElapsedRealtime: Long = 0L,
    val stopEndsAtElapsedRealtime: Long? = null,
    val sessionPackageName: String? = null,
    val sessionAppLabel: String? = null,
    val sessionTitle: String? = null,
    val sessionArtist: String? = null,
    val lastMessage: String? = null,
) {
    val delayRemainingMillis: Long
        get() =
            (delayEndsAtElapsedRealtime - android.os.SystemClock.elapsedRealtime()).coerceAtLeast(
                0L,
            )

    val stopRemainingMillis: Long?
        get() =
            stopEndsAtElapsedRealtime?.let {
                (it - android.os.SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            }
}
