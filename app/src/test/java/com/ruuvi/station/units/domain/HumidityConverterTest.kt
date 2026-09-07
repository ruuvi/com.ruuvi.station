package com.ruuvi.station.units.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HumidityConverterTest {
    @Test
    fun `relative humidity is accepted as percent`() {
        val converter = HumidityConverter(
            celsiusTemperature = 20.0,
            relativeHumidityPercent = 50.0,
        )

        assertEquals(8.6450, converter.absoluteHumidity, 0.001)
        assertEquals(9.2715, converter.toDewCelsius ?: Double.NaN, 0.001)
        assertEquals(48.6887, converter.toDewFahrenheit ?: Double.NaN, 0.001)
        assertEquals(282.4215, converter.toDewKelvin ?: Double.NaN, 0.001)
    }

    @Test
    fun `dew point remains unavailable outside coefficient range`() {
        assertNull(HumidityConverter(-80.0, 50.0).toDewCelsius)
        assertNull(HumidityConverter(360.0, 50.0).toDewCelsius)
    }
}
