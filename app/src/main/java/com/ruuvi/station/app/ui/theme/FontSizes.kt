package com.ruuvi.station.app.ui.theme

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.ruuvi.station.util.extensions.scaledSp

data class RuuviStationFontSizes(
    val smallest: TextUnit = 11.sp,
    val tiny: TextUnit = 12.sp,
    val small: TextUnit = 14.sp,
    val normal: TextUnit = 16.sp,
    val extended: TextUnit = 18.sp,
    val big: TextUnit = 20.sp,
    val bigger: TextUnit = 32.sp
)

val ruuviStationFontsSizes = RuuviStationFontSizes()