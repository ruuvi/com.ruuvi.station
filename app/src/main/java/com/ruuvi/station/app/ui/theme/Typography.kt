package com.ruuvi.station.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

data class RuuviStationTypography(
    val subtitle: TextStyle,
    val paragraph: TextStyle,
    val paragraphOnboarding: TextStyle,
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
    val dashboardBigValue: TextStyle,
    val dashboardBigValueUnit: TextStyle,
    val dashboardSecondary: TextStyle,
    val onboardingTitle: TextStyle,
    val onboardingSubtitle: TextStyle,
    val onboardingText: TextStyle,
    val otpChar: TextStyle,
    val emailTextField: TextStyle,
    val emailHintTextField: TextStyle,
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
        paragraphOnboarding = TextStyle(
            color = colors.onboardingTextColor,
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
            fontSize = ruuviStationFontsSizes.smallest,
            textAlign = TextAlign.Left
        ),
        dashboardBigValue = TextStyle(
            color = colors.settingsTitleText,
            fontFamily = ruuviStationFonts.oswaldBold,
            fontSize = ruuviStationFontsSizes.bigger,
            textAlign = TextAlign.Left
        ),
        dashboardBigValueUnit = TextStyle(
            color = colors.settingsTitleText,
            fontFamily = ruuviStationFonts.oswaldRegular,
            fontSize = ruuviStationFontsSizes.normal,
            textAlign = TextAlign.Left
        ),
        dashboardSecondary = TextStyle(
            color = colors.secondaryTextColor,
            fontFamily = ruuviStationFonts.mulishRegular,
            fontSize = ruuviStationFontsSizes.smallest,
        ),
        onboardingTitle = TextStyle(
            color = colors.onboardingTextColor,
            fontFamily = ruuviStationFonts.montserratExtraBold,
            fontSize = 36.sp
        ),
        onboardingSubtitle = TextStyle(
            color = colors.onboardingTextColor,
            fontFamily = ruuviStationFonts.mulishSemiBoldItalic,
            fontSize = 20.sp,
            lineHeight = 26.sp
        ),
        onboardingText = TextStyle(
            color = colors.onboardingTextColor,
            fontFamily = ruuviStationFonts.mulishRegular,
            fontSize = 16.sp,
            lineHeight = 20.sp
        ),
        otpChar = TextStyle(
            color = colors.onboardingTextColor,
            fontFamily = ruuviStationFonts.montserratExtraBold,
            fontSize = 30.sp
        ),
        emailTextField = TextStyle(
            color = Color.White,
            fontFamily = ruuviStationFonts.mulishRegular,
            fontSize = ruuviStationFontsSizes.normal
        ),
        emailHintTextField = TextStyle(
            color = Color.LightGray,
            fontFamily = ruuviStationFonts.mulishRegular,
            fontSize = ruuviStationFontsSizes.normal
        ),
    )
}