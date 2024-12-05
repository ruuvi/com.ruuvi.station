package com.ruuvi.station.units.domain.aqi

import com.ruuvi.station.bluetooth.util.extensions.roundHalfUp
import kotlin.math.max
import kotlin.math.sqrt

sealed class AQI (val score: Int?) {

    class GreenAQI (score: Int): AQI(score)
    class YellowAQI(score: Int): AQI(score)
    class RedAQI(score: Int): AQI(score)
    data object UndefinedAQI: AQI(null)

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
                val index = 100 - sqrt(distances.reduce{ acc, value -> acc + value * value} / distances.size).roundHalfUp(0).toInt()
                return when (index) {
                    in 0 .. 33 -> RedAQI(index)
                    in 34..66 -> YellowAQI(index)
                    in 67..100 -> GreenAQI(index)
                    else -> UndefinedAQI
                }

            } else {
                return UndefinedAQI
            }
        }

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
    }
}