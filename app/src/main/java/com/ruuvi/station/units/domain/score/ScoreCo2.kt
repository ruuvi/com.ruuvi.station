package com.ruuvi.station.units.domain.score

object ScoreCo2: IScoreMeasurement {
    override fun score(value: Double): QualityRange {
        return when {
            value <= 600 -> QualityRange.Excellent
            value <= 800 -> QualityRange.Good
            value <= 1400 -> QualityRange.Fair
            value <= 2100 -> QualityRange.Poor
            else -> QualityRange.VeryPoor
        }
    }
}