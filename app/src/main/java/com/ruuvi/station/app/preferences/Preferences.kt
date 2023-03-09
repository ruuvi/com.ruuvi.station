package com.ruuvi.station.app.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.ruuvi.station.app.ui.DarkModeState
import com.ruuvi.station.dashboard.DashboardType
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.HumidityUnit
import com.ruuvi.station.units.model.PressureUnit
import com.ruuvi.station.units.model.TemperatureUnit
import com.ruuvi.station.util.BackgroundScanModes
import java.util.*

class Preferences constructor(val context: Context) {

    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(
            context
        )
    }

    var backgroundScanInterval: Int
        get() = sharedPreferences.getInt(PREF_BACKGROUND_SCAN_INTERVAL, DEFAULT_SCAN_INTERVAL)
        set(interval) {
            sharedPreferences.edit().putInt(PREF_BACKGROUND_SCAN_INTERVAL, interval).apply()
        }

    var backgroundScanMode: BackgroundScanModes
        get() = BackgroundScanModes.fromInt(
            sharedPreferences.getInt(
                PREF_BACKGROUND_SCAN_MODE,
                BackgroundScanModes.DISABLED.value
            )
        )
            ?: BackgroundScanModes.DISABLED
        set(mode) {
            sharedPreferences.edit().putInt(PREF_BACKGROUND_SCAN_MODE, mode.value).apply()
        }

    var isFirstStart: Boolean
        get() = sharedPreferences.getBoolean(PREF_FIRST_START, true)
        set(enabled) {
            sharedPreferences.edit().putBoolean(PREF_FIRST_START, enabled).apply()
        }

    var isFirstGraphVisit: Boolean
        get() = sharedPreferences.getBoolean(PREF_FIRST_GRAPH, true)
        set(enabled) {
            sharedPreferences.edit().putBoolean(PREF_FIRST_GRAPH, enabled).apply()
        }

    var temperatureUnit: TemperatureUnit
        get() {
            return when (sharedPreferences.getString(
                PREF_TEMPERATURE_UNIT,
                DEFAULT_TEMPERATURE_UNIT
            )) {
                "C" -> TemperatureUnit.CELSIUS
                "F" -> TemperatureUnit.FAHRENHEIT
                "K" -> TemperatureUnit.KELVIN
                else -> TemperatureUnit.CELSIUS
            }
        }
        set(unit) {
            sharedPreferences.edit().putString(PREF_TEMPERATURE_UNIT, unit.code).apply()
        }

    var humidityUnit: HumidityUnit
        get() {
            return when (sharedPreferences.getInt(PREF_HUMIDITY_UNIT, 0)) {
                0 -> HumidityUnit.PERCENT
                1 -> HumidityUnit.GM3
                2 -> HumidityUnit.DEW
                else -> HumidityUnit.PERCENT
            }
        }
        set(value) {
            sharedPreferences.edit().putInt(PREF_HUMIDITY_UNIT, value.code).apply()
        }

    var pressureUnit: PressureUnit
        get() {
            return when (sharedPreferences.getInt(PREF_PRESSURE_UNIT, 1)) {
                0 -> PressureUnit.PA
                1 -> PressureUnit.HPA
                2 -> PressureUnit.MMHG
                3 -> PressureUnit.INHG
                else -> PressureUnit.HPA
            }
        }
        set(value) {
            sharedPreferences.edit().putInt(PREF_PRESSURE_UNIT, value.code).apply()
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
            sharedPreferences.edit().putInt(PREF_ACCURACY_TEMPERATURE, value.code).apply()
        }

    var humidityAccuracy: Accuracy
        get() {
            return when (sharedPreferences.getInt(PREF_ACCURACY_HUMIDITY, 2)) {
                0 -> Accuracy.Accuracy0
                1 -> Accuracy.Accuracy1
                2 -> Accuracy.Accuracy2
                else -> Accuracy.Accuracy2
            }
        }
        set(value) {
            sharedPreferences.edit().putInt(PREF_ACCURACY_HUMIDITY, value.code).apply()
        }

    var pressureAccuracy: Accuracy
        get() {
            return when (sharedPreferences.getInt(PREF_ACCURACY_PRESSURE, 2)) {
                0 -> Accuracy.Accuracy0
                1 -> Accuracy.Accuracy1
                2 -> Accuracy.Accuracy2
                else -> Accuracy.Accuracy2
            }
        }
        set(value) {
            sharedPreferences.edit().putInt(PREF_ACCURACY_PRESSURE, value.code).apply()
        }

    var dataForwardingUrl: String
        get() = sharedPreferences.getString(PREF_BACKEND, DEFAULT_DATA_FORWARDING_URL)
            ?: DEFAULT_DATA_FORWARDING_URL
        set(url) {
            sharedPreferences.edit().putString(PREF_BACKEND, url).apply()
        }

    var dataForwardingLocationEnabled: Boolean
        get() = sharedPreferences.getBoolean(PREF_BACKEND_LOCATION, false)
        set(locationEnabled) {
            sharedPreferences.edit().putBoolean(PREF_BACKEND_LOCATION, locationEnabled).apply()
        }

    var dataForwardingDuringSyncEnabled: Boolean
        get() = sharedPreferences.getBoolean(PREF_BACKEND_FORWARDING_DURING_SYNC, false)
        set(forwardingDuringSyncEnabled) {
            sharedPreferences.edit()
                .putBoolean(PREF_BACKEND_FORWARDING_DURING_SYNC, forwardingDuringSyncEnabled)
                .apply()
        }

    var deviceId: String
        get() = sharedPreferences.getString(PREF_DEVICE_ID, DEFAULT_DEVICE_ID) ?: DEFAULT_DEVICE_ID
        set(id) {
            sharedPreferences.edit().putString(PREF_DEVICE_ID, id).apply()
        }

    var serviceWakelock: Boolean
        get() = sharedPreferences.getBoolean(PREF_WAKELOCK, false)
        set(enabled) {
            sharedPreferences.edit().putBoolean(PREF_WAKELOCK, enabled).apply()
        }

    var dashboardEnabled: Boolean
        get() = sharedPreferences.getBoolean(PREF_DASHBOARD_ENABLED, false)
        set(enabled) {
            sharedPreferences.edit().putBoolean(PREF_DASHBOARD_ENABLED, enabled).apply()
        }

    var batterySaverEnabled: Boolean
        get() = sharedPreferences.getBoolean(PREF_BGSCAN_BATTERY_SAVING, false)
        set(enabled) {
            sharedPreferences.edit().putBoolean(PREF_BGSCAN_BATTERY_SAVING, enabled).apply()
        }

    // chart interval between data points (in minutes)
    var graphPointInterval: Int
        get() = sharedPreferences.getInt(PREF_GRAPH_POINT_INTERVAL, DEFAULT_GRAPH_POINT_INTERVAL)
        set(interval) {
            sharedPreferences.edit().putInt(PREF_GRAPH_POINT_INTERVAL, interval).apply()
        }

    // chart view period (in hours)
    var graphViewPeriod: Int
        get() = sharedPreferences.getInt(PREF_GRAPH_VIEW_PERIOD, DEFAULT_GRAPH_VIEW_PERIOD)
        set(period) {
            sharedPreferences.edit().putInt(PREF_GRAPH_VIEW_PERIOD, period).apply()
        }

    // chart view period (in days)
    var graphViewPeriodDays: Int
        get() = sharedPreferences.getInt(
            PREF_GRAPH_VIEW_PERIOD_DAYS,
            DEFAULT_GRAPH_VIEW_PERIOD_DAYS
        )
        set(period) {
            sharedPreferences.edit().putInt(PREF_GRAPH_VIEW_PERIOD_DAYS, period).apply()
        }


    var graphShowAllPoint: Boolean
        get() = sharedPreferences.getBoolean(
            PREF_GRAPH_SHOW_ALL_POINTS,
            DEFAULT_GRAPH_SHOW_ALL_POINTS
        )
        set(showAllPoints) {
            sharedPreferences.edit().putBoolean(PREF_GRAPH_SHOW_ALL_POINTS, showAllPoints).apply()
        }

    var graphDrawDots: Boolean
        get() = sharedPreferences.getBoolean(PREF_GRAPH_DRAW_DOTS, DEFAULT_GRAPH_DRAW_DOTS)
        set(drawDots) {
            sharedPreferences.edit().putBoolean(PREF_GRAPH_DRAW_DOTS, drawDots).apply()
        }

    var locale: String
        get() {
            var preferenceLocale = sharedPreferences.getString(PREF_LOCALE, null)
            if (preferenceLocale == null) {
                val defaultLanguage = Locale.getDefault().language
                if (SUPPORTED_LOCALES.contains(defaultLanguage)) {
                    preferenceLocale = defaultLanguage
                } else {
                    preferenceLocale = DEFAULT_LOCALE
                }
                preferenceLocale?.let {
                    locale = preferenceLocale
                }
            }
            return preferenceLocale ?: DEFAULT_LOCALE
        }
        set(locale) {
            sharedPreferences.edit().putString(PREF_LOCALE, locale).apply()
        }

    var networkEmail: String
        get() = sharedPreferences.getString(PREF_NETWORK_EMAIL, "") ?: ""
        set(email) {
            sharedPreferences.edit().putString(PREF_NETWORK_EMAIL, email).apply()
        }

    var networkToken: String
        get() = sharedPreferences.getString(PREF_NETWORK_TOKEN, "") ?: ""
        set(token) {
            sharedPreferences.edit().putString(PREF_NETWORK_TOKEN, token).apply()
        }

    var lastSyncDate: Long
        get() = sharedPreferences.getLong(PREF_LAST_SYNC_DATE, Long.MIN_VALUE)
        set(syncDate) {
            sharedPreferences.edit().putLong(PREF_LAST_SYNC_DATE, syncDate).apply()
        }

    var experimentalFeatures: Boolean
        get() = sharedPreferences.getBoolean(PREF_EXPERIMENTAL_FEATURES, false)
        set(experimentalFeatures) {
            sharedPreferences.edit().putBoolean(PREF_EXPERIMENTAL_FEATURES, experimentalFeatures)
                .apply()
        }

    var requestForReviewDate: Long
        get() = sharedPreferences.getLong(
            PREF_REQUEST_FOR_REVIEW_DATE,
            DEFAULT_REQUEST_FOR_REVIEW_DATE
        )
        set(requestDate) {
            sharedPreferences.edit().putLong(PREF_REQUEST_FOR_REVIEW_DATE, requestDate).apply()
        }

    var requestForAppUpdateDate: Long
        get() = sharedPreferences.getLong(
            PREF_LAST_APP_UPDATE_REQUEST,
            DEFAULT_REQUEST_FOR_APP_UPDATE_DATE
        )
        set(requestDate) {
            sharedPreferences.edit().putLong(PREF_LAST_APP_UPDATE_REQUEST, requestDate).apply()
        }

    var cloudModeEnabled: Boolean
        get() = sharedPreferences.getBoolean(PREF_CLOUD_MODE, false)
        set(enabled) {
            sharedPreferences.edit().putBoolean(PREF_CLOUD_MODE, enabled).apply()
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
        sharedPreferences.edit().putInt(PREF_DARKMODE, darkMode.code).apply()
    }

    var dashboardType: DashboardType
        get() {
            return DashboardType.getByCode(sharedPreferences.getString(PREF_DASHBOARD_TYPE, "") ?: "")
        }
        set(type) {
            sharedPreferences.edit().putString(PREF_DASHBOARD_TYPE, type.code).apply()
        }

    var registeredToken: String
        get() = sharedPreferences.getString(PREF_REGISTERED_DEVICE_TOKEN, "") ?: ""
        set(token) {
            sharedPreferences.edit().putString(PREF_REGISTERED_DEVICE_TOKEN, token).apply()
        }

    var deviceTokenRefreshDate: Long
        get() = sharedPreferences.getLong(PREF_DEVICE_TOKEN_REFRESH_DATE, Long.MIN_VALUE)
        set(refreshDate) {
            sharedPreferences.edit().putLong(PREF_DEVICE_TOKEN_REFRESH_DATE, refreshDate).apply()
        }

    var subscriptionRefreshDate: Long
        get() = sharedPreferences.getLong(PREF_SUBSCRIPTION_REFRESH_DATE, Long.MIN_VALUE)
        set(refreshDate) {
            sharedPreferences.edit().putLong(PREF_SUBSCRIPTION_REFRESH_DATE, refreshDate).apply()
        }

    var subscriptionMaxSharesPerSensor: Int
        get() = sharedPreferences.getInt(
            PREF_SUBSCRIPTION_MAX_SHARES_PER_SENSOR,
            DEFAULT_MAX_SHARES_PER_SENSOR
        )
        set(maxShares) {
            sharedPreferences.edit().putInt(
                PREF_SUBSCRIPTION_MAX_SHARES_PER_SENSOR,
                maxShares
            ).apply()
        }

    fun getUserEmailLiveData() =
        SharedPreferenceStringLiveData(sharedPreferences, PREF_NETWORK_EMAIL, "")

    fun getLastSyncDateLiveData() =
        SharedPreferenceLongLiveData(sharedPreferences, PREF_LAST_SYNC_DATE, Long.MIN_VALUE)

    fun getExperimentalFeaturesLiveData() =
        SharedPreferenceBooleanLiveData(sharedPreferences, PREF_EXPERIMENTAL_FEATURES, false)

    companion object {
        private const val DEFAULT_SCAN_INTERVAL = 5 * 60
        private const val PREF_BACKGROUND_SCAN_INTERVAL = "pref_background_scan_interval"
        private const val PREF_BACKGROUND_SCAN_MODE = "pref_background_scan_mode"
        private const val PREF_FIRST_START = "FIRST_START_PREF2"
        private const val PREF_FIRST_GRAPH = "first_graph_visit"
        private const val PREF_TEMPERATURE_UNIT = "pref_temperature_unit"
        private const val PREF_HUMIDITY_UNIT = "pref_humidity_unit"
        private const val PREF_PRESSURE_UNIT = "pref_pressure_unit"
        private const val PREF_ACCURACY_TEMPERATURE = "pref_accuracy_temperature"
        private const val PREF_ACCURACY_HUMIDITY = "pref_accuracy_humidity"
        private const val PREF_ACCURACY_PRESSURE = "pref_accuracy_pressure"
        private const val PREF_BACKEND = "pref_backend"
        private const val PREF_BACKEND_LOCATION = "pref_backend_location"
        private const val PREF_BACKEND_FORWARDING_DURING_SYNC =
            "pref_backend_forwarding_during_sync"
        private const val PREF_DEVICE_ID = "pref_device_id"
        private const val PREF_WAKELOCK = "pref_wakelock"
        private const val PREF_DASHBOARD_ENABLED = "DASHBOARD_ENABLED_PREF"
        private const val PREF_BGSCAN_BATTERY_SAVING = "pref_bgscan_battery_saving"
        private const val PREF_GRAPH_POINT_INTERVAL = "pref_graph_point_interval"
        private const val PREF_GRAPH_VIEW_PERIOD = "pref_graph_view_period"
        private const val PREF_GRAPH_VIEW_PERIOD_DAYS = "pref_graph_view_period_days"
        private const val PREF_GRAPH_SHOW_ALL_POINTS = "pref_graph_show_all_points"
        private const val PREF_GRAPH_DRAW_DOTS = "pref_graph_draw_dots"
        private const val PREF_LOCALE = "pref_locale"
        private const val PREF_NETWORK_EMAIL = "pref_network_email"
        private const val PREF_NETWORK_TOKEN = "pref_network_token"
        private const val PREF_LAST_SYNC_DATE = "pref_last_sync_date"
        private const val PREF_EXPERIMENTAL_FEATURES = "pref_experimental_features"
        private const val PREF_REQUEST_FOR_REVIEW_DATE = "pref_request_for_review_date"
        private const val PREF_CLOUD_MODE = "pref_cloud_mode_enabled"
        private const val PREF_LAST_APP_UPDATE_REQUEST = "pref_last_app_update_request"
        private const val PREF_DARKMODE = "pref_darkmode"
        private const val PREF_DASHBOARD_TYPE = "pref_dashboard_type"
        private const val PREF_REGISTERED_DEVICE_TOKEN = "pref_registered_device_token"
        private const val PREF_DEVICE_TOKEN_REFRESH_DATE = "pref_device_token_refresh_date"
        private const val PREF_SUBSCRIPTION_REFRESH_DATE = "pref_subscription_refresh_date"
        private const val PREF_SUBSCRIPTION_MAX_SHARES_PER_SENSOR = "pref_subscription_maxSharesPerSensor"

        private const val DEFAULT_TEMPERATURE_UNIT = "C"
        private const val DEFAULT_DATA_FORWARDING_URL = ""
        private const val DEFAULT_DEVICE_ID = ""
        private const val DEFAULT_GRAPH_POINT_INTERVAL = 1
        private const val DEFAULT_GRAPH_VIEW_PERIOD = 24
        private const val DEFAULT_GRAPH_VIEW_PERIOD_DAYS = 10
        private const val DEFAULT_GRAPH_SHOW_ALL_POINTS = false
        private const val DEFAULT_GRAPH_DRAW_DOTS = false
        private const val DEFAULT_LOCALE = "en"
        private const val DEFAULT_MAX_SHARES_PER_SENSOR = 10
        private const val DEFAULT_REQUEST_FOR_REVIEW_DATE = 0L
        private const val DEFAULT_REQUEST_FOR_APP_UPDATE_DATE = 0L
        private const val DEFAULT_DARKMODE = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        private val SUPPORTED_LOCALES = listOf("en", "fi", "sv", "ru")
    }
}