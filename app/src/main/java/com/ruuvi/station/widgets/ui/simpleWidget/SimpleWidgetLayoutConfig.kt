package com.ruuvi.station.widgets.ui.simpleWidget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruuvi.station.app.ui.theme.ruuviStationFontsSizes
import com.ruuvi.station.widgets.data.WidgetType
import kotlin.times

data class SimpleWidgetLayoutConfig(
    val displayNameFontSize: TextUnit,
    val valueFontSize: TextUnit,
    val secondaryFontSize: TextUnit,
    val unitPadding: Dp,
    val dotSize: Dp,
    val glowSize: Dp,
    val barHeight: Dp,
    val refreshButtonSize: Dp,
    val refreshIconSize: Dp
) {
    companion object {
        /**
         * @param height       Widget height in **stable** dp (zoom-independent). Convert from
         *                     LocalSize by multiplying with zoomFactor before calling.
         * @param zoomFactor   currentDensityDpi / DENSITY_DEVICE_STABLE (1f when no zoom). Used
         *                     only to scale layout dp values to fit the current widget.
         */
        fun fromHeight(height: Dp, zoomFactor: Float = 1f): SimpleWidgetLayoutConfig {

            val baseConfig = when {
                height >= 90.dp -> SimpleWidgetLayoutConfig(
                    displayNameFontSize = ruuviStationFontsSizes.normal,
                    valueFontSize = ruuviStationFontsSizes.bigger,
                    secondaryFontSize = ruuviStationFontsSizes.tiny,
                    unitPadding = 6.dp,
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
                    unitPadding = 3.dp,
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
                    unitPadding = 3.dp,
                    dotSize = 3.dp,
                    glowSize = 8.dp,
                    barHeight = 1.dp,
                    refreshButtonSize = 28.dp,
                    refreshIconSize = 12.dp
                )
            }

            val scaled = if (zoomFactor != 1f) baseConfig.copy(
                unitPadding = baseConfig.unitPadding / zoomFactor,
                dotSize = baseConfig.dotSize / zoomFactor,
                glowSize = baseConfig.glowSize / zoomFactor,
                barHeight = baseConfig.barHeight / zoomFactor,
                refreshButtonSize = baseConfig.refreshButtonSize / zoomFactor,
                refreshIconSize = baseConfig.refreshIconSize / zoomFactor
            ) else baseConfig

            val resolvedValueFontSize = resolveValueFontSize(
                height = height, // Name, Measurement, Timestamp
                displayNameFontSize = scaled.displayNameFontSize
            )

            return scaled.copy(valueFontSize = resolvedValueFontSize / zoomFactor)
        }

        private fun resolveValueFontSize(
            height: Dp,
            displayNameFontSize: TextUnit
        ): TextUnit {

            val verticalPadding = 12.dp
            val lineReserved = displayNameFontSize.value.dp * 1.3f
            val fixedElementsHeight = lineReserved * 3

            val totalReserved = verticalPadding + fixedElementsHeight
            val availableForValue = (height - totalReserved).coerceAtLeast(10.dp)
            val factor = if (height < 75.dp) 0.8f else 1.1f
            var valueFontSize = (availableForValue.value * factor).coerceIn(10f, 100f).sp
            if (valueFontSize > 24.sp) valueFontSize = 24.sp
            return valueFontSize
        }
    }
}
