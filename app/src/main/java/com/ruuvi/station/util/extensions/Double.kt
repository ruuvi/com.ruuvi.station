package com.ruuvi.station.util.extensions

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

fun Double.equalsEpsilon(other: Double, epsilon: Double = 0.00000001) = abs(this - other) < epsilon

fun Double.diff(second: Double): Double = Math.abs(this - second)

fun Double.round(places: Int): Double {
    require(places >= 0)
    var bd = BigDecimal.valueOf(this)
    bd = bd.setScale(places, RoundingMode.HALF_UP)
    return bd.toDouble()
}