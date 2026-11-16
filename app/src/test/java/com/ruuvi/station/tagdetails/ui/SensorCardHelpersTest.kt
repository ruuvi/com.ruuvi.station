package com.ruuvi.station.tagdetails.ui

import com.ruuvi.station.tag.domain.ruuviTagPreview
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.units.model.UnitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class SensorCardHelpersTest {
    @Test
    fun `round robin distributes values in display order across columns`() {
        assertEquals(
            listOf(listOf(1, 4), listOf(2, 5), listOf(3)),
            distributeRoundRobin((1..5).toList(), 3),
        )
    }

    @Test
    fun `round robin retains empty columns and supports an empty list`() {
        assertEquals(
            listOf(listOf("value"), emptyList(), emptyList()),
            distributeRoundRobin(listOf("value"), 3),
        )
        assertEquals(
            listOf(emptyList<Int>(), emptyList()),
            distributeRoundRobin(emptyList<Int>(), 2),
        )
    }

    @Test
    fun `round robin rejects a non-positive column count`() {
        assertThrows(IllegalArgumentException::class.java) {
            distributeRoundRobin(listOf(1), 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            distributeRoundRobin(listOf(1), -1)
        }
    }

    @Test
    fun `sheet lookup returns null when selected unit is absent`() {
        assertNull(
            resolveCurrentSheetValue(
                sensor = ruuviTagPreview.copy(
                    valuesToDisplay = listOf(
                        environmentValue(UnitType.TemperatureUnit.Celsius),
                    ),
                ),
                selectedUnitType = UnitType.PressureUnit.HectoPascal,
            ),
        )
    }

    @Test
    fun `sheet lookup falls back to hidden carbon dioxide value`() {
        val co2 = environmentValue(UnitType.CO2.Ppm)
        val sensor = ruuviTagPreview.copy(
            valuesToDisplay = emptyList(),
            latestMeasurement = ruuviTagPreview.latestMeasurement?.copy(co2 = co2),
        )

        assertEquals(co2, resolveCurrentSheetValue(sensor, UnitType.CO2.Ppm))
    }

    private fun environmentValue(unitType: UnitType) = EnvironmentValue(
        original = 1.0,
        value = 1.0,
        accuracy = Accuracy.Accuracy1,
        valueWithUnit = "1 unit",
        valueWithoutUnit = "1",
        unitString = "unit",
        unitType = unitType,
    )
}
