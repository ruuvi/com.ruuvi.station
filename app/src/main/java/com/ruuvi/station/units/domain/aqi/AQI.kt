package com.ruuvi.station.units.domain.aqi

import androidx.compose.ui.graphics.Color
import com.ruuvi.station.R
import com.ruuvi.station.bluetooth.util.extensions.roundHalfUp
import com.ruuvi.station.tag.domain.SensorMeasurements
import kotlin.math.max
import kotlin.math.sqrt

sealed class AQI (val score: Double?) {
    abstract val color: Color
    abstract val descriptionRes: Int
    abstract val scoreString: String

    class CalculatedAQI (score: Double): AQI(score) {
        override val color = getColorByScore(score)
        override val descriptionRes = getDescriptionByScore(score)
        override val scoreString: String = score.roundHalfUp(0).toInt().toString()
    }

    data object UndefinedAQI: AQI(null) {
        override val color = Color.Gray
        override val descriptionRes = R.string.aqi_level_0
        override val scoreString: String = "-"
    }

    companion object {
        fun getAQI(pm25: Double?,
                   co2: Int?
        ): AQI {
            val distances = mutableListOf<Double>()
            pm25?.let { distances.add(scorePpm(it)) }
            co2?.let { distances.add(scoreCO2(it)) }
            if (distances.size > 0) {
                val index = max(0.0, 100f - sqrt(distances.fold(0.0) { acc, value -> acc + value * value} / distances.size).roundHalfUp(1))
                return when (index) {
                    in 0.0..100.0 -> CalculatedAQI(index)
                    else -> UndefinedAQI
                }

            } else {
                return UndefinedAQI
            }
        }

        fun getAQI(measurements: SensorMeasurements) =
            getAQI(
                measurements.pm25?.value,
                measurements.co2?.value?.toInt(),
            )

        private fun scorePpm(pm: Double): Double {
            return max(0.0, (pm - 12) * 2)
        }

        private fun scoreVoc(voc: Int): Double {
            return max(0.0, voc - 200.0)
        }

        private fun scoreNox(nox: Int): Double {
            return max(0.0, nox - 200.0)
        }

        private fun scoreCO2(co2: Int): Double {
            return max(0.0, (co2 - 600.0) / 10.0)
        }

        private fun getColorByScore(score: Double): Color{
            return when (score.roundHalfUp(0).toInt()) {
                in 0..10 -> UnhealthyColor
                in 11 .. 30 -> PoorColor
                in 31 .. 70 -> ModerateColor
                in 71 .. 90 -> GoodColor
                in 91 .. 100 -> ExcellentColor
                else -> UnspecifiedColor
            }
        }

        private fun getDescriptionByScore(score: Double): Int{
            return when (score.roundHalfUp(0).toInt()) {
                in 0..10 -> R.string.aqi_level_1
                in 11 .. 30 -> R.string.aqi_level_2
                in 31 .. 70 -> R.string.aqi_level_3
                in 71 .. 90 -> R.string.aqi_level_4
                in 91 .. 100 -> R.string.aqi_level_5
                else -> R.string.aqi_level_0
            }
        }

        val UnspecifiedColor = Color.Gray
        val UnhealthyColor = Color(0xFFED5021)
        val PoorColor = Color(0xFFF79C21)
        val ModerateColor = Color(0xFFF7E13E)
        val GoodColor = Color(0xFF96CC48)
        val ExcellentColor = Color(0xFF4BC8B9)
    }
}