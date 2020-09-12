package com.ruuvi.station.settings.ui

import androidx.lifecycle.ViewModel
import com.ruuvi.station.settings.domain.AppSettingsInteractor
import com.ruuvi.station.units.model.HumidityUnit
import com.ruuvi.station.units.model.PressureUnit

class AppSettingsHumidityViewModel (private val appSettingsInteractor: AppSettingsInteractor) : ViewModel() {

    fun getAllPressureUnits():Array<PressureUnit> = appSettingsInteractor.getAllPressureUnits()

    fun getPressureUnit(): PressureUnit = appSettingsInteractor.getPressureUnit()

    fun setPressureUnit(unit: PressureUnit) {
        appSettingsInteractor.setPressureUnit(unit)
    }

    fun getAllHumidityUnits(): Array<HumidityUnit> = appSettingsInteractor.getAllHumidityUnits()

    fun getHumidityUnit(): HumidityUnit = appSettingsInteractor.getHumidityUnit()

    fun setHumidityUnit(unit: HumidityUnit) {
        appSettingsInteractor.setHumidityUnit(unit)
    }
}