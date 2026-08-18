package com.ruuvi.station.app.preferences

import androidx.lifecycle.map
import com.ruuvi.station.BuildConfig
import com.ruuvi.station.app.ui.DarkModeState
import com.ruuvi.station.dashboard.DashboardTapAction
import com.ruuvi.station.dashboard.DashboardType
import com.ruuvi.station.network.domain.NetworkSettingNames
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.UnitType.*
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.util.DeviceIdGenerator
import com.ruuvi.station.util.extensions.toBooleanExtra
import java.util.*

class PreferencesRepository(
    private val preferences: Preferences
    ) {
    fun getTemperatureUnit(): TemperatureUnit =
        preferences.temperatureUnit

    fun getTemperatureAccuracy(): Accuracy =
        preferences.temperatureAccuracy

    fun getHumidityUnit(): HumidityUnit =
        preferences.humidityUnit

    fun getHumidityAccuracy(): Accuracy =
        preferences.humidityAccuracy

    fun getRelativeHumidityAccuracy(): Accuracy =
        preferences.relativeHumidityAccuracy

    fun getAbsoluteHumidityAccuracy(): Accuracy =
        preferences.absoluteHumidityAccuracy

    fun getDewPointAccuracy(): Accuracy =
        preferences.dewPointAccuracy

    fun getPressureUnit(): PressureUnit =
        preferences.pressureUnit

    fun getPressureAccuracy(): Accuracy =
        preferences.pressureAccuracy

    fun getPmAccuracy(): Accuracy =
        preferences.pmAccuracy

    fun getAccelerationAccuracy(): Accuracy =
        preferences.accelerationAccuracy

    fun getVoltageAccuracy(): Accuracy =
        preferences.voltageAccuracy

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

    fun getBackgroundScanInterval(): Int =
        preferences.backgroundScanInterval

    fun isShowAllGraphPoint(): Boolean =
        preferences.graphShowAllPoint

    fun getUseWebShare(): Boolean =
        preferences.useWebShare

    fun setUseWebShare(useWebShare: Boolean) {
        preferences.useWebShare = useWebShare
    }

    fun graphDrawDots(): Boolean = preferences.graphDrawDots

    fun getGraphPointInterval(): Int =
        preferences.graphPointInterval

    fun setGraphPointInterval(newInterval: Int) {
        preferences.graphPointInterval = newInterval
    }

    fun getGraphViewPeriodHours(): Int =
        preferences.graphViewPeriodHours

    fun setGraphViewPeriodHours(newPeriod: Int) {
        preferences.graphViewPeriodHours = newPeriod
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

    fun isDeveloperSettingsEnabled(): Boolean =
        preferences.developerSettings

    fun setDeveloperFeaturesEnabled(developerSettingsEnabled: Boolean) {
        preferences.developerSettings = developerSettingsEnabled
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

    fun getDeveloperSettingsLiveData() = preferences.getDeveloperSettingsLiveData()

    fun getTemperatureUnitLiveData() =
        preferences.getTemperatureUnitCodeLiveData().map { TemperatureUnit.getByCode(it) }

    fun getHumidityUnitLiveData() =
        preferences.getHumidityUnitCodeLiveData().map { HumidityUnit.getByCode(it.toString()) }

    fun getPressureUnitLiveData() =
        preferences.getPressureUnitCodeLiveData().map { PressureUnit.getByCode(it.toString()) }

    fun getTemperatureAccuracyLiveData() =
        preferences.getTemperatureAccuracyCodeLiveData().map { Accuracy.getByCode(it) ?: Accuracy.Accuracy2 }

    fun getHumidityAccuracyLiveData() =
        preferences.getHumidityAccuracyCodeLiveData().map { Accuracy.getByCode(it) ?: Accuracy.Accuracy2 }

    fun getRelativeHumidityAccuracyLiveData() =
        preferences.getRelativeHumidityAccuracyCodeLiveData().map { Accuracy.getByCode(it) ?: Accuracy.Accuracy2 }

    fun getAbsoluteHumidityAccuracyLiveData() =
        preferences.getAbsoluteHumidityAccuracyCodeLiveData().map { Accuracy.getByCode(it) ?: Accuracy.Accuracy2 }

    fun getDewPointAccuracyLiveData() =
        preferences.getDewPointAccuracyCodeLiveData().map { Accuracy.getByCode(it) ?: Accuracy.Accuracy2 }

    fun getPressureAccuracyLiveData() =
        preferences.getPressureAccuracyCodeLiveData().map { Accuracy.getByCode(it) ?: Accuracy.Accuracy2 }

    fun getPmAccuracyLiveData() =
        preferences.getPmAccuracyCodeLiveData().map { Accuracy.getByCode(it) ?: Accuracy.Accuracy1 }

    fun getAccelerationAccuracyLiveData() =
        preferences.getAccelerationAccuracyCodeLiveData().map { Accuracy.getByCode(it) ?: Accuracy.Accuracy2 }

    fun getVoltageAccuracyLiveData() =
        preferences.getVoltageAccuracyCodeLiveData().map { Accuracy.getByCode(it) ?: Accuracy.Accuracy2 }

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

    fun isDevServerEnabled(): Boolean =
        preferences.useDevServer

    fun setDevServerEnabled(isEnabled: Boolean) {
        preferences.useDevServer = isEnabled
    }

    fun getDarkMode(): DarkModeState = preferences.darkMode

    fun updateDarkMode(darkMode: DarkModeState) {
        preferences.darkMode = darkMode
    }

    fun getDashboardType(): DashboardType = preferences.dashboardType

    fun updateDashboardType(dashboardType: DashboardType) {
        preferences.dashboardType = dashboardType
    }

    fun getDashboardTapAction(): DashboardTapAction = preferences.dashboardTapAction

    fun updateDashboardTapAction(dashboardTapAction: DashboardTapAction) {
        preferences.dashboardTapAction = dashboardTapAction
    }

    fun getRegisteredToken(): String = preferences.registeredToken

    fun updateRegisteredToken(token: String) {
        preferences.registeredToken = token
    }

    fun getRegisteredTokenLanguage(): String = preferences.registeredTokenLanguage

    fun updateRegisteredTokenLanguage(language: String) {
        preferences.registeredTokenLanguage = language
    }

    fun getDeviceTokenRefreshDate(): Long =
        preferences.deviceTokenRefreshDate

    fun setDeviceTokenRefreshDate(refreshDate: Long) {
        preferences.deviceTokenRefreshDate = refreshDate
    }

    fun getSubscriptionRefreshDate(): Long =
        preferences.subscriptionRefreshDate

    fun setSubscriptionRefreshDate(refreshDate: Long) {
        preferences.subscriptionRefreshDate = refreshDate
    }

    fun getSubscriptionMaxSharesPerSensor(): Int =
        preferences.subscriptionMaxSharesPerSensor

    fun setSubscriptionMaxSharesPerSensor(maxShares: Int) {
        preferences.subscriptionMaxSharesPerSensor = maxShares
    }

    fun getDontShowGattSync(): Boolean =
        preferences.dontShowGattSync

    fun setDontShowGattSync(value: Boolean) {
        preferences.dontShowGattSync = value
    }

    fun getShowChartStats(): Boolean =
        preferences.showChartStats

    fun setShowChartStats(value: Boolean) {
        preferences.showChartStats = value
    }

    fun getLimitLocalAlerts(): Boolean =
        preferences.limitLocalAlerts

    fun setLimitLocalAlerts(value: Boolean) {
        preferences.limitLocalAlerts = value
    }

    fun getSignedInOnce(): Boolean =
        preferences.signedInOnce

    fun getSortedSensors(): String =
        preferences.sortedSensors

    fun isNewChartsUI(): Boolean =
        preferences.newChartsUI

    fun setNewChartsUI(newChartsUI: Boolean) {
        preferences.newChartsUI = newChartsUI
    }

    fun isAcceptTerms(): Boolean =
        preferences.acceptTerms

    fun setAcceptTerms(acceptTerms: Boolean) {
        preferences.acceptTerms = acceptTerms
    }

    fun isIncreasedChartSize(): Boolean =
        preferences.increasedChartSize

    fun setIncreasedChartSize(increasedChartSize: Boolean) {
        preferences.increasedChartSize = increasedChartSize
    }

    fun isFirebaseConsent(): Boolean =
        preferences.firebaseConsent

    fun setFirebaseConsent(firebaseConsent: Boolean) {
        preferences.firebaseConsent = firebaseConsent
    }

    fun isDisablePushNotifications(): Boolean =
        preferences.disablePushNotifications

    fun isDisableEmailNotifications(): Boolean =
        preferences.disableEmailNotifications

    fun getMarketingPermission(): Boolean =
        preferences.marketingPermission

    fun setNetworkSetting(settingName: String, value: String?, timestamp: Long) {
        when (settingName) {
            NetworkSettingNames.BACKGROUND_SCAN_MODE -> {
                val mode = value?.toIntOrNull()?.let(BackgroundScanModes::fromInt) ?: return
                preferences.backgroundScanMode = mode
                preferences.backgroundScanModeLastUpdated = timestamp
            }

            NetworkSettingNames.BACKGROUND_SCAN_INTERVAL -> {
                val interval = value?.toIntOrNull() ?: return
                preferences.backgroundScanInterval = interval
                preferences.backgroundScanIntervalLastUpdated = timestamp
            }

            NetworkSettingNames.UNIT_TEMPERATURE -> {
                val unitCode = value ?: return
                preferences.temperatureUnit = TemperatureUnit.getByCode(unitCode)
                preferences.temperatureUnitLastUpdated = timestamp
            }

            NetworkSettingNames.UNIT_HUMIDITY -> {
                val unitCode = value ?: return
                preferences.humidityUnit = HumidityUnit.getByCode(unitCode)
                preferences.humidityUnitLastUpdated = timestamp
            }

            NetworkSettingNames.UNIT_PRESSURE -> {
                val unitCode = value ?: return
                preferences.pressureUnit = PressureUnit.getByCode(unitCode)
                preferences.pressureUnitLastUpdated = timestamp
            }

            NetworkSettingNames.ACCURACY_TEMPERATURE -> {
                val accuracy = value?.toIntOrNull()?.let(Accuracy::getByCode) ?: return
                preferences.temperatureAccuracy = accuracy
                preferences.temperatureAccuracyLastUpdated = timestamp
            }

            NetworkSettingNames.ACCURACY_HUMIDITY -> {
                val accuracy = value?.toIntOrNull()?.let(Accuracy::getByCode) ?: return
                preferences.humidityAccuracy = accuracy
                preferences.humidityAccuracyLastUpdated = timestamp
                preferences.relativeHumidityAccuracyLastUpdated = timestamp
                preferences.absoluteHumidityAccuracyLastUpdated = timestamp
                preferences.dewPointAccuracyLastUpdated = timestamp
            }

            NetworkSettingNames.ACCURACY_HUMIDITY_RELATIVE -> {
                val accuracy = value?.toIntOrNull()?.let(Accuracy::getByCode) ?: return
                preferences.relativeHumidityAccuracy = accuracy
                preferences.relativeHumidityAccuracyLastUpdated = timestamp
            }

            NetworkSettingNames.ACCURACY_HUMIDITY_ABSOLUTE -> {
                val accuracy = value?.toIntOrNull()?.let(Accuracy::getByCode) ?: return
                preferences.absoluteHumidityAccuracy = accuracy
                preferences.absoluteHumidityAccuracyLastUpdated = timestamp
            }

            NetworkSettingNames.ACCURACY_HUMIDITY_DEW_POINT -> {
                val accuracy = value?.toIntOrNull()?.let(Accuracy::getByCode) ?: return
                preferences.dewPointAccuracy = accuracy
                preferences.dewPointAccuracyLastUpdated = timestamp
            }

            NetworkSettingNames.ACCURACY_PRESSURE -> {
                val accuracy = value?.toIntOrNull()?.let(Accuracy::getByCode) ?: return
                preferences.pressureAccuracy = accuracy
                preferences.pressureAccuracyLastUpdated = timestamp
            }

            NetworkSettingNames.ACCURACY_PM -> {
                val accuracy = value?.toIntOrNull()?.let(Accuracy::getByCode) ?: return
                preferences.pmAccuracy = accuracy
                preferences.pmAccuracyLastUpdated = timestamp
            }

            NetworkSettingNames.ACCURACY_ACCELERATION -> {
                val accuracy = value?.toIntOrNull()?.let(Accuracy::getByCode) ?: return
                preferences.accelerationAccuracy = accuracy
                preferences.accelerationAccuracyLastUpdated = timestamp
            }

            NetworkSettingNames.ACCURACY_VOLTAGE -> {
                val accuracy = value?.toIntOrNull()?.let(Accuracy::getByCode) ?: return
                preferences.voltageAccuracy = accuracy
                preferences.voltageAccuracyLastUpdated = timestamp
            }

            NetworkSettingNames.DASHBOARD_TYPE -> {
                val dashboardTypeCode = value ?: return
                if (!DashboardType.isValidCode(dashboardTypeCode)) return
                preferences.dashboardType = DashboardType.getByCode(dashboardTypeCode)
                preferences.dashboardTypeLastUpdated = timestamp
            }

            NetworkSettingNames.DASHBOARD_TAP_ACTION -> {
                val dashboardTapActionCode = value ?: return
                if (!DashboardTapAction.isValidCode(dashboardTapActionCode)) return
                preferences.dashboardTapAction = DashboardTapAction.getByCode(dashboardTapActionCode)
                preferences.dashboardTapActionLastUpdated = timestamp
            }

            NetworkSettingNames.CLOUD_MODE_ENABLED -> {
                val cloudMode = value?.toBooleanExtra() ?: return
                preferences.cloudModeEnabled = cloudMode
                preferences.cloudModeEnabledLastUpdated = timestamp
            }

            NetworkSettingNames.CHART_SHOW_ALL_POINTS -> {
                val showAllPoints = value?.toBooleanExtra() ?: return
                preferences.graphShowAllPoint = showAllPoints
                preferences.graphShowAllPointLastUpdated = timestamp
            }

            NetworkSettingNames.CHART_DRAW_DOTS -> {
                val drawDots = value?.toBooleanExtra() ?: return
                preferences.graphDrawDots = drawDots
                preferences.graphDrawDotsLastUpdated = timestamp
            }

NetworkSettingNames.SENSOR_ORDER -> {
                val sensorsOrder = value?.takeIf { it.isNotBlank() } ?: return
                preferences.sortedSensors = sensorsOrder
                preferences.sensorsOrderLastUpdated = timestamp
            }

            NetworkSettingNames.DISABLE_EMAIL_NOTIFICATIONS -> {
                val isDisabled = value?.toBooleanExtra() ?: return
                preferences.disableEmailNotifications = isDisabled
                preferences.disableEmailNotificationsLastUpdated = timestamp
            }

            NetworkSettingNames.DISABLE_PUSH_NOTIFICATIONS -> {
                val isDisabled = value?.toBooleanExtra() ?: return
                preferences.disablePushNotifications = isDisabled
                preferences.disablePushNotificationsLastUpdated = timestamp
            }

            NetworkSettingNames.DISABLE_TELEGRAM_NOTIFICATIONS -> {
                val isDisabled = value?.toBooleanExtra() ?: return
                preferences.disableTelegramNotifications = isDisabled
                preferences.disableTelegramNotificationsLastUpdated = timestamp
            }

            NetworkSettingNames.TIPS_ALLOWED -> {
                val isAllowed = value?.toBooleanExtra() ?: return
                preferences.tipsAllowed = isAllowed
                preferences.tipsAllowedLastUpdated = timestamp
            }

            NetworkSettingNames.MARKETING_PERMISSION -> {
                val isAllowed = value?.toBooleanExtra() ?: return
                preferences.marketingPermission = isAllowed
                preferences.marketingPermissionLastUpdated = timestamp
            }

            else -> Unit
        }
    }

    fun getNetworkSetting(settingName: String): String? {
        return when (settingName) {
            NetworkSettingNames.BACKGROUND_SCAN_MODE -> preferences.backgroundScanMode.value.toString()
            NetworkSettingNames.BACKGROUND_SCAN_INTERVAL -> preferences.backgroundScanInterval.toString()
            NetworkSettingNames.UNIT_TEMPERATURE -> preferences.temperatureUnit.unitCode
            NetworkSettingNames.UNIT_HUMIDITY -> preferences.humidityUnit.unitCode
            NetworkSettingNames.UNIT_PRESSURE -> preferences.pressureUnit.unitCode
            NetworkSettingNames.ACCURACY_TEMPERATURE -> preferences.temperatureAccuracy.code.toString()
            NetworkSettingNames.ACCURACY_HUMIDITY -> preferences.humidityAccuracy.code.toString()
            NetworkSettingNames.ACCURACY_HUMIDITY_RELATIVE -> preferences.relativeHumidityAccuracy.code.toString()
            NetworkSettingNames.ACCURACY_HUMIDITY_ABSOLUTE -> preferences.absoluteHumidityAccuracy.code.toString()
            NetworkSettingNames.ACCURACY_HUMIDITY_DEW_POINT -> preferences.dewPointAccuracy.code.toString()
            NetworkSettingNames.ACCURACY_PRESSURE -> preferences.pressureAccuracy.code.toString()
            NetworkSettingNames.ACCURACY_PM -> preferences.pmAccuracy.code.toString()
            NetworkSettingNames.ACCURACY_ACCELERATION -> preferences.accelerationAccuracy.code.toString()
            NetworkSettingNames.ACCURACY_VOLTAGE -> preferences.voltageAccuracy.code.toString()
            NetworkSettingNames.DASHBOARD_TYPE -> preferences.dashboardType.code
            NetworkSettingNames.DASHBOARD_TAP_ACTION -> preferences.dashboardTapAction.code
            NetworkSettingNames.CLOUD_MODE_ENABLED -> preferences.cloudModeEnabled.toString()
            NetworkSettingNames.CHART_SHOW_ALL_POINTS -> preferences.graphShowAllPoint.toString()
            NetworkSettingNames.CHART_DRAW_DOTS -> preferences.graphDrawDots.toString()
            NetworkSettingNames.SENSOR_ORDER -> preferences.sortedSensors.takeIf { it.isNotEmpty() }
            NetworkSettingNames.DISABLE_EMAIL_NOTIFICATIONS -> if (preferences.disableEmailNotifications) "1" else "0"
            NetworkSettingNames.DISABLE_PUSH_NOTIFICATIONS -> if (preferences.disablePushNotifications) "1" else "0"
            NetworkSettingNames.DISABLE_TELEGRAM_NOTIFICATIONS -> if (preferences.disableTelegramNotifications) "1" else "0"
            NetworkSettingNames.TIPS_ALLOWED -> if (preferences.tipsAllowed) "1" else "0"
            NetworkSettingNames.MARKETING_PERMISSION -> if (preferences.marketingPermission) "1" else "0"
            else -> null
        }
    }

    fun getNetworkSettingLastUpdated(settingName: String): Long {
        return when (settingName) {
            NetworkSettingNames.BACKGROUND_SCAN_MODE -> preferences.backgroundScanModeLastUpdated
            NetworkSettingNames.BACKGROUND_SCAN_INTERVAL -> preferences.backgroundScanIntervalLastUpdated
            NetworkSettingNames.UNIT_TEMPERATURE -> preferences.temperatureUnitLastUpdated
            NetworkSettingNames.UNIT_HUMIDITY -> preferences.humidityUnitLastUpdated
            NetworkSettingNames.UNIT_PRESSURE -> preferences.pressureUnitLastUpdated
            NetworkSettingNames.ACCURACY_TEMPERATURE -> preferences.temperatureAccuracyLastUpdated
            NetworkSettingNames.ACCURACY_HUMIDITY -> preferences.humidityAccuracyLastUpdated
            NetworkSettingNames.ACCURACY_HUMIDITY_RELATIVE -> preferences.relativeHumidityAccuracyLastUpdated
            NetworkSettingNames.ACCURACY_HUMIDITY_ABSOLUTE -> preferences.absoluteHumidityAccuracyLastUpdated
            NetworkSettingNames.ACCURACY_HUMIDITY_DEW_POINT -> preferences.dewPointAccuracyLastUpdated
            NetworkSettingNames.ACCURACY_PRESSURE -> preferences.pressureAccuracyLastUpdated
            NetworkSettingNames.ACCURACY_PM -> preferences.pmAccuracyLastUpdated
            NetworkSettingNames.ACCURACY_ACCELERATION -> preferences.accelerationAccuracyLastUpdated
            NetworkSettingNames.ACCURACY_VOLTAGE -> preferences.voltageAccuracyLastUpdated
            NetworkSettingNames.DASHBOARD_TYPE -> preferences.dashboardTypeLastUpdated
            NetworkSettingNames.DASHBOARD_TAP_ACTION -> preferences.dashboardTapActionLastUpdated
            NetworkSettingNames.CLOUD_MODE_ENABLED -> preferences.cloudModeEnabledLastUpdated
            NetworkSettingNames.CHART_SHOW_ALL_POINTS -> preferences.graphShowAllPointLastUpdated
            NetworkSettingNames.CHART_DRAW_DOTS -> preferences.graphDrawDotsLastUpdated
            NetworkSettingNames.SENSOR_ORDER -> preferences.sensorsOrderLastUpdated
            NetworkSettingNames.DISABLE_EMAIL_NOTIFICATIONS -> preferences.disableEmailNotificationsLastUpdated
            NetworkSettingNames.DISABLE_PUSH_NOTIFICATIONS -> preferences.disablePushNotificationsLastUpdated
            NetworkSettingNames.DISABLE_TELEGRAM_NOTIFICATIONS -> preferences.disableTelegramNotificationsLastUpdated
            NetworkSettingNames.TIPS_ALLOWED -> preferences.tipsAllowedLastUpdated
            NetworkSettingNames.MARKETING_PERMISSION -> preferences.marketingPermissionLastUpdated
            else -> 0L
        }
    }

    fun isDisableTelegramNotifications(): Boolean =
        preferences.disableTelegramNotifications

    fun isBannerDisabled(): Boolean =
        preferences.bannerDisabledForVersion == BuildConfig.VERSION_NAME

    fun disableBanner() {
        preferences.bannerDisabledForVersion = BuildConfig.VERSION_NAME
    }

    fun isBluetoothPermissionRequested(): Boolean =
        preferences.bluetoothPermissionRequested

    fun bluetoothPermissionRequested() {
        preferences.bluetoothPermissionRequested = true
    }
}
