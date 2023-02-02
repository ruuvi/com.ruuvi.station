package com.ruuvi.station.app.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.ruuvi.station.R

data class RuuviStationFonts constructor(
    val mulishRegular: FontFamily = FontFamily(Font(R.font.mulish_regular)),
    val mulishBold: FontFamily = FontFamily(Font(R.font.mulish_bold)),
    val mulishExtraBold: FontFamily = FontFamily(Font(R.font.mulish_extrabold)),
    val mulishSemiBoldItalic: FontFamily = FontFamily(Font(R.font.mulish_semibolditalic)),

    val oswaldRegular: FontFamily = FontFamily(Font(R.font.oswald_regular)),
    val oswaldBold: FontFamily = FontFamily(Font(R.font.oswald_bold)),

    val montserratBold: FontFamily = FontFamily(Font(R.font.montserrat_bold_ttf)),
    val montserratRegular: FontFamily = FontFamily(Font(R.font.montserrat_regular)),
    val montserratExtraBold: FontFamily = FontFamily(Font(R.font.montserrat_extra_bold_ttf)),
)

val ruuviStationFonts = RuuviStationFonts()