package com.ruuvi.station.units.domain.score

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.ruuvi.station.R

sealed class QualityRange (
    val color: Color,
    @StringRes val description: Int
){
    object VeryPoor: QualityRange(color = UnhealthyColor, description = R.string.verypoor)
    object Poor: QualityRange(color = PoorColor, description = R.string.poor)
    object Moderate: QualityRange(color = ModerateColor, description = R.string.moderate)
    object Good: QualityRange(color = GoodColor, description = R.string.good)
    object Excellent: QualityRange(color = ExcellentColor, description = R.string.excellent)
    class Specific(color: Color, description: Int): QualityRange(color, description)
}

val UnspecifiedColor = Color.Gray
val UnhealthyColor = Color(0xFFED5021)
val UnhealthyToPoorColor = Color(0xFFF37921)
val PoorColor = Color(0xFFF79C21)
val PoorToModerateColor = Color(0xFFF8C239)
val ModerateColor = Color(0xFFF7E13E)
val ModerateToGoodColor = Color(0xFFCEDD51)
val GoodColor = Color(0xFF96CC48)
val GoodToExcellentColor = Color(0xFF74CD87)
val ExcellentColor = Color(0xFF4BC8B9)