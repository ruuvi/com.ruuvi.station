package com.ruuvi.station.widgets.ui.simpleWidget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.ruuvi.station.app.ui.theme.ruuviStationFontsSizes

data class SimpleWidgetLayoutConfig(
    val displayNameFontSize: TextUnit,
    val valueFontSize: TextUnit,
    val secondaryFontSize: TextUnit,
    val miniatureFontSize: TextUnit,
    val aqiBoxHeight: Dp,
    val unitPadding: Dp,
    val dotSize: Dp,
    val glowSize: Dp,
    val barHeight: Dp,
    val refreshButtonSize: Dp,
    val refreshIconSize: Dp
) {
    companion object {
        fun fromHeight(height: Dp): SimpleWidgetLayoutConfig {
            return when {
                height >= 80.dp -> SimpleWidgetLayoutConfig(
                    displayNameFontSize = ruuviStationFontsSizes.normal,
                    valueFontSize = ruuviStationFontsSizes.bigger,
                    secondaryFontSize = ruuviStationFontsSizes.tiny,
                    miniatureFontSize = ruuviStationFontsSizes.petite,
                    aqiBoxHeight = 34.dp,
                    unitPadding = 4.dp,
                    dotSize = 6.dp,
                    glowSize = 14.dp,
                    barHeight = 3.dp,
                    refreshButtonSize = 40.dp,
                    refreshIconSize = 16.dp
                )
                height >= 70.dp -> SimpleWidgetLayoutConfig(
                    displayNameFontSize = ruuviStationFontsSizes.petite,
                    valueFontSize = ruuviStationFontsSizes.extended,
                    secondaryFontSize = ruuviStationFontsSizes.tiny,
                    miniatureFontSize = ruuviStationFontsSizes.tiny2,
                    aqiBoxHeight = 27.dp,
                    unitPadding = 2.dp,
                    dotSize = 5.dp,
                    glowSize = 12.dp,
                    barHeight = 2.dp,
                    refreshButtonSize = 36.dp,
                    refreshIconSize = 15.dp
                )
                else -> SimpleWidgetLayoutConfig(
                    displayNameFontSize = ruuviStationFontsSizes.tiny,
                    valueFontSize = ruuviStationFontsSizes.compact,
                    secondaryFontSize = ruuviStationFontsSizes.micro,
                    miniatureFontSize = ruuviStationFontsSizes.micro,
                    aqiBoxHeight = 20.dp,
                    unitPadding = 0.dp,
                    dotSize = 4.dp,
                    glowSize = 10.dp,
                    barHeight = 1.5.dp,
                    refreshButtonSize = 30.dp,
                    refreshIconSize = 13.dp
                )
            }
        }
    }
}
