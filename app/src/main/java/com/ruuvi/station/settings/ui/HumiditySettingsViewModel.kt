package com.ruuvi.station.settings.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.ruuvi.station.settings.domain.AppSettingsInteractor
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.UnitType.*

class HumiditySettingsViewModel (
    private val appSettingsInteractor: AppSettingsInteractor
): ViewModel() {
    fun getAllHumidityUnits(): List<HumidityUnit> = appSettingsInteractor.getAllHumidityUnits()

    val humidityUnit: LiveData<HumidityUnit> = appSettingsInteractor.getHumidityUnitLiveData()

    fun setHumidityUnit(unit: HumidityUnit) {
        appSettingsInteractor.setHumidityUnit(unit)
    }

    fun getAccuracyList() = appSettingsInteractor.getAccuracyList()

    val humidityAccuracy: LiveData<Accuracy> = appSettingsInteractor.getHumidityAccuracyLiveData()

    fun setHumidityAccuracy(accuracy: Accuracy) {
        appSettingsInteractor.setHumidityAccuracy(accuracy)
    }
}
