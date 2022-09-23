package com.ruuvi.station.app.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign

data class RuuviStationTypography(
    val subtitle: TextStyle,
    val paragraph: TextStyle,
    val paragraphSmall: TextStyle,
    val warning: TextStyle,
    val buttonText: TextStyle,
    val textButtonText: TextStyle,
    val topBarText: TextStyle,
    val title: TextStyle,
    val success: TextStyle
 )

fun provideTypography(colors: RuuviStationColors): RuuviStationTypography {
    return RuuviStationTypography(
        subtitle = TextStyle(
            color = colors.primary,
            fontFamily = ruuviStationFonts.bold,
            fontSize = ruuviStationFontsSizes.normal,
        ),
        paragraph = TextStyle(
            color = colors.primary,
            fontFamily = ruuviStationFonts.regular,
            fontSize = ruuviStationFontsSizes.normal,
        ),
        paragraphSmall = TextStyle(
            color = colors.primary,
            fontFamily = ruuviStationFonts.regular,
            fontSize = ruuviStationFontsSizes.small,
        ),
        warning = TextStyle(
            color = colors.warning,
            fontFamily = ruuviStationFonts.regular,
            fontSize = ruuviStationFontsSizes.normal,
        ),
        buttonText = TextStyle(
            fontFamily = ruuviStationFonts.extraBold,
            fontSize = ruuviStationFontsSizes.normal,
            textAlign = TextAlign.Center
        ),
        textButtonText = TextStyle(
            fontFamily = ruuviStationFonts.extraBold,
            fontSize = ruuviStationFontsSizes.normal,
            textAlign = TextAlign.Center
        ),
        topBarText = TextStyle(
            color = colors.topBarText,
            fontFamily = ruuviStationFonts.bold,
            fontSize = ruuviStationFontsSizes.big
        ),
        title = TextStyle(
            color = colors.settingsTitleText,
            fontFamily = ruuviStationFonts.extraBold,
            fontSize = ruuviStationFontsSizes.extended,
            textAlign = TextAlign.Left
        ),
        success = TextStyle(
            color = colors.successText,
            fontFamily = ruuviStationFonts.regular,
            fontSize = ruuviStationFontsSizes.normal,
        ),
    )
}