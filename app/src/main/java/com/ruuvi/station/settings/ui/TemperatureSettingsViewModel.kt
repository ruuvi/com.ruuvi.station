package com.ruuvi.station.settings.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.ruuvi.station.settings.domain.AppSettingsInteractor
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.UnitType.*

class TemperatureSettingsViewModel (
    private val appSettingsInteractor: AppSettingsInteractor
): ViewModel() {
    fun getAllTemperatureUnits(): List<TemperatureUnit> = appSettingsInteractor.getAllTemperatureUnits()

    val temperatureUnit: LiveData<TemperatureUnit> = appSettingsInteractor.getTemperatureUnitLiveData()

    fun setTemperatureUnit(unit: TemperatureUnit) {
        appSettingsInteractor.setTemperatureUnit(unit)
    }

    fun getAccuracyList() = appSettingsInteractor.getAccuracyList()

    val temperatureAccuracy: LiveData<Accuracy> = appSettingsInteractor.getTemperatureAccuracyLiveData()

    fun setTemperatureAccuracy(accuracy: Accuracy) {
        appSettingsInteractor.setTemperatureAccuracy(accuracy)
    }
}
