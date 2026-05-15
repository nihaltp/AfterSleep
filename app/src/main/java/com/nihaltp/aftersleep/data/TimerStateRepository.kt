package com.nihaltp.aftersleep.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nihaltp.aftersleep.data.model.MediaSessionSnapshot
import com.nihaltp.aftersleep.data.model.PlaybackStage
import com.nihaltp.aftersleep.data.model.TimerState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TimerStateRepository(private val context: Context) {
    private object Keys {
        val ACTIVE = booleanPreferencesKey("active")
        val STAGE = stringPreferencesKey("stage")
        val DELAY_MILLIS = longPreferencesKey("delay_millis")
        val STOP_AFTER_MILLIS = longPreferencesKey("stop_after_millis")
        val DELAY_ENDS = longPreferencesKey("delay_ends_at")
        val STOP_ENDS = longPreferencesKey("stop_ends_at")
        val SESSION_PACKAGE = stringPreferencesKey("session_package")
        val SESSION_APP_LABEL = stringPreferencesKey("session_app_label")
        val SESSION_TITLE = stringPreferencesKey("session_title")
        val SESSION_ARTIST = stringPreferencesKey("session_artist")
        val LAST_MESSAGE = stringPreferencesKey("last_message")
    }

    val timerStateFlow: Flow<TimerState> =
        context.timerDataStore.data.map { preferences ->
            preferences.toTimerState()
        }

    suspend fun saveStartedTimer(
        session: MediaSessionSnapshot,
        delayMillis: Long,
        stopAfterMillis: Long?,
    ) {
        val now = android.os.SystemClock.elapsedRealtime()
        context.timerDataStore.edit { preferences,
            ->
            preferences[Keys.ACTIVE] = true
            preferences[Keys.STAGE] = PlaybackStage.WaitingToStart.name
            preferences[Keys.DELAY_MILLIS] = delayMillis
            if (stopAfterMillis != null) {
                preferences[Keys.STOP_AFTER_MILLIS] = stopAfterMillis
            } else {
                preferences.remove(Keys.STOP_AFTER_MILLIS)
            }
            preferences[Keys.DELAY_ENDS] = now + delayMillis
            preferences.remove(Keys.STOP_ENDS)
            preferences[Keys.SESSION_PACKAGE] = session.packageName
            preferences[Keys.SESSION_APP_LABEL] = session.appLabel
            session.title?.let {
                preferences[Keys.SESSION_TITLE] = it
            } ?: preferences.remove(Keys.SESSION_TITLE)
            session.artist?.let {
                preferences[Keys.SESSION_ARTIST] = it
            } ?: preferences.remove(Keys.SESSION_ARTIST)
            preferences[Keys.LAST_MESSAGE] = "Waiting to resume"
        }
    }

    suspend fun markPlaying(stopAfterMillis: Long?) {
        val now = android.os.SystemClock.elapsedRealtime()
        context.timerDataStore.edit { preferences ->
            preferences[Keys.ACTIVE] = true
            preferences[Keys.STAGE] =
                if (stopAfterMillis != null) {
                    PlaybackStage.WaitingToStop.name
                } else {
                    PlaybackStage.Playing.name
                }
            if (stopAfterMillis != null) {
                preferences[Keys.STOP_AFTER_MILLIS] = stopAfterMillis
                preferences[Keys.STOP_ENDS] = now + stopAfterMillis
                preferences[Keys.LAST_MESSAGE] = "Playback resumed"
            } else {
                preferences.remove(Keys.STOP_ENDS)
                preferences[Keys.LAST_MESSAGE] = "Playback resumed"
            }
        }
    }

    suspend fun markStopped(message: String = "Playback paused") {
        context.timerDataStore.edit { preferences ->
            preferences[Keys.ACTIVE] = false
            preferences[Keys.STAGE] = PlaybackStage.Completed.name
            preferences[Keys.LAST_MESSAGE] = message
            preferences.remove(Keys.DELAY_ENDS)
            preferences.remove(Keys.STOP_ENDS)
        }
    }

    suspend fun markError(message: String) {
        context.timerDataStore.edit { preferences ->
            preferences[Keys.ACTIVE] = false
            preferences[Keys.STAGE] = PlaybackStage.Error.name
            preferences[Keys.LAST_MESSAGE] = message
        }
    }

    suspend fun clear() {
        context.timerDataStore.edit { it.clear() }
    }

    private fun Preferences.toTimerState(): TimerState {
        val active = this[Keys.ACTIVE] ?: false
        val stage =
            runCatching {
                this[Keys.STAGE]?.let { PlaybackStage.valueOf(it) }
            }.getOrNull() ?: PlaybackStage.Idle

        return TimerState(
            active = active,
            stage = stage,
            delayMillis = this[Keys.DELAY_MILLIS] ?: 0L,
            stopAfterMillis = this[Keys.STOP_AFTER_MILLIS],
            delayEndsAtElapsedRealtime = this[Keys.DELAY_ENDS] ?: 0L,
            stopEndsAtElapsedRealtime = this[Keys.STOP_ENDS],
            sessionPackageName = this[Keys.SESSION_PACKAGE],
            sessionAppLabel = this[Keys.SESSION_APP_LABEL],
            sessionTitle = this[Keys.SESSION_TITLE],
            sessionArtist = this[Keys.SESSION_ARTIST],
            lastMessage = this[Keys.LAST_MESSAGE],
        )
    }
}
