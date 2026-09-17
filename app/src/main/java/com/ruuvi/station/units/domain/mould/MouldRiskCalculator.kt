package com.ruuvi.station.units.domain.mould

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Current conditions at the sensor, not detected mould or accumulated growth.
 * The critical-RH reference comes from Ojanen et al. (2007), equation 1:
 * https://bwk.kuleuven.be/bwf/projects/annex41/protected/data/VTT%20Oct%202007%20Paper%20A41-T4-Fin-07-1.pdf
 * The 80% floor, 0–100 mapping and categories are Ruuvi product choices.
 */
object MouldRiskCalculator {
    fun calculate(temperatureCelsius: Double?, relativeHumidity: Double?): MouldRiskResult {
        if (temperatureCelsius == null || relativeHumidity == null) {
            return MouldRiskResult.Unavailable(MouldRiskUnavailableReason.MISSING_INPUT)
        }
        if (!temperatureCelsius.isFinite() || !relativeHumidity.isFinite() ||
            relativeHumidity !in 0.0..100.0
        ) {
            return MouldRiskResult.Unavailable(MouldRiskUnavailableReason.INVALID_INPUT)
        }
        if (temperatureCelsius <= 0.0 || temperatureCelsius >= 50.0) {
            return MouldRiskResult.Unavailable(MouldRiskUnavailableReason.TEMPERATURE_OUT_OF_RANGE)
        }
        val t = temperatureCelsius
        val referenceRh = if (t < 20.0) {
            max(80.0, -0.00267 * t * t * t + 0.160 * t * t - 3.13 * t + 100.0)
        } else {
            80.0
        }
        return MouldRiskResult.Available((50.0 + 2.5 * (relativeHumidity - referenceRh)).coerceIn(0.0, 100.0))
    }
}

sealed class MouldRiskResult {
    abstract val score: Double?

    data class Available(override val score: Double) : MouldRiskResult() {
        val displayedScore: Int get() = score.roundToInt()
        val level: MouldRiskLevel get() = MouldRiskLevel.fromScore(score)
    }

    data class Unavailable(val reason: MouldRiskUnavailableReason) : MouldRiskResult() {
        override val score: Double? = null
    }
}

enum class MouldRiskUnavailableReason { MISSING_INPUT, INVALID_INPUT, TEMPERATURE_OUT_OF_RANGE }

enum class MouldRiskLevel {
    VERY_LOW, LOW, ELEVATED, HIGH, VERY_HIGH;

    companion object {
        fun fromScore(score: Double): MouldRiskLevel {
            require(score.isFinite() && score in 0.0..100.0)
            return when (score.roundToInt()) {
                in 0..9 -> VERY_LOW
                in 10..24 -> LOW
                in 25..49 -> ELEVATED
                in 50..74 -> HIGH
                else -> VERY_HIGH
            }
        }
    }
}
