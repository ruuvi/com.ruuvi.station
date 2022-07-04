package com.ruuvi.station.app.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.ruuvi.station.R

data class RuuviStationFonts(
    val regular: FontFamily = FontFamily(Font(R.font.mulish_regular)),
    val bold: FontFamily = FontFamily(Font(R.font.mulish_bold)),
    val extraBold: FontFamily = FontFamily(Font(R.font.mulish_extrabold)),
)

val ruuviStationFonts = RuuviStationFonts()