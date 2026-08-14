package com.ruuvi.station.app.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.ruuvi.station.app.ui.DarkModeState
import com.ruuvi.station.dashboard.DashboardTapAction
import com.ruuvi.station.dashboard.DashboardType
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.UnitType.*
import com.ruuvi.station.util.BackgroundScanModes
import androidx.core.content.edit

class Preferences (val context: Context) {

    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(
            context
        )
    }

    var backgroundScanInterval: Int
        get() = sharedPreferences.getInt(PREF_BACKGROUND_SCAN_INTERVAL, DEFAULT_SCAN_INTERVAL)
        set(interval) {
            sharedPreferences.edit { putInt(PREF_BACKGROUND_SCAN_INTERVAL, interval) }
        }

    var backgroundScanIntervalLastUpdated: Long
        get() = sharedPreferences.getLong(PREF_BACKGROUND_SCAN_INTERVAL_LAST_UPDATED, 0L)
        set(value) {
            sharedPreferences.edit { putLong(PREF_BACKGROUND_SCAN_INTERVAL_LAST_UPDATED, value) }
        }

    var backgroundScanMode: BackgroundScanModes
        get() = BackgroundScanModes.fromInt(
            sharedPreferences.getInt(
                PREF_BACKGROUND_SCAN_MODE,
                BackgroundScanModes.BACKGROUND.value
            )
        )
            ?: BackgroundScanModes.BACKGROUND
        set(mode) {
            sharedPreferences.edit { putInt(PREF_BACKGROUND_SCAN_MODE, mode.value) }
        }

    var backgroundScanModeLastUpdated: Long
        get() = sharedPreferences.getLong(
            PREF_BACKGROUND_SCAN_MODE_LAST_UPDATED,
            0L
        )
        set(value) {
            sharedPreferences.edit { putLong(PREF_BACKGROUND_SCAN_MODE_LAST_UPDATED, value) }
        }

    var isFirstStart: Boolean
        get() = sharedPreferences.getBoolean(PREF_FIRST_START, true)
        set(enabled) {
            sharedPreferences.edit { putBoolean(PREF_FIRST_START, enabled) }
        }

    var isFirstGraphVisit: Boolean
        get() = sharedPreferences.getBoolean(PREF_FIRST_GRAPH, true)
        set(enabled) {
            sharedPreferences.edit { putBoolean(PREF_FIRST_GRAPH, enabled) }
        }

    var temperatureUnit: TemperatureUnit
        get() {
            return when (sharedPreferences.getString(
                PREF_TEMPERATURE_UNIT,
                DEFAULT_TEMPERATURE_UNIT
            )) {
                "C" -> TemperatureUnit.Celsius
                "F" -> TemperatureUnit.Fahrenheit
                "K" -> TemperatureUnit.Kelvin
                else -> TemperatureUnit.Celsius
            }
        }
        set(unit) {
            sharedPreferences.edit { putString(PREF_TEMPERATURE_UNIT, unit.unitCode) }
        }

    var temperatureUnitLastUpdated: Long
        get() = sharedPreferences.getLong(PREF_TEMPERATURE_UNIT_LAST_UPDATED, 0L)
        set(value) {
            sharedPreferences.edit { putLong(PREF_TEMPERATURE_UNIT_LAST_UPDATED, value) }
        }

    var humidityUnit: HumidityUnit
        get() {
            return when (sharedPreferences.getInt(PREF_HUMIDITY_UNIT, 0)) {
                0 -> HumidityUnit.Relative
                1 -> HumidityUnit.Absolute
                2 -> HumidityUnit.DewPoint
                else -> HumidityUnit.Relative
            }
        }
        set(value) {
            sharedPreferences.edit { putInt(PREF_HUMIDITY_UNIT, value.unitCode.toInt()) }
        }

    var humidityUnitLastUpdated: Long
        get() = sharedPreferences.getLong(PREF_HUMIDITY_UNIT_LAST_UPDATED, 0L)
        set(value) {
            sharedPreferences.edit { putLong(PREF_HUMIDITY_UNIT_LAST_UPDATED, value) }
        }

    var pressureUnit: PressureUnit
        get() {
            return when (sharedPreferences.getInt(PREF_PRESSURE_UNIT, 1)) {
                0 -> PressureUnit.Pascal
                1 -> PressureUnit.HectoPascal
                2 -> PressureUnit.MmHg
                3 -> PressureUnit.InchHg
                else -> PressureUnit.HectoPascal
            }
        }
        set(value) {
            sharedPreferences.edit { putInt(PREF_PRESSURE_UNIT, value.unitCode.toInt()) }
        }

    var pressureUnitLastUpdated: Long
        get() = sharedPreferences.getLong(PREF_PRESSURE_UNIT_LAST_UPDATED, 0L)
        set(value) {
            sharedPreferences.edit { putLong(PREF_PRESSURE_UNIT_LAST_UPDATED, value) }
        }

    var temperatureAccuracy: Accuracy
        get() {
            return when (sharedPreferences.getInt(PREF_ACCURACY_TEMPERATURE, 2)) {
                0 -> Accuracy.Accuracy0
                1 -> Accuracy.Accuracy1
                2 -> Accuracy.Accuracy2
                else -> Accuracy.Accuracy2
            }
        }
        set(value) {
            sharedPreferences.edit { putInt(PREF_ACCURACY_TEMPERATURE, value.code) }
        }

    var temperatureAccuracyLastUpdated: Long
        get() = sharedPreferences.getLong(PREF_ACCURACY_TEMPERATURE_LAST_UPDATED, 0L)
        set(value) {
            sharedPreferences.edit { putLong(PREF_ACCURACY_TEMPERATURE_LAST_UPDATED, value) }
        }

    var humidityAccuracy: Accuracy
        get() {
            return getAccuracy(PREF_ACCURACY_HUMIDITY, Accuracy.Accuracy2)
        }
        set(value) {
            sharedPreferences.edit {
                putInt(PREF_ACCURACY_HUMIDITY, value.code)
                putInt(PREF_ACCURACY_HUMIDITY_RELATIVE, value.code)
                putInt(PREF_ACCURACY_HUMIDITY_ABSOLUTE, value.code)
                putInt(PREF_ACCURACY_HUMIDITY_DEW_POINT, value.code)
            }

        }

    var humidityAccuracyLastUpdated: Long
        get() = sharedPreferences.getLong(PREF_ACCURACY_HUMIDITY_LAST_UPDATED, 0L)
        set(value) {
            sharedPreferences.edit { putLong(PREF_ACCURACY_HUMIDITY_LAST_UPDATED, value) }
        }

    var pressureAccuracy: Accuracy
        get() {
            return getAccuracy(PREF_ACCURACY_PRESSURE, Accuracy.Accuracy2)
        }
        set(value) {
            sharedPreferences.edit { putInt(PREF_ACCURACY_PRESSURE, value.code) }
        }

    var pressureAccuracyLastUpdated: Long
        get() = sharedPreferences.getLong(PREF_ACCURACY_PRESSURE_LAST_UPDATED, 0L)
        set(value) {
            sharedPreferences.edit { putLong(PREF_ACCURACY_PRESSURE_LAST_UPDATED, value) }
        }

    var relativeHumidityAccuracy: Accuracy
        get() = getAccuracy(PREF_ACCURACY_HUMIDITY_RELATIVE, humidityAccuracy)
        set(value) {
            sharedPreferences.edit().putInt(PREF_ACCURACY_HUMIDITY_RELATIVE, value.code).apply()
        }

    var relativeHumidityAccuracyLastUpdated: Long
        get() = sharedPreferences.getLong(PREF_ACCURACY_HUMIDITY_RELATIVE_LAST_UPDATED, 0L)
        set(value) {
            sharedPreferences.edit { putLong(PREF_ACCURACY_HUMIDITY_RELATIVE_LAST_UPDATED, value) }
        }

    var absoluteHumidityAccuracy: Accuracy
        get() = getAccuracy(PREF_ACCURACY_HUMIDITY_ABSOLUTE, humidityAccuracy)
        set(value) {
            sharedPreferences.edit { putInt(PREF_ACCURACY_HUMIDITY_ABSOLUTE, value.code) }
        }

    var absoluteHumidityAccuracyLastUpdated: Long
        get() = sharedPreferences.getLong(PREF_ACCURACY_HUMIDITY_ABSOLUTE_LAST_UPDATED, 0L)
        set(value) {
            sharedPreferences.edit { putLong(PREF_ACCURACY_HUMIDITY_ABSOLUTE_LAST_UPDATED, value) }
        }

    var dewPointAccuracy: Accuracy
        get() = getAccuracy(PREF_ACCURACY_HUMIDITY_DEW_POINT, humidityAccuracy)
        set(value) {
            sharedPreferences.edit { putInt(PREF_ACCURACY_HUMIDITY_DEW_POINT, value.code) }
        }

    var dewPointAccuracyLastUpdated: Long
        get() = sharedPreferences.getLong(PREF_ACCURACY_HUMIDITY_DEW_POINT_LAST_UPDATED, 0L)
        set(value) {
            sharedPreferences.edit { putLong(PREF_ACCURACY_HUMIDITY_DEW_POINT_LAST_UPDATED, value) }
        }

    var pmAccuracy: Accuracy
        get() = getAccuracy(PREF_ACCURACY_PM, Accuracy.Accuracy1)
        set(value) {
            sharedPreferences.edit { putInt(PREF_ACCURACY_PM, value.code) }
        }

    var pmAccuracyLastUpdated: Long
        get() = sharedPreferences.getLong(PREF_ACCURACY_PM_LAST_UPDATED, 0L)
        set(value) {
            sharedPreferences.edit { putLong(PREF_ACCURACY_PM_LAST_UPDATED, value) }
        }

    var accelerationAccuracy: Accuracy
        get() = getAccuracy(PREF_ACCURACY_ACCELERATION, Accuracy.Accuracy2)
        set(value) {
            sharedPreferences.edit { putInt(PREF_ACCURACY_ACCELERATION, value.code) }
        }

    var accelerationAccuracyLastUpdated: Long
        get() = sharedPreferences.getLong(PREF_ACCURACY_ACCELERATION_LAST_UPDATED, 0L)
        set(value) {
            sharedPreferences.edit { putLong(PREF_ACCURACY_ACCELERATION_LAST_UPDATED, value) }
        }

    var voltageAccuracy: Accuracy
        get() = getAccuracy(PREF_ACCURACY_VOLTAGE, Accuracy.Accuracy2)
        set(value) {
            sharedPreferences.edit { putInt(PREF_ACCURACY_VOLTAGE, value.code) }
        }

    var voltageAccuracyLastUpdated: Long
        get() = sharedPreferences.getLong(PREF_ACCURACY_VOLTAGE_LAST_UPDATED, 0L)
        set(value) {
            sharedPreferences.edit { putLong(PREF_ACCURACY_VOLTAGE_LAST_UPDATED, value) }
        }

    var dataForwardingUrl: String
        get() = sharedPreferences.getString(PREF_BACKEND, DEFAULT_DATA_FORWARDING_URL)
            ?: DEFAULT_DATA_FORWARDING_URL
        set(url) {
            sharedPreferences.edit { putString(PREF_BACKEND, url) }
        }

    var dataForwardingLocationEnabled: Boolean
        get() = sharedPreferences.getBoolean(PREF_BACKEND_LOCATION, false)
        set(locationEnabled) {
            sharedPreferences.edit { putBoolean(PREF_BACKEND_LOCATION, locationEnabled) }
        }

    var dataForwardingDuringSyncEnabled: Boolean
        get() = sharedPreferences.getBoolean(PREF_BACKEND_FORWARDING_DURING_SYNC, false)
        set(forwardingDuringSyncEnabled) {
            sharedPreferences.edit {
                putBoolean(PREF_BACKEND_FORWARDING_DURING_SYNC, forwardingDuringSyncEnabled)
            }
        }

    var deviceId: String
        get() = sharedPreferences.getString(PREF_DEVICE_ID, DEFAULT_DEVICE_ID) ?: DEFAULT_DEVICE_ID
        set(id) {
            sharedPreferences.edit { putString(PREF_DEVICE_ID, id) }
        }

    var serviceWakelock: Boolean
        get() = sharedPreferences.getBoolean(PREF_WAKELOCK, false)
        set(enabled) {
            sharedPreferences.edit { putBoolean(PREF_WAKELOCK, enabled) }
        }

    var batterySaverEnabled: Boolean
        get() = sharedPreferences.getBoolean(PREF_BGSCAN_BATTERY_SAVING, false)
        set(enabled) {
            sharedPreferences.edit { putBoolean(PREF_BGSCAN_BATTERY_SAVING, enabled) }
        }

    // chart interval between data points (in minutes)
    var graphPointInterval: Int
        get() = sharedPreferences.getInt(PREF_GRAPH_POINT_INTERVAL, DEFAULT_GRAPH_POINT_INTERVAL)
        set(interval) {
            sharedPreferences.edit { putInt(PREF_GRAPH_POINT_INTERVAL, interval) }
        }

    var graphViewPeriodHours: Int
        get() = sharedPreferences.getInt(
            PREF_GRAPH_VIEW_PERIOD,
            graphViewPeriodDays * 24
        )
        set(period) {
            sharedPreferences.edit { putInt(PREF_GRAPH_VIEW_PERIOD, period) }
        }

    // chart view period (in days)
    var graphViewPeriodDays: Int
        get() = sharedPreferences.getInt(
            PREF_GRAPH_VIEW_PERIOD_DAYS,
            DEFAULT_GRAPH_VIEW_PERIOD_DAYS
        )
        set(period) {
            sharedPreferences.edit { putInt(PREF_GRAPH_VIEW_PERIOD_DAYS, period) }
        }


    var graphShowAllPoint: Boolean
        get() = sharedPreferences.getBoolean(
            PREF_GRAPH_SHOW_ALL_POINTS,
            DEFAULT_GRAPH_SHOW_ALL_POINTS
        )
        set(showAllPoints) {
            sharedPreferences.edit { putBoolean(PREF_GRAPH_SHOW_ALL_POINTS, showAllPoints) }
        }

    var graphShowAllPointLastUpdated: Long
        get() = sharedPreferences.getLong(PREF_GRAPH_SHOW_ALL_POINTS_LAST_UPDATED, 0L)
        set(lastUpdated) {
            sharedPreferences.edit { putLong(PREF_GRAPH_SHOW_ALL_POINTS_LAST_UPDATED, lastUpdated) }
        }

    var graphDrawDots: Boolean
        get() = sharedPreferences.getBoolean(PREF_GRAPH_DRAW_DOTS, DEFAULT_GRAPH_DRAW_DOTS)
        set(drawDots) {
            sharedPreferences.edit { putBoolean(PREF_GRAPH_DRAW_DOTS, drawDots) }
        }

    var graphDrawDotsLastUpdated: Long
        get() = sharedPreferences.getLong(PREF_GRAPH_DRAW_DOTS_LAST_UPDATED, 0L)
        set(lastUpdated) {
            sharedPreferences.edit { putLong(PREF_GRAPH_DRAW_DOTS_LAST_UPDATED, lastUpdated) }
        }

    var networkEmail: String
        get() = sharedPreferences.getString(PREF_NETWORK_EMAIL, "") ?: ""
        set(email) {
            sharedPreferences.edit { putString(PREF_NETWORK_EMAIL, email) }
        }

    var networkToken: String
        get() = sharedPreferences.getString(PREF_NETWORK_TOKEN, "") ?: ""
        set(token) {
            sharedPreferences.edit { putString(PREF_NETWORK_TOKEN, token) }
        }

    var signedInOnce: Boolean
        get() = sharedPreferences.getBoolean(PREF_SIGNED_IN_ONCE, false)
        set(signedIn) {
            sharedPreferences.edit { putBoolean(PREF_SIGNED_IN_ONCE, signedIn) }
        }

    var lastSyncDate: Long
        get() = sharedPreferences.getLong(PREF_LAST_SYNC_DATE, Long.MIN_VALUE)
        set(syncDate) {
            sharedPreferences.edit { putLong(PREF_LAST_SYNC_DATE, syncDate) }
        }

    var experimentalFeatures: Boolean
        get() = sharedPreferences.getBoolean(PREF_EXPERIMENTAL_FEATURES, false)
        set(experimentalFeatures) {
            sharedPreferences.edit().putBoolean(PREF_EXPERIMENTAL_FEATURES, experimentalFeatures)
                .apply()
        }

    var developerSettings: Boolean
        get() = sharedPreferences.getBoolean(PREF_DEVELOPER_SETTINGS, false)
        set(developerSettings) {
            sharedPreferences.edit {
                putBoolean(PREF_DEVELOPER_SETTINGS, developerSettings)
            }
        }

    var requestForReviewDate: Long
        get() = sharedPreferences.getLong(
            PREF_REQUEST_FOR_REVIEW_DATE,
            DEFAULT_REQUEST_FOR_REVIEW_DATE
        )
        set(requestDate) {
            sharedPreferences.edit { putLong(PREF_REQUEST_FOR_REVIEW_DATE, requestDate) }
        }

    var requestForAppUpdateDate: Long
        get() = sharedPreferences.getLong(
            PREF_LAST_APP_UPDATE_REQUEST,
            DEFAULT_REQUEST_FOR_APP_UPDATE_DATE
        )
        set(requestDate) {
            sharedPreferences.edit { putLong(PREF_LAST_APP_UPDATE_REQUEST, requestDate) }
        }

    var cloudModeEnabled: Boolean
        get() = sharedPreferences.getBoolean(PREF_CLOUD_MODE, false)
        set(enabled) {
            sharedPreferences.edit { putBoolean(PREF_CLOUD_MODE, enabled) }
        }

    var cloudModeEnabledLastUpdated: Long
        get() = sharedPreferences.getLong(PREF_CLOUD_MODE_LAST_UPDATED, 0L)
        set(lastUpdated) {
            sharedPreferences.edit { putLong(PREF_CLOUD_MODE_LAST_UPDATED, lastUpdated) }
        }

    var useDevServer: Boolean
        get() = sharedPreferences.getBoolean(PREF_USE_DEVSERVER, false)
        set(enabled) {
            sharedPreferences.edit { putBoolean(PREF_USE_DEVSERVER, enabled) }
        }

    var darkMode: DarkModeState
        get() {
            return when (sharedPreferences.getInt(PREF_DARKMODE, DEFAULT_DARKMODE)) {
                DarkModeState.SYSTEM_THEME.code -> DarkModeState.SYSTEM_THEME
                DarkModeState.DARK_THEME.code -> DarkModeState.DARK_THEME
                DarkModeState.LIGHT_THEME.code -> DarkModeState.LIGHT_THEME
                else -> DarkModeState.SYSTEM_THEME
            }
        }
    set(darkMode) {
        sharedPreferences.edit { putInt(PREF_DARKMODE, darkMode.code) }
    }

    var dashboardType: DashboardType
        get() {
            return DashboardType.getByCode(sharedPreferences.getString(PREF_DASHBOARD_TYPE, "") ?: "")
        }
        set(type) {
            sharedPreferences.edit { putString(PREF_DASHBOARD_TYPE, type.code) }
        }

    var dashboardTypeLastUpdated: Long
        get() {
            return sharedPreferences.getLong(PREF_DASHBOARD_TYPE_LAST_UPDATED, 0L)
        }
        set(lastUpdated) {
            sharedPreferences.edit { putLong(PREF_DASHBOARD_TYPE_LAST_UPDATED, lastUpdated) }
        }

    var dashboardTapAction: DashboardTapAction
        get() {
            return DashboardTapAction.getByCode(sharedPreferences.getString(PREF_DASHBOARD_TAP_ACTION, "") ?: "")
        }
        set(type) {
            sharedPreferences.edit { putString(PREF_DASHBOARD_TAP_ACTION, type.code) }
        }

    var dashboardTapActionLastUpdated: Long
        get() {
            return sharedPreferences.getLong(PREF_DASHBOARD_TAP_ACTION_LAST_UPDATED, 0L)
        }
        set(lastUpdated) {
            sharedPreferences.edit { putLong(PREF_DASHBOARD_TAP_ACTION_LAST_UPDATED, lastUpdated) }
        }

    var registeredToken: String
        get() = sharedPreferences.getString(PREF_REGISTERED_DEVICE_TOKEN, "") ?: ""
        set(token) {
            sharedPreferences.edit { putString(PREF_REGISTERED_DEVICE_TOKEN, token) }
        }

    var registeredTokenLanguage: String
        get() = sharedPreferences.getString(PREF_REGISTERED_TOKEN_LANGUAGE, "") ?: ""
        set(language) {
            sharedPreferences.edit { putString(PREF_REGISTERED_TOKEN_LANGUAGE, language) }
        }

    var deviceTokenRefreshDate: Long
        get() = sharedPreferences.getLong(PREF_DEVICE_TOKEN_REFRESH_DATE, Long.MIN_VALUE)
        set(refreshDate) {
            sharedPreferences.edit { putLong(PREF_DEVICE_TOKEN_REFRESH_DATE, refreshDate) }
        }

    var subscriptionRefreshDate: Long
        get() = sharedPreferences.getLong(PREF_SUBSCRIPTION_REFRESH_DATE, Long.MIN_VALUE)
        set(refreshDate) {
            sharedPreferences.edit { putLong(PREF_SUBSCRIPTION_REFRESH_DATE, refreshDate) }
        }

    var subscriptionMaxSharesPerSensor: Int
        get() = sharedPreferences.getInt(
            PREF_SUBSCRIPTION_MAX_SHARES_PER_SENSOR,
            DEFAULT_MAX_SHARES_PER_SENSOR
        )
        set(maxShares) {
            sharedPreferences.edit {
                putInt(
                    PREF_SUBSCRIPTION_MAX_SHARES_PER_SENSOR,
                    maxShares
                )
            }
        }

    var dontShowGattSync: Boolean
        get() = sharedPreferences.getBoolean(PREF_DONT_SHOW_GATT_SYNC, false)
        set(value) {
            sharedPreferences.edit { putBoolean(PREF_DONT_SHOW_GATT_SYNC, value) }
        }

    var newChartsUI: Boolean
        get() = sharedPreferences.getBoolean(PREF_NEW_CHARTS_UI, false)
        set(value) {
            sharedPreferences.edit { putBoolean(PREF_NEW_CHARTS_UI, value) }
        }

    var limitLocalAlerts: Boolean
        get() = sharedPreferences.getBoolean(PREF_LIMIT_LOCAL_ALERTS, true)
        set(enabled) {
            sharedPreferences.edit { putBoolean(PREF_LIMIT_LOCAL_ALERTS, enabled) }
        }

    var showChartStats: Boolean
        get() = sharedPreferences.getBoolean(PREF_SHOW_CHART_STATS, true)
        set(value) {
            sharedPreferences.edit { putBoolean(PREF_SHOW_CHART_STATS, value) }
        }

    var acceptTerms: Boolean
        get() = sharedPreferences.getBoolean(PREF_ACCEPT_TERMS, false)
        set(value) {
            sharedPreferences.edit { putBoolean(PREF_ACCEPT_TERMS, value) }
        }

    var firebaseConsent: Boolean
        get() = sharedPreferences.getBoolean(PREF_FIREBASE_CONSENT, false)
        set(value) {
            sharedPreferences.edit { putBoolean(PREF_FIREBASE_CONSENT, value) }
        }

    var visibleMeasurements: Boolean
        get() = sharedPreferences.getBoolean(PREF_SHOW_VISIBLE_MEASUREMENTS, false)
        set(value) {
            sharedPreferences.edit { putBoolean(PREF_SHOW_VISIBLE_MEASUREMENTS, value) }
        }

    var useWebShare: Boolean
        get() = sharedPreferences.getBoolean(PREF_USE_WEB_SHARE, false)
        set(value) {
            sharedPreferences.edit { putBoolean(PREF_USE_WEB_SHARE, value) }
        }

    var sortedSensors: String
        get() = sharedPreferences.getString(PREF_DASHBOARD_SORTED_SENSORS, "") ?: ""
        set(sortedSensors) {
            sharedPreferences.edit { putString(PREF_DASHBOARD_SORTED_SENSORS, sortedSensors) }
        }

    var sensorsOrderLastUpdated: Long
        get() = sharedPreferences.getLong(PREF_DASHBOARD_SORTED_SENSORS_LAST_UPDATED, 0L)
        set(lastUpdated) {
            sharedPreferences.edit { putLong(PREF_DASHBOARD_SORTED_SENSORS_LAST_UPDATED, lastUpdated) }
        }

    var bannerDisabledForVersion: String
        get() = sharedPreferences.getString(PREF_BANNER_DISABLED_FOR_VERSION, "") ?: ""
        set(version) {
            sharedPreferences.edit { putString(PREF_BANNER_DISABLED_FOR_VERSION, version) }
        }

    var disableEmailNotifications: Boolean
        get() = sharedPreferences.getBoolean(PREF_DISABLE_EMAIL_NOTIFICATIONS, false)
        set(value) {
            sharedPreferences.edit { putBoolean(PREF_DISABLE_EMAIL_NOTIFICATIONS, value) }
        }

    var disableEmailNotificationsLastUpdated: Long
        get() = sharedPreferences.getLong(PREF_DISABLE_EMAIL_NOTIFICATIONS_LAST_UPDATED, 0L)
        set(value) {
            sharedPreferences.edit { putLong(PREF_DISABLE_EMAIL_NOTIFICATIONS_LAST_UPDATED, value) }
        }

    var tipsAllowed: Boolean
        get() = sharedPreferences.getBoolean(PREF_TIPS_ALLOWED, true)
        set(value) {
            sharedPreferences.edit { putBoolean(PREF_TIPS_ALLOWED, value) }
        }

    var tipsAllowedLastUpdated: Long
        get() = sharedPreferences.getLong(PREF_TIPS_ALLOWED_LAST_UPDATED, 0L)
        set(value) {
            sharedPreferences.edit { putLong(PREF_TIPS_ALLOWED_LAST_UPDATED, value) }
        }

    var marketingPermission: Boolean
        get() = sharedPreferences.getBoolean(PREF_MARKETING_PERMISSION, true)
        set(value) {
            sharedPreferences.edit { putBoolean(PREF_MARKETING_PERMISSION, value) }
        }

    var marketingPermissionLastUpdated: Long
        get() = sharedPreferences.getLong(PREF_MARKETING_PERMISSION_LAST_UPDATED, 0L)
        set(value) {
            sharedPreferences.edit { putLong(PREF_MARKETING_PERMISSION_LAST_UPDATED, value) }
        }

    var disablePushNotifications: Boolean
        get() = sharedPreferences.getBoolean(PREF_DISABLE_PUSH_NOTIFICATIONS, false)
        set(value) {
            sharedPreferences.edit { putBoolean(PREF_DISABLE_PUSH_NOTIFICATIONS, value) }
        }

    var disablePushNotificationsLastUpdated: Long
        get() = sharedPreferences.getLong(PREF_DISABLE_PUSH_NOTIFICATIONS_LAST_UPDATED, 0L)
        set(value) {
            sharedPreferences.edit { putLong(PREF_DISABLE_PUSH_NOTIFICATIONS_LAST_UPDATED, value) }
        }

    var disableTelegramNotifications: Boolean
        get() = sharedPreferences.getBoolean(PREF_DISABLE_TELEGRAM_NOTIFICATIONS, false)
        set(value) {
            sharedPreferences.edit { putBoolean(PREF_DISABLE_TELEGRAM_NOTIFICATIONS, value) }
        }

    var disableTelegramNotificationsLastUpdated: Long
        get() = sharedPreferences.getLong(PREF_DISABLE_TELEGRAM_NOTIFICATIONS_LAST_UPDATED, 0L)
        set(value) {
            sharedPreferences.edit { putLong(PREF_DISABLE_TELEGRAM_NOTIFICATIONS_LAST_UPDATED, value) }
        }

    var increasedChartSize: Boolean
        get() = sharedPreferences.getBoolean(PREF_INCREASED_CHART_SIZE, false)
        set(value) {
            sharedPreferences.edit().putBoolean(PREF_INCREASED_CHART_SIZE, value).apply()
        }

    var bluetoothPermissionRequested: Boolean
        get() = sharedPreferences.getBoolean(PREF_BLUETOOTH_PERMISSION_REQUESTED, false)
        set(value) {
            sharedPreferences.edit().putBoolean(PREF_BLUETOOTH_PERMISSION_REQUESTED, value).apply()
        }

    fun getUserEmailLiveData() =
        SharedPreferenceStringLiveData(sharedPreferences, PREF_NETWORK_EMAIL, "")

    fun getLastSyncDateLiveData() =
        SharedPreferenceLongLiveData(sharedPreferences, PREF_LAST_SYNC_DATE, Long.MIN_VALUE)

    fun getExperimentalFeaturesLiveData() =
        SharedPreferenceBooleanLiveData(sharedPreferences, PREF_EXPERIMENTAL_FEATURES, false)

    fun getDeveloperSettingsLiveData() =
        SharedPreferenceBooleanLiveData(sharedPreferences, PREF_DEVELOPER_SETTINGS, false)

    fun getTemperatureUnitCodeLiveData() =
        SharedPreferenceStringLiveData(sharedPreferences, PREF_TEMPERATURE_UNIT, DEFAULT_TEMPERATURE_UNIT)

    fun getHumidityUnitCodeLiveData() =
        SharedPreferenceIntLiveData(sharedPreferences, PREF_HUMIDITY_UNIT, 0)

    fun getPressureUnitCodeLiveData() =
        SharedPreferenceIntLiveData(sharedPreferences, PREF_PRESSURE_UNIT, 1)

    fun getTemperatureAccuracyCodeLiveData() =
        SharedPreferenceIntLiveData(sharedPreferences, PREF_ACCURACY_TEMPERATURE, Accuracy.Accuracy2.code)

    fun getHumidityAccuracyCodeLiveData() =
        SharedPreferenceIntLiveData(sharedPreferences, PREF_ACCURACY_HUMIDITY, Accuracy.Accuracy2.code)

    fun getRelativeHumidityAccuracyCodeLiveData() =
        SharedPreferenceIntLiveData(sharedPreferences, PREF_ACCURACY_HUMIDITY_RELATIVE, humidityAccuracy.code)

    fun getAbsoluteHumidityAccuracyCodeLiveData() =
        SharedPreferenceIntLiveData(sharedPreferences, PREF_ACCURACY_HUMIDITY_ABSOLUTE, humidityAccuracy.code)

    fun getDewPointAccuracyCodeLiveData() =
        SharedPreferenceIntLiveData(sharedPreferences, PREF_ACCURACY_HUMIDITY_DEW_POINT, humidityAccuracy.code)

    fun getPressureAccuracyCodeLiveData() =
        SharedPreferenceIntLiveData(sharedPreferences, PREF_ACCURACY_PRESSURE, Accuracy.Accuracy2.code)

    fun getPmAccuracyCodeLiveData() =
        SharedPreferenceIntLiveData(sharedPreferences, PREF_ACCURACY_PM, Accuracy.Accuracy1.code)

    fun getAccelerationAccuracyCodeLiveData() =
        SharedPreferenceIntLiveData(sharedPreferences, PREF_ACCURACY_ACCELERATION, Accuracy.Accuracy2.code)

    fun getVoltageAccuracyCodeLiveData() =
        SharedPreferenceIntLiveData(sharedPreferences, PREF_ACCURACY_VOLTAGE, Accuracy.Accuracy2.code)

    private fun getAccuracy(key: String, fallback: Accuracy): Accuracy {
        return when (sharedPreferences.getInt(key, fallback.code)) {
            0 -> Accuracy.Accuracy0
            1 -> Accuracy.Accuracy1
            2 -> Accuracy.Accuracy2
            else -> fallback
        }
    }

    companion object {
        private const val DEFAULT_SCAN_INTERVAL = 5 * 60
        private const val PREF_BACKGROUND_SCAN_INTERVAL = "pref_background_scan_interval"
        private const val PREF_BACKGROUND_SCAN_INTERVAL_LAST_UPDATED = "pref_background_scan_interval_last_updated"
        private const val PREF_BACKGROUND_SCAN_MODE = "pref_background_scan_mode"
        private const val PREF_BACKGROUND_SCAN_MODE_LAST_UPDATED = "pref_background_scan_mode_last_updated"
        private const val PREF_FIRST_START = "FIRST_START_PREF2"
        private const val PREF_FIRST_GRAPH = "first_graph_visit"
        private const val PREF_TEMPERATURE_UNIT = "pref_temperature_unit"
        private const val PREF_TEMPERATURE_UNIT_LAST_UPDATED = "pref_temperature_unit_last_updated"
        private const val PREF_HUMIDITY_UNIT = "pref_humidity_unit"
        private const val PREF_HUMIDITY_UNIT_LAST_UPDATED = "pref_humidity_unit_last_updated"
        private const val PREF_PRESSURE_UNIT = "pref_pressure_unit"
        private const val PREF_PRESSURE_UNIT_LAST_UPDATED = "pref_pressure_unit_last_updated"
        private const val PREF_ACCURACY_TEMPERATURE = "pref_accuracy_temperature"
        private const val PREF_ACCURACY_TEMPERATURE_LAST_UPDATED = "pref_accuracy_temperature_last_updated"
        private const val PREF_ACCURACY_HUMIDITY = "pref_accuracy_humidity"
        private const val PREF_ACCURACY_HUMIDITY_LAST_UPDATED = "pref_accuracy_humidity_last_updated"
        private const val PREF_ACCURACY_PRESSURE = "pref_accuracy_pressure"
        private const val PREF_ACCURACY_PRESSURE_LAST_UPDATED = "pref_accuracy_pressure_last_updated"
        private const val PREF_ACCURACY_HUMIDITY_RELATIVE = "pref_accuracy_humidity_relative"
        private const val PREF_ACCURACY_HUMIDITY_RELATIVE_LAST_UPDATED = "pref_accuracy_humidity_relative_last_updated"
        private const val PREF_ACCURACY_HUMIDITY_ABSOLUTE = "pref_accuracy_humidity_absolute"
        private const val PREF_ACCURACY_HUMIDITY_ABSOLUTE_LAST_UPDATED = "pref_accuracy_humidity_absolute_last_updated"
        private const val PREF_ACCURACY_HUMIDITY_DEW_POINT = "pref_accuracy_humidity_dew_point"
        private const val PREF_ACCURACY_HUMIDITY_DEW_POINT_LAST_UPDATED = "pref_accuracy_humidity_dew_point_last_updated"
        private const val PREF_ACCURACY_PM = "pref_accuracy_pm"
        private const val PREF_ACCURACY_PM_LAST_UPDATED = "pref_accuracy_pm_last_updated"
        private const val PREF_ACCURACY_ACCELERATION = "pref_accuracy_acceleration"
        private const val PREF_ACCURACY_ACCELERATION_LAST_UPDATED = "pref_accuracy_acceleration_last_updated"
        private const val PREF_ACCURACY_VOLTAGE = "pref_accuracy_voltage"
        private const val PREF_ACCURACY_VOLTAGE_LAST_UPDATED = "pref_accuracy_voltage_last_updated"
        private const val PREF_BACKEND = "pref_backend"
        private const val PREF_BACKEND_LOCATION = "pref_backend_location"
        private const val PREF_BACKEND_FORWARDING_DURING_SYNC =
            "pref_backend_forwarding_during_sync"
        private const val PREF_DEVICE_ID = "pref_device_id"
        private const val PREF_WAKELOCK = "pref_wakelock"
        private const val PREF_BGSCAN_BATTERY_SAVING = "pref_bgscan_battery_saving"
        private const val PREF_GRAPH_POINT_INTERVAL = "pref_graph_point_interval"
        private const val PREF_GRAPH_VIEW_PERIOD = "pref_graph_view_period_hours"
        private const val PREF_GRAPH_VIEW_PERIOD_DAYS = "pref_graph_view_period_days"
        private const val PREF_GRAPH_SHOW_ALL_POINTS = "pref_graph_show_all_points"
        private const val PREF_GRAPH_SHOW_ALL_POINTS_LAST_UPDATED = "pref_graph_show_all_points_last_updated"
        private const val PREF_GRAPH_DRAW_DOTS = "pref_graph_draw_dots"
        private const val PREF_GRAPH_DRAW_DOTS_LAST_UPDATED = "pref_graph_draw_dots_last_updated"
        private const val PREF_NETWORK_EMAIL = "pref_network_email"
        private const val PREF_NETWORK_TOKEN = "pref_network_token"
        private const val PREF_LAST_SYNC_DATE = "pref_last_sync_date"
        private const val PREF_EXPERIMENTAL_FEATURES = "pref_experimental_features"
        private const val PREF_DEVELOPER_SETTINGS = "pref_developer_settings"
        private const val PREF_REQUEST_FOR_REVIEW_DATE = "pref_request_for_review_date"
        private const val PREF_CLOUD_MODE = "pref_cloud_mode_enabled"
        private const val PREF_CLOUD_MODE_LAST_UPDATED = "pref_cloud_mode_last_updated"
        private const val PREF_LAST_APP_UPDATE_REQUEST = "pref_last_app_update_request"
        private const val PREF_DARKMODE = "pref_darkmode"
        private const val PREF_DASHBOARD_TYPE = "pref_dashboard_type"
        private const val PREF_DASHBOARD_TYPE_LAST_UPDATED = "pref_dashboard_type_last_updated"
        private const val PREF_DASHBOARD_TAP_ACTION = "pref_dashboard_tap_action"
        private const val PREF_DASHBOARD_TAP_ACTION_LAST_UPDATED = "pref_dashboard_tap_action_last_updated"
        private const val PREF_REGISTERED_DEVICE_TOKEN = "pref_registered_device_token"
        private const val PREF_REGISTERED_TOKEN_LANGUAGE = "pref_registered_token_language"
        private const val PREF_DEVICE_TOKEN_REFRESH_DATE = "pref_device_token_refresh_date"
        private const val PREF_SUBSCRIPTION_REFRESH_DATE = "pref_subscription_refresh_date"
        private const val PREF_SUBSCRIPTION_MAX_SHARES_PER_SENSOR = "pref_subscription_maxSharesPerSensor"
        private const val PREF_DONT_SHOW_GATT_SYNC = "pref_dont_show_gatt_sync"
        private const val PREF_NEW_CHARTS_UI = "pref_new_charts_ui"
        private const val PREF_USE_DEVSERVER = "pref_use_devserver"
        private const val PREF_LIMIT_LOCAL_ALERTS = "pref_limit_local_alerts"
        private const val PREF_SHOW_CHART_STATS = "pref_show_chart_stats"
        private const val PREF_SIGNED_IN_ONCE = "pref_signed_in_once"
        private const val PREF_DASHBOARD_SORTED_SENSORS = "pref_dashboard_sorted_sensors"
        private const val PREF_DASHBOARD_SORTED_SENSORS_LAST_UPDATED = "pref_dashboard_sorted_sensors_last_updated"
        private const val PREF_ACCEPT_TERMS = "pref_accept_terms"
        private const val PREF_FIREBASE_CONSENT = "pref_firebase_consent"
        private const val PREF_DISABLE_EMAIL_NOTIFICATIONS = "pref_disable_email_notifications"
        private const val PREF_DISABLE_EMAIL_NOTIFICATIONS_LAST_UPDATED = "pref_disable_email_notifications_last_updated"
        private const val PREF_DISABLE_PUSH_NOTIFICATIONS = "pref_disable_push_notifications"
        private const val PREF_DISABLE_PUSH_NOTIFICATIONS_LAST_UPDATED = "pref_disable_push_notifications_last_updated"
        private const val PREF_DISABLE_TELEGRAM_NOTIFICATIONS = "pref_disable_telegram_notifications"
        private const val PREF_DISABLE_TELEGRAM_NOTIFICATIONS_LAST_UPDATED = "pref_disable_telegram_notifications_last_updated"
        private const val PREF_BANNER_DISABLED_FOR_VERSION = "pref_banner_disabled_for_version"
        private const val PREF_INCREASED_CHART_SIZE = "pref_increased_chart_size"
        private const val PREF_BLUETOOTH_PERMISSION_REQUESTED = "pref_bluetooth_permission_requested"
        private const val PREF_SHOW_VISIBLE_MEASUREMENTS = "pref_show_visible_measurements"
        private const val PREF_TIPS_ALLOWED = "pref_tips_allowed"
        private const val PREF_TIPS_ALLOWED_LAST_UPDATED = "pref_tips_allowed_last_updated"
        private const val PREF_MARKETING_PERMISSION = "pref_marketing_permission"
        private const val PREF_MARKETING_PERMISSION_LAST_UPDATED = "pref_marketing_permission_last_updated"

        private const val PREF_USE_WEB_SHARE = "pref_use_web_share"

        private const val DEFAULT_TEMPERATURE_UNIT = "C"
        private const val DEFAULT_DATA_FORWARDING_URL = ""
        private const val DEFAULT_DEVICE_ID = ""
        private const val DEFAULT_GRAPH_POINT_INTERVAL = 1
        private const val DEFAULT_GRAPH_VIEW_PERIOD_DAYS = 0
        private const val DEFAULT_GRAPH_SHOW_ALL_POINTS = false
        private const val DEFAULT_GRAPH_DRAW_DOTS = false
        private const val DEFAULT_MAX_SHARES_PER_SENSOR = 10
        private const val DEFAULT_REQUEST_FOR_REVIEW_DATE = 0L
        private const val DEFAULT_REQUEST_FOR_APP_UPDATE_DATE = 0L
        private const val DEFAULT_DARKMODE = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
}
