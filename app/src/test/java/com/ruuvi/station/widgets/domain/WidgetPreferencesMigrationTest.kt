package com.ruuvi.station.widgets.domain

import com.ruuvi.station.units.model.UnitType
import com.ruuvi.station.widgets.data.WidgetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetPreferencesMigrationTest {
    @Test
    fun `simple legacy measurements snapshot every global unit variant`() {
        val temperatureCases = mapOf(
            UnitType.TemperatureUnit.Celsius to WidgetType.TEMPERATURE,
            UnitType.TemperatureUnit.Fahrenheit to WidgetType.TEMPERATURE_F,
            UnitType.TemperatureUnit.Kelvin to WidgetType.TEMPERATURE_K,
        )
        temperatureCases.forEach { (unit, expected) ->
            assertEquals(expected, migrateSimple(WidgetType.TEMPERATURE.code, unit))
        }

        assertEquals(
            WidgetType.HUMIDITY,
            migrateSimple(humidityUnit = UnitType.HumidityUnit.Relative),
        )
        assertEquals(
            WidgetType.HUMIDITY_ABSOLUTE,
            migrateSimple(humidityUnit = UnitType.HumidityUnit.Absolute),
        )
        temperatureCases.forEach { (unit, _) ->
            val expected = when (unit) {
                UnitType.TemperatureUnit.Celsius -> WidgetType.DEW_POINT_C
                UnitType.TemperatureUnit.Fahrenheit -> WidgetType.DEW_POINT_F
                UnitType.TemperatureUnit.Kelvin -> WidgetType.DEW_POINT_K
            }
            assertEquals(
                expected,
                migrateSimple(
                    temperatureUnit = unit,
                    humidityUnit = UnitType.HumidityUnit.DewPoint,
                ),
            )
        }

        val pressureCases = mapOf(
            UnitType.PressureUnit.Pascal to WidgetType.PRESSURE_PA,
            UnitType.PressureUnit.HectoPascal to WidgetType.PRESSURE,
            UnitType.PressureUnit.MmHg to WidgetType.PRESSURE_MMHG,
            UnitType.PressureUnit.InchHg to WidgetType.PRESSURE_INHG,
        )
        pressureCases.forEach { (unit, expected) ->
            assertEquals(
                expected,
                migrateSimple(
                    storedCode = WidgetType.PRESSURE.code,
                    pressureUnit = unit,
                ),
            )
        }

        assertEquals(WidgetType.SOUND_AVERAGE, migrateSimple(storedCode = 31))
        assertEquals(WidgetType.PM25, migrateSimple(storedCode = WidgetType.PM25.code))
    }

    @Test
    fun `complex legacy units and instant sound migrate once`() {
        val legacy = ComplexWidgetPreferenceItem(
            sensorId = "sensor",
            checkedTemperature = true,
            checkedHumidity = true,
            checkedPressure = true,
            checkedSoundAverage = false,
        )

        val migrated = ComplexWidgetPreferencesInteractor.migrateLegacyComplexWidgetPreference(
            item = legacy,
            legacySoundRealTime = true,
            temperatureUnit = UnitType.TemperatureUnit.Fahrenheit,
            humidityUnit = UnitType.HumidityUnit.DewPoint,
            pressureUnit = UnitType.PressureUnit.InchHg,
        )

        assertFalse(migrated.checkedTemperature)
        assertTrue(migrated.checkedTemperatureF)
        assertFalse(migrated.checkedHumidity)
        assertTrue(migrated.checkedDewPointF)
        assertFalse(migrated.checkedPressure)
        assertTrue(migrated.checkedPressureInHg)
        assertTrue(migrated.checkedSoundAverage)

        assertEquals(
            migrated,
            ComplexWidgetPreferencesInteractor.migrateLegacyComplexWidgetPreference(
                item = migrated,
                legacySoundRealTime = false,
                temperatureUnit = UnitType.TemperatureUnit.Fahrenheit,
                humidityUnit = UnitType.HumidityUnit.DewPoint,
                pressureUnit = UnitType.PressureUnit.InchHg,
            ),
        )
    }

    @Test
    fun `complex legacy measurements snapshot every global unit variant`() {
        val temperatureCases = mapOf(
            UnitType.TemperatureUnit.Celsius to WidgetType.TEMPERATURE,
            UnitType.TemperatureUnit.Fahrenheit to WidgetType.TEMPERATURE_F,
            UnitType.TemperatureUnit.Kelvin to WidgetType.TEMPERATURE_K,
        )
        temperatureCases.forEach { (temperatureUnit, expectedType) ->
            val migrated = migrateComplex(
                item = ComplexWidgetPreferenceItem(
                    sensorId = "sensor",
                    checkedTemperature = true,
                ),
                temperatureUnit = temperatureUnit,
            )
            assertEquals(listOf(expectedType), WidgetType.entries.filter(migrated::isChecked))
        }

        val humidityCases = listOf(
            Triple(
                UnitType.HumidityUnit.Relative,
                UnitType.TemperatureUnit.Celsius,
                WidgetType.HUMIDITY,
            ),
            Triple(
                UnitType.HumidityUnit.Absolute,
                UnitType.TemperatureUnit.Celsius,
                WidgetType.HUMIDITY_ABSOLUTE,
            ),
            Triple(
                UnitType.HumidityUnit.DewPoint,
                UnitType.TemperatureUnit.Celsius,
                WidgetType.DEW_POINT_C,
            ),
            Triple(
                UnitType.HumidityUnit.DewPoint,
                UnitType.TemperatureUnit.Fahrenheit,
                WidgetType.DEW_POINT_F,
            ),
            Triple(
                UnitType.HumidityUnit.DewPoint,
                UnitType.TemperatureUnit.Kelvin,
                WidgetType.DEW_POINT_K,
            ),
        )
        humidityCases.forEach { (humidityUnit, temperatureUnit, expectedType) ->
            val migrated = migrateComplex(
                item = ComplexWidgetPreferenceItem(
                    sensorId = "sensor",
                    checkedHumidity = true,
                ),
                temperatureUnit = temperatureUnit,
                humidityUnit = humidityUnit,
            )
            assertEquals(listOf(expectedType), WidgetType.entries.filter(migrated::isChecked))
        }

        val pressureCases = mapOf(
            UnitType.PressureUnit.Pascal to WidgetType.PRESSURE_PA,
            UnitType.PressureUnit.HectoPascal to WidgetType.PRESSURE,
            UnitType.PressureUnit.MmHg to WidgetType.PRESSURE_MMHG,
            UnitType.PressureUnit.InchHg to WidgetType.PRESSURE_INHG,
        )
        pressureCases.forEach { (pressureUnit, expectedType) ->
            val migrated = migrateComplex(
                item = ComplexWidgetPreferenceItem(
                    sensorId = "sensor",
                    checkedPressure = true,
                ),
                pressureUnit = pressureUnit,
            )
            assertEquals(listOf(expectedType), WidgetType.entries.filter(migrated::isChecked))
        }
    }

    private fun migrateSimple(
        storedCode: Int = WidgetType.HUMIDITY.code,
        temperatureUnit: UnitType.TemperatureUnit = UnitType.TemperatureUnit.Celsius,
        humidityUnit: UnitType.HumidityUnit = UnitType.HumidityUnit.Relative,
        pressureUnit: UnitType.PressureUnit = UnitType.PressureUnit.HectoPascal,
    ) = WidgetPreferencesInteractor.migrateLegacyWidgetType(
        storedCode = storedCode,
        temperatureUnit = temperatureUnit,
        humidityUnit = humidityUnit,
        pressureUnit = pressureUnit,
    )

    private fun migrateComplex(
        item: ComplexWidgetPreferenceItem,
        temperatureUnit: UnitType.TemperatureUnit = UnitType.TemperatureUnit.Celsius,
        humidityUnit: UnitType.HumidityUnit = UnitType.HumidityUnit.Relative,
        pressureUnit: UnitType.PressureUnit = UnitType.PressureUnit.HectoPascal,
    ) = ComplexWidgetPreferencesInteractor.migrateLegacyComplexWidgetPreference(
        item = item,
        legacySoundRealTime = false,
        temperatureUnit = temperatureUnit,
        humidityUnit = humidityUnit,
        pressureUnit = pressureUnit,
    )
}
