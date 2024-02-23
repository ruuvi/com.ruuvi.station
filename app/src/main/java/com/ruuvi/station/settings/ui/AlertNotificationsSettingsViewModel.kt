package com.ruuvi.station.settings.ui

import androidx.lifecycle.ViewModel
import com.ruuvi.station.settings.domain.AppSettingsInteractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AlertNotificationsSettingsViewModel(
    private val interactor: AppSettingsInteractor
): ViewModel() {

    private var _limitLocalAlerts = MutableStateFlow(interactor.isLimitLocalAlertsEnabled())
    val limitLocalAlerts: StateFlow<Boolean> = _limitLocalAlerts

    fun setLimitLocalAlerts(isEnabled: Boolean) {
        interactor.setLimitLocalAlertsEnabled(isEnabled)
        _limitLocalAlerts.value = interactor.isLimitLocalAlertsEnabled()
    }
}