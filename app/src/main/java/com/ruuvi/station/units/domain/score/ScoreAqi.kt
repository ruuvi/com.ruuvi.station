package com.ruuvi.station.units.domain.score

import androidx.compose.ui.graphics.Color
import com.ruuvi.station.R
import com.ruuvi.station.bluetooth.util.extensions.roundHalfUp

object ScoreAqi: IScoreMeasurement {
    override fun score(value: Double): QualityRange {
        return QualityRange.Specific(
            color = getColorByScore(value),
            description = getDescriptionByScore(value)
        )
    }

    private fun getColorByScore(score: Double): Color {
        return when (score.roundHalfUp(0).toInt()) {
            in 0..9 -> UnhealthyColor
            10 -> UnhealthyToPoorColor
            in 11..49 -> PoorColor
            50 -> PoorToModerateColor
            in 51..79 -> ModerateColor
            80 -> ModerateToGoodColor
            in 81..89 -> GoodColor
            90 -> GoodToExcellentColor
            in 91..100 -> ExcellentColor
            else -> UnspecifiedColor
        }
    }

    private fun getDescriptionByScore(score: Double): Int{
        return when (score.roundHalfUp(0).toInt()) {
            in 0..10 -> R.string.aqi_level_1
            in 11 .. 50 -> R.string.aqi_level_2
            in 51 .. 80 -> R.string.aqi_level_3
            in 81 .. 90 -> R.string.aqi_level_4
            in 91 .. 100 -> R.string.aqi_level_5
            else -> R.string.aqi_level_0
        }
    }
}