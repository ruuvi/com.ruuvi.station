package com.ruuvi.station.units.domain.aqi

import androidx.compose.ui.graphics.Color
import com.ruuvi.station.R
import com.ruuvi.station.bluetooth.util.extensions.roundHalfUp
import com.ruuvi.station.tag.domain.SensorMeasurements
import kotlin.math.max

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
            if (pm25 == null || co2 == null ) return UndefinedAQI

            val pm25Clamped = pm25.coerceIn(PM25_MIN, PM25_MAX)
            val co2Clamped = co2.coerceIn(CO2_MIN, CO2_MAX)

            val dx = (pm25Clamped - PM25_MIN) * PM25_SCALE
            val dy = (co2Clamped - CO2_MIN) * CO2_SCALE

            val r = kotlin.math.hypot(dx, dy)

            return CalculatedAQI((AQI_MAX - r).coerceIn(0.0, AQI_MAX).roundHalfUp(2))
        }

        fun getAQI(measurements: SensorMeasurements) =
            getAQI(
                measurements.pm25?.value,
                measurements.co2?.value?.toInt(),
            )

        private fun scorePpm(pm: Double): Double {
            return max(0.0, (pm - 12) * 2)
        }

        private fun scoreCO2(co2: Int): Double {
            return max(0.0, (co2 - 600.0) / 10.0)
        }

        private fun getColorByScore(score: Double): Color{
            return when (score.roundHalfUp(0).toInt()) {
                in 0..9 -> UnhealthyColor
                10 -> UnhealthyToPoorColor
                in 11 .. 49 -> PoorColor
                50 -> PoorToModerateColor
                in 51 .. 79 -> ModerateColor
                80 ->  ModerateToGoodColor
                in 81 .. 89 -> GoodColor
                90 -> GoodToExcellentColor
                in 91 .. 100 -> ExcellentColor
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

        const val AQI_MAX = 100.0
        const val PM25_MAX = 60.0
        const val PM25_MIN = 0.0
        const val PM25_SCALE = AQI_MAX / (PM25_MAX - PM25_MIN)

        const val CO2_MAX = 2300
        const val CO2_MIN = 420
        const val CO2_SCALE  = AQI_MAX / (CO2_MAX - CO2_MIN)
    }
}