package com.ruuvi.station.settings.ui

import androidx.lifecycle.ViewModel
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.network.domain.RuuviNetworkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DeveloperSettingsViewModel(
    val preferencesRepository: PreferencesRepository,
    val ruuviNetworkRepository: RuuviNetworkRepository
): ViewModel() {

    private var _devServerEnabled = MutableStateFlow(preferencesRepository.isDevServerEnabled())
    val devServerEnabled: StateFlow<Boolean> = _devServerEnabled

    fun setDevServerEnabled(isEnabled: Boolean) {
        preferencesRepository.setDevServerEnabled(isEnabled)
        _devServerEnabled.value = preferencesRepository.isDevServerEnabled()
        ruuviNetworkRepository.reinitialize()
    }
}