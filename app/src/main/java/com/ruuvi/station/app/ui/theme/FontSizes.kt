package com.ruuvi.station.app.ui.theme

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

data class RuuviStationFontSizes(
    val tiny: TextUnit = 9.5.sp,
    val petite: TextUnit = 11.sp,
    val miniature: TextUnit = 12.sp,
    val compact: TextUnit = 14.sp,
    val normal: TextUnit = 16.sp,
    val extended: TextUnit = 18.sp,
    val big: TextUnit = 20.sp,
    val huge: TextUnit = 32.sp
)

val ruuviStationFontsSizes = RuuviStationFontSizes()