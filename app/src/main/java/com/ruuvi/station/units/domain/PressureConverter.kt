package com.ruuvi.station.units.domain

import com.ruuvi.station.util.Utils

class PressureConverter {
    companion object {
        const val mmMercuryMultiplier  = 133.322368
        const val inchMercuryMultiplier = 3386.388666
        const val hectoMultiplier = 100.0

        fun pascalToHectopascal(pressure: Double): Double = Utils.round(pressure / hectoMultiplier, 2)

        fun pascalToMmMercury(pressure: Double): Double = Utils.round(pressure / mmMercuryMultiplier, 2)

        fun pascalToInchMercury(pressure: Double): Double = Utils.round(pressure / inchMercuryMultiplier, 2)

        fun hectopascalToPascal(pressure: Double): Double = Utils.round(pressure * hectoMultiplier, 2)

        fun mmMercuryToPascal(pressure: Double): Double = Utils.round(pressure * mmMercuryMultiplier, 2)

        fun inchMercuryToPascal(pressure: Double): Double = Utils.round(pressure * inchMercuryMultiplier, 2)
    }
}