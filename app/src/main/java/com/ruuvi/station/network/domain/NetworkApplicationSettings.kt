package com.ruuvi.station.network.domain

import com.ruuvi.station.app.locale.LocaleInteractor
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.dashboard.DashboardTapAction
import com.ruuvi.station.dashboard.DashboardType
import com.ruuvi.station.network.data.response.NetworkUserSettings
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.UnitType.*
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.util.extensions.toBooleanExtra
import com.ruuvi.station.util.extensions.toInt
import timber.log.Timber

class NetworkApplicationSettings (
    private val tokenRepository: NetworkTokenRepository,
    private val networkRepository: RuuviNetworkRepository,
    private val networkInteractor: RuuviNetworkInteractor,
    private val preferencesRepository: PreferencesRepository,
    private val unitsConverter: UnitsConverter,
    private val localeInteractor: LocaleInteractor
    ) {

    private fun getToken() = tokenRepository.getTokenInfo()

    suspend fun updateSettingsFromNetwork() {
        getToken()?.token?.let { token ->
            val response = networkRepository.getUserSettings(token)
            if (response?.data != null && response.isSuccess()) {
                val cloudSettings = response.data.settings
                if (cloudSettings.isEmpty()) {
                    syncAllLocalSettingsToCloud()
                    return
                }
                syncLocalAndCloudSettings(cloudSettings)
            }
        }
    }

    private fun syncLocalAndCloudSettings(settings: NetworkUserSettings) {
        TRACKED_SETTINGS.forEach { settingName ->
            val cloudValue = settings.valueFor(settingName)
            val cloudTimestamp = settings.timestampFor(settingName)
            val localTimestamp = preferencesRepository.getNetworkSettingLastUpdated(settingName)

            when {
                cloudValue == null -> {
                    if (localTimestamp > 0L) {
                        pushLocalSettingToCloud(settingName, localTimestamp)
                    }
                }
                cloudTimestamp > localTimestamp -> {
                    applyCloudSetting(settingName, settings)
                    preferencesRepository.setNetworkSettingLastUpdated(settingName, cloudTimestamp)
                }
                localTimestamp > cloudTimestamp -> {
                    pushLocalSettingToCloud(settingName, localTimestamp)
                }
                cloudTimestamp == 0L -> {
                    applyCloudSetting(settingName, settings)
                    preferencesRepository.setNetworkSettingLastUpdated(
                        settingName,
                        System.currentTimeMillis() / 1000
                    )
                }
            }
        }
    }

    private fun syncAllLocalSettingsToCloud() {
        TRACKED_SETTINGS.forEach { settingName ->
            val localTimestamp = ensureLocalSettingTimestamp(settingName)
            pushLocalSettingToCloud(settingName, localTimestamp)
        }
    }

    private fun applyCloudSetting(settingName: String, settings: NetworkUserSettings) {
        when (settingName) {
            BACKGROUND_SCAN_MODE -> applyBackgroundScanMode(settings)
            BACKGROUND_SCAN_INTERVAL -> applyBackgroundScanInterval(settings)
            UNIT_TEMPERATURE -> applyTemperatureUnit(settings)
            UNIT_HUMIDITY -> applyHumidityUnit(settings)
            UNIT_PRESSURE -> applyPressureUnit(settings)
            ACCURACY_TEMPERATURE -> applyTemperatureAccuracy(settings)
            ACCURACY_HUMIDITY -> applyHumidityAccuracy(settings)
            ACCURACY_HUMIDITY_RELATIVE -> applyRelativeHumidityAccuracy(settings)
            ACCURACY_HUMIDITY_ABSOLUTE -> applyAbsoluteHumidityAccuracy(settings)
            ACCURACY_HUMIDITY_DEW_POINT -> applyDewPointAccuracy(settings)
            ACCURACY_PRESSURE -> applyPressureAccuracy(settings)
            ACCURACY_PM -> applyPmAccuracy(settings)
            ACCURACY_ACCELERATION -> applyAccelerationAccuracy(settings)
            ACCURACY_VOLTAGE -> applyVoltageAccuracy(settings)
            DASHBOARD_TYPE -> applyDashboardType(settings)
            DASHBOARD_TAP_ACTION -> applyDashboardTapAction(settings)
            PROFILE_LANGUAGE_CODE -> Unit
            CLOUD_MODE_ENABLED -> applyCloudModeEnabled(settings)
            CHART_SHOW_ALL_POINTS -> applyChartShowAllPoints(settings)
            CHART_DRAW_DOTS -> applyChartDrawDots(settings)
            SENSOR_ORDER -> applySensorsOrder(settings)
            DISABLE_EMAIL_NOTIFICATIONS -> applyDisableEmailNotifications(settings)
            DISABLE_PUSH_NOTIFICATIONS -> applyDisablePushNotifications(settings)
            DISABLE_TELEGRAM_NOTIFICATIONS -> applyDisableTelegramNotifications(settings)
            TIPS_ALLOWED -> applyMarketingPermission(settings)
        }
    }

    private fun pushLocalSettingToCloud(settingName: String, timestamp: Long) {
        if (!networkInteractor.signedIn) return
        val value = localSettingValue(settingName) ?: return
        networkInteractor.updateUserSetting(settingName, value, timestamp)
    }

    private fun localSettingValue(settingName: String): String? {
        return when (settingName) {
            BACKGROUND_SCAN_MODE -> preferencesRepository.getBackgroundScanMode().value.toString()
            BACKGROUND_SCAN_INTERVAL -> preferencesRepository.getBackgroundScanInterval().toString()
            UNIT_TEMPERATURE -> unitsConverter.getTemperatureUnit().unitCode
            UNIT_HUMIDITY -> unitsConverter.getHumidityUnit().unitCode
            UNIT_PRESSURE -> unitsConverter.getPressureUnit().unitCode
            ACCURACY_TEMPERATURE -> preferencesRepository.getTemperatureAccuracy().code.toString()
            ACCURACY_HUMIDITY -> preferencesRepository.getHumidityAccuracy().code.toString()
            ACCURACY_HUMIDITY_RELATIVE -> preferencesRepository.getRelativeHumidityAccuracy().code.toString()
            ACCURACY_HUMIDITY_ABSOLUTE -> preferencesRepository.getAbsoluteHumidityAccuracy().code.toString()
            ACCURACY_HUMIDITY_DEW_POINT -> preferencesRepository.getDewPointAccuracy().code.toString()
            ACCURACY_PRESSURE -> preferencesRepository.getPressureAccuracy().code.toString()
            ACCURACY_PM -> preferencesRepository.getPmAccuracy().code.toString()
            ACCURACY_ACCELERATION -> preferencesRepository.getAccelerationAccuracy().code.toString()
            ACCURACY_VOLTAGE -> preferencesRepository.getVoltageAccuracy().code.toString()
            DASHBOARD_TYPE -> preferencesRepository.getDashboardType().code
            DASHBOARD_TAP_ACTION -> preferencesRepository.getDashboardTapAction().code
            PROFILE_LANGUAGE_CODE -> localeInteractor.getCurrentLocaleLanguage()
            CLOUD_MODE_ENABLED -> preferencesRepository.isCloudModeEnabled().toString()
            CHART_SHOW_ALL_POINTS -> preferencesRepository.isShowAllGraphPoint().toString()
            CHART_DRAW_DOTS -> preferencesRepository.graphDrawDots().toString()
            SENSOR_ORDER -> preferencesRepository.getSortedSensors().takeIf { it.isNotEmpty() }
            DISABLE_EMAIL_NOTIFICATIONS -> preferencesRepository.isDisableEmailNotifications().toInt().toString()
            DISABLE_PUSH_NOTIFICATIONS -> preferencesRepository.isDisablePushNotifications().toInt().toString()
            DISABLE_TELEGRAM_NOTIFICATIONS -> preferencesRepository.isDisableTelegramNotifications().toInt().toString()
            TIPS_ALLOWED -> preferencesRepository.getMarketingPermission().toInt().toString()
            else -> null
        }
    }

    private fun ensureLocalSettingTimestamp(settingName: String): Long {
        val localTimestamp = preferencesRepository.getNetworkSettingLastUpdated(settingName)
        return if (localTimestamp > 0L) {
            localTimestamp
        } else {
            markLocalSettingUpdated(settingName)
        }
    }

    private fun markLocalSettingUpdated(settingName: String): Long {
        val timestamp = System.currentTimeMillis() / 1000
        preferencesRepository.setNetworkSettingLastUpdated(settingName, timestamp)
        return timestamp
    }

    private fun applyBackgroundScanMode(settings: NetworkUserSettings) {
        val mode = settings.BACKGROUND_SCAN_MODE?.toIntOrNull()
        if (mode != null) {
            BackgroundScanModes.fromInt(mode)?.let {
                Timber.d("NetworkApplicationSettings-applyBackgroundScanMode: $it")
                preferencesRepository.setBackgroundScanMode(it)
            }
        }
    }

    private fun applyBackgroundScanInterval(settings: NetworkUserSettings) {
        settings.BACKGROUND_SCAN_INTERVAL?.toIntOrNull()?.let {
            Timber.d("NetworkApplicationSettings-applyBackgroundScanInterval: $it")
            preferencesRepository.setBackgroundScanInterval(it)
        }
    }

    private fun applyTemperatureUnit(settings: NetworkUserSettings) {
        settings.UNIT_TEMPERATURE?.let {
            val unit = TemperatureUnit.getByCode(it)
            if (unit != null) {
                Timber.d("NetworkApplicationSettings-applyTemperatureUnit: $unit")
                preferencesRepository.setTemperatureUnit(unit)
            }
        }
    }

    private fun applyHumidityUnit(settings: NetworkUserSettings) {
        settings.UNIT_HUMIDITY?.let {
            val unit = HumidityUnit.getByCode(it)
            if (unit != null) {
                Timber.d("NetworkApplicationSettings-applyHumidityUnit: $unit")
                preferencesRepository.setHumidityUnit(unit)
            }
        }
    }

    private fun applyPressureUnit(settings: NetworkUserSettings) {
        settings.UNIT_PRESSURE?.let {
            val unit = PressureUnit.getByCode(it)
            if (unit != null) {
                Timber.d("NetworkApplicationSettings-applyPressureUnit: $unit")
                preferencesRepository.setPressureUnit(unit)
            }
        }
    }

    private fun applyDashboardType(settings: NetworkUserSettings) {
        if (settings.DASHBOARD_TYPE != null && DashboardType.isValidCode(settings.DASHBOARD_TYPE)) {
            Timber.d("NetworkApplicationSettings-applyDashboardType: ${settings.DASHBOARD_TYPE}")
            preferencesRepository.updateDashboardType(DashboardType.getByCode(settings.DASHBOARD_TYPE))
        }
    }

    private fun applyDashboardTapAction(settings: NetworkUserSettings) {
        if (settings.DASHBOARD_TAP_ACTION != null && DashboardTapAction.isValidCode(settings.DASHBOARD_TAP_ACTION)) {
            Timber.d("NetworkApplicationSettings-applyDashboardTapAction: ${settings.DASHBOARD_TAP_ACTION}")
            preferencesRepository.updateDashboardTapAction(DashboardTapAction.getByCode(settings.DASHBOARD_TAP_ACTION))
        }
    }

    private fun applyCloudModeEnabled(settings: NetworkUserSettings) {
        if (settings.CLOUD_MODE_ENABLED != null) {
            settings.CLOUD_MODE_ENABLED.toBooleanExtra().let {
                Timber.d("NetworkApplicationSettings-applyCloudModeEnabled: $it")
                preferencesRepository.setIsCloudModeEnabled(it)
            }
        }
    }

    private fun applyChartShowAllPoints(settings: NetworkUserSettings) {
        if (settings.CHART_SHOW_ALL_POINTS != null) {
            settings.CHART_SHOW_ALL_POINTS.toBooleanExtra().let {
                Timber.d("NetworkApplicationSettings-applyChartShowAllPoints: $it")
                preferencesRepository.setIsShowAllGraphPoint(it)
            }
        }
    }

    private fun applyChartDrawDots(settings: NetworkUserSettings) {
        if (settings.CHART_DRAW_DOTS != null) {
            settings.CHART_DRAW_DOTS.toBooleanExtra().let {
                Timber.d("NetworkApplicationSettings-applyChartDrawDots: $it")
                preferencesRepository.setGraphDrawDots(it)
            }
        }
    }

    private fun applyTemperatureAccuracy(settings: NetworkUserSettings) {
        settings.ACCURACY_TEMPERATURE?.toIntOrNull()?.let {
            val accuracy = Accuracy.getByCode(it)
            if (accuracy != null) {
                Timber.d("NetworkApplicationSettings-applyTemperatureAccuracy: $accuracy")
                preferencesRepository.setTemperatureAccuracy(accuracy)
            }
        }
    }

    private fun applyHumidityAccuracy(settings: NetworkUserSettings) {
        settings.ACCURACY_HUMIDITY?.toIntOrNull()?.let {
            val accuracy = Accuracy.getByCode(it)
            if (accuracy != null) {
                Timber.d("NetworkApplicationSettings-applyHumidityAccuracy: $accuracy")
                preferencesRepository.setHumidityAccuracy(accuracy)
            }
        }
    }

    private fun applyRelativeHumidityAccuracy(settings: NetworkUserSettings) {
        settings.ACCURACY_HUMIDITY_RELATIVE?.toIntOrNull()?.let {
            val accuracy = Accuracy.getByCode(it)
            if (accuracy != null) {
                Timber.d("NetworkApplicationSettings-applyRelativeHumidityAccuracy: $accuracy")
                preferencesRepository.setRelativeHumidityAccuracy(accuracy)
            }
        }
    }

    private fun applyAbsoluteHumidityAccuracy(settings: NetworkUserSettings) {
        settings.ACCURACY_HUMIDITY_ABSOLUTE?.toIntOrNull()?.let {
            val accuracy = Accuracy.getByCode(it)
            if (accuracy != null) {
                Timber.d("NetworkApplicationSettings-applyAbsoluteHumidityAccuracy: $accuracy")
                preferencesRepository.setAbsoluteHumidityAccuracy(accuracy)
            }
        }
    }

    private fun applyDewPointAccuracy(settings: NetworkUserSettings) {
        settings.ACCURACY_HUMIDITY_DEW_POINT?.toIntOrNull()?.let {
            val accuracy = Accuracy.getByCode(it)
            if (accuracy != null) {
                Timber.d("NetworkApplicationSettings-applyDewPointAccuracy: $accuracy")
                preferencesRepository.setDewPointAccuracy(accuracy)
            }
        }
    }

    private fun applyPressureAccuracy(settings: NetworkUserSettings) {
        settings.ACCURACY_PRESSURE?.toIntOrNull()?.let {
            val accuracy = Accuracy.getByCode(it)
            if (accuracy != null) {
                Timber.d("NetworkApplicationSettings-applyPressureAccuracy: $accuracy")
                preferencesRepository.setPressureAccuracy(accuracy)
            }
        }
    }

    private fun applyPmAccuracy(settings: NetworkUserSettings) {
        settings.ACCURACY_PM?.toIntOrNull()?.let {
            val accuracy = Accuracy.getByCode(it)
            if (accuracy != null) {
                Timber.d("NetworkApplicationSettings-applyPmAccuracy: $accuracy")
                preferencesRepository.setPmAccuracy(accuracy)
            }
        }
    }

    private fun applyAccelerationAccuracy(settings: NetworkUserSettings) {
        settings.ACCURACY_ACCELERATION?.toIntOrNull()?.let {
            val accuracy = Accuracy.getByCode(it)
            if (accuracy != null) {
                Timber.d("NetworkApplicationSettings-applyAccelerationAccuracy: $accuracy")
                preferencesRepository.setAccelerationAccuracy(accuracy)
            }
        }
    }

    private fun applyVoltageAccuracy(settings: NetworkUserSettings) {
        settings.ACCURACY_VOLTAGE?.toIntOrNull()?.let {
            val accuracy = Accuracy.getByCode(it)
            if (accuracy != null) {
                Timber.d("NetworkApplicationSettings-applyVoltageAccuracy: $accuracy")
                preferencesRepository.setVoltageAccuracy(accuracy)
            }
        }
    }

    private fun applySensorsOrder(settings: NetworkUserSettings) {
        settings.SENSOR_ORDER?.let {sensorsOrder ->
            Timber.d("NetworkApplicationSettings-applySensorsOrder: $sensorsOrder")
            preferencesRepository.setSortedSensors(sensorsOrder)
        }
    }

    private fun applyDisableEmailNotifications(settings: NetworkUserSettings) {
        if (settings.DISABLE_EMAIL_NOTIFICATIONS != null) {
            val disableEmail = settings.DISABLE_EMAIL_NOTIFICATIONS.toBooleanExtra()
            Timber.d("NetworkApplicationSettings-applyDisableEmailNotifications: $disableEmail")
            preferencesRepository.setDisableEmailNotifications(disableEmail)
        }
    }

    private fun applyDisablePushNotifications(settings: NetworkUserSettings) {
        if (settings.DISABLE_PUSH_NOTIFICATIONS != null) {
            val disablePush = settings.DISABLE_PUSH_NOTIFICATIONS.toBooleanExtra()
            Timber.d("NetworkApplicationSettings-applyDisablePushNotifications: $disablePush")
            preferencesRepository.setDisablePushNotifications(disablePush)
        }
    }

    private fun applyDisableTelegramNotifications(settings: NetworkUserSettings) {
        if (settings.DISABLE_TELEGRAM_NOTIFICATIONS != null) {
            val disableTelegram = settings.DISABLE_TELEGRAM_NOTIFICATIONS.toBooleanExtra()
            Timber.d("NetworkApplicationSettings-applyDisableTelegramNotifications: $disableTelegram")
            preferencesRepository.setDisableTelegramNotifications(disableTelegram)
        }
    }

    private fun applyMarketingPermission(settings: NetworkUserSettings) {
        if (settings.MARKETING_PERMISSION != null) {
            val marketingPermission = settings.MARKETING_PERMISSION.toBooleanExtra()
            Timber.d("NetworkApplicationSettings-applyMarketingPermission: $marketingPermission")
            preferencesRepository.setMarketingPermission(marketingPermission)
        }
    }

    fun updateBackgroundScanMode() {
        val timestamp = markLocalSettingUpdated(BACKGROUND_SCAN_MODE)
        if (networkInteractor.signedIn) {
            Timber.d("NetworkApplicationSettings-updateBackgroundScanMode: ${preferencesRepository.getBackgroundScanMode().value}")
            networkInteractor.updateUserSetting(
                BACKGROUND_SCAN_MODE,
                preferencesRepository.getBackgroundScanMode().value.toString(),
                timestamp
            )
        }
    }

    fun updateTemperatureUnit() {
        val timestamp = markLocalSettingUpdated(UNIT_TEMPERATURE)
        if (networkInteractor.signedIn) {
            Timber.d("NetworkApplicationSettings-updateTemperatureUnit: ${unitsConverter.getTemperatureUnit().unitCode}")
            networkInteractor.updateUserSetting(
                UNIT_TEMPERATURE,
                unitsConverter.getTemperatureUnit().unitCode,
                timestamp
            )
        }
    }

    fun updateTemperatureAccuracy() {
        val timestamp = markLocalSettingUpdated(ACCURACY_TEMPERATURE)
        if (networkInteractor.signedIn) {
            Timber.d("NetworkApplicationSettings-updateTemperatureAccuracy: ${preferencesRepository.getTemperatureAccuracy().code}")
            networkInteractor.updateUserSetting(
                ACCURACY_TEMPERATURE,
                preferencesRepository.getTemperatureAccuracy().code.toString(),
                timestamp
            )
        }
    }

    fun updateHumidityUnit() {
        val timestamp = markLocalSettingUpdated(UNIT_HUMIDITY)
        if (networkInteractor.signedIn) {
            Timber.d("NetworkApplicationSettings-updateHumidityUnit: ${unitsConverter.getHumidityUnit().unitCode}")
            networkInteractor.updateUserSetting(
                UNIT_HUMIDITY,
                unitsConverter.getHumidityUnit().unitCode,
                timestamp
            )
        }
    }

    fun updateHumidityAccuracy() {
        val timestamp = markLocalSettingUpdated(ACCURACY_HUMIDITY)
        if (networkInteractor.signedIn) {
            Timber.d("NetworkApplicationSettings-updateHumidityAccuracy: ${preferencesRepository.getHumidityAccuracy().code}")
            networkInteractor.updateUserSetting(
                ACCURACY_HUMIDITY,
                preferencesRepository.getHumidityAccuracy().code.toString(),
                timestamp
            )
        }
    }

    fun updateRelativeHumidityAccuracy() {
        val timestamp = markLocalSettingUpdated(ACCURACY_HUMIDITY_RELATIVE)
        if (networkInteractor.signedIn) {
            Timber.d("NetworkApplicationSettings-updateRelativeHumidityAccuracy: ${preferencesRepository.getRelativeHumidityAccuracy().code}")
            networkInteractor.updateUserSetting(
                ACCURACY_HUMIDITY_RELATIVE,
                preferencesRepository.getRelativeHumidityAccuracy().code.toString(),
                timestamp
            )
        }
    }

    fun updateAbsoluteHumidityAccuracy() {
        val timestamp = markLocalSettingUpdated(ACCURACY_HUMIDITY_ABSOLUTE)
        if (networkInteractor.signedIn) {
            Timber.d("NetworkApplicationSettings-updateAbsoluteHumidityAccuracy: ${preferencesRepository.getAbsoluteHumidityAccuracy().code}")
            networkInteractor.updateUserSetting(
                ACCURACY_HUMIDITY_ABSOLUTE,
                preferencesRepository.getAbsoluteHumidityAccuracy().code.toString(),
                timestamp
            )
        }
    }

    fun updateDewPointAccuracy() {
        val timestamp = markLocalSettingUpdated(ACCURACY_HUMIDITY_DEW_POINT)
        if (networkInteractor.signedIn) {
            Timber.d("NetworkApplicationSettings-updateDewPointAccuracy: ${preferencesRepository.getDewPointAccuracy().code}")
            networkInteractor.updateUserSetting(
                ACCURACY_HUMIDITY_DEW_POINT,
                preferencesRepository.getDewPointAccuracy().code.toString(),
                timestamp
            )
        }
    }

    fun updateDashboardType() {
        val timestamp = markLocalSettingUpdated(DASHBOARD_TYPE)
        if (networkInteractor.signedIn) {
            Timber.d("NetworkApplicationSettings-updateDashboardType: ${preferencesRepository.getDashboardType().code}")
            networkInteractor.updateUserSetting(
                DASHBOARD_TYPE,
                preferencesRepository.getDashboardType().code,
                timestamp
            )
        }
    }

    fun updateCloudModeEnabled() {
        val timestamp = markLocalSettingUpdated(CLOUD_MODE_ENABLED)
        if (networkInteractor.signedIn) {
            Timber.d("NetworkApplicationSettings-updateCloudModeEnabled: ${preferencesRepository.isCloudModeEnabled()}")
            networkInteractor.updateUserSetting(
                CLOUD_MODE_ENABLED,
                preferencesRepository.isCloudModeEnabled().toString(),
                timestamp
            )
        }
    }

    fun updateBackgroundScanInterval() {
        val timestamp = markLocalSettingUpdated(BACKGROUND_SCAN_INTERVAL)
        if (networkInteractor.signedIn) {
            Timber.d("NetworkApplicationSettings-updateBackgroundScanInterval: ${preferencesRepository.getBackgroundScanInterval()}")
            networkInteractor.updateUserSetting(
                BACKGROUND_SCAN_INTERVAL,
                preferencesRepository.getBackgroundScanInterval().toString(),
                timestamp
            )
        }
    }

    fun updateChartShowAllPoints() {
        val timestamp = markLocalSettingUpdated(CHART_SHOW_ALL_POINTS)
        if (networkInteractor.signedIn) {
            Timber.d("NetworkApplicationSettings-updateChartShowAllPoints: ${preferencesRepository.isShowAllGraphPoint()}")
            networkInteractor.updateUserSetting(
                CHART_SHOW_ALL_POINTS,
                preferencesRepository.isShowAllGraphPoint().toString(),
                timestamp
            )
        }
    }

    fun updateChartDrawDots() {
        val timestamp = markLocalSettingUpdated(CHART_DRAW_DOTS)
        if (networkInteractor.signedIn) {
            Timber.d("NetworkApplicationSettings-updateChartDrawDots: ${preferencesRepository.graphDrawDots()}")
            networkInteractor.updateUserSetting(
                CHART_DRAW_DOTS,
                preferencesRepository.graphDrawDots().toString(),
                timestamp
            )
        }
    }

    fun updatePressureUnit() {
        val timestamp = markLocalSettingUpdated(UNIT_PRESSURE)
        if (networkInteractor.signedIn) {
            Timber.d("NetworkApplicationSettings-updatePressureUnit: ${unitsConverter.getPressureUnit().unitCode}")
            networkInteractor.updateUserSetting(
                UNIT_PRESSURE,
                unitsConverter.getPressureUnit().unitCode,
                timestamp
            )
        }
    }

    fun updatePressureAccuracy() {
        val timestamp = markLocalSettingUpdated(ACCURACY_PRESSURE)
        if (networkInteractor.signedIn) {
            Timber.d("NetworkApplicationSettings-updatePressureAccuracy: ${preferencesRepository.getPressureAccuracy().code}")
            networkInteractor.updateUserSetting(
                ACCURACY_PRESSURE,
                preferencesRepository.getPressureAccuracy().code.toString(),
                timestamp
            )
        }
    }

    fun updatePmAccuracy() {
        val timestamp = markLocalSettingUpdated(ACCURACY_PM)
        if (networkInteractor.signedIn) {
            Timber.d("NetworkApplicationSettings-updatePmAccuracy: ${preferencesRepository.getPmAccuracy().code}")
            networkInteractor.updateUserSetting(
                ACCURACY_PM,
                preferencesRepository.getPmAccuracy().code.toString(),
                timestamp
            )
        }
    }

    fun updateAccelerationAccuracy() {
        val timestamp = markLocalSettingUpdated(ACCURACY_ACCELERATION)
        if (networkInteractor.signedIn) {
            Timber.d("NetworkApplicationSettings-updateAccelerationAccuracy: ${preferencesRepository.getAccelerationAccuracy().code}")
            networkInteractor.updateUserSetting(
                ACCURACY_ACCELERATION,
                preferencesRepository.getAccelerationAccuracy().code.toString(),
                timestamp
            )
        }
    }

    fun updateVoltageAccuracy() {
        val timestamp = markLocalSettingUpdated(ACCURACY_VOLTAGE)
        if (networkInteractor.signedIn) {
            Timber.d("NetworkApplicationSettings-updateVoltageAccuracy: ${preferencesRepository.getVoltageAccuracy().code}")
            networkInteractor.updateUserSetting(
                ACCURACY_VOLTAGE,
                preferencesRepository.getVoltageAccuracy().code.toString(),
                timestamp
            )
        }
    }

    fun updateDashboardTapAction() {
        val timestamp = markLocalSettingUpdated(DASHBOARD_TAP_ACTION)
        if (networkInteractor.signedIn) {
            Timber.d("NetworkApplicationSettings-updateDashboardTapAction: ${preferencesRepository.getDashboardTapAction().code}")
            networkInteractor.updateUserSetting(
                DASHBOARD_TAP_ACTION,
                preferencesRepository.getDashboardTapAction().code,
                timestamp
            )
        }
    }

    fun updateProfileLanguage() {
        val timestamp = markLocalSettingUpdated(PROFILE_LANGUAGE_CODE)
        if (networkInteractor.signedIn) {
            val language = localeInteractor.getCurrentLocaleLanguage()
            Timber.d("NetworkApplicationSettings-updateProfileLanguage: $language")
            networkInteractor.updateUserSetting(
                PROFILE_LANGUAGE_CODE,
                language,
                timestamp
            )
        }
    }

    fun updateSensorsOrder() {
        val timestamp = markLocalSettingUpdated(SENSOR_ORDER)
        if (networkInteractor.signedIn) {
            val sensorsOrder = preferencesRepository.getSortedSensors()
            if (sensorsOrder.isNotEmpty()) {
                Timber.d("NetworkApplicationSettings-updateSensorsOrder: $sensorsOrder")
                networkInteractor.updateUserSetting(
                    SENSOR_ORDER,
                    sensorsOrder,
                    timestamp
                )
            }
        }
    }

    fun updateDisableEmailNotifications() {
        val timestamp = markLocalSettingUpdated(DISABLE_EMAIL_NOTIFICATIONS)
        if (networkInteractor.signedIn) {
            val disableEmailNotifications = preferencesRepository.isDisableEmailNotifications()
            Timber.d("NetworkApplicationSettings-updateDisableEmailNotifications: $disableEmailNotifications")
            networkInteractor.updateUserSetting(
                DISABLE_EMAIL_NOTIFICATIONS,
                disableEmailNotifications.toInt().toString(),
                timestamp
            )
        }
    }

    fun updateMarketingPermission() {
        val timestamp = markLocalSettingUpdated(TIPS_ALLOWED)
        if (networkInteractor.signedIn) {
            val marketingPermission = preferencesRepository.getMarketingPermission()
            Timber.d("NetworkApplicationSettings-updateMarketingPermission: $marketingPermission")
            networkInteractor.updateUserSetting(
                TIPS_ALLOWED,
                marketingPermission.toInt().toString(),
                timestamp
            )
        }
    }

    fun updateDisablePushNotifications() {
        val timestamp = markLocalSettingUpdated(DISABLE_PUSH_NOTIFICATIONS)
        if (networkInteractor.signedIn) {
            val disablePushNotifications = preferencesRepository.isDisablePushNotifications()
            Timber.d("NetworkApplicationSettings-updateDisablePushNotifications: $disablePushNotifications")
            networkInteractor.updateUserSetting(
                DISABLE_PUSH_NOTIFICATIONS,
                disablePushNotifications.toInt().toString(),
                timestamp
            )
        }
    }

    fun updateDisableTelegramNotifications() {
        val timestamp = markLocalSettingUpdated(DISABLE_TELEGRAM_NOTIFICATIONS)
        if (networkInteractor.signedIn) {
            val disableTelegramNotifications = preferencesRepository.isDisableTelegramNotifications()
            Timber.d("NetworkApplicationSettings-updateDisableTelegramNotifications: $disableTelegramNotifications")
            networkInteractor.updateUserSetting(
                DISABLE_TELEGRAM_NOTIFICATIONS,
                disableTelegramNotifications.toInt().toString(),
                timestamp
            )
        }
    }

    companion object {
        val BACKGROUND_SCAN_MODE = "BACKGROUND_SCAN_MODE"
        val BACKGROUND_SCAN_INTERVAL = "BACKGROUND_SCAN_INTERVAL"
        val UNIT_TEMPERATURE = "UNIT_TEMPERATURE"
        val UNIT_HUMIDITY = "UNIT_HUMIDITY"
        val UNIT_PRESSURE = "UNIT_PRESSURE"
        val ACCURACY_TEMPERATURE = "ACCURACY_TEMPERATURE"
        val ACCURACY_HUMIDITY = "ACCURACY_HUMIDITY"
        val ACCURACY_HUMIDITY_RELATIVE = "ACCURACY_HUMIDITY_RELATIVE"
        val ACCURACY_HUMIDITY_ABSOLUTE = "ACCURACY_HUMIDITY_ABSOLUTE"
        val ACCURACY_HUMIDITY_DEW_POINT = "ACCURACY_HUMIDITY_DEW_POINT"
        val ACCURACY_PRESSURE = "ACCURACY_PRESSURE"
        val ACCURACY_PM = "ACCURACY_PM"
        val ACCURACY_ACCELERATION = "ACCURACY_ACCELERATION"
        val ACCURACY_VOLTAGE = "ACCURACY_VOLTAGE"
        val DASHBOARD_TYPE = "DASHBOARD_TYPE"
        val DASHBOARD_TAP_ACTION = "DASHBOARD_TAP_ACTION"
        val PROFILE_LANGUAGE_CODE = "PROFILE_LANGUAGE_CODE"
        val CLOUD_MODE_ENABLED = "CLOUD_MODE_ENABLED"
        val CHART_SHOW_ALL_POINTS = "CHART_SHOW_ALL_POINTS"
        val CHART_DRAW_DOTS = "CHART_DRAW_DOTS"
        val SENSOR_ORDER = "SENSOR_ORDER"
        val DISABLE_EMAIL_NOTIFICATIONS = "DISABLE_EMAIL_NOTIFICATIONS"
        val DISABLE_PUSH_NOTIFICATIONS = "DISABLE_PUSH_NOTIFICATIONS"
        val DISABLE_TELEGRAM_NOTIFICATIONS = "DISABLE_TELEGRAM_NOTIFICATIONS"
        val TIPS_ALLOWED = "TIPS_ALLOWED"
        private val TRACKED_SETTINGS = listOf(
            BACKGROUND_SCAN_MODE,
            BACKGROUND_SCAN_INTERVAL,
            UNIT_TEMPERATURE,
            UNIT_HUMIDITY,
            UNIT_PRESSURE,
            ACCURACY_TEMPERATURE,
            ACCURACY_HUMIDITY,
            ACCURACY_HUMIDITY_RELATIVE,
            ACCURACY_HUMIDITY_ABSOLUTE,
            ACCURACY_HUMIDITY_DEW_POINT,
            ACCURACY_PRESSURE,
            ACCURACY_PM,
            ACCURACY_ACCELERATION,
            ACCURACY_VOLTAGE,
            DASHBOARD_TYPE,
            DASHBOARD_TAP_ACTION,
            PROFILE_LANGUAGE_CODE,
            CLOUD_MODE_ENABLED,
            CHART_SHOW_ALL_POINTS,
            CHART_DRAW_DOTS,
            SENSOR_ORDER,
            DISABLE_EMAIL_NOTIFICATIONS,
            DISABLE_PUSH_NOTIFICATIONS,
            DISABLE_TELEGRAM_NOTIFICATIONS,
            TIPS_ALLOWED
        )
    }
}
