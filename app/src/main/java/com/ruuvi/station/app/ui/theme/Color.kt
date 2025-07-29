package com.ruuvi.station.app.ui.theme

import androidx.compose.ui.graphics.Color

val White = Color.White
val White80 = Color(0xCCFFFFFF)
val White50 = Color(0x80FFFFFF)
val Black = Color.Black
val Titan = Color(0xFF083C3D)
val Titan50 = Color(0x80083C3D)
val Titan70 = Color(0xB3083C3D)
val Ruuvi = Color(0xFF158EA5)
val RuuviNew = Color(0xFFC6EEE6)
val Red = Color.Red
val Inactive = Color(0xFFE7EBEB)
val OnInactive = Color(0xFF8DA09F)
val Keppel = Color(0xFF35AD9F)
val Keppel15 = Color(0x2635AD9F)
val Keppel30 = Color(0x4D35AD9F)
val Keppel50 = Color(0x8035AD9F)
val Keppel70 = Color(0xB335AD9F)
val Elm = Color(0xFF1F9385)
val Error = Color(0xFFF15A24)
val Track = Color(0xFFDFEFEC)
val TrackInactive = Color(0xFF9DAFAF)
val Dark = Color(0xFF001D1B)
val Lines = Color(0xFFE5EAE9)
val Lines10 = Color(0xFF1F3836)
val Orange = Color(0xCCF48021)
val OrangeSolid = Color(0xFFF48021)
val Orange2 = Color(0xFFF8B075)
val Milky = Color(0xFFD6EAE7)
val Milky2 = Color(0xFFEEF2F2)
val DefaultSensorBackgroundDark = Color(0xFF2D605C)
val DefaultSensorBackgroundLight = Color(0xFFD8EDEA)

data class RuuviStationColors(
    val primary: Color,
    val background: Color,
    val systemBars: Color,
    val warning: Color,
    val accent: Color,
    val inactive: Color,
    val onInactive: Color,
    val buttonText: Color,
    val successText: Color,
    val errorText: Color,
    val trackColor: Color,
    val trackInactive: Color,
    val topBar: Color,
    val topBarText: Color,
    val settingsTitle: Color,
    val settingsSubTitle: Color,
    val settingsTitleText: Color,
    val divider: Color,
    val activeAlert: Color,
    val dashboardBackground: Color,
    val dashboardCardBackground: Color,
    val dashboardIcons: Color,
    val dashboardBurger: Color,
    val defaultSensorBackground: Color,
    val secondaryTextColor: Color,
    val backgroundAlpha: Float,
    val onboardingTextColor: Color,
    val dangerousButton: Color,
    val navigationTransparent: Color,
    val bannerBackground: Color,
    val indicatorColor: Color,
    val sensorValueBottomSheetBackground: Color,
    val chartLine: Color
)

val lightPalette = RuuviStationColors(
    primary = Titan,
    background = White,
    systemBars = Titan,
    warning = Red,
    accent = Keppel,
    inactive = Inactive,
    onInactive = OnInactive,
    buttonText = White,
    successText = Keppel,
    errorText = Error,
    trackColor = Track,
    trackInactive = TrackInactive,
    topBar = Titan,
    topBarText = White,
    settingsTitle = Milky,
    settingsSubTitle = Keppel15,
    settingsTitleText = Titan,
    divider = Lines,
    activeAlert = Orange,
    dashboardBackground = Milky2,
    dashboardCardBackground = White,
    dashboardIcons = Titan,
    dashboardBurger = Titan,
    defaultSensorBackground = DefaultSensorBackgroundLight,
    secondaryTextColor = Titan50,
    backgroundAlpha = 0.3f,
    onboardingTextColor = White,
    dangerousButton = OrangeSolid,
    navigationTransparent = White50,
    bannerBackground = RuuviNew,
    indicatorColor = Titan50,
    sensorValueBottomSheetBackground = Titan,
    chartLine = Keppel
)

val darkPalette = RuuviStationColors(
    primary = White80,
    background = Dark,
    systemBars = Titan,
    warning = Red,
    accent = Keppel,
    inactive = Inactive,
    onInactive = OnInactive,
    buttonText = White,
    successText = Keppel,
    errorText = Error,
    trackColor = Track,
    trackInactive = TrackInactive,
    topBar = Titan,
    topBarText = White,
    settingsTitle = Titan,
    settingsSubTitle = Titan50,
    settingsTitleText = White,
    divider = Lines10,
    activeAlert = Orange,
    dashboardBackground = Dark,
    dashboardCardBackground = Titan,
    dashboardIcons = Keppel,
    dashboardBurger = White,
    defaultSensorBackground = DefaultSensorBackgroundDark,
    secondaryTextColor = White50,
    backgroundAlpha = 0.75f,
    onboardingTextColor = White,
    dangerousButton = OrangeSolid,
    navigationTransparent = Titan50,
    bannerBackground = Keppel50,
    indicatorColor = Keppel50,
    sensorValueBottomSheetBackground = Titan,
    chartLine = Keppel
)