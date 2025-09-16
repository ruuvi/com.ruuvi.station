package com.ruuvi.station.units.domain.score

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.ruuvi.station.R

sealed class QualityRange (
    val color: Color,
    @StringRes val description: Int
){
    object VeryPoor: QualityRange(color = VeryPoorColor, description = R.string.verypoor)
    object Poor: QualityRange(color = PoorColor, description = R.string.poor)
    object Fair: QualityRange(color = FairColor, description = R.string.fair)
    object Good: QualityRange(color = GoodColor, description = R.string.good)
    object Excellent: QualityRange(color = ExcellentColor, description = R.string.excellent)
}

val VeryPoorColor = Color(0xFFED5021)
val PoorColor = Color(0xFFF79C21)
val FairColor = Color(0xFFF7E13E)
val GoodColor = Color(0xFF96CC48)
val ExcellentColor = Color(0xFF4BC8B9)