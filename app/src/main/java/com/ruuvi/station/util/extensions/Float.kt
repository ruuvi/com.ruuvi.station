package com.ruuvi.station.util.extensions

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

fun Float.equalsEpsilon(other: Float, epsilon: Float = 0.00000001f) = abs(this - other) < epsilon

fun Float.isInteger(epsilon: Float = 0.00000001f) = this.equalsEpsilon(this.round(0), epsilon)

fun Float.round(places: Int): Float {
    require(places >= 0)
    var bd = BigDecimal.valueOf(this.toDouble())
    bd = bd.setScale(places, RoundingMode.HALF_UP)
    return bd.toFloat()
}

fun Float.diff(second: Float): Float = Math.abs(this - second)