package com.ruuvi.station.tagsettings.ui

import android.content.Context
import com.ruuvi.station.R

object SensorSettingsRoutes {
    const val SENSOR_SETTINGS_ROOT = "sensor_settings_root"
    const val SENSOR_REMOVE = "sensor_remove"
    const val VISIBLE_MEASUREMENTS = "visible_measurements"
    const val LED_CONTROL = "led_control"
    const val NOTES = "notes"


    fun getTitleByRoute(context: Context, route: String): String {
        return when (route) {
            SENSOR_SETTINGS_ROOT -> context.getString(R.string.sensor_settings)
            SENSOR_REMOVE -> context.getString(R.string.tagsettings_sensor_remove)
            VISIBLE_MEASUREMENTS -> context.getString(R.string.visible_measurements)
            LED_CONTROL -> context.getString(R.string.led_brightness_control)
            NOTES -> context.getString(R.string.notes)
            else -> context.getString(R.string.sensor_settings)
        }
    }
}