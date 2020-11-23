package com.ruuvi.station.settings.ui

import androidx.lifecycle.ViewModel
import com.ruuvi.station.settings.domain.AppSettingsInteractor
import com.ruuvi.station.units.model.TemperatureUnit

class AppSettingsTemperatureUnitViewModel (private val appSettingsInteractor: AppSettingsInteractor) : ViewModel() {

    fun getAllTemperatureUnits():Array<TemperatureUnit> = appSettingsInteractor.getAllTemperatureUnits()

    fun getTemperatureUnit(): TemperatureUnit = appSettingsInteractor.getTemperatureUnit()

    fun setTemperatureUnit(unit: TemperatureUnit) {
        appSettingsInteractor.setTemperatureUnit(unit)
    }
}