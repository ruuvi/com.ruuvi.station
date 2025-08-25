package com.ruuvi.station.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun RuuviTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable() () -> Unit
) {
    val colors = if (darkTheme) darkPalette else lightPalette

    val typography = provideTypography(colors)

    CompositionLocalProvider(
        LocalRuuviStationColors provides colors,
        LocalRuuviStationTypography provides typography,
        LocalRuuviStationFonts provides ruuviStationFonts,
        LocalRuuviStationFontSizes provides ruuviStationFontsSizes,
        content = content
    )
}