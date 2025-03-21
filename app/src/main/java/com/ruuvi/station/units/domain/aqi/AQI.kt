package com.ruuvi.station.units.domain.aqi

import androidx.compose.ui.graphics.Color
import com.ruuvi.station.R
import com.ruuvi.station.bluetooth.util.extensions.roundHalfUp
import com.ruuvi.station.tag.domain.SensorMeasurements
import kotlin.math.max
import kotlin.math.sqrt

sealed class AQI (val score: Int?) {
    abstract val color: Color
    abstract val descriptionRes: Int

    class CalculatedAQI (score: Int): AQI(score) {
        override val color = getColorByScore(score)
        override val descriptionRes = getDescriptionByScore(score)
    }

    data object UndefinedAQI: AQI(null) {
        override val color = Color.Gray
        override val descriptionRes = R.string.aqi_level_0
    }

    companion object {
        fun getAQI(pm25: Double?,
                   co2: Int?,
                   voc: Int?,
                   nox: Int?
        ): AQI {
            val distances = mutableListOf<Double>()
            pm25?.let { distances.add(scorePpm(it)) }
            voc?.let { distances.add(scoreVoc(it)) }
            nox?.let { distances.add(scoreNox(it)) }
            co2?.let { distances.add(scoreCO2(it)) }
            if (distances.size > 0) {
                val index = max(0, 100 - sqrt(distances.fold(0.0) { acc, value -> acc + value * value} / distances.size).roundHalfUp(0).toInt())
                return when (index) {
                    in 0..100 -> CalculatedAQI(index)
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
                measurements.voc?.value?.toInt(),
                measurements.nox?.value?.toInt()
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

        private fun getColorByScore(score: Int): Color{
            return when (score) {
                in 0..33 -> Color.Red
                in 34 .. 66 -> Color.Yellow
                in 67 .. 100 -> Color.Green
                else -> Color.Gray
            }
        }

        private fun getDescriptionByScore(score: Int): Int{
            return when (score) {
                in 0..20 -> R.string.aqi_level_1
                in 21 .. 40 -> R.string.aqi_level_2
                in 41 .. 60 -> R.string.aqi_level_3
                in 61 .. 80 -> R.string.aqi_level_4
                in 80 .. 100 -> R.string.aqi_level_5
                else -> R.string.aqi_level_0
            }
        }
    }
}