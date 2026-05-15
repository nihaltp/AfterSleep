package com.nihaltp.aftersleep.service

import android.os.SystemClock
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class TimerRunner {
    suspend fun runCountdown(
        endsAtElapsedRealtime: Long,
        onTick: suspend (remainingMillis: Long) -> Unit,
        shouldContinue: suspend () -> Boolean = { true },
    ): Boolean {
        while (currentCoroutineContext().isActive && SystemClock.elapsedRealtime() < endsAtElapsedRealtime) {
            if (!shouldContinue()) return false
            val remaining = endsAtElapsedRealtime - SystemClock.elapsedRealtime()
            if (remaining > 0) {
                onTick(remaining)
                delay(1000)
            }
        }
        return currentCoroutineContext().isActive
    }
}
