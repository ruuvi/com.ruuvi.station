package com.ruuvi.station.units.domain.score

object ScorePM: IScoreMeasurement {
    override fun score(value: Double): QualityRange {
        return when {
            value <= 6 -> QualityRange.Excellent
            value <= 12 -> QualityRange.Good
            value <= 30 -> QualityRange.Fair
            value <= 55 -> QualityRange.Poor
            else -> QualityRange.VeryPoor
        }
    }
}