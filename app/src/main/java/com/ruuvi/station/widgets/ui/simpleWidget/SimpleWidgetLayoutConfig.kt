package com.ruuvi.station.widgets.ui.simpleWidget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.ruuvi.station.app.ui.theme.ruuviStationFontsSizes

data class SimpleWidgetLayoutConfig(
    val displayNameFontSize: TextUnit,
    val valueFontSize: TextUnit,
    val secondaryFontSize: TextUnit,
    val aqiBoxHeight: Dp,
    val unitPadding: Dp,
    val aqiMeasurementPadding: Dp,
    val dotSize: Dp,
    val glowSize: Dp,
    val barHeight: Dp,
    val refreshButtonSize: Dp,
    val refreshIconSize: Dp
) {
    companion object {
        fun fromHeight(height: Dp): SimpleWidgetLayoutConfig {
            return when {
                height >= 90.dp -> SimpleWidgetLayoutConfig(
                    displayNameFontSize = ruuviStationFontsSizes.normal,
                    valueFontSize = ruuviStationFontsSizes.bigger,
                    secondaryFontSize = ruuviStationFontsSizes.tiny,
                    aqiBoxHeight = 34.dp,
                    unitPadding = 6.dp,
                    aqiMeasurementPadding = 4.dp,
                    dotSize = 6.dp,
                    glowSize = 12.dp,
                    barHeight = 3.dp,
                    refreshButtonSize = 40.dp,
                    refreshIconSize = 16.dp
                )
                height >= 75.dp -> SimpleWidgetLayoutConfig(
                    displayNameFontSize = ruuviStationFontsSizes.petite,
                    valueFontSize = ruuviStationFontsSizes.big,
                    secondaryFontSize = ruuviStationFontsSizes.tiny,
                    aqiBoxHeight = 27.dp,
                    unitPadding = 3.dp,
                    aqiMeasurementPadding = 2.dp,
                    dotSize = 5.dp,
                    glowSize = 12.dp,
                    barHeight = 2.dp,
                    refreshButtonSize = 36.dp,
                    refreshIconSize = 14.dp
                )
                else -> SimpleWidgetLayoutConfig(
                    displayNameFontSize = ruuviStationFontsSizes.tiny,
                    valueFontSize = ruuviStationFontsSizes.miniature,
                    secondaryFontSize = ruuviStationFontsSizes.nano,
                    aqiBoxHeight = 18.dp,
                    unitPadding = 3.dp,
                    aqiMeasurementPadding = 3.dp,
                    dotSize = 3.dp,
                    glowSize = 8.dp,
                    barHeight = 1.dp,
                    refreshButtonSize = 28.dp,
                    refreshIconSize = 12.dp
                )
            }
        }
    }
}
