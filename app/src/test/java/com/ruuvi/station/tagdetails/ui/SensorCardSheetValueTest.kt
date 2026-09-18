package com.ruuvi.station.tagdetails.ui

import com.ruuvi.station.tag.domain.ruuviTagPreview
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.units.model.UnitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SensorCardSheetValueTest {
    @Test
    fun `selected display value is resolved from latest sensor state`() {
        val oldValue = environmentValue(20.0, UnitType.TemperatureUnit.Celsius)
        val latestValue = environmentValue(21.0, UnitType.TemperatureUnit.Celsius)
        val sensor = ruuviTagPreview.copy(valuesToDisplay = listOf(latestValue))

        val result = resolveCurrentSheetValue(sensor, oldValue.unitType)

        assertEquals(latestValue, result)
    }

    @Test
    fun `AQI detail value is resolved even when it is not visible on the card`() {
        val latestPm25 = environmentValue(12.0, UnitType.PM.PM25)
        val sensor = ruuviTagPreview.copy(
            valuesToDisplay = emptyList(),
            latestMeasurement = ruuviTagPreview.latestMeasurement?.copy(pm25 = latestPm25),
        )

        val result = resolveCurrentSheetValue(sensor, UnitType.PM.PM25)

        assertEquals(latestPm25, result)
    }

    @Test
    fun `missing selection has no sheet value`() {
        assertNull(resolveCurrentSheetValue(ruuviTagPreview, null))
    }

    private fun environmentValue(value: Double, unitType: UnitType) = EnvironmentValue(
        original = value,
        value = value,
        accuracy = Accuracy.Accuracy1,
        valueWithUnit = value.toString(),
        valueWithoutUnit = value.toString(),
        unitString = "",
        unitType = unitType,
    )
}
