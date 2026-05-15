package com.nihaltp.aftersleep.ui.model

import com.nihaltp.aftersleep.data.model.MediaSessionSnapshot
import com.nihaltp.aftersleep.data.model.TimerState
import com.nihaltp.aftersleep.data.model.UserSettings

data class MainUiState(
    val settings: UserSettings = UserSettings(),
    val activeSession: MediaSessionSnapshot? = null,
    val sessions: List<MediaSessionSnapshot> = emptyList(),
    val timerState: TimerState = TimerState(),
    val permissions: PermissionStateSnapshot = PermissionStateSnapshot(),
    val nowElapsedRealtime: Long = 0L,
    val greeting: String = "Good evening",
)
