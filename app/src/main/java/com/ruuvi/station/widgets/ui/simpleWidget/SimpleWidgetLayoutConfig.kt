package com.ruuvi.station.widgets.ui.simpleWidget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.ruuvi.station.app.ui.theme.ruuviStationFontsSizes

data class SimpleWidgetLayoutConfig(
    val displayNameFontSize: TextUnit,
    val valueFontSize: TextUnit,
    val secondaryFontSize: TextUnit,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val inlineSpacing: Dp,
    val aqiBoxHeight: Dp,
    val unitPadding: Dp,
    val aqiMeasurementPadding: Dp,
    val dotSize: Dp,
    val glowSize: Dp,
    val barHeight: Dp
) {
    companion object {
        fun fromHeight(
            height: Dp,
            zoomFactor: Float = 1f
        ): SimpleWidgetLayoutConfig {
            val baseConfig = when {
                height >= 90.dp -> SimpleWidgetLayoutConfig(
                    displayNameFontSize = ruuviStationFontsSizes.normal,
                    valueFontSize = ruuviStationFontsSizes.bigger,
                    secondaryFontSize = ruuviStationFontsSizes.tiny,
                    horizontalPadding = 12.dp,
                    verticalPadding = 8.dp,
                    inlineSpacing = 2.dp,
                    aqiBoxHeight = 34.dp,
                    unitPadding = 6.dp,
                    aqiMeasurementPadding = 4.dp,
                    dotSize = 6.dp,
                    glowSize = 12.dp,
                    barHeight = 3.dp
                )

                height >= 75.dp -> SimpleWidgetLayoutConfig(
                    displayNameFontSize = ruuviStationFontsSizes.petite,
                    valueFontSize = ruuviStationFontsSizes.big,
                    secondaryFontSize = ruuviStationFontsSizes.tiny,
                    horizontalPadding = 10.dp,
                    verticalPadding = 6.dp,
                    inlineSpacing = 2.dp,
                    aqiBoxHeight = 27.dp,
                    unitPadding = 3.dp,
                    aqiMeasurementPadding = 2.dp,
                    dotSize = 5.dp,
                    glowSize = 12.dp,
                    barHeight = 2.dp
                )

                else -> SimpleWidgetLayoutConfig(
                    displayNameFontSize = ruuviStationFontsSizes.tiny,
                    valueFontSize = ruuviStationFontsSizes.miniature,
                    secondaryFontSize = ruuviStationFontsSizes.nano,
                    horizontalPadding = 8.dp,
                    verticalPadding = 4.dp,
                    inlineSpacing = 1.dp,
                    aqiBoxHeight = 18.dp,
                    unitPadding = 3.dp,
                    aqiMeasurementPadding = 3.dp,
                    dotSize = 3.dp,
                    glowSize = 8.dp,
                    barHeight = 1.dp
                )
            }

            val scaled = if (zoomFactor != 1f) {
                baseConfig.copy(
                    horizontalPadding = baseConfig.horizontalPadding / zoomFactor,
                    verticalPadding = baseConfig.verticalPadding / zoomFactor,
                    inlineSpacing = baseConfig.inlineSpacing / zoomFactor,
                    aqiBoxHeight = baseConfig.aqiBoxHeight / zoomFactor,
                    unitPadding = baseConfig.unitPadding / zoomFactor,
                    aqiMeasurementPadding = baseConfig.aqiMeasurementPadding / zoomFactor,
                    dotSize = baseConfig.dotSize / zoomFactor,
                    glowSize = baseConfig.glowSize / zoomFactor,
                    barHeight = baseConfig.barHeight / zoomFactor
                )
            } else {
                baseConfig
            }

            return scaled
        }
    }
}
