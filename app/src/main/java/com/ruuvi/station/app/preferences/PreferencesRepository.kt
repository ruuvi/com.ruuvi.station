package com.ruuvi.station.app.preferences

import com.ruuvi.station.settings.ui.DarkModeState
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.HumidityUnit
import com.ruuvi.station.units.model.PressureUnit
import com.ruuvi.station.units.model.TemperatureUnit
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.util.DeviceIdGenerator
import java.util.*

class PreferencesRepository(
    private val preferences: Preferences
    ) {
    fun getTemperatureUnit(): TemperatureUnit =
        preferences.temperatureUnit

    fun setTemperatureUnit(unit: TemperatureUnit) {
        preferences.temperatureUnit = unit
    }

    fun getTemperatureAccuracy(): Accuracy =
        preferences.temperatureAccuracy

    fun setTemperatureAccuracy(accuracy: Accuracy) {
        preferences.temperatureAccuracy = accuracy
    }

    fun getHumidityUnit(): HumidityUnit =
        preferences.humidityUnit

    fun setHumidityUnit(unit: HumidityUnit) {
        preferences.humidityUnit = unit
    }

    fun getHumidityAccuracy(): Accuracy =
        preferences.humidityAccuracy

    fun setHumidityAccuracy(accuracy: Accuracy) {
        preferences.humidityAccuracy = accuracy
    }

    fun getPressureUnit(): PressureUnit =
        preferences.pressureUnit

    fun setPressureUnit(unit: PressureUnit) {
        preferences.pressureUnit = unit
    }

    fun getPressureAccuracy(): Accuracy =
        preferences.pressureAccuracy

    fun setPressureAccuracy(accuracy: Accuracy) {
        preferences.pressureAccuracy = accuracy
    }

    fun getDataForwardingUrl(): String =
        preferences.dataForwardingUrl

    fun setDataForwardingUrl(url: String) {
        preferences.dataForwardingUrl = url
    }

    fun getDataForwardingLocationEnabled(): Boolean =
            preferences.dataForwardingLocationEnabled

    fun setDataForwardingLocationEnabled(locationEnabled: Boolean) {
        preferences.dataForwardingLocationEnabled = locationEnabled
    }

    fun getDataForwardingDuringSyncEnabled(): Boolean =
            preferences.dataForwardingDuringSyncEnabled

    fun setDataForwardingDuringSyncEnabled(forwardingDuringSyncEnabled: Boolean) {
        preferences.dataForwardingDuringSyncEnabled = forwardingDuringSyncEnabled
    }

    fun getDeviceId(): String {
        var deviceId = preferences.deviceId
        if (deviceId.isEmpty()) {
            deviceId = DeviceIdGenerator.generateId()
            setDeviceId(deviceId)
        }
        return deviceId
    }

    fun setDeviceId(deviceId: String) {
        preferences.deviceId = deviceId
    }

    fun saveUrlAndDeviceId(url: String, deviceId: String) {
        preferences.dataForwardingUrl = url
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

    fun getGraphViewPeriodDays(): Int =
        preferences.graphViewPeriodDays

    fun setGraphViewPeriodDays(newPeriod: Int) {
        preferences.graphViewPeriodDays = newPeriod
    }

    fun isFirstGraphVisit(): Boolean =
        preferences.isFirstGraphVisit

    fun setIsFirstGraphVisit(isFirst: Boolean) {
        preferences.isFirstGraphVisit = isFirst
    }

    fun isExperimentalFeaturesEnabled(): Boolean =
        preferences.experimentalFeatures

    fun setIsExperimentalFeaturesEnabled(experimentalEnabled: Boolean) {
        preferences.experimentalFeatures = experimentalEnabled
    }

    fun isFirstStart(): Boolean =
        preferences.isFirstStart

    fun setFirstStart(isFirstStart: Boolean) {
        preferences.isFirstStart = isFirstStart
    }

    fun getLastSyncDate(): Long =
        preferences.lastSyncDate

    fun setLastSyncDate(lastSyncDate: Long) {
        preferences.lastSyncDate = lastSyncDate
    }

    fun getUserEmailLiveData() = preferences.getUserEmailLiveData()

    fun getLastSyncDateLiveData() = preferences.getLastSyncDateLiveData()

    fun getExperimentalFeaturesLiveData() = preferences.getExperimentalFeaturesLiveData()

    fun getLocale(): String =
        preferences.locale

    fun setLocale(locale: String) {
        preferences.locale = locale
    }

    fun getUserEmail() = preferences.networkEmail

    fun signedIn() = preferences.networkEmail.isNotEmpty() && preferences.networkToken.isNotEmpty()

    fun getRequestForReviewDate() = preferences.requestForReviewDate

    fun updateRequestForReviewDate() {
        preferences.requestForReviewDate = Date().time
    }

    fun getRequestForAppUpdateDate() = preferences.requestForAppUpdateDate

    fun updateRequestForAppUpdateDate() {
        preferences.requestForAppUpdateDate = Date().time
    }

    fun isCloudModeEnabled(): Boolean =
        preferences.cloudModeEnabled

    fun setIsCloudModeEnabled(isEnabled: Boolean) {
        preferences.cloudModeEnabled = isEnabled
    }

    fun getDarkMode(): DarkModeState = preferences.darkMode

    fun updateDarkMode(darkMode: DarkModeState) {
        preferences.darkMode = darkMode
    }
}