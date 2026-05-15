package com.nihaltp.aftersleep.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nihaltp.aftersleep.data.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    val settings =
        container.settingsRepository.settingsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = com.nihaltp.aftersleep.data.model.UserSettings(),
        )

    fun updateDefaultDelay(millis: Long) {
        viewModelScope.launch { container.settingsRepository.setDefaultDelayMillis(millis) }
    }

    fun updateDefaultStopAfter(millis: Long?) {
        viewModelScope.launch { container.settingsRepository.setDefaultStopAfterMillis(millis) }
    }

    fun updateFadeInVolume(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setFadeInVolumeEnabled(enabled) }
    }

    fun updateFadeOutVolume(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setFadeOutVolumeEnabled(enabled) }
    }

    fun updateKeepScreenDim(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setKeepScreenDimEnabled(enabled) }
    }

    fun updateAutoOpenLastUsedApp(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setAutoOpenLastUsedMediaApp(enabled) }
    }

    fun updateMonochromeMode(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setMonochromeMode(enabled) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(container) as T
            }
    }
}
