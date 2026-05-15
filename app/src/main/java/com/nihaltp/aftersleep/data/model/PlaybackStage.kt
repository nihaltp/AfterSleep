package com.nihaltp.aftersleep.data.model

enum class PlaybackStage {
    Idle,
    WaitingToStart,
    Playing,
    WaitingToStop,
    Completed,
    Error,
}
