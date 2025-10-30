package com.ruuvi.station.units.domain.score

import com.ruuvi.station.bluetooth.util.extensions.roundHalfUp

object ScoreAqi: IScoreMeasurement {
    override fun score(value: Double): QualityRange {
        return when (value.roundHalfUp(0).toInt()) {
            in 0..9 -> QualityRange.VeryPoor
            in 10..49 -> QualityRange.Poor
            in 50..79 -> QualityRange.Fair
            in 80..89 -> QualityRange.Good
            else -> QualityRange.Excellent
        }
    }
}