package com.ruuvi.station.settings.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ruuvi.station.settings.domain.AppSettingsInteractor
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.TemperatureUnit

class TemperatureSettingsViewModel (
    private val appSettingsInteractor: AppSettingsInteractor
): ViewModel() {
    fun getAllTemperatureUnits():Array<TemperatureUnit> = appSettingsInteractor.getAllTemperatureUnits()

    private val _temperatureUnit = MutableLiveData<TemperatureUnit> (appSettingsInteractor.getTemperatureUnit())
    val temperatureUnit: LiveData<TemperatureUnit> = _temperatureUnit

    fun setTemperatureUnit(unit: TemperatureUnit) {
        appSettingsInteractor.setTemperatureUnit(unit)
        _temperatureUnit.value = appSettingsInteractor.getTemperatureUnit()
    }

    fun getAccuracyList() = appSettingsInteractor.getAccuracyList()

    private val _temperatureAccuracy = MutableLiveData<Accuracy> (appSettingsInteractor.getTemperatureAccuracy())
    val temperatureAccuracy: LiveData<Accuracy> = _temperatureAccuracy

    fun setTemperatureAccuracy(accuracy: Accuracy) {
        appSettingsInteractor.setTemperatureAccuracy(accuracy)
        _temperatureAccuracy.value = appSettingsInteractor.getTemperatureAccuracy()
    }
}