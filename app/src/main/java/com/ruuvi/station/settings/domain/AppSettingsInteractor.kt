package com.ruuvi.station.settings.domain

import androidx.lifecycle.LiveData
import com.google.gson.JsonObject
import com.koushikdutta.async.future.FutureCallback
import com.koushikdutta.ion.Response
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.app.ui.DarkModeState
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.dataforwarding.domain.DataForwardingSender
import com.ruuvi.station.network.domain.NetworkApplicationSettings
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.UnitType.*
import com.ruuvi.station.util.BackgroundScanModes

class AppSettingsInteractor(
    private val preferencesRepository: PreferencesRepository,
    private val dataForwardingSender: DataForwardingSender,
    private val unitsConverter: UnitsConverter,
    private val networkApplicationSettings: NetworkApplicationSettings,
    private val sensorSettingsRepository: SensorSettingsRepository,
    private val tagRepository: TagRepository
) {

    fun getTemperatureUnit(): TemperatureUnit =
        preferencesRepository.getTemperatureUnit()

    fun getTemperatureUnitLiveData() =
        preferencesRepository.getTemperatureUnitLiveData()

    fun setTemperatureUnit(unit: TemperatureUnit) {
        preferencesRepository.setTemperatureUnit(unit)
        networkApplicationSettings.updateTemperatureUnit()
    }

    fun getTemperatureAccuracy(): Accuracy =
        preferencesRepository.getTemperatureAccuracy()

    fun getTemperatureAccuracyLiveData() =
        preferencesRepository.getTemperatureAccuracyLiveData()

    fun setTemperatureAccuracy(accuracy: Accuracy) {
        preferencesRepository.setTemperatureAccuracy(accuracy)
        networkApplicationSettings.updateTemperatureAccuracy()
    }

    fun getHumidityUnit(): HumidityUnit =
        preferencesRepository.getHumidityUnit()

    fun getHumidityUnitLiveData() =
        preferencesRepository.getHumidityUnitLiveData()

    fun setHumidityUnit(unit: HumidityUnit) {
        preferencesRepository.setHumidityUnit(unit)
        networkApplicationSettings.updateHumidityUnit()
    }

    fun getHumidityAccuracy(): Accuracy =
        preferencesRepository.getHumidityAccuracy()

    fun getHumidityAccuracyLiveData() =
        preferencesRepository.getHumidityAccuracyLiveData()

    fun setHumidityAccuracy(accuracy: Accuracy) {
        preferencesRepository.setHumidityAccuracy(accuracy)
        networkApplicationSettings.updateHumidityAccuracy()
        networkApplicationSettings.updateRelativeHumidityAccuracy()
        networkApplicationSettings.updateAbsoluteHumidityAccuracy()
        networkApplicationSettings.updateDewPointAccuracy()
    }

    fun getRelativeHumidityAccuracy(): Accuracy =
        preferencesRepository.getRelativeHumidityAccuracy()

    fun getRelativeHumidityAccuracyLiveData() =
        preferencesRepository.getRelativeHumidityAccuracyLiveData()

    fun setRelativeHumidityAccuracy(accuracy: Accuracy) {
        preferencesRepository.setRelativeHumidityAccuracy(accuracy)
        networkApplicationSettings.updateRelativeHumidityAccuracy()
    }

    fun getAbsoluteHumidityAccuracy(): Accuracy =
        preferencesRepository.getAbsoluteHumidityAccuracy()

    fun getAbsoluteHumidityAccuracyLiveData() =
        preferencesRepository.getAbsoluteHumidityAccuracyLiveData()

    fun setAbsoluteHumidityAccuracy(accuracy: Accuracy) {
        preferencesRepository.setAbsoluteHumidityAccuracy(accuracy)
        networkApplicationSettings.updateAbsoluteHumidityAccuracy()
    }

    fun getDewPointAccuracy(): Accuracy =
        preferencesRepository.getDewPointAccuracy()

    fun getDewPointAccuracyLiveData() =
        preferencesRepository.getDewPointAccuracyLiveData()

    fun setDewPointAccuracy(accuracy: Accuracy) {
        preferencesRepository.setDewPointAccuracy(accuracy)
        networkApplicationSettings.updateDewPointAccuracy()
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

    fun getAllPressureUnits(): List<PressureUnit> = unitsConverter.getAllPressureUnits()

    fun getPressureUnit(): PressureUnit = unitsConverter.getPressureUnit()

    fun getPressureUnitLiveData() =
        preferencesRepository.getPressureUnitLiveData()

    fun setPressureUnit(unit: PressureUnit) {
        preferencesRepository.setPressureUnit(unit)
        networkApplicationSettings.updatePressureUnit()
    }

    fun getPressureAccuracy(): Accuracy =
        preferencesRepository.getPressureAccuracy()

    fun getPressureAccuracyLiveData() =
        preferencesRepository.getPressureAccuracyLiveData()

    fun setPressureAccuracy(accuracy: Accuracy) {
        preferencesRepository.setPressureAccuracy(accuracy)
        networkApplicationSettings.updatePressureAccuracy()
    }

    fun getPmAccuracy(): Accuracy =
        preferencesRepository.getPmAccuracy()

    fun getPmAccuracyLiveData() =
        preferencesRepository.getPmAccuracyLiveData()

    fun setPmAccuracy(accuracy: Accuracy) {
        preferencesRepository.setPmAccuracy(accuracy)
        networkApplicationSettings.updatePmAccuracy()
    }

    fun getAccelerationAccuracy(): Accuracy =
        preferencesRepository.getAccelerationAccuracy()

    fun getAccelerationAccuracyLiveData() =
        preferencesRepository.getAccelerationAccuracyLiveData()

    fun setAccelerationAccuracy(accuracy: Accuracy) {
        preferencesRepository.setAccelerationAccuracy(accuracy)
        networkApplicationSettings.updateAccelerationAccuracy()
    }

    fun getVoltageAccuracy(): Accuracy =
        preferencesRepository.getVoltageAccuracy()

    fun getVoltageAccuracyLiveData() =
        preferencesRepository.getVoltageAccuracyLiveData()

    fun setVoltageAccuracy(accuracy: Accuracy) {
        preferencesRepository.setVoltageAccuracy(accuracy)
        networkApplicationSettings.updateVoltageAccuracy()
    }

    fun getAllTemperatureUnits(): List<TemperatureUnit> = unitsConverter.getAllTemperatureUnits()

    fun getAllHumidityUnits(): List<HumidityUnit> = unitsConverter.getAllHumidityUnits()

    fun getAccuracyList(): Array<Accuracy> = Accuracy.values()

    fun getAccuracy(target: ResolutionSettingsTarget): Accuracy =
        when (target) {
            ResolutionSettingsTarget.Temperature -> getTemperatureAccuracy()
            ResolutionSettingsTarget.RelativeHumidity -> getRelativeHumidityAccuracy()
            ResolutionSettingsTarget.AbsoluteHumidity -> getAbsoluteHumidityAccuracy()
            ResolutionSettingsTarget.DewPoint -> getDewPointAccuracy()
            ResolutionSettingsTarget.Pressure -> getPressureAccuracy()
            ResolutionSettingsTarget.ParticulateMatter -> getPmAccuracy()
            ResolutionSettingsTarget.Acceleration -> getAccelerationAccuracy()
            ResolutionSettingsTarget.Voltage -> getVoltageAccuracy()
        }

    fun getAccuracyLiveData(target: ResolutionSettingsTarget): LiveData<Accuracy> =
        when (target) {
            ResolutionSettingsTarget.Temperature -> getTemperatureAccuracyLiveData()
            ResolutionSettingsTarget.RelativeHumidity -> getRelativeHumidityAccuracyLiveData()
            ResolutionSettingsTarget.AbsoluteHumidity -> getAbsoluteHumidityAccuracyLiveData()
            ResolutionSettingsTarget.DewPoint -> getDewPointAccuracyLiveData()
            ResolutionSettingsTarget.Pressure -> getPressureAccuracyLiveData()
            ResolutionSettingsTarget.ParticulateMatter -> getPmAccuracyLiveData()
            ResolutionSettingsTarget.Acceleration -> getAccelerationAccuracyLiveData()
            ResolutionSettingsTarget.Voltage -> getVoltageAccuracyLiveData()
        }

    fun setAccuracy(target: ResolutionSettingsTarget, accuracy: Accuracy) {
        when (target) {
            ResolutionSettingsTarget.Temperature -> setTemperatureAccuracy(accuracy)
            ResolutionSettingsTarget.RelativeHumidity -> setRelativeHumidityAccuracy(accuracy)
            ResolutionSettingsTarget.AbsoluteHumidity -> setAbsoluteHumidityAccuracy(accuracy)
            ResolutionSettingsTarget.DewPoint -> setDewPointAccuracy(accuracy)
            ResolutionSettingsTarget.Pressure -> setPressureAccuracy(accuracy)
            ResolutionSettingsTarget.ParticulateMatter -> setPmAccuracy(accuracy)
            ResolutionSettingsTarget.Acceleration -> setAccelerationAccuracy(accuracy)
            ResolutionSettingsTarget.Voltage -> setVoltageAccuracy(accuracy)
        }
    }

    fun getResolutionTargets(): List<ResolutionSettingsTarget> {
        val sensors = tagRepository.getFavoriteSensors()
        if (sensors.isEmpty()) {
            return emptyList()
        }

        val latestMeasurements = sensors.mapNotNull { it.latestMeasurement }
        if (latestMeasurements.isEmpty()) {
            return ResolutionSettingsTarget.values().toList()
        }

        val targets = mutableListOf<ResolutionSettingsTarget>()
        if (latestMeasurements.any { it.temperature != null }) {
            targets.add(ResolutionSettingsTarget.Temperature)
        }
        if (latestMeasurements.any { it.humidity != null }) {
            targets.add(ResolutionSettingsTarget.RelativeHumidity)
        }
        if (latestMeasurements.any { it.humidity != null && it.temperature != null }) {
            targets.add(ResolutionSettingsTarget.AbsoluteHumidity)
            targets.add(ResolutionSettingsTarget.DewPoint)
        }
        if (latestMeasurements.any { it.pressure != null }) {
            targets.add(ResolutionSettingsTarget.Pressure)
        }
        if (latestMeasurements.any { it.pm10 != null || it.pm25 != null || it.pm40 != null || it.pm100 != null }) {
            targets.add(ResolutionSettingsTarget.ParticulateMatter)
        }
        if (latestMeasurements.any { it.accelerationX != null || it.accelerationY != null || it.accelerationZ != null }) {
            targets.add(ResolutionSettingsTarget.Acceleration)
        }
        if (latestMeasurements.any { it.voltage.value > 0 }) {
            targets.add(ResolutionSettingsTarget.Voltage)
        }

        return targets
    }

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

    fun getMarketingPermission(): Boolean = preferencesRepository.getMarketingPermission()

    fun setMarketingPermission(enabled: Boolean) {
        preferencesRepository.setMarketingPermission(enabled)
        networkApplicationSettings.updateMarketingPermission()
    }

    fun isPushAlerts(): Boolean = !preferencesRepository.isDisablePushNotifications()

    fun setPushAlerts(enabled: Boolean) {
        preferencesRepository.setDisablePushNotifications(!enabled)
        networkApplicationSettings.updateDisablePushNotifications()
    }
}
