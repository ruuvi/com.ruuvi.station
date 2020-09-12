package com.ruuvi.station.app.preferences

import com.ruuvi.station.units.model.HumidityUnit
import com.ruuvi.station.units.model.PressureUnit
import com.ruuvi.station.units.model.TemperatureUnit
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.util.DeviceIdGenerator

class PreferencesRepository(private val preferences: Preferences) {

    fun getTemperatureUnit(): TemperatureUnit =
        preferences.temperatureUnit

    fun setTemperatureUnit(unit: TemperatureUnit) {
        preferences.temperatureUnit = unit
    }

    fun getHumidityUnit(): HumidityUnit =
        preferences.humidityUnit

    fun setHumidityUnit(unit: HumidityUnit) {
        preferences.humidityUnit = unit
    }

    fun getPressureUnit(): PressureUnit =
        preferences.pressureUnit

    fun setPressureUnit(unit: PressureUnit) {
        preferences.pressureUnit = unit
    }

    fun getGatewayUrl(): String =
        preferences.gatewayUrl

    fun setGatewayUrl(gatewayUrl: String) {
        preferences.gatewayUrl = gatewayUrl
    }

    fun getDeviceId(): String {
        var deviceId = preferences.deviceId
        if(deviceId.isEmpty()){
            deviceId = DeviceIdGenerator.generateId()
            setDeviceId(deviceId)
        }
        return deviceId
    }

    fun setDeviceId(deviceId: String) {
        preferences.deviceId = deviceId
    }

    fun saveUrlAndDeviceId(url: String, deviceId: String) {
        preferences.gatewayUrl = url
        preferences.deviceId = deviceId
    }

    fun isServiceWakelock() =
        preferences.serviceWakelock

    fun setIsServiceWakeLock(isLocked: Boolean) {
        preferences.serviceWakelock = isLocked
    }

    fun getBackgroundScanMode(): BackgroundScanModes =
        preferences.backgroundScanMode

    fun setBackgroundScanMode(mode: BackgroundScanModes) {
        preferences.backgroundScanMode = mode
    }

    fun isDashboardEnabled(): Boolean =
        preferences.dashboardEnabled

    fun setIsDashboardEnabled(isEnabled: Boolean) {
        preferences.dashboardEnabled = isEnabled
    }

    fun getBackgroundScanInterval(): Int =
        preferences.backgroundScanInterval

    fun setBackgroundScanInterval(newInterval: Int) {
        preferences.backgroundScanInterval = newInterval
    }

    fun isShowAllGraphPoint(): Boolean =
        preferences.graphShowAllPoint

    fun setIsShowAllGraphPoint(isShow: Boolean) {
        preferences.graphShowAllPoint = isShow
    }

    fun graphDrawDots(): Boolean = preferences.graphDrawDots

    fun setGraphDrawDots(drawDots: Boolean) {
        preferences.graphDrawDots = drawDots
    }

    fun getGraphPointInterval(): Int =
        preferences.graphPointInterval

    fun setGraphPointInterval(newInterval: Int) {
        preferences.graphPointInterval = newInterval
    }

    fun getGraphViewPeriod(): Int =
        preferences.graphViewPeriod

    fun setGraphViewPeriod(newPeriod: Int) {
        preferences.graphViewPeriod = newPeriod
    }

    fun isFirstGraphVisit(): Boolean =
        preferences.isFirstGraphVisit

    fun setIsFirstGraphVisit(isFirst: Boolean) {
        preferences.isFirstGraphVisit = isFirst
    }

    fun isFirstStart(): Boolean =
        preferences.isFirstStart

    fun setFirstStart(isFirstStart: Boolean){
        preferences.isFirstStart = isFirstStart
    }
}