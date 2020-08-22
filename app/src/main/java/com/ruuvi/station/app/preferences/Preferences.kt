package com.ruuvi.station.app.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.ruuvi.station.model.HumidityUnit
import com.ruuvi.station.util.BackgroundScanModes

class Preferences(val context: Context) {
    private val sharedPreferences: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

    var backgroundScanInterval: Int
        get() = sharedPreferences.getInt(PREF_BACKGROUND_SCAN_INTERVAL, DEFAULT_SCAN_INTERVAL)
        set(interval) {
            sharedPreferences.edit().putInt(PREF_BACKGROUND_SCAN_INTERVAL, interval).apply()
        }

    var backgroundScanMode: BackgroundScanModes
        get() = BackgroundScanModes.fromInt(sharedPreferences.getInt("pref_background_scan_mode", BackgroundScanModes.DISABLED.value))
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

    var temperatureUnit: String
        get() = sharedPreferences.getString(PREF_TEMPERATURE_UNIT, DEFAULT_TEMPERATURE_UNIT) ?: DEFAULT_TEMPERATURE_UNIT
        set(unit) {
            sharedPreferences.edit().putString(PREF_TEMPERATURE_UNIT, unit).apply()
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

    var gatewayUrl: String
        get() = sharedPreferences.getString(PREF_BACKEND, DEFAULT_GATEWAY_URL) ?: DEFAULT_GATEWAY_URL
        set(url) {
            sharedPreferences.edit().putString(PREF_BACKEND, url).apply()
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

    var graphShowAllPoint: Boolean
        get() = sharedPreferences.getBoolean(PREF_GRAPH_SHOW_ALL_POINTS, DEFAULT_GRAPH_SHOW_ALL_POINTS)
        set(period) {
            sharedPreferences.edit().putBoolean(PREF_GRAPH_SHOW_ALL_POINTS, period).apply()
        }

    companion object {
        private const val DEFAULT_SCAN_INTERVAL = 15 * 60
        private const val PREF_BACKGROUND_SCAN_INTERVAL = "PREF_BACKGROUND_SCAN_INTERVAL"
        private const val PREF_BACKGROUND_SCAN_MODE = "PREF_BACKGROUND_SCAN_MODE"
        private const val PREF_FIRST_START = "PREF_FIRST_START"
        private const val PREF_FIRST_GRAPH = "PREF_FIRST_GRAPH"
        private const val PREF_TEMPERATURE_UNIT = "PREF_TEMPERATURE_UNIT"
        private const val PREF_HUMIDITY_UNIT = "PREF_HUMIDITY_UNIT"
        private const val PREF_BACKEND = "PREF_BACKEND"
        private const val PREF_DEVICE_ID = "PREF_DEVICE_ID"
        private const val PREF_WAKELOCK = "PREF_WAKELOCK"
        private const val PREF_DASHBOARD_ENABLED = "PREF_DASHBOARD_ENABLED"
        private const val PREF_BGSCAN_BATTERY_SAVING = "PREF_BGSCAN_BATTERY_SAVING"
        private const val PREF_GRAPH_POINT_INTERVAL = "PREF_GRAPH_POINT_INTERVAL"
        private const val PREF_GRAPH_VIEW_PERIOD = "PREF_GRAPH_VIEW_PERIOD"
        private const val PREF_GRAPH_SHOW_ALL_POINTS = "PREF_GRAPH_SHOW_ALL_POINTS"

        private const val DEFAULT_TEMPERATURE_UNIT = "C"
        private const val DEFAULT_GATEWAY_URL = ""
        private const val DEFAULT_DEVICE_ID = ""
        private const val DEFAULT_GRAPH_POINT_INTERVAL = 1
        private const val DEFAULT_GRAPH_VIEW_PERIOD = 24
        private const val DEFAULT_GRAPH_SHOW_ALL_POINTS = true
    }
}