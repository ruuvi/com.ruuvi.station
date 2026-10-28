package com.ruuvi.station.widgets.ui.simpleWidget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruuvi.station.app.ui.theme.ruuviStationFontsSizes
import com.ruuvi.station.widgets.ui.glance.WidgetContentPaddingDefaults
import com.ruuvi.station.widgets.ui.glance.WidgetTextMetrics
import com.ruuvi.station.widgets.ui.glance.scaledBy

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
    fits: (SimpleWidgetLayoutConfig) -> Boolean
): SimpleWidgetLayoutConfig {
    if (fits(config)) return config

    var lowerScale = 0f
    var upperScale = 1f
    var fitted: SimpleWidgetLayoutConfig? = null
    repeat(12) {
        val scale = (lowerScale + upperScale) / 2f
        val candidate = config.copy(
            displayNameFontSize = config.displayNameFontSize.scaledBy(scale),
            valueFontSize = config.valueFontSize.scaledBy(scale),
            secondaryFontSize = config.secondaryFontSize.scaledBy(scale),
            timestampFontSize = config.timestampFontSize.scaledBy(scale)
        )
        if (fits(candidate)) {
            fitted = candidate
            lowerScale = scale
        } else {
            upperScale = scale
        }
    }
    return checkNotNull(fitted) { "Simple widget text cannot fit within the available space" }
}

internal data class SimpleWidgetAqiTextLayout(
    val valueTopPadding: Dp,
    val unitTopPadding: Dp,
    val measurementTopPadding: Dp,
    val height: Dp
)

internal fun calculateAqiMinimumValueHeight(
    unit: WidgetTextMetrics,
    measurement: WidgetTextMetrics,
    indicatorSize: Dp,
    spacing: Dp
): Dp {
    val unitRowHeight = maxOf(unit.height, indicatorSize)
    val unitTextTop = (unitRowHeight - unit.height) / 2
    val unitGlyphTop = unitTextTop + unit.glyphTop
    val unitContentBottom = maxOf(
        unitTextTop + unit.glyphBottom,
        (unitRowHeight + indicatorSize) / 2
    )
    return unitContentBottom - unitGlyphTop +
        measurement.baseline - measurement.glyphTop + spacing
}

internal fun calculateAqiTextLayout(
    value: WidgetTextMetrics,
    unit: WidgetTextMetrics,
    measurement: WidgetTextMetrics,
    indicatorSize: Dp,
    measurementUnitTopSpacing: Dp = 0.dp
): SimpleWidgetAqiTextLayout {
    val unitRowHeight = maxOf(unit.height, indicatorSize)
    val unitTextTop = (unitRowHeight - unit.height) / 2
    val unitTop = value.glyphTop - unit.glyphTop - unitTextTop - measurementUnitTopSpacing
    val measurementTop = value.baseline - measurement.baseline
    val valueTop = maxOf(0.dp, -unitTop, -measurementTop)

    return SimpleWidgetAqiTextLayout(
        valueTopPadding = valueTop,
        unitTopPadding = valueTop + unitTop,
        measurementTopPadding = valueTop + measurementTop,
        height = maxOf(
            valueTop + value.height,
            valueTop + unitTop + unitRowHeight,
            valueTop + measurementTop + measurement.height
        )
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
