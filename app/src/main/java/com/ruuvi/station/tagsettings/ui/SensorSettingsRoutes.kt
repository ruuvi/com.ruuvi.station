package com.ruuvi.station.tagsettings.ui

import android.content.Context
import com.ruuvi.station.R

object SensorSettingsRoutes {
    const val SENSOR_SETTINGS_ROOT = "sensor_settings_root"
    const val SENSOR_REMOVE = "sensor_remove"

    fun getTitleByRoute(context: Context, route: String): String {
        return when (route) {
            SENSOR_SETTINGS_ROOT -> context.getString(R.string.sensor_settings)
            SENSOR_REMOVE -> context.getString(R.string.tagsettings_sensor_remove)
            else -> context.getString(R.string.sensor_settings)
        }
    }
}