package com.ruuvi.station.app.preferences

import androidx.core.text.util.LocalePreferences
import com.ruuvi.station.units.model.UnitType.TemperatureUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class PreferencesTest {
    @Test
    fun `uses the OS Fahrenheit preference as the default temperature unit`() {
        assertEquals(
            TemperatureUnit.Fahrenheit.unitCode,
            Preferences.defaultTemperatureUnitCode(
                LocalePreferences.TemperatureUnit.FAHRENHEIT
            )
        )
    }

    @Test
    fun `uses the OS Celsius preference as the default temperature unit`() {
        assertEquals(
            TemperatureUnit.Celsius.unitCode,
            Preferences.defaultTemperatureUnitCode(
                LocalePreferences.TemperatureUnit.CELSIUS
            )
        )
    }

    @Test
    fun `uses the OS Kelvin preference as the default temperature unit`() {
        assertEquals(
            TemperatureUnit.Kelvin.unitCode,
            Preferences.defaultTemperatureUnitCode(
                LocalePreferences.TemperatureUnit.KELVIN
            )
        )
    }
}
