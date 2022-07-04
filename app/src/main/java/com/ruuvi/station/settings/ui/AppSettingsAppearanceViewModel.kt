package com.ruuvi.station.settings.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ruuvi.station.settings.domain.AppSettingsInteractor

class AppSettingsAppearanceViewModel (
    private val interactor: AppSettingsInteractor,
) : ViewModel() {

    private val _darkMode = MutableLiveData<DarkModeState> (interactor.getDarkMode())
    val darkMode: LiveData<DarkModeState> = _darkMode

    private  val _dashboardEnabled = MutableLiveData<Boolean> (interactor.isDashboardEnabled())
    val dashboardEnabled: LiveData<Boolean> = _dashboardEnabled

    fun setIsDashboardEnabled(isEnabled: Boolean) =
        interactor.setIsDashboardEnabled(isEnabled)

    fun setDarkMode(mode: DarkModeState) {
        interactor.updateDarkMode(mode)
        AppCompatDelegate.setDefaultNightMode(mode.code)
    }
}

enum class DarkModeState(val code: Int) {
    SYSTEM_THEME(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    DARK_THEME(AppCompatDelegate.MODE_NIGHT_YES),
    LIGHT_THEME(AppCompatDelegate.MODE_NIGHT_NO)
}