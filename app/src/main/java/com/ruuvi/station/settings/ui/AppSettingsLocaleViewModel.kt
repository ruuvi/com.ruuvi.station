package com.ruuvi.station.settings.ui

import androidx.lifecycle.ViewModel
import com.ruuvi.station.app.locale.LocaleType
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.settings.domain.AppSettingsInteractor

class AppSettingsLocaleViewModel(
    private val appSettingsInteractor: AppSettingsInteractor,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    fun getAllTLocales():Array<LocaleType> = appSettingsInteractor.getAllLocales()

    fun getLocale() = preferencesRepository.getLocale()

    fun setLocale(locale: String) {
        preferencesRepository.setLocale(locale)
    }
}