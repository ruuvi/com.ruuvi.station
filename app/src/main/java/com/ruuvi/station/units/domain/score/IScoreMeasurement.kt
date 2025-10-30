package com.ruuvi.station.units.domain.score

interface IScoreMeasurement {
    fun score(value: Double): QualityRange
}