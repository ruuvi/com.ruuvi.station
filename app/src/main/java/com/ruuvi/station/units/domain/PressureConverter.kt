package com.ruuvi.station.units.domain

class PressureConverter {
    companion object {
        fun pascalToHectopascal(pressure: Double): Double = pressure / 100.0

        fun pascalToMmMercury(pressure: Double): Double = pressure / 133.322368

        fun pascalToInchMercury(pressure: Double): Double = pressure / 3386.388666
    }
}