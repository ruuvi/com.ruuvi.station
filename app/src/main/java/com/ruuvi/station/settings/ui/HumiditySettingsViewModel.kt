package com.ruuvi.station.settings.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ruuvi.station.settings.domain.AppSettingsInteractor
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.UnitType.*

class HumiditySettingsViewModel (
    private val appSettingsInteractor: AppSettingsInteractor
): ViewModel() {
    fun getAllHumidityUnits(): List<HumidityUnit> = appSettingsInteractor.getAllHumidityUnits()

    private val _humidityUnit = MutableLiveData<HumidityUnit> (appSettingsInteractor.getHumidityUnit())
    val humidityUnit: LiveData<HumidityUnit> = _humidityUnit

    fun setHumidityUnit(unit: HumidityUnit) {
        appSettingsInteractor.setHumidityUnit(unit)
        _humidityUnit.value = appSettingsInteractor.getHumidityUnit()
    }

    fun getAccuracyList() = appSettingsInteractor.getAccuracyList()

    private val _humidityAccuracy = MutableLiveData<Accuracy> (appSettingsInteractor.getHumidityAccuracy())
    val humidityAccuracy: LiveData<Accuracy> = _humidityAccuracy

    fun setHumidityAccuracy(accuracy: Accuracy) {
        appSettingsInteractor.setHumidityAccuracy(accuracy)
        _humidityAccuracy.value = appSettingsInteractor.getHumidityAccuracy()
    }
}