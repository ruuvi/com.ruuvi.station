package com.ruuvi.station.app.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
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
    val success: TextStyle,
    val syncStatusText: TextStyle,
    val menuItem: TextStyle,
    val dashboardValue: TextStyle,
    val dashboardUnit: TextStyle,
    val dashboardTemperature: TextStyle,
    val dashboardTemperatureUnit: TextStyle
)

fun provideTypography(colors: RuuviStationColors): RuuviStationTypography {
    return RuuviStationTypography(
        subtitle = TextStyle(
            color = colors.primary,
            fontFamily = ruuviStationFonts.mulishBold,
            fontSize = ruuviStationFontsSizes.normal,
        ),
        paragraph = TextStyle(
            color = colors.primary,
            fontFamily = ruuviStationFonts.mulishRegular,
            fontSize = ruuviStationFontsSizes.normal,
        ),
        paragraphSmall = TextStyle(
            color = colors.primary,
            fontFamily = ruuviStationFonts.mulishRegular,
            fontSize = ruuviStationFontsSizes.small,
        ),
        warning = TextStyle(
            color = colors.warning,
            fontFamily = ruuviStationFonts.mulishRegular,
            fontSize = ruuviStationFontsSizes.normal,
        ),
        buttonText = TextStyle(
            fontFamily = ruuviStationFonts.mulishExtraBold,
            fontSize = ruuviStationFontsSizes.normal,
            textAlign = TextAlign.Center
        ),
        textButtonText = TextStyle(
            fontFamily = ruuviStationFonts.mulishExtraBold,
            fontSize = ruuviStationFontsSizes.normal,
            textAlign = TextAlign.Center
        ),
        topBarText = TextStyle(
            color = colors.topBarText,
            fontFamily = ruuviStationFonts.mulishBold,
            fontSize = ruuviStationFontsSizes.big
        ),
        title = TextStyle(
            color = colors.settingsTitleText,
            fontFamily = ruuviStationFonts.mulishExtraBold,
            fontSize = ruuviStationFontsSizes.extended,
            textAlign = TextAlign.Left
        ),
        success = TextStyle(
            color = colors.successText,
            fontFamily = ruuviStationFonts.mulishRegular,
            fontSize = ruuviStationFontsSizes.normal,
        ),
        syncStatusText = TextStyle(
            color = White80,
            fontFamily = ruuviStationFonts.mulishRegular,
            fontSize = ruuviStationFontsSizes.normal,
        ),
        menuItem = TextStyle(
            color = colors.settingsTitleText,
            fontFamily = ruuviStationFonts.mulishBold,
            fontSize = ruuviStationFontsSizes.normal,
        ),
        dashboardValue = TextStyle(
            color = colors.primary,
            fontFamily = ruuviStationFonts.montserratBold,
            fontSize = ruuviStationFontsSizes.small,
            textAlign = TextAlign.Left
        ),
        dashboardUnit = TextStyle(
            color = colors.settingsTitleText,
            fontFamily = ruuviStationFonts.montserratRegular,
            fontSize = ruuviStationFontsSizes.tiny,
            textAlign = TextAlign.Left
        ),
        dashboardTemperature = TextStyle(
            color = colors.settingsTitleText,
            fontFamily = ruuviStationFonts.oswaldBold,
            fontSize = ruuviStationFontsSizes.bigger,
            textAlign = TextAlign.Left
        ),
        dashboardTemperatureUnit = TextStyle(
            color = colors.settingsTitleText,
            fontFamily = ruuviStationFonts.oswaldRegular,
            fontSize = ruuviStationFontsSizes.normal,
            textAlign = TextAlign.Left
        ),
    )
}