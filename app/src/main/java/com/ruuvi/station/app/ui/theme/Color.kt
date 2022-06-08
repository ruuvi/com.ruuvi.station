package com.ruuvi.station.app.ui.theme

import androidx.compose.ui.graphics.Color

val White = Color.White
val Black = Color.Black
val Titan80 = Color(0xCC083C3D)
val Ruuvi = Color(0xFF158EA5)
val RuuviNew = Color(0xFF4BC9BA)
val Red = Color.Red
val Inactive = Color(0xFFE7EBEB)
val OnInactive = Color(0xFF8DA09F)
val Success = Color(0xFF35AD9F)
val Error = Color(0xFFF15A24)

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
    val errorText: Color
)

val lightPalette = RuuviStationColors(
    primary = Titan80,
    background = White,
    systemBars = Ruuvi,
    warning = Red,
    accent = RuuviNew,
    inactive = Inactive,
    onInactive = OnInactive,
    buttonText = White,
    successText = Success,
    errorText = Error
)

val darkPalette = RuuviStationColors(
    primary = White,
    background = Black,
    systemBars = Ruuvi,
    warning = Red,
    accent = RuuviNew,
    inactive = Inactive,
    onInactive = OnInactive,
    buttonText = White,
    successText = Success,
    errorText = Error
)