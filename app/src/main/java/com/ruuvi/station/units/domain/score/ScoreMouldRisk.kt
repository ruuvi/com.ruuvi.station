package com.ruuvi.station.units.domain.score

import com.ruuvi.station.units.domain.mould.MouldRiskLevel

object ScoreMouldRisk : IScoreMeasurement {
    override fun score(value: Double): QualityRange = when (MouldRiskLevel.fromScore(value)) {
        MouldRiskLevel.VERY_LOW -> QualityRange.MouldVeryLow
        MouldRiskLevel.LOW -> QualityRange.MouldLow
        MouldRiskLevel.ELEVATED -> QualityRange.MouldElevated
        MouldRiskLevel.HIGH -> QualityRange.MouldHigh
        MouldRiskLevel.VERY_HIGH -> QualityRange.MouldVeryHigh
    }
}
