package com.ruuvi.station.widgets.domain

import android.content.Context
import com.ruuvi.station.R
import com.ruuvi.station.tag.domain.ruuviTagPreview
import com.ruuvi.station.units.model.UnitType
import com.ruuvi.station.util.extensions.diffGreaterThan
import com.ruuvi.station.util.extensions.hours24
import com.ruuvi.station.util.extensions.localizedDate
import com.ruuvi.station.util.extensions.localizedTime
import com.ruuvi.station.widgets.data.SensorValue
import com.ruuvi.station.widgets.data.WidgetSensorSnapshot
import com.ruuvi.station.widgets.data.WidgetType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.util.Date

class WidgetInteractorTest {
    private val context = mockk<Context>()
    private val snapshotProvider = mockk<WidgetSensorSnapshotProvider>()
    private val measurementFormatter = mockk<WidgetMeasurementFormatterRegistry>()
    private val interactor = WidgetInteractor(
        context = context,
        snapshotProvider = snapshotProvider,
        measurementFormatter = measurementFormatter,
    )

    @Before
    fun setUp() {
        mockkStatic("com.ruuvi.station.util.extensions.DateKt")
        every { any<Date>().diffGreaterThan(hours24) } returns false
        every { any<Date>().localizedTime(context) } returns FORMATTED_TIME
        every { any<Date>().localizedDate(context) } returns FORMATTED_DATE
        every { context.getString(R.string.no_data) } returns NO_DATA
    }

    @After
    fun tearDown() {
        unmockkStatic("com.ruuvi.station.util.extensions.DateKt")
    }

    @Test
    fun `simple and complex widgets consume the same formatted measurement`() = runBlocking {
        val sensor = sensorWithSupportedTypes(UnitType.TemperatureUnit.Celsius)
        val snapshot = snapshot()
        val value = SensorValue(
            type = WidgetType.TEMPERATURE,
            sensorValue = "21.5",
            unit = "°C",
        )
        val settings = ComplexWidgetPreferenceItem(
            sensorId = SENSOR_ID,
            checkedTemperature = true,
        )
        every { context.getString(WidgetType.TEMPERATURE.titleResId) } returns "Temperature"
        every { snapshotProvider.getFavoriteSensor(SENSOR_ID) } returns sensor
        coEvery { snapshotProvider.getLatestSnapshot(sensor) } returns snapshot
        every {
            measurementFormatter.format(WidgetType.TEMPERATURE, snapshot)
        } returns value
        every {
            measurementFormatter.format(listOf(WidgetType.TEMPERATURE), snapshot)
        } returns listOf(value)

        val simple = interactor.getSimpleWidgetData(SENSOR_ID, WidgetType.TEMPERATURE)
        val complex = interactor.getComplexWidgetData(SENSOR_ID, settings)

        assertNotNull(simple)
        assertEquals(value.sensorValue, simple?.sensorValue)
        assertEquals(value.unit, simple?.unit)
        assertEquals(listOf(value), complex.sensorValues)
        assertEquals(Date(MEASURED_AT), simple?.timestamp)
        assertEquals(Date(MEASURED_AT), complex.timestamp)
        assertEquals(FORMATTED_TIME, simple?.updated)
        assertEquals(FORMATTED_TIME, complex.updated)
    }

    @Test
    fun `complex measurements follow widget display order`() = runBlocking {
        val sensor = sensorWithSupportedTypes(
            UnitType.HumidityUnit.Relative,
            UnitType.TemperatureUnit.Celsius,
            UnitType.PM.PM25,
        )
        val snapshot = snapshot()
        val expectedTypes = listOf(
            WidgetType.PM25,
            WidgetType.TEMPERATURE,
            WidgetType.HUMIDITY,
        )
        val expectedValues = expectedTypes.map {
            SensorValue(type = it, sensorValue = it.code.toString(), unit = "")
        }
        val settings = ComplexWidgetPreferenceItem(
            sensorId = SENSOR_ID,
            checkedTemperature = true,
            checkedHumidity = true,
            checkedPM25 = true,
        )
        every { snapshotProvider.getFavoriteSensor(SENSOR_ID) } returns sensor
        coEvery { snapshotProvider.getLatestSnapshot(sensor) } returns snapshot
        every {
            measurementFormatter.format(expectedTypes, snapshot)
        } returns expectedValues

        val result = interactor.getComplexWidgetData(SENSOR_ID, settings)

        assertEquals(expectedTypes, result.sensorValues.map { it.type })
    }

    @Test
    fun `null complex settings select no measurements`() = runBlocking {
        val sensor = sensorWithSupportedTypes(UnitType.TemperatureUnit.Celsius)
        val snapshot = snapshot()
        every { snapshotProvider.getFavoriteSensor(SENSOR_ID) } returns sensor
        coEvery { snapshotProvider.getLatestSnapshot(sensor) } returns snapshot
        every {
            measurementFormatter.format(emptyList(), snapshot)
        } returns emptyList()

        val result = interactor.getComplexWidgetData(SENSOR_ID, settings = null)

        assertEquals(emptyList<SensorValue>(), result.sensorValues)
        verify(exactly = 1) {
            measurementFormatter.format(emptyList(), snapshot)
        }
    }

    @Test
    fun `existing sensor without measurements preserves complex sensor name`() = runBlocking {
        val sensor = sensorWithSupportedTypes(UnitType.TemperatureUnit.Celsius)
        every { snapshotProvider.getFavoriteSensor(SENSOR_ID) } returns sensor
        coEvery { snapshotProvider.getLatestSnapshot(sensor) } returns null

        val result = interactor.getComplexWidgetData(SENSOR_ID, settings = null)

        assertEquals(SENSOR_NAME, result.displayName)
        assertEquals(Date(0), result.timestamp)
        assertEquals(emptyList<SensorValue>(), result.sensorValues)
        assertEquals(null, result.updated)
    }

    @Test
    fun `missing sensor retains existing no-data results`() = runBlocking {
        every { snapshotProvider.getFavoriteSensor(SENSOR_ID) } returns null

        val simple = interactor.getSimpleWidgetData(SENSOR_ID, WidgetType.TEMPERATURE)
        val complex = interactor.getComplexWidgetData(SENSOR_ID, settings = null)

        assertEquals(NO_DATA, simple?.displayName)
        assertEquals(Date(0), simple?.timestamp)
        assertEquals(NO_DATA, complex.displayName)
        assertEquals(Date(0), complex.timestamp)
    }

    @Test
    fun `measurements older than one day use localized date`() = runBlocking {
        val sensor = sensorWithSupportedTypes(UnitType.TemperatureUnit.Celsius)
        val snapshot = snapshot()
        val value = SensorValue(WidgetType.TEMPERATURE, "21.5", "°C")
        every { any<Date>().diffGreaterThan(hours24) } returns true
        every { context.getString(WidgetType.TEMPERATURE.titleResId) } returns "Temperature"
        every { snapshotProvider.getFavoriteSensor(SENSOR_ID) } returns sensor
        coEvery { snapshotProvider.getLatestSnapshot(sensor) } returns snapshot
        every {
            measurementFormatter.format(WidgetType.TEMPERATURE, snapshot)
        } returns value

        val result = interactor.getSimpleWidgetData(SENSOR_ID, WidgetType.TEMPERATURE)

        assertEquals(FORMATTED_DATE, result?.updated)
    }

    private fun sensorWithSupportedTypes(vararg types: UnitType) =
        ruuviTagPreview.copy(
            id = SENSOR_ID,
            displayName = SENSOR_NAME,
            displayOrder = types.toList(),
            possibleDisplayOptions = emptyList(),
        )

    private fun snapshot() = WidgetSensorSnapshot(
        sensorId = SENSOR_ID,
        displayName = SENSOR_NAME,
        timestampEpochMillis = MEASURED_AT,
        temperatureCelsius = 21.5,
    )

    companion object {
        private const val SENSOR_ID = "AA:BB:CC:DD:EE:FF"
        private const val SENSOR_NAME = "Test sensor"
        private const val MEASURED_AT = 1_234_567L
        private const val FORMATTED_TIME = "12:34"
        private const val FORMATTED_DATE = "2026-07-26"
        private const val NO_DATA = "No data"
    }
}
