package com.ruuvi.station.settings.domain

import com.google.gson.JsonObject
import com.koushikdutta.async.future.FutureCallback
import com.koushikdutta.ion.Response
import com.ruuvi.station.app.locale.LocaleType
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.gateway.GatewaySender
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.HumidityUnit
import com.ruuvi.station.units.model.PressureUnit
import com.ruuvi.station.units.model.TemperatureUnit
import com.ruuvi.station.util.BackgroundScanModes

class AppSettingsInteractor(
    private val preferencesRepository: PreferencesRepository,
    private val gatewaySender: GatewaySender,
    private val unitsConverter: UnitsConverter
) {

    fun getTemperatureUnit(): TemperatureUnit =
        preferencesRepository.getTemperatureUnit()

    fun setTemperatureUnit(unit: TemperatureUnit) =
        preferencesRepository.setTemperatureUnit(unit)

    fun getHumidityUnit(): HumidityUnit =
        preferencesRepository.getHumidityUnit()

    fun setHumidityUnit(unit: HumidityUnit) =
        preferencesRepository.setHumidityUnit(unit)

    fun getGatewayUrl(): String =
        preferencesRepository.getGatewayUrl()

    fun setGatewayUrl(gatewayUrl: String) {
        preferencesRepository.setGatewayUrl(gatewayUrl)
    }

    fun getDeviceId(): String =
        preferencesRepository.getDeviceId()

    fun setDeviceId(deviceId: String) {
        preferencesRepository.setDeviceId(deviceId)
    }

    fun isServiceWakeLock(): Boolean =
        preferencesRepository.isServiceWakelock()

    fun setIsServiceWakeLock(isLocked: Boolean) =
        preferencesRepository.setIsServiceWakeLock(isLocked)

    fun saveUrlAndDeviceId(url: String, deviceId: String) =
        preferencesRepository.saveUrlAndDeviceId(url, deviceId)

    fun getBackgroundScanMode(): BackgroundScanModes =
        preferencesRepository.getBackgroundScanMode()

    fun setBackgroundScanMode(mode: BackgroundScanModes) =
        preferencesRepository.setBackgroundScanMode(mode)

    fun isDashboardEnabled(): Boolean =
        preferencesRepository.isDashboardEnabled()

    fun setIsDashboardEnabled(isEnabled: Boolean) =
        preferencesRepository.setIsDashboardEnabled(isEnabled)

    fun getBackgroundScanInterval(): Int =
        preferencesRepository.getBackgroundScanInterval()

    fun setBackgroundScanInterval(interval: Int) =
        preferencesRepository.setBackgroundScanInterval(interval)

    fun isShowAllGraphPoint(): Boolean =
        preferencesRepository.isShowAllGraphPoint()

    fun setIsShowAllGraphPoint(isShow: Boolean) =
        preferencesRepository.setIsShowAllGraphPoint(isShow)

    fun graphDrawDots(): Boolean =
        preferencesRepository.graphDrawDots()

    fun setGraphDrawDots(isDrawDots: Boolean) =
        preferencesRepository.setGraphDrawDots(isDrawDots)

    fun getGraphPointInterval(): Int =
        preferencesRepository.getGraphPointInterval()

    fun setGraphPointInterval(newInterval: Int) =
        preferencesRepository.setGraphPointInterval(newInterval)

    fun getGraphViewPeriod(): Int =
        preferencesRepository.getGraphViewPeriod()

    fun setGraphViewPeriod(newPeriod: Int) =
        preferencesRepository.setGraphViewPeriod(newPeriod)

    fun testGateway(
        gatewayUrl: String,
        deviceId: String,
        callback: FutureCallback<Response<JsonObject>>
    ) = gatewaySender.test(gatewayUrl, deviceId, callback)

    fun getAllPressureUnits(): Array<PressureUnit> = unitsConverter.getAllPressureUnits()

    fun getPressureUnit(): PressureUnit = unitsConverter.getPressureUnit()

    fun setPressureUnit(unit: PressureUnit) {
        preferencesRepository.setPressureUnit(unit)
    }

    fun getAllTemperatureUnits(): Array<TemperatureUnit> = unitsConverter.getAllTemperatureUnits()

    fun getAllHumidityUnits(): Array<HumidityUnit> = unitsConverter.getAllHumidityUnits()

    fun getAllLocales(): Array<LocaleType> = LocaleType.values()
}