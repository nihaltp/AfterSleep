package com.nihaltp.aftersleep.service

import android.content.Context
import android.os.PowerManager

class WakeLockHelper(private val context: Context) {
    suspend fun <T> withWakeLock(
        timeoutMillis: Long = 20_000L,
        block: suspend () -> T,
    ): T {
        val powerManager = context.getSystemService(PowerManager::class.java) ?: return block()
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AfterSleep:media")
        wakeLock.setReferenceCounted(false)
        wakeLock.acquire(timeoutMillis)
        return try {
            block()
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }
}
