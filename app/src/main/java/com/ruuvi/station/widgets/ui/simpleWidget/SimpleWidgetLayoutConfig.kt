package com.ruuvi.station.widgets.ui.simpleWidget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val barHeight: Dp,
    val style: SimpleWidgetLayoutStyle = SimpleWidgetLayoutStyle.STANDARD
) {
    companion object {
        fun fromSize(
            width: Dp,
            height: Dp,
            isTablet: Boolean = false,
            zoomFactor: Float = 1f
        ): SimpleWidgetLayoutConfig = if (
            height < SHORT_LAYOUT_HEIGHT ||
            isWideShortSize(width, height) ||
            (!isTablet && isHorizontalSize(width, height))
        ) {
            balancedShortConfig(isTablet)
        } else {
            fromHeight(height, zoomFactor)
        }

        fun fromHeight(
            height: Dp,
            @Suppress("UNUSED_PARAMETER")
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
                    verticalPadding = COMPACT_VERTICAL_PADDING,
                    inlineSpacing = 1.dp,
                    aqiBoxHeight = 18.dp,
                    unitPadding = 3.dp,
                    aqiMeasurementPadding = 3.dp,
                    dotSize = 3.dp,
                    glowSize = 8.dp,
                    barHeight = 1.dp
                )
            }

            return baseConfig
        }

        private fun balancedShortConfig(isTablet: Boolean) = SimpleWidgetLayoutConfig(
            displayNameFontSize = if (isTablet) 10.sp else 13.sp,
            valueFontSize = if (isTablet) 14.sp else 19.sp,
            secondaryFontSize = if (isTablet) 8.sp else 10.sp,
            horizontalPadding = WidgetContentPaddingDefaults.edgePadding,
            verticalPadding = if (isTablet) 8.dp else 6.dp,
            inlineSpacing = 1.dp,
            aqiBoxHeight = 20.dp,
            unitPadding = 2.dp,
            aqiMeasurementPadding = 2.dp,
            dotSize = 4.dp,
            glowSize = 8.dp,
            barHeight = 1.dp,
            style = SimpleWidgetLayoutStyle.BALANCED_SHORT
        )
    }
}

enum class SimpleWidgetLayoutStyle {
    STANDARD,
    BALANCED_SHORT
}

private fun isWideShortSize(width: Dp, height: Dp): Boolean =
    width.value.isFinite() &&
        height.value.isFinite() &&
        height.value > 0f &&
        width.value / height.value >= WIDE_SHORT_ASPECT_RATIO

private fun isHorizontalSize(width: Dp, height: Dp): Boolean =
    width.value.isFinite() &&
        height.value.isFinite() &&
        height.value > 0f &&
        width > height

private val COMPACT_VERTICAL_PADDING = 8.dp
private val SHORT_LAYOUT_HEIGHT = 75.dp
private const val WIDE_SHORT_ASPECT_RATIO = 1.6f

internal fun calculateAqiValueFontSize(
    config: SimpleWidgetLayoutConfig,
    showMeasurementDescription: Boolean
): TextUnit {
    val stackedInfoSize = if (showMeasurementDescription) {
        config.secondaryFontSize.value * 2f +
            config.unitPadding.value +
            config.aqiMeasurementPadding.value
    } else {
        config.secondaryFontSize.value + config.unitPadding.value
    }
    return maxOf(config.valueFontSize.value, stackedInfoSize).sp
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
    isAirQuality: Boolean,
    primaryContentMandatory: Boolean = false
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
    val showDisplayName = primaryContentMandatory ||
        usedHeight + displayNameHeight <= availableHeight
    if (showDisplayName) usedHeight += displayNameHeight
    if (primaryContentMandatory) usedHeight += secondaryHeight

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
    val showTimestamp = primaryContentMandatory ||
        usedHeight + secondaryHeight <= availableHeight
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
