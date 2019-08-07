package com.ruuvi.station.util

import kotlin.math.E
import kotlin.math.log10
import kotlin.math.pow

data class Humidity(val c: Double, val rh: Double) {

    val k: Double = c + 273.15 // kelvin °K
    val f: Double = (c * 9.0/5.0) + 32.0 // fahrenheit °F
    val ah: Double by lazy { // absolute humidity g/m³
        cgkJ * (rh * Pws()) / k
    }
    val Td: Double? by lazy { // dew point °C
        val m = m(c = c)
        val A = A(c = c)
        val Tn = Tn(c = c)
        val Pw = Pws() * rh / 100.0
        if (m != null && A != null && Tn != null) {
            Tn / ((m / (log10(Pw / A))) - 1.0)
        } else {
            null
        }
    }

    val TdF: Double? by lazy { // dew point °F
        if (Td != null) {
            (Td!! * 9.0 / 5.0) + 32.0
        } else {
            null
        }
    }

    private val cgkJ: Double = 2.16679 // gk/J

    private val Tc: Double = 647.096 // K
    private val c1: Double = -7.85951783
    private val c2: Double = 1.84408259
    private val c3: Double = -11.7866497
    private val c4: Double = 22.6807411
    private val c5: Double = -15.9618719
    private val c6: Double = 1.80122502
    private val Pc: Double = 22064000.0 // Pa

    private val Tn: Double = 273.16 // K
    private val a0: Double = -13.928169
    private val a1: Double = 34.707823
    private val Pn: Double = 611.657 // Pa

    private fun Pws(): Double {
        if (c > 0.01) { // estimate for 0°C-373°C
            val n = 1 - (k / Tc)
            val p = Tc / k * (c1 * n + c2 * n.pow(1.5) + c3 * n.pow(3) + c4 * n.pow(3.5) + c5 * n.pow(4) + c6 * n.pow(7.5))
            val l = E.pow(p)
            return Pc * l
        } else { // estimate for -100°C-0.01°C
            val n = k / Tn
            val p = a0 * (1 - n.pow(-1.5)) + a1 * (1 - n.pow(-1.25))
            val l = E.pow(p)
            return Pn * l
        }
    }

    private fun Tn(c: Double): Double? {
        when(c) {
            in -70.0..(-20.0 - Double.MIN_VALUE) -> return 273.1466
            in -20.0..(50.0 - Double.MIN_VALUE) -> return 240.7263
            in 50.0..(100.0 - Double.MIN_VALUE) -> return 229.3975
            in 100.0..(150.0 - Double.MIN_VALUE) -> return 225.1033
            in 150.0..(200.0 - Double.MIN_VALUE) -> return 227.1704
            in 200.0..(350.0 - Double.MIN_VALUE) -> return 263.1239
            else -> return null
        }
    }

    private fun A(c: Double): Double? {
        when (c) {
            in -70.0..(-20.0 - Double.MIN_VALUE) -> return 6.114742
            in -20.0..(50.0 - Double.MIN_VALUE) -> return 6.116441
            in 50.0..(100.0 - Double.MIN_VALUE) -> return 6.004918
            in 100.0..(150.0 - Double.MIN_VALUE) -> return 5.856548
            in 150.0..(200.0 - Double.MIN_VALUE) -> return 6.002859
            in 200.0..(350.0 - Double.MIN_VALUE) -> return 9.980622
            else -> return null
        }
    }

    private fun m(c: Double): Double? {
        when (c) {
            in -70.0..(-20.0 - Double.MIN_VALUE) -> return 9.778707
            in -20.0..(50.0 - Double.MIN_VALUE) -> return 7.591386
            in 50.0..(100.0 - Double.MIN_VALUE) -> return 7.337936
            in 100.0..(150.0 - Double.MIN_VALUE) -> return 7.27731
            in 150.0..(200.0 - Double.MIN_VALUE) -> return 7.290361
            in 200.0..(350.0 - Double.MIN_VALUE) -> return 7.388931
            else -> return null
        }
    }
}