package com.ruuvi.station.widgets.ui.glance

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.graphics.toArgb
import androidx.glance.color.ColorProvider
import com.ruuvi.station.app.ui.theme.darkPalette
import com.ruuvi.station.app.ui.theme.lightPalette

object GlanceColors {
    val background = ColorProvider(day = lightPalette.background, night = darkPalette.background)
    val logoColor = ColorProvider(day = lightPalette.dashboardIcons, night = darkPalette.dashboardIcons)
    val widgetSensorName = ColorProvider(day = lightPalette.widgetSensorName, night = darkPalette.widgetSensorName)
    val valueColor = ColorProvider(day = lightPalette.primary, night = darkPalette.primary)
    val refreshButtonColor = ColorProvider(day = lightPalette.widgetSensorName, night = darkPalette.widgetSensorName)

    fun resolvedValueColor(context: Context): Int {
        return if (isNightMode(context)) {
            darkPalette.primary.toArgb()
        } else {
            lightPalette.primary.toArgb()
        }
    }

    fun resolvedWidgetSensorNameColor(context: Context): Int {
        return if (isNightMode(context)) {
            darkPalette.widgetSensorName.toArgb()
        } else {
            lightPalette.widgetSensorName.toArgb()
        }
    }

    private fun isNightMode(context: Context): Boolean {
        val nightMode = context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK
        return nightMode == Configuration.UI_MODE_NIGHT_YES
    }
}
