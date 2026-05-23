package com.nihaltp.aftersleep.media

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import com.nihaltp.aftersleep.data.model.MediaSessionSnapshot
import com.nihaltp.aftersleep.service.ActiveMediaNotificationListenerService

class ActiveMediaSessionReader(private val context: Context) {
    private val sessionManager: MediaSessionManager? =
        context.getSystemService(
            MediaSessionManager::class.java,
        )

    fun readActiveControllers(): List<MediaController> {
        val listenerComponent =
            ComponentName(context, ActiveMediaNotificationListenerService::class.java)
        return runCatching {
            sessionManager?.getActiveSessions(listenerComponent).orEmpty()
        }.getOrDefault(emptyList())
    }

    fun readActiveSessions(): List<MediaSessionSnapshot> =
        readActiveControllers().mapNotNull { controller ->
            controller.toSnapshot(context.packageManager)
        }

    fun getBestSession(): MediaSessionSnapshot? {
        val snapshots = readActiveSessions()
        return snapshots.firstOrNull { it.isPlaying } ?: snapshots.firstOrNull()
    }

    fun getBestController(): MediaController? {
        val bestSession = getBestSession() ?: return null
        return readActiveControllers().firstOrNull { it.packageName == bestSession.packageName }
    }

    fun getController(packageName: String?): MediaController? {
        if (packageName.isNullOrBlank()) return null
        return readActiveControllers().firstOrNull { it.packageName == packageName }
    }

    private fun MediaController.toSnapshot(packageManager: PackageManager): MediaSessionSnapshot? {
        val state = playbackState?.state ?: PlaybackState.STATE_NONE
        val label =
            runCatching {
                val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
                packageManager.getApplicationLabel(applicationInfo).toString()
            }.getOrNull() ?: packageName

        val metadata = metadata
        val title = metadata?.getText(MediaMetadata.METADATA_KEY_TITLE)?.toString()
        val artist = metadata?.getText(MediaMetadata.METADATA_KEY_ARTIST)?.toString()

        return MediaSessionSnapshot(
            packageName = packageName,
            appLabel = label,
            title = title,
            artist = artist,
            playbackState = state,
            isActive =
                state == PlaybackState.STATE_PLAYING ||
                    state == PlaybackState.STATE_BUFFERING ||
                    state == PlaybackState.STATE_PAUSED,
            lastUpdatedElapsedRealtime = SystemClock.elapsedRealtime(),
        )
    }
}
