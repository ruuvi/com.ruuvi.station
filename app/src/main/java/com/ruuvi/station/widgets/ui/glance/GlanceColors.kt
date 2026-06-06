package com.ruuvi.station.widgets.ui.glance

import androidx.glance.color.ColorProvider
import com.ruuvi.station.app.ui.theme.darkPalette
import com.ruuvi.station.app.ui.theme.lightPalette

object GlanceColors {
    val background = ColorProvider(day = lightPalette.background, night = darkPalette.background)
    val logoColor = ColorProvider(day = lightPalette.dashboardIcons, night = darkPalette.dashboardIcons)
    val widgetSensorName = ColorProvider(day = lightPalette.widgetSensorName, night = darkPalette.widgetSensorName)
    val valueColor = ColorProvider(day = lightPalette.primary, night = darkPalette.primary)
    val refreshButtonColor = ColorProvider(day = lightPalette.primary, night = lightPalette.primary)
}
