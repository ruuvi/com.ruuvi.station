package com.ruuvi.station.settings.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.ruuvi.station.settings.domain.AppSettingsInteractor
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.UnitType.*

class PressureSettingsViewModel (
    private val appSettingsInteractor: AppSettingsInteractor
): ViewModel() {
    fun getAllPressureUnits(): List<PressureUnit> = appSettingsInteractor.getAllPressureUnits()

    val pressureyUnit: LiveData<PressureUnit> = appSettingsInteractor.getPressureUnitLiveData()

    fun setPressureUnit(unit: PressureUnit) {
        appSettingsInteractor.setPressureUnit(unit)
    }

    fun getAccuracyList() = appSettingsInteractor.getAccuracyList()

    val pressureAccuracy: LiveData<Accuracy> = appSettingsInteractor.getPressureAccuracyLiveData()

    fun setPressureAccuracy(accuracy: Accuracy) {
        appSettingsInteractor.setPressureAccuracy(accuracy)
    }
}
