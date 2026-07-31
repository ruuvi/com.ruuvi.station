package com.ruuvi.station.settings.ui

import android.content.Context
import com.ruuvi.station.R

object SettingsRoutes {
    const val LIST = "list"
    const val TEMPERATURE = "temperature"
    const val HUMIDITY = "humidity"
    const val PRESSURE = "pressure"
    const val GLOBAL_UNITS = "global_units"
    const val GLOBAL_UNIT_TYPE_ARG = "unitType"
    const val GLOBAL_UNIT_SELECT = "global_unit_select/{$GLOBAL_UNIT_TYPE_ARG}"
    const val RESOLUTION = "resolution"
    const val RESOLUTION_TARGET_ARG = "resolutionTarget"
    const val RESOLUTION_SELECT = "resolution_select/{$RESOLUTION_TARGET_ARG}"
    const val APPEARANCE = "appearance"
    const val ALERT_NOTIFICATIONS = "alert_notifications"
    const val BACKGROUNDSCAN = "backgroundscan"
    const val CHARTS = "charts"
    const val CLOUD = "cloud"
    const val DATAFORWARDING = "dataforwarding"
    const val DEVELOPER = "developer"
    const val SHARINGWEB = "sharingweb"

    fun getTitleByRoute(context: Context, route: String): String {
        return when (route) {
            APPEARANCE -> context.getString(R.string.settings_appearance)
            LIST -> context.getString(R.string.menu_app_settings)
            GLOBAL_UNITS -> context.getString(R.string.settings_global_units)
            GLOBAL_UNIT_SELECT -> context.getString(R.string.settings_global_units)
            RESOLUTION -> context.getString(R.string.settings_resolution)
            RESOLUTION_SELECT -> context.getString(R.string.settings_resolution)
            TEMPERATURE -> context.getString(R.string.settings_temperature)
            HUMIDITY -> context.getString(R.string.settings_humidity)
            PRESSURE -> context.getString(R.string.settings_pressure)
            BACKGROUNDSCAN -> context.getString(R.string.settings_background_scan)
            CHARTS -> context.getString(R.string.settings_chart)
            CLOUD -> context.getString(R.string.ruuvi_cloud)
            DATAFORWARDING -> context.getString(R.string.settings_data_forwarding)
            ALERT_NOTIFICATIONS -> context.getString(R.string.settings_alert_notifications)
            DEVELOPER -> context.getString(R.string.settings_developer)
            else -> context.getString(R.string.menu_app_settings)
        }
    }

    fun globalUnitSelectRoute(unitType: String): String =
        "global_unit_select/$unitType"

    fun resolutionSelectRoute(target: String): String =
        "resolution_select/$target"
}
