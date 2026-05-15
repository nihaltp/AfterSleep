package com.nihaltp.aftersleep.data

import android.content.Context
import com.nihaltp.aftersleep.data.model.MediaSessionSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ActiveSessionRepository(private val context: Context) {
    private val _sessions = MutableStateFlow<List<MediaSessionSnapshot>>(emptyList())
    private val _activeSession = MutableStateFlow<MediaSessionSnapshot?>(null)

    val sessions: StateFlow<List<MediaSessionSnapshot>> = _sessions.asStateFlow()
    val activeSession: StateFlow<MediaSessionSnapshot?> = _activeSession.asStateFlow()

    fun update(sessions: List<MediaSessionSnapshot>) {
        val ordered =
            sessions.sortedWith(
                compareByDescending<MediaSessionSnapshot> { it.isPlaying }
                    .thenByDescending { it.lastUpdatedElapsedRealtime },
            )
        _sessions.value = ordered
        _activeSession.value = ordered.firstOrNull()
    }

    fun clear() {
        _sessions.value = emptyList()
        _activeSession.value = null
    }
}
