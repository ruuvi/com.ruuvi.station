package com.ruuvi.station.units.domain

import kotlin.math.E
import kotlin.math.log10
import kotlin.math.pow

/***
 * @see https://doi.org/10.1063/1.1461829
 */
data class HumidityConverter(val celsiusTemperature: Double, val rh: Double) {

    private val kelvinTemperature: Double = celsiusTemperature + 273.15 // kelvin K
    val fahrenheitTemperature: Double = (celsiusTemperature * 9.0 / 5.0) + 32.0 // fahrenheit °F
    val absoluteHumidity: Double by lazy { // absolute humidity g/m³
        cgkJ * (rh * pws()) / kelvinTemperature
    }
    val toDewCelsius: Double? by lazy { // dew point °C
        val m = m(c = celsiusTemperature)
        val a = a(c = celsiusTemperature)
        val tn = tn(c = celsiusTemperature)
        val pw = pws() * rh / 100.0
        if (m != null && a != null && tn != null) {
            tn / ((m / (log10(pw / a))) - 1.0)
        } else {
            null
        }
    }

    val toDewFahrenheit: Double? by lazy { // dew point °F
        if (toDewCelsius != null) {
            (toDewCelsius!! * 9.0 / 5.0) + 32.0
        } else {
            null
        }
    }

    val toDewKelvin: Double? by lazy { // dew point K
        if (toDewCelsius != null) {
            toDewCelsius!! + 273.15
        } else {
            null
        }
    }



    private fun pws(): Double {
        return if (celsiusTemperature > 0.01) { // estimate for 0°C-373°C
            val n = 1 - (kelvinTemperature / tc)
            val p = tc / kelvinTemperature * (c1 * n + c2 * n.pow(1.5) + c3 * n.pow(3) + c4 * n.pow(3.5) + c5 * n.pow(4) + c6 * n.pow(7.5))
            val l = E.pow(p)
            pc * l
        } else { // estimate for -100°C-0.01°C
            val n = kelvinTemperature / tn
            val p = a0 * (1 - n.pow(-1.5)) + a1 * (1 - n.pow(-1.25))
            val l = E.pow(p)
            pn * l
        }
    }

    private fun tn(c: Double): Double? {
        return when (c) {
            in -70.0..(-20.0 - Double.MIN_VALUE) -> 273.1466
            in -20.0..(50.0 - Double.MIN_VALUE) -> 240.7263
            in 50.0..(100.0 - Double.MIN_VALUE) -> 229.3975
            in 100.0..(150.0 - Double.MIN_VALUE) -> 225.1033
            in 150.0..(200.0 - Double.MIN_VALUE) -> 227.1704
            in 200.0..(350.0 - Double.MIN_VALUE) -> 263.1239
            else -> null
        }
    }

    private fun a(c: Double): Double? {
        return when (c) {
            in -70.0..(-20.0 - Double.MIN_VALUE) -> 6.114742
            in -20.0..(50.0 - Double.MIN_VALUE) -> 6.116441
            in 50.0..(100.0 - Double.MIN_VALUE) -> 6.004918
            in 100.0..(150.0 - Double.MIN_VALUE) -> 5.856548
            in 150.0..(200.0 - Double.MIN_VALUE) -> 6.002859
            in 200.0..(350.0 - Double.MIN_VALUE) -> 9.980622
            else -> null
        }
    }

    private fun m(c: Double): Double? {
        return when (c) {
            in -70.0..(-20.0 - Double.MIN_VALUE) -> 9.778707
            in -20.0..(50.0 - Double.MIN_VALUE) -> 7.591386
            in 50.0..(100.0 - Double.MIN_VALUE) -> 7.337936
            in 100.0..(150.0 - Double.MIN_VALUE) -> 7.27731
            in 150.0..(200.0 - Double.MIN_VALUE) -> 7.290361
            in 200.0..(350.0 - Double.MIN_VALUE) -> 7.388931
            else -> null
        }
    }

    companion object {
        private const val cgkJ: Double = 2.16679 // gk/J

        private const val tc: Double = 647.096 // K
        private const val c1: Double = -7.85951783
        private const val c2: Double = 1.84408259
        private const val c3: Double = -11.7866497
        private const val c4: Double = 22.6807411
        private const val c5: Double = -15.9618719
        private const val c6: Double = 1.80122502
        private const val pc: Double = 22064000.0 // Pa

        private const val tn: Double = 273.16 // K
        private const val a0: Double = -13.928169
        private const val a1: Double = 34.707823
        private const val pn: Double = 611.657 // Pa
    }
}