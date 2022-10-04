package com.ruuvi.station.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

object RuuviStationTheme {

    val colors: RuuviStationColors
        @Composable
        get() = LocalRuuviStationColors.current

    val typography: RuuviStationTypography
        @Composable
        get() = LocalRuuviStationTypography.current

    val fonts: RuuviStationFonts
        @Composable
        get() = LocalRuuviStationFonts.current

    val fontSizes: RuuviStationFontSizes
        @Composable
        get() = LocalRuuviStationFontSizes.current

    val dimensions: RuuviDimensions
        @Composable
        get() = LocalRuuviStationDimensions.current
}

val LocalRuuviStationColors = staticCompositionLocalOf<RuuviStationColors> {
    error("No colors provided")
}

val LocalRuuviStationTypography = staticCompositionLocalOf<RuuviStationTypography> {
    error("No typography provided")
}

val LocalRuuviStationFonts = staticCompositionLocalOf<RuuviStationFonts> {
    ruuviStationFonts
}

val LocalRuuviStationFontSizes = staticCompositionLocalOf<RuuviStationFontSizes> {
    ruuviStationFontsSizes
}

val LocalRuuviStationDimensions = staticCompositionLocalOf<RuuviDimensions> {
    ruuviDimensions
}