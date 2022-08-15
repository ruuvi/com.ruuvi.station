package com.ruuvi.station.settings.ui

import androidx.lifecycle.ViewModel
import com.ruuvi.station.settings.domain.AppSettingsInteractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CloudSettingsViewModel(
    private val interactor: AppSettingsInteractor
) : ViewModel() {

    private var _cloudModeEnabled = MutableStateFlow(interactor.isCloudModeEnabled())
    val cloudModeEnabled: StateFlow<Boolean> = _cloudModeEnabled

    fun setIsCloudModeEnabled(isEnabled: Boolean) {
        interactor.setIsCloudModeEnabled(isEnabled)
        _cloudModeEnabled.value = interactor.isCloudModeEnabled()
    }
}