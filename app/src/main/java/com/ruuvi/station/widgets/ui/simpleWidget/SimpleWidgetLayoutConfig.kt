package com.ruuvi.station.widgets.ui.simpleWidget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.ruuvi.station.app.ui.theme.ruuviStationFontsSizes
import com.ruuvi.station.widgets.ui.glance.WidgetContentPaddingDefaults

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
                    horizontalPadding = WidgetContentPaddingDefaults.edgePadding,
                    verticalPadding = WidgetContentPaddingDefaults.edgePadding,
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
                    horizontalPadding = WidgetContentPaddingDefaults.edgePadding,
                    verticalPadding = WidgetContentPaddingDefaults.edgePadding,
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
                    horizontalPadding = WidgetContentPaddingDefaults.edgePadding,
                    verticalPadding = WidgetContentPaddingDefaults.edgePadding,
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

internal data class SimpleWidgetTextHeights(
    val displayName: Dp,
    val value: Dp,
    val secondary: Dp
)

internal data class SimpleWidgetContentVisibility(
    val showDisplayName: Boolean,
    val showMeasurementDescription: Boolean,
    val showAqiProgress: Boolean,
    val showTimestamp: Boolean,
    val aqiInfoHeight: Dp
)

internal fun calculateSimpleWidgetContentVisibility(
    widgetHeight: Dp,
    config: SimpleWidgetLayoutConfig,
    textHeights: SimpleWidgetTextHeights,
    hasUnit: Boolean,
    isAirQuality: Boolean
): SimpleWidgetContentVisibility {
    val availableHeight =
        (safeDpValue(widgetHeight) - (safeDpValue(config.verticalPadding) * 2f))
            .coerceAtLeast(0f)
    val displayNameHeight = safeDpValue(textHeights.displayName)
    val valueHeight = safeDpValue(textHeights.value)
    val secondaryHeight = safeDpValue(textHeights.secondary)
    val unitHeight = if (hasUnit || isAirQuality) {
        secondaryHeight + safeDpValue(config.unitPadding)
    } else {
        0f
    }
    val aqiInfoHeightWithoutDescription = unitHeight
    val valueRowHeight = maxOf(valueHeight, unitHeight)

    var usedHeight = valueRowHeight
    val showDisplayName = usedHeight + displayNameHeight <= availableHeight
    if (showDisplayName) usedHeight += displayNameHeight

    val aqiInfoHeightWithDescription = maxOf(
        safeDpValue(config.aqiBoxHeight),
        secondaryHeight * 2f +
            safeDpValue(config.unitPadding) +
            safeDpValue(config.aqiMeasurementPadding)
    )
    val measurementDescriptionHeight = if (isAirQuality) {
        maxOf(valueHeight, aqiInfoHeightWithDescription) - valueRowHeight
    } else {
        secondaryHeight
    }
    val showMeasurementDescription =
        usedHeight + measurementDescriptionHeight <= availableHeight
    if (showMeasurementDescription) usedHeight += measurementDescriptionHeight

    val aqiProgressHeight =
        safeDpValue(config.glowSize) + SIMPLE_WIDGET_AQI_PROGRESS_BOTTOM_PADDING.value
    val showAqiProgress =
        isAirQuality && usedHeight + aqiProgressHeight <= availableHeight
    if (showAqiProgress) usedHeight += aqiProgressHeight

    val showTimestamp = usedHeight + secondaryHeight <= availableHeight
    val aqiInfoHeight = if (showMeasurementDescription) {
        aqiInfoHeightWithDescription
    } else {
        aqiInfoHeightWithoutDescription
    }

    return SimpleWidgetContentVisibility(
        showDisplayName = showDisplayName,
        showMeasurementDescription = showMeasurementDescription,
        showAqiProgress = showAqiProgress,
        showTimestamp = showTimestamp,
        aqiInfoHeight = aqiInfoHeight.dp
    )
}

internal val SIMPLE_WIDGET_AQI_PROGRESS_BOTTOM_PADDING = 2.dp

private fun safeDpValue(value: Dp): Float =
    value.value.takeIf { it.isFinite() && it >= 0f } ?: 0f
