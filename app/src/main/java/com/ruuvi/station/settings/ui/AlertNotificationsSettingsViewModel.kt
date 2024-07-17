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

    private var _emailAlerts = MutableStateFlow(interactor.isEmailAlerts())
    val emailAlerts: StateFlow<Boolean> = _emailAlerts

    private var _pushAlerts = MutableStateFlow(interactor.isPushAlerts())
    val pushAlerts: StateFlow<Boolean> = _pushAlerts

    fun setLimitLocalAlerts(isEnabled: Boolean) {
        interactor.setLimitLocalAlertsEnabled(isEnabled)
        _limitLocalAlerts.value = interactor.isLimitLocalAlertsEnabled()
    }

    fun setEmailAlerts(isEnabled: Boolean) {
        interactor.setEmailAlerts(isEnabled)
        _emailAlerts.value = interactor.isEmailAlerts()
    }

    fun setPushAlerts(isEnabled: Boolean) {
        interactor.setPushAlerts(isEnabled)
        _pushAlerts.value = interactor.isPushAlerts()
    }
}