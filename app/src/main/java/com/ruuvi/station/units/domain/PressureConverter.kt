package com.ruuvi.station.units.domain

import com.ruuvi.station.util.Utils

class PressureConverter {
    companion object {
        fun pascalToHectopascal(pressure: Double): Double = Utils.round(pressure / 100.0, 2)

        fun pascalToMmMercury(pressure: Double): Double = Utils.round(pressure / 133.322368, 2)

        fun pascalToInchMercury(pressure: Double): Double = Utils.round(pressure / 3386.388666, 2)
    }
}