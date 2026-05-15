package com.nihaltp.aftersleep.data.model

import android.media.session.PlaybackState

data class MediaSessionSnapshot(
    val packageName: String,
    val appLabel: String,
    val title: String?,
    val artist: String?,
    val playbackState: Int,
    val isActive: Boolean,
    val lastUpdatedElapsedRealtime: Long,
) {
    val playbackStateLabel: String =
        when (playbackState) {
            PlaybackState.STATE_PLAYING -> "Playing"
            PlaybackState.STATE_PAUSED -> "Paused"
            PlaybackState.STATE_BUFFERING -> "Buffering"
            PlaybackState.STATE_FAST_FORWARDING -> "Playing fast"
            PlaybackState.STATE_REWINDING -> "Rewinding"
            PlaybackState.STATE_STOPPED -> "Stopped"
            PlaybackState.STATE_ERROR -> "Unavailable"
            else -> "Idle"
        }

    val displayTitle: String
        get() = title?.takeIf { it.isNotBlank() } ?: "Unknown title"

    val displaySubtitle: String
        get() = artist?.takeIf { it.isNotBlank() } ?: packageName

    val isPlaying: Boolean
        get() = playbackState == PlaybackState.STATE_PLAYING
}
