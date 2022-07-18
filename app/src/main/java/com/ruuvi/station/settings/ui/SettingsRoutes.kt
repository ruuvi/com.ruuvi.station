package com.ruuvi.station.settings.ui

import android.content.Context
import com.ruuvi.station.R

object SettingsRoutes {
    const val LIST = "list"
    const val TEMPERATURE = "temperature"
    const val HUMIDITY = "humidity"
    const val PRESSURE = "pressure"
    const val APPEARANCE = "appearance"

    fun getTitleByRoute(context: Context, route: String): String {
        return when (route) {
            APPEARANCE -> context.getString(R.string.settings_appearance)
            LIST -> context.getString(R.string.menu_app_settings)
            TEMPERATURE -> context.getString(R.string.settings_temperature_unit)
            HUMIDITY -> context.getString(R.string.settings_humidity_unit)
            PRESSURE -> context.getString(R.string.settings_pressure_unit)
            else -> context.getString(R.string.menu_app_settings)
        }
    }
}