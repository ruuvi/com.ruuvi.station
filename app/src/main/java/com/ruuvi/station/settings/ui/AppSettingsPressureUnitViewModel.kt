package com.ruuvi.station.settings.ui

import androidx.lifecycle.ViewModel
import com.ruuvi.station.settings.domain.AppSettingsInteractor
import com.ruuvi.station.units.model.UnitType.*

class AppSettingsPressureUnitViewModel (private val appSettingsInteractor: AppSettingsInteractor) : ViewModel() {

    fun getAllPressureUnits(): List<PressureUnit> = appSettingsInteractor.getAllPressureUnits()

    fun getPressureUnit(): PressureUnit = appSettingsInteractor.getPressureUnit()

    fun setPressureUnit(unit: PressureUnit) {
        appSettingsInteractor.setPressureUnit(unit)
    }
}