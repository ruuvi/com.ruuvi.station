package com.ruuvi.station.settings.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ruuvi.station.app.ui.DarkModeState
import com.ruuvi.station.settings.domain.AppSettingsInteractor

class AppearanceSettingsViewModel (
    private val interactor: AppSettingsInteractor,
) : ViewModel() {

    private val _darkMode = MutableLiveData<DarkModeState> (interactor.getDarkMode())
    val darkMode: LiveData<DarkModeState> = _darkMode

    private  val _dashboardEnabled = MutableLiveData<Boolean> (interactor.isDashboardEnabled())
    val dashboardEnabled: LiveData<Boolean> = _dashboardEnabled

    fun getThemeOptions(): Array<DarkModeState> = DarkModeState.values()

    fun setIsDashboardEnabled(isEnabled: Boolean) {
        interactor.setIsDashboardEnabled(isEnabled)
        _dashboardEnabled.value = interactor.isDashboardEnabled()
    }

    fun setDarkMode(mode: DarkModeState) {
        interactor.updateDarkMode(mode)
        _darkMode.value = interactor.getDarkMode()
        AppCompatDelegate.setDefaultNightMode(mode.code)
    }
}