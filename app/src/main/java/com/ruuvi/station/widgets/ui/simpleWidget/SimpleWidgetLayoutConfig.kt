package com.ruuvi.station.widgets.ui.simpleWidget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruuvi.station.app.ui.theme.ruuviStationFontsSizes
import com.ruuvi.station.widgets.ui.glance.WidgetContentPaddingDefaults
import com.ruuvi.station.widgets.ui.glance.WidgetTextMetrics
import com.ruuvi.station.widgets.ui.glance.scaledBy
import kotlin.math.roundToInt

data class SimpleWidgetLayoutConfig(
    val displayNameFontSize: TextUnit,
    val valueFontSize: TextUnit,
    val secondaryFontSize: TextUnit,
    val timestampFontSize: TextUnit,
    val measurementNameFontSize: TextUnit,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val inlineSpacing: Dp,
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

                height >= 120.dp -> SimpleWidgetLayoutConfig(
                    displayNameFontSize = ruuviStationFontsSizes.bigger,
                    valueFontSize = ruuviStationFontsSizes.extraHuge,
                    secondaryFontSize = ruuviStationFontsSizes.miniature,
                    timestampFontSize = ruuviStationFontsSizes.compact,
                    measurementNameFontSize = ruuviStationFontsSizes.tiny2,
                    horizontalPadding = WidgetContentPaddingDefaults.edgePadding,
                    verticalPadding = WidgetContentPaddingDefaults.edgePadding,
                    inlineSpacing = 2.dp,
                    unitPadding = 6.dp,
                    aqiMeasurementPadding = 4.dp,
                    dotSize = 6.dp,
                    glowSize = 12.dp,
                    barHeight = 3.dp
                )

                height >= 90.dp -> SimpleWidgetLayoutConfig(
                    displayNameFontSize = ruuviStationFontsSizes.big,
                    valueFontSize = ruuviStationFontsSizes.huge,
                    secondaryFontSize = ruuviStationFontsSizes.petite,
                    timestampFontSize = ruuviStationFontsSizes.miniature,
                    measurementNameFontSize = ruuviStationFontsSizes.tiny2,
                    horizontalPadding = WidgetContentPaddingDefaults.edgePadding,
                    verticalPadding = WidgetContentPaddingDefaults.edgePadding,
                    inlineSpacing = 2.dp,
                    unitPadding = 6.dp,
                    aqiMeasurementPadding = 4.dp,
                    dotSize = 6.dp,
                    glowSize = 12.dp,
                    barHeight = 3.dp
                )

                height >= 75.dp -> SimpleWidgetLayoutConfig(
                    displayNameFontSize = ruuviStationFontsSizes.normal,
                    valueFontSize = ruuviStationFontsSizes.bigger,
                    secondaryFontSize = ruuviStationFontsSizes.petite,
                    timestampFontSize = ruuviStationFontsSizes.miniature,
                    measurementNameFontSize = ruuviStationFontsSizes.tiny2,
                    horizontalPadding = WidgetContentPaddingDefaults.edgePadding,
                    verticalPadding = WidgetContentPaddingDefaults.edgePadding,
                    inlineSpacing = 2.dp,
                    unitPadding = 3.dp,
                    aqiMeasurementPadding = 2.dp,
                    dotSize = 5.dp,
                    glowSize = 12.dp,
                    barHeight = 2.dp
                )

                else -> SimpleWidgetLayoutConfig(
                    displayNameFontSize = ruuviStationFontsSizes.small,
                    valueFontSize = ruuviStationFontsSizes.big,
                    secondaryFontSize = ruuviStationFontsSizes.petite,
                    timestampFontSize = ruuviStationFontsSizes.miniature,
                    measurementNameFontSize = ruuviStationFontsSizes.tiny2,
                    horizontalPadding = WidgetContentPaddingDefaults.edgePadding,
                    verticalPadding = COMPACT_VERTICAL_PADDING,
                    inlineSpacing = 1.dp,
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
            displayNameFontSize = if (isTablet) 14.sp else 16.sp,
            valueFontSize = if (isTablet) 36.sp else 24.sp,
            secondaryFontSize = if (isTablet) 11.sp else 12.sp,
            timestampFontSize = if (isTablet) 11.sp else 12.sp,
            measurementNameFontSize = if (isTablet) 10.sp else 10.sp,
            horizontalPadding = WidgetContentPaddingDefaults.edgePadding,
            verticalPadding = if (isTablet) 8.dp else 6.dp,
            inlineSpacing = 1.dp,
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

private fun isHorizontalSize(width: Dp, height: Dp): Boolean =
    width.value.isFinite() &&
        height.value.isFinite() &&
        height.value > 0f &&
        width > height

private val COMPACT_VERTICAL_PADDING = 8.dp
private val SHORT_LAYOUT_HEIGHT = 75.dp

internal fun calculateAqiValueFontSize(config: SimpleWidgetLayoutConfig): TextUnit {
    val stackedInfoSize = config.secondaryFontSize.value * 2f +
        config.unitPadding.value +
        config.aqiMeasurementPadding.value
    return maxOf(config.valueFontSize.value, stackedInfoSize).sp
}

internal fun fitSimpleWidgetTypography(
    config: SimpleWidgetLayoutConfig,
    maximumValueFontSize: TextUnit = config.valueFontSize,
    fits: (SimpleWidgetLayoutConfig) -> Boolean
): SimpleWidgetLayoutConfig {
    var fitted = config
    if (!fits(config)) {
        var lowerScale = 0f
        var upperScale = 1f
        var smallerConfig: SimpleWidgetLayoutConfig? = null
        repeat(12) {
            val scale = (lowerScale + upperScale) / 2f
            val candidate = config.copy(
                displayNameFontSize = config.displayNameFontSize.scaledBy(scale),
                valueFontSize = config.valueFontSize.scaledBy(scale),
                secondaryFontSize = config.secondaryFontSize.scaledBy(scale),
                timestampFontSize = config.timestampFontSize.scaledBy(scale),
                measurementNameFontSize = config.measurementNameFontSize.scaledBy(scale)
            )
            if (fits(candidate)) {
                smallerConfig = candidate
                lowerScale = scale
            } else {
                upperScale = scale
            }
        }
        fitted = checkNotNull(smallerConfig) { "Simple widget text cannot fit within the available space" }
    }

    var lowerValueSize = fitted.valueFontSize.value
    var upperValueSize = maxOf(lowerValueSize, maximumValueFontSize.value)
    if (upperValueSize == lowerValueSize) return fitted

    repeat(12) {
        val candidate = fitted.copy(valueFontSize = ((lowerValueSize + upperValueSize) / 2f).sp)
        if (fits(candidate)) {
            fitted = candidate
            lowerValueSize = candidate.valueFontSize.value
        } else {
            upperValueSize = candidate.valueFontSize.value
        }
    }
    return fitted
}

internal fun reduceSimpleWidgetValueFontSize(fontSize: TextUnit): TextUnit =
    (fontSize.value - 8f).coerceAtLeast(minOf(fontSize.value, 2f)).sp

internal fun fitSimpleWidgetLabelFontSize(
    maximumFontSize: TextUnit,
    fits: (TextUnit) -> Boolean
): TextUnit {
    var lowerSize = 0f
    var upperSize = maximumFontSize.value
    repeat(12) {
        val candidate = (lowerSize + upperSize) / 2f
        if (fits(candidate.sp)) {
            lowerSize = candidate
        } else {
            upperSize = candidate
        }
    }
    check(lowerSize > 0f) { "Simple widget label cannot fit within its half of the value height" }
    return lowerSize.sp
}

internal data class SimpleWidgetLabelHeights(
    val unit: Dp,
    val measurement: Dp
)

internal fun calculateSimpleWidgetLabelHeights(
    valueHeight: Dp,
    density: Float
): SimpleWidgetLabelHeights {
    val heightPx = (valueHeight.value * density).roundToInt()
    val unitHeightPx = (heightPx + 1) / 2
    return SimpleWidgetLabelHeights(
        unit = (unitHeightPx / density).dp,
        measurement = ((heightPx - unitHeightPx) / density).dp
    )
}

internal data class SimpleWidgetTextLayout(
    val height: Dp,
    val unitTopPadding: Dp,
    val measurementTopPadding: Dp
)

internal fun calculateSimpleWidgetTextLayout(
    valueMetrics: WidgetTextMetrics,
    unitMetrics: WidgetTextMetrics,
    measurementMetrics: WidgetTextMetrics,
    density: Float
): SimpleWidgetTextLayout {
    fun pixels(value: Dp): Int = (value.value * density).roundToInt()

    val unitTop = (pixels(valueMetrics.glyphTop) - pixels(unitMetrics.glyphTop)).coerceAtLeast(0)
    val measurementTop = (pixels(valueMetrics.baseline) - pixels(measurementMetrics.baseline))
        .coerceAtLeast(0)
    // Native text can extend below the numeric baseline; keep its descenders clear of the timestamp.
    val height = maxOf(
        pixels(valueMetrics.height),
        unitTop + pixels(unitMetrics.height),
        measurementTop + pixels(measurementMetrics.height)
    )
    return SimpleWidgetTextLayout(
        height = (height / density).dp,
        unitTopPadding = (unitTop / density).dp,
        measurementTopPadding = (measurementTop / density).dp
    )
}

internal data class SimpleWidgetTextHeights(
    val displayName: Dp,
    val value: Dp,
    val secondary: Dp
)

internal data class SimpleWidgetContentVisibility(
    val showDisplayName: Boolean,
    val showMeasurementDescription: Boolean,
    val showTimestamp: Boolean
)

internal fun calculateSimpleWidgetContentVisibility(
    widgetHeight: Dp,
    config: SimpleWidgetLayoutConfig,
    textHeights: SimpleWidgetTextHeights,
    hasUnit: Boolean,
    isAirQuality: Boolean,
    primaryContentMandatory: Boolean = false
): SimpleWidgetContentVisibility {
    return SimpleWidgetContentVisibility(
        showDisplayName = true,
        showMeasurementDescription = true,
        showTimestamp = true
    )
}

private fun safeDpValue(value: Dp): Float =
    value.value.takeIf { it.isFinite() && it >= 0f } ?: 0f
