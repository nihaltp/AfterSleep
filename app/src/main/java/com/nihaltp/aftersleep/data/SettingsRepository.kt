package com.nihaltp.aftersleep.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.nihaltp.aftersleep.data.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val context: Context) {
    private object Keys {
        val DEFAULT_DELAY = longPreferencesKey("default_delay_millis")
        val DEFAULT_STOP_AFTER = longPreferencesKey("default_stop_after_millis")
        val DEFAULT_STOP_AFTER_ENABLED = booleanPreferencesKey("default_stop_after_enabled")
        val FADE_IN_VOLUME = booleanPreferencesKey("fade_in_volume")
        val FADE_OUT_VOLUME = booleanPreferencesKey("fade_out_volume")
        val KEEP_SCREEN_DIM = booleanPreferencesKey("keep_screen_dim")
        val AUTO_OPEN_LAST_APP = booleanPreferencesKey("auto_open_last_app")
        val MONOCHROME_MODE = booleanPreferencesKey("monochrome_mode")
    }

    val settingsFlow: Flow<UserSettings> =
        context.settingsDataStore.data.map { preferences ->
            preferences.toUserSettings()
        }

    suspend fun setDefaultDelayMillis(value: Long) {
        context.settingsDataStore.edit { it[Keys.DEFAULT_DELAY] = value.coerceAtLeast(60_000L) }
    }

    suspend fun setDefaultStopAfterMillis(value: Long?) {
        context.settingsDataStore.edit { preferences ->
            if (value == null) {
                preferences.remove(Keys.DEFAULT_STOP_AFTER)
                preferences.remove(Keys.DEFAULT_STOP_AFTER_ENABLED)
            } else {
                preferences[Keys.DEFAULT_STOP_AFTER] = value.coerceAtLeast(60_000L)
                preferences[Keys.DEFAULT_STOP_AFTER_ENABLED] = true
            }
        }
    }

    suspend fun setFadeInVolumeEnabled(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.FADE_IN_VOLUME] = value }
    }

    suspend fun setFadeOutVolumeEnabled(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.FADE_OUT_VOLUME] = value }
    }

    suspend fun setKeepScreenDimEnabled(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.KEEP_SCREEN_DIM] = value }
    }

    suspend fun setAutoOpenLastUsedMediaApp(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.AUTO_OPEN_LAST_APP] = value }
    }

    suspend fun setMonochromeMode(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.MONOCHROME_MODE] = value }
    }

    private fun Preferences.toUserSettings(): UserSettings {
        val defaultDelay = this[Keys.DEFAULT_DELAY] ?: 10 * 60_000L
        val defaultStopAfter =
            if (this[Keys.DEFAULT_STOP_AFTER_ENABLED] == true) {
                this[Keys.DEFAULT_STOP_AFTER]
            } else {
                null
            }

        return UserSettings(
            defaultDelayMillis = defaultDelay,
            defaultStopAfterMillis = defaultStopAfter,
            fadeInVolumeEnabled = this[Keys.FADE_IN_VOLUME] ?: true,
            fadeOutVolumeEnabled = this[Keys.FADE_OUT_VOLUME] ?: false,
            keepScreenDimEnabled = this[Keys.KEEP_SCREEN_DIM] ?: true,
            autoOpenLastUsedMediaApp = this[Keys.AUTO_OPEN_LAST_APP] ?: false,
            monochromeMode = this[Keys.MONOCHROME_MODE] ?: false,
        )
    }
}
