package com.ruuvi.station.units.domain.score

import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.units.model.UnitType

object QualityCalculator {
    fun calc(value: EnvironmentValue): QualityRange? {
        return when (value.unitType) {
            UnitType.AirQuality.AqiIndex -> ScoreAqi.score(value.value)
            UnitType.CO2.Ppm -> ScoreCo2.score(value.value)
            UnitType.PM.PM25 -> ScorePM.score(value.value)
            else -> null
        }
    }
}