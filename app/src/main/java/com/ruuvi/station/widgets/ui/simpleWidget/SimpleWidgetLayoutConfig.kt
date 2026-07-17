package com.ruuvi.station.widgets.ui.simpleWidget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruuvi.station.app.ui.theme.ruuviStationFontsSizes

data class SimpleWidgetLayoutConfig(
    val displayNameFontSize: TextUnit,
    val valueFontSize: TextUnit,
    val unitFontSize: TextUnit,
    val secondaryFontSize: TextUnit,
    val sideTextTopOffset: Dp,
    val dotSize: Dp,
    val glowSize: Dp,
    val barHeight: Dp,
    val refreshButtonSize: Dp,
    val refreshIconSize: Dp
) {
    companion object {
        // Height anchors for smooth interpolation between the old small/medium/large tiers.
        private val TIER_SMALL_HEIGHT = 60.dp
        private val TIER_MEDIUM_HEIGHT = 80.dp
        private val TIER_LARGE_HEIGHT = 100.dp

        private val TIER_SMALL = SimpleWidgetLayoutConfig(
            displayNameFontSize = ruuviStationFontsSizes.petite,
            valueFontSize = ruuviStationFontsSizes.compact,
            unitFontSize = ruuviStationFontsSizes.petite,
            secondaryFontSize = ruuviStationFontsSizes.tiny,
            sideTextTopOffset = 3.dp,
            dotSize = 3.dp,
            glowSize = 8.dp,
            barHeight = 1.dp,
            refreshButtonSize = 28.dp,
            refreshIconSize = 12.dp
        )
        private val TIER_MEDIUM = SimpleWidgetLayoutConfig(
            displayNameFontSize = ruuviStationFontsSizes.normal,
            valueFontSize = ruuviStationFontsSizes.bigger,
            unitFontSize = ruuviStationFontsSizes.small,
            secondaryFontSize = ruuviStationFontsSizes.tiny2,
            sideTextTopOffset = 5.dp,
            dotSize = 5.dp,
            glowSize = 12.dp,
            barHeight = 2.dp,
            refreshButtonSize = 36.dp,
            refreshIconSize = 14.dp
        )
        private val TIER_LARGE = SimpleWidgetLayoutConfig(
            displayNameFontSize = ruuviStationFontsSizes.normal,
            valueFontSize = ruuviStationFontsSizes.huge,
            unitFontSize = ruuviStationFontsSizes.compact,
            secondaryFontSize = ruuviStationFontsSizes.petite,
            sideTextTopOffset = 7.dp,
            dotSize = 6.dp,
            glowSize = 12.dp,
            barHeight = 3.dp,
            refreshButtonSize = 40.dp,
            refreshIconSize = 16.dp
        )

        // Allow normal 2x1 Pixel Launcher widgets to reach the larger visual tier, while
        // still scaling down on genuinely narrow launchers.
        private val NARROW_WIDTH = 110.dp
        private val WIDE_WIDTH = 160.dp

        // Floors keep below-anchor extrapolation readable on very short launcher cells.
        private val MIN_DISPLAY_NAME_FONT = ruuviStationFontsSizes.tiny
        private val MIN_VALUE_FONT = ruuviStationFontsSizes.miniature
        private val MIN_UNIT_FONT = ruuviStationFontsSizes.tiny2
        private val MIN_SECONDARY_FONT = ruuviStationFontsSizes.micro
        private val MIN_SIDE_TEXT_TOP_OFFSET = 1.dp
        private val MIN_DOT_SIZE = 2.dp
        private val MIN_GLOW_SIZE = 6.dp
        private val MIN_BAR_HEIGHT = 1.dp
        private val MIN_REFRESH_BUTTON_SIZE = 20.dp
        private val MIN_REFRESH_ICON_SIZE = 10.dp

        fun fromSize(width: Dp, height: Dp): SimpleWidgetLayoutConfig {
            val widthFraction = ((width - NARROW_WIDTH) / (WIDE_WIDTH - NARROW_WIDTH)).coerceIn(0f, 1f)
            val widthCappedHeight = lerp(TIER_SMALL_HEIGHT, TIER_LARGE_HEIGHT, widthFraction)
            return fromHeight(minOf(height, widthCappedHeight))
        }

        // Extrapolate below the 60dp design anchor; some 1-row launcher cells are closer
        // to 40dp after density/display-size conversion.
        fun fromHeight(height: Dp): SimpleWidgetLayoutConfig {
            val clampedHeight = height.coerceAtMost(TIER_LARGE_HEIGHT)
            val extrapolated = if (clampedHeight <= TIER_MEDIUM_HEIGHT) {
                val t = (clampedHeight - TIER_SMALL_HEIGHT) / (TIER_MEDIUM_HEIGHT - TIER_SMALL_HEIGHT)
                lerp(TIER_SMALL, TIER_MEDIUM, t)
            } else {
                val t = (clampedHeight - TIER_MEDIUM_HEIGHT) / (TIER_LARGE_HEIGHT - TIER_MEDIUM_HEIGHT)
                lerp(TIER_MEDIUM, TIER_LARGE, t)
            }
            return extrapolated.coerceToMinimums()
        }

        private fun lerp(a: SimpleWidgetLayoutConfig, b: SimpleWidgetLayoutConfig, t: Float): SimpleWidgetLayoutConfig {
            return SimpleWidgetLayoutConfig(
                displayNameFontSize = lerp(a.displayNameFontSize, b.displayNameFontSize, t),
                valueFontSize = lerp(a.valueFontSize, b.valueFontSize, t),
                unitFontSize = lerp(a.unitFontSize, b.unitFontSize, t),
                secondaryFontSize = lerp(a.secondaryFontSize, b.secondaryFontSize, t),
                sideTextTopOffset = lerp(a.sideTextTopOffset, b.sideTextTopOffset, t),
                dotSize = lerp(a.dotSize, b.dotSize, t),
                glowSize = lerp(a.glowSize, b.glowSize, t),
                barHeight = lerp(a.barHeight, b.barHeight, t),
                refreshButtonSize = lerp(a.refreshButtonSize, b.refreshButtonSize, t),
                refreshIconSize = lerp(a.refreshIconSize, b.refreshIconSize, t)
            )
        }

        private fun lerp(a: Dp, b: Dp, t: Float): Dp = a + (b - a) * t

        private fun lerp(a: TextUnit, b: TextUnit, t: Float): TextUnit = (a.value + (b.value - a.value) * t).sp

        private fun SimpleWidgetLayoutConfig.coerceToMinimums(): SimpleWidgetLayoutConfig = copy(
            displayNameFontSize = displayNameFontSize.coerceAtLeast(MIN_DISPLAY_NAME_FONT),
            valueFontSize = valueFontSize.coerceAtLeast(MIN_VALUE_FONT),
            unitFontSize = unitFontSize.coerceAtLeast(MIN_UNIT_FONT),
            secondaryFontSize = secondaryFontSize.coerceAtLeast(MIN_SECONDARY_FONT),
            sideTextTopOffset = sideTextTopOffset.coerceAtLeast(MIN_SIDE_TEXT_TOP_OFFSET),
            dotSize = dotSize.coerceAtLeast(MIN_DOT_SIZE),
            glowSize = glowSize.coerceAtLeast(MIN_GLOW_SIZE),
            barHeight = barHeight.coerceAtLeast(MIN_BAR_HEIGHT),
            refreshButtonSize = refreshButtonSize.coerceAtLeast(MIN_REFRESH_BUTTON_SIZE),
            refreshIconSize = refreshIconSize.coerceAtLeast(MIN_REFRESH_ICON_SIZE)
        )

        private fun TextUnit.coerceAtLeast(minimum: TextUnit): TextUnit =
            if (value < minimum.value) minimum else this
    }
}
