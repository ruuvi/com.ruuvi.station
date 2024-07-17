package com.ruuvi.station.settings.domain

import com.google.gson.JsonObject
import com.koushikdutta.async.future.FutureCallback
import com.koushikdutta.ion.Response
import com.ruuvi.station.app.locale.LocaleType
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.app.ui.DarkModeState
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.dataforwarding.domain.DataForwardingSender
import com.ruuvi.station.network.domain.NetworkApplicationSettings
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.HumidityUnit
import com.ruuvi.station.units.model.PressureUnit
import com.ruuvi.station.units.model.TemperatureUnit
import com.ruuvi.station.util.BackgroundScanModes

class AppSettingsInteractor(
    private val preferencesRepository: PreferencesRepository,
    private val dataForwardingSender: DataForwardingSender,
    private val unitsConverter: UnitsConverter,
    private val networkApplicationSettings: NetworkApplicationSettings,
    private val sensorSettingsRepository: SensorSettingsRepository
) {

    fun getTemperatureUnit(): TemperatureUnit =
        preferencesRepository.getTemperatureUnit()

    fun setTemperatureUnit(unit: TemperatureUnit) {
        preferencesRepository.setTemperatureUnit(unit)
        networkApplicationSettings.updateTemperatureUnit()
    }

    fun getTemperatureAccuracy(): Accuracy =
        preferencesRepository.getTemperatureAccuracy()

    fun setTemperatureAccuracy(accuracy: Accuracy) {
        preferencesRepository.setTemperatureAccuracy(accuracy)
        networkApplicationSettings.updateTemperatureAccuracy()
    }

    fun getHumidityUnit(): HumidityUnit =
        preferencesRepository.getHumidityUnit()

    fun setHumidityUnit(unit: HumidityUnit) {
        preferencesRepository.setHumidityUnit(unit)
        networkApplicationSettings.updateHumidityUnit()
    }

    fun getHumidityAccuracy(): Accuracy =
        preferencesRepository.getHumidityAccuracy()

    fun setHumidityAccuracy(accuracy: Accuracy) {
        preferencesRepository.setHumidityAccuracy(accuracy)
        networkApplicationSettings.updateHumidityAccuracy()
    }

    fun getHumidityUnitString(): String = unitsConverter.getHumidityUnitString()

    fun getDataForwardingUrl(): String =
        preferencesRepository.getDataForwardingUrl()

    fun setDataForwardingUrl(url: String) {
        preferencesRepository.setDataForwardingUrl(url)
    }

    fun getDataForwardingLocationEnabled():Boolean =
        preferencesRepository.getDataForwardingLocationEnabled()

    fun setDataForwardingLocationEnabled(locationEnabled: Boolean) {
        preferencesRepository.setDataForwardingLocationEnabled(locationEnabled)
    }

    fun getDataForwardingDuringSyncEnabled():Boolean =
        preferencesRepository.getDataForwardingDuringSyncEnabled()

    fun setDataForwardingDuringSyncEnabled(forwardingDurinSyncEnabled: Boolean) {
        preferencesRepository.setDataForwardingDuringSyncEnabled(forwardingDurinSyncEnabled)
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

    fun setBackgroundScanMode(mode: BackgroundScanModes) {
        if (mode != preferencesRepository.getBackgroundScanMode()) {
            preferencesRepository.setBackgroundScanMode(mode)
            networkApplicationSettings.updateBackgroundScanMode()
        }
    }

    fun isCloudModeEnabled(): Boolean =
        preferencesRepository.isCloudModeEnabled()

    fun setIsCloudModeEnabled(isEnabled: Boolean) {
        preferencesRepository.setIsCloudModeEnabled(isEnabled)
        networkApplicationSettings.updateCloudModeEnabled()
    }

    fun shouldShowCloudMode(): Boolean {
        return preferencesRepository.signedIn()
            && sensorSettingsRepository.getSensorSettings()
            .any { it.networkSensor && it.networkLastSync != null }
    }

    fun getBackgroundScanInterval(): Int =
        preferencesRepository.getBackgroundScanInterval()

    fun setBackgroundScanInterval(interval: Int) {
        if (interval != preferencesRepository.getBackgroundScanInterval()) {
            preferencesRepository.setBackgroundScanInterval(interval)
            networkApplicationSettings.updateBackgroundScanInterval()
        }
    }

    fun isShowAllGraphPoint(): Boolean =
        preferencesRepository.isShowAllGraphPoint()

    fun setIsShowAllGraphPoint(isShowAll: Boolean) {
        if (isShowAll != preferencesRepository.isShowAllGraphPoint()) {
            preferencesRepository.setIsShowAllGraphPoint(isShowAll)
            networkApplicationSettings.updateChartShowAllPoints()
        }
    }

    fun graphDrawDots(): Boolean =
        preferencesRepository.graphDrawDots()

    fun setGraphDrawDots(isDrawDots: Boolean) {
        if (isDrawDots != preferencesRepository.graphDrawDots()) {
            preferencesRepository.setGraphDrawDots(isDrawDots)
            networkApplicationSettings.updateChartDrawDots()
        }
    }

    fun getGraphPointInterval(): Int =
        preferencesRepository.getGraphPointInterval()

    fun setGraphPointInterval(newInterval: Int) =
        preferencesRepository.setGraphPointInterval(newInterval)

    fun getGraphViewPeriod(): Int =
        preferencesRepository.getGraphViewPeriodHours()

    fun setGraphViewPeriod(newPeriod: Int) {
        if (newPeriod != preferencesRepository.getGraphViewPeriodHours()) {
            preferencesRepository.setGraphViewPeriodHours(newPeriod)
        }
    }

    fun testGateway(
        gatewayUrl: String,
        deviceId: String,
        callback: FutureCallback<Response<JsonObject>>
    ) = dataForwardingSender.test(gatewayUrl, deviceId, callback)

    fun getAllPressureUnits(): Array<PressureUnit> = unitsConverter.getAllPressureUnits()

    fun getPressureUnit(): PressureUnit = unitsConverter.getPressureUnit()

    fun setPressureUnit(unit: PressureUnit) {
        preferencesRepository.setPressureUnit(unit)
        networkApplicationSettings.updatePressureUnit()
    }

    fun getPressureAccuracy(): Accuracy =
        preferencesRepository.getPressureAccuracy()

    fun setPressureAccuracy(accuracy: Accuracy) {
        preferencesRepository.setPressureAccuracy(accuracy)
        networkApplicationSettings.updatePressureAccuracy()
    }

    fun getAllTemperatureUnits(): Array<TemperatureUnit> = unitsConverter.getAllTemperatureUnits()

    fun getAllHumidityUnits(): Array<HumidityUnit> = unitsConverter.getAllHumidityUnits()

    fun getAccuracyList(): Array<Accuracy> = Accuracy.values()

    fun getAllLocales(): Array<LocaleType> = LocaleType.values()

    fun clearLastSync() = sensorSettingsRepository.clearLastSyncGatt()

    fun getDarkMode(): DarkModeState = preferencesRepository.getDarkMode()

    fun updateDarkMode(darkModeState: DarkModeState) {
        preferencesRepository.updateDarkMode(darkModeState)
    }

    fun isLimitLocalAlertsEnabled(): Boolean =
        preferencesRepository.getLimitLocalAlerts()

    fun setLimitLocalAlertsEnabled(isEnabled: Boolean) {
        preferencesRepository.setLimitLocalAlerts(isEnabled)
    }
    
    fun isEmailAlerts(): Boolean = !preferencesRepository.isDisableEmailNotifications()

    fun setEmailAlerts(enabled: Boolean) {
        preferencesRepository.setDisableEmailNotifications(!enabled)
        networkApplicationSettings.updateDisableEmailNotifications()
    }

    fun isPushAlerts(): Boolean = !preferencesRepository.isDisablePushNotifications()

    fun setPushAlerts(enabled: Boolean) {
        preferencesRepository.setDisablePushNotifications(!enabled)
        networkApplicationSettings.updateDisablePushNotifications()
    }
}