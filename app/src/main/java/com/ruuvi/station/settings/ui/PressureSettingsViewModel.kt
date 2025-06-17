package com.ruuvi.station.settings.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ruuvi.station.settings.domain.AppSettingsInteractor
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.UnitType.*

class PressureSettingsViewModel (
    private val appSettingsInteractor: AppSettingsInteractor
): ViewModel() {
    fun getAllPressureUnits(): List<PressureUnit> = appSettingsInteractor.getAllPressureUnits()

    private val _pressureyUnit = MutableLiveData<PressureUnit> (appSettingsInteractor.getPressureUnit())
    val pressureyUnit: LiveData<PressureUnit> = _pressureyUnit

    fun setPressureUnit(unit: PressureUnit) {
        appSettingsInteractor.setPressureUnit(unit)
        _pressureyUnit.value = appSettingsInteractor.getPressureUnit()
    }

    fun getAccuracyList() = appSettingsInteractor.getAccuracyList()

    private val _pressureAccuracy = MutableLiveData<Accuracy> (appSettingsInteractor.getPressureAccuracy())
    val pressureAccuracy: LiveData<Accuracy> = _pressureAccuracy

    fun setPressureAccuracy(accuracy: Accuracy) {
        appSettingsInteractor.setPressureAccuracy(accuracy)
        _pressureAccuracy.value = appSettingsInteractor.getPressureAccuracy()
    }
}