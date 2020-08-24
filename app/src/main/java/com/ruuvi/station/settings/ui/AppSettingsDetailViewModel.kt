package com.ruuvi.station.settings.ui

import androidx.lifecycle.ViewModel
import com.ruuvi.station.model.HumidityUnit
import com.ruuvi.station.settings.domain.AppSettingsInteractor

class AppSettingsDetailViewModel(private val interactor: AppSettingsInteractor) : ViewModel() {

    var gatewayUrl = ""
    var deviceId = ""

    fun saveUrlAndDeviceId() {
        interactor.saveUrlAndDeviceId(gatewayUrl, deviceId)
    }

    fun restoreUrlAndDeviceId() {
        if (gatewayUrl.isEmpty()) gatewayUrl = getUrl()
        if (deviceId.isEmpty()) deviceId = getId()
    }

    fun getTemperatureUnit(): String =
        interactor.getTemperatureUnit()

    fun setTemperatureUnit(unit: String) =
        interactor.setTemperatureUnit(unit)

    fun getHumidityUnit(): HumidityUnit =
        interactor.getHumidityUnit()

    fun setHumidityUnit(unit: HumidityUnit) =
        interactor.setHumidityUnit(unit)

    fun isServiceWakeLock(): Boolean =
        interactor.isServiceWakeLock()

    fun setIsServiceWakeLock(isLocked: Boolean) =
        interactor.setIsServiceWakeLock(isLocked)

    private fun getUrl(): String =
        interactor.getGatewayUrl()

    fun getId(): String =
        interactor.getDeviceId()
}