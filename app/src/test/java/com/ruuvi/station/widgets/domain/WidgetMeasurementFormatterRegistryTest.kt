package com.ruuvi.station.widgets.domain

import android.content.Context
import com.ruuvi.station.R
import com.ruuvi.station.units.domain.AccelerationConverter
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.units.model.UnitType
import com.ruuvi.station.widgets.data.WidgetSensorSnapshot
import com.ruuvi.station.widgets.data.WidgetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class WidgetMeasurementFormatterRegistryTest {
    private lateinit var context: Context
    private lateinit var unitsConverter: UnitsConverter
    private lateinit var accelerationConverter: AccelerationConverter
    private lateinit var registry: WidgetMeasurementFormatterRegistry

    @Before
    fun setUp() {
        context = mock()
        unitsConverter = mock()
        accelerationConverter = mock()
        whenever(context.getString(any<Int>())).thenAnswer {
            val resourceId = it.getArgument<Int>(0)
            if (resourceId == R.string.empty) "" else "unit-$resourceId"
        }
        whenever(unitsConverter.getTemperatureAccuracy()).thenReturn(Accuracy.Accuracy2)
        whenever(unitsConverter.getPressureAccuracy()).thenReturn(Accuracy.Accuracy2)
        whenever(unitsConverter.getHumidityAccuracy()).thenReturn(Accuracy.Accuracy2)
        whenever(
            unitsConverter.getTemperatureStringWithoutUnit(
                anyOrNull(),
                any(),
                any(),
            ),
        ).thenReturn("-")
        whenever(
            unitsConverter.getHumidityStringWithoutUnit(
                anyOrNull(),
                anyOrNull(),
                any(),
            ),
        ).thenReturn("-")
        whenever(
            unitsConverter.getPressureStringWithoutUnit(
                anyOrNull(),
                any(),
                any(),
            ),
        ).thenReturn("-")
        registry = WidgetMeasurementFormatterRegistry(
            context = context,
            unitsConverter = unitsConverter,
            accelerationConverter = accelerationConverter,
        )
    }

    @Test
    fun `registry covers every widget type and preserves caller ordering`() {
        val allTypes = WidgetType.entries.toList()
        val allValues = registry.format(allTypes, emptySnapshot())
        val repeatedOrder = listOf(
            WidgetType.PM100,
            WidgetType.TEMPERATURE,
            WidgetType.PM100,
            WidgetType.AIR_QUALITY,
        )

        assertEquals(allTypes, allValues.map { it.type })
        assertEquals(
            repeatedOrder,
            registry.format(repeatedOrder, emptySnapshot()).map { it.type },
        )
    }

    @Test
    fun `full snapshot has a defined canonical value and unit for every widget type`() {
        stubFullSnapshotFormatting()
        val expectedValues = mapOf(
            WidgetType.TEMPERATURE to "temperature-c",
            WidgetType.HUMIDITY to "humidity-relative",
            WidgetType.PRESSURE to "pressure-hpa",
            WidgetType.MOVEMENT to "3",
            WidgetType.VOLTAGE to "voltage",
            WidgetType.SIGNAL_STRENGTH to "signal",
            WidgetType.ACCELERATION_X to "acceleration-x",
            WidgetType.ACCELERATION_Y to "acceleration-y",
            WidgetType.ACCELERATION_Z to "acceleration-z",
            WidgetType.SOUND_AVERAGE to "sound-average",
            WidgetType.SOUND_PEAK to "sound-peak",
            WidgetType.MEASUREMENT_SEQUENCE_NUMBER to "42",
            WidgetType.TEMPERATURE_F to "temperature-f",
            WidgetType.TEMPERATURE_K to "temperature-k",
            WidgetType.HUMIDITY_ABSOLUTE to "humidity-absolute",
            WidgetType.DEW_POINT_C to "dew-c",
            WidgetType.DEW_POINT_F to "dew-f",
            WidgetType.DEW_POINT_K to "dew-k",
            WidgetType.PRESSURE_PA to "pressure-pa",
            WidgetType.PRESSURE_MMHG to "pressure-mmhg",
            WidgetType.AIR_QUALITY to "96/100",
            WidgetType.LUMINOSITY to "luminosity",
            WidgetType.CO2 to "co2",
            WidgetType.NOX to "nox",
            WidgetType.PM10 to "pm1",
            WidgetType.PM25 to "pm2.5",
            WidgetType.PM40 to "pm4",
            WidgetType.PM100 to "pm10",
            WidgetType.VOC to "voc",
            WidgetType.PRESSURE_INHG to "pressure-inhg",
        )

        val formatted = registry.format(WidgetType.entries, fullSnapshot())

        assertEquals(WidgetType.entries.size, expectedValues.size)
        formatted.forEach { measurement ->
            assertEquals(
                "Unexpected value for ${measurement.type}",
                expectedValues.getValue(measurement.type),
                measurement.sensorValue,
            )
            assertEquals(
                "Unexpected unit for ${measurement.type}",
                expectedUnit(measurement.type),
                measurement.unit,
            )
        }
    }

    @Test
    fun `unavailable snapshot has canonical value and type unit for every widget type`() {
        val formatted = registry.format(WidgetType.entries, emptySnapshot())

        formatted.forEach { measurement ->
            val expectedValue = if (measurement.type == WidgetType.AIR_QUALITY) {
                "-/100"
            } else {
                "-"
            }
            assertEquals(
                "Unexpected unavailable value for ${measurement.type}",
                expectedValue,
                measurement.sensorValue,
            )
            assertEquals(
                "Unexpected unavailable unit for ${measurement.type}",
                expectedUnit(measurement.type),
                measurement.unit,
            )
        }
    }

    @Test
    fun `aqi always uses the score out of one hundred representation`() {
        val available = registry.format(
            WidgetType.AIR_QUALITY,
            emptySnapshot(
                pm2_5MicrogramsPerCubicMeter = 0.0,
                co2Ppm = 420,
            ),
        )
        val unavailable = registry.format(
            WidgetType.AIR_QUALITY,
            emptySnapshot(
                pm2_5MicrogramsPerCubicMeter = Double.NaN,
                co2Ppm = 420,
            ),
        )
        val negativePm = registry.format(
            WidgetType.AIR_QUALITY,
            emptySnapshot(
                pm2_5MicrogramsPerCubicMeter = -0.1,
                co2Ppm = 420,
            ),
        )
        val negativeCo2 = registry.format(
            WidgetType.AIR_QUALITY,
            emptySnapshot(
                pm2_5MicrogramsPerCubicMeter = 0.0,
                co2Ppm = -1,
            ),
        )

        assertEquals("100/100", available.sensorValue)
        assertEquals("-/100", unavailable.sensorValue)
        assertEquals("-/100", negativePm.sensorValue)
        assertEquals("-/100", negativeCo2.sensorValue)
    }

    @Test
    fun `absolute humidity requires a temperature`() {
        whenever(
            unitsConverter.getHumidityStringWithoutUnit(
                humidity = 50.0,
                temperature = 20.0,
                humidityUnit = UnitType.HumidityUnit.Absolute,
            ),
        ).thenReturn(" 8.64 ")
        whenever(
            unitsConverter.getHumidityStringWithoutUnit(
                humidity = 0.0,
                temperature = 20.0,
                humidityUnit = UnitType.HumidityUnit.Absolute,
            ),
        ).thenReturn(" 0.00 ")

        val withoutTemperature = registry.format(
            WidgetType.HUMIDITY_ABSOLUTE,
            emptySnapshot(relativeHumidityPercent = 50.0),
        )
        val withTemperature = registry.format(
            WidgetType.HUMIDITY_ABSOLUTE,
            emptySnapshot(
                relativeHumidityPercent = 50.0,
                temperatureCelsius = 20.0,
            ),
        )
        val zeroHumidity = registry.format(
            WidgetType.HUMIDITY_ABSOLUTE,
            emptySnapshot(
                relativeHumidityPercent = 0.0,
                temperatureCelsius = 20.0,
            ),
        )
        val invalidHumidityValues = listOf(-0.1, 100.1, Double.NaN).map { humidity ->
            registry.format(
                WidgetType.HUMIDITY_ABSOLUTE,
                emptySnapshot(
                    relativeHumidityPercent = humidity,
                    temperatureCelsius = 20.0,
                ),
            )
        }

        assertEquals("-", withoutTemperature.sensorValue)
        assertEquals("8.64", withTemperature.sensorValue)
        assertEquals("0.00", zeroHumidity.sensorValue)
        assertTrue(invalidHumidityValues.all { it.sensorValue == "-" })
        verify(unitsConverter).getHumidityStringWithoutUnit(
            humidity = 50.0,
            temperature = 20.0,
            humidityUnit = UnitType.HumidityUnit.Absolute,
        )
        verify(unitsConverter).getHumidityStringWithoutUnit(
            humidity = 0.0,
            temperature = 20.0,
            humidityUnit = UnitType.HumidityUnit.Absolute,
        )
        verify(unitsConverter, times(2)).getHumidityStringWithoutUnit(
            humidity = anyOrNull(),
            temperature = anyOrNull(),
            humidityUnit = eq(UnitType.HumidityUnit.Absolute),
        )
    }

    @Test
    fun `dew point rejects nonpositive out of range and nonfinite inputs`() {
        listOf(0.0, -1.0, 101.0, Double.NaN).forEach { humidity ->
            assertEquals(
                "-",
                registry.format(
                    WidgetType.DEW_POINT_C,
                    emptySnapshot(
                        relativeHumidityPercent = humidity,
                        temperatureCelsius = 20.0,
                    ),
                ).sensorValue,
            )
        }
        assertEquals(
            "-",
            registry.format(
                WidgetType.DEW_POINT_C,
                emptySnapshot(
                    relativeHumidityPercent = 50.0,
                    temperatureCelsius = 400.0,
                ),
            ).sensorValue,
        )
        verify(unitsConverter, never()).getValueWithoutUnit(anyOrNull(), any())
    }

    @Test
    fun `valid dew point is converted and trimmed`() {
        val dewPoint = argumentCaptor<Double>()
        whenever(
            unitsConverter.getValueWithoutUnit(
                anyOrNull(),
                eq(Accuracy.Accuracy2),
            ),
        ).thenReturn(" 9.26 ")

        val formatted = registry.format(
            WidgetType.DEW_POINT_C,
            emptySnapshot(
                relativeHumidityPercent = 50.0,
                temperatureCelsius = 20.0,
            ),
        )

        assertEquals("9.26", formatted.sensorValue)
        verify(unitsConverter).getValueWithoutUnit(
            dewPoint.capture(),
            eq(Accuracy.Accuracy2),
        )
        assertEquals(9.26, dewPoint.firstValue, 0.02)
    }

    @Test
    fun `particulate widget types use their explicit persisted field mapping`() {
        whenever(context.getString(UnitType.PM.PM10.unit)).thenReturn(" µg/m³ ")
        whenever(
            unitsConverter.getPmEnvironmentValue(1.0, UnitType.PM.PM10)
        ).thenReturn(environmentValue(" PM1 ", 1.0, UnitType.PM.PM10))
        whenever(
            unitsConverter.getPmEnvironmentValue(2.5, UnitType.PM.PM25)
        ).thenReturn(environmentValue(" PM2.5 ", 2.5, UnitType.PM.PM25))
        whenever(
            unitsConverter.getPmEnvironmentValue(4.0, UnitType.PM.PM40)
        ).thenReturn(environmentValue(" PM4 ", 4.0, UnitType.PM.PM40))
        whenever(
            unitsConverter.getPmEnvironmentValue(10.0, UnitType.PM.PM100)
        ).thenReturn(environmentValue(" PM10 ", 10.0, UnitType.PM.PM100))

        val formatted = registry.format(
            listOf(
                WidgetType.PM10,
                WidgetType.PM25,
                WidgetType.PM40,
                WidgetType.PM100,
            ),
            emptySnapshot(
                pm1MicrogramsPerCubicMeter = 1.0,
                pm2_5MicrogramsPerCubicMeter = 2.5,
                pm4MicrogramsPerCubicMeter = 4.0,
                pm10MicrogramsPerCubicMeter = 10.0,
            ),
        )

        assertEquals(
            listOf("PM1", "PM2.5", "PM4", "PM10"),
            formatted.map { it.sensorValue },
        )
        assertTrue(formatted.all { it.unit == "µg/m³" })
        verify(unitsConverter).getPmEnvironmentValue(1.0, UnitType.PM.PM10)
        verify(unitsConverter).getPmEnvironmentValue(2.5, UnitType.PM.PM25)
        verify(unitsConverter).getPmEnvironmentValue(4.0, UnitType.PM.PM40)
        verify(unitsConverter).getPmEnvironmentValue(10.0, UnitType.PM.PM100)
    }

    private fun emptySnapshot(
        temperatureCelsius: Double? = null,
        relativeHumidityPercent: Double? = null,
        co2Ppm: Int? = null,
        pm1MicrogramsPerCubicMeter: Double? = null,
        pm2_5MicrogramsPerCubicMeter: Double? = null,
        pm4MicrogramsPerCubicMeter: Double? = null,
        pm10MicrogramsPerCubicMeter: Double? = null,
    ) = WidgetSensorSnapshot(
        sensorId = "AA:BB:CC:DD:EE:FF",
        displayName = "Test sensor",
        timestampEpochMillis = 1_000L,
        temperatureCelsius = temperatureCelsius,
        relativeHumidityPercent = relativeHumidityPercent,
        co2Ppm = co2Ppm,
        pm1MicrogramsPerCubicMeter = pm1MicrogramsPerCubicMeter,
        pm2_5MicrogramsPerCubicMeter = pm2_5MicrogramsPerCubicMeter,
        pm4MicrogramsPerCubicMeter = pm4MicrogramsPerCubicMeter,
        pm10MicrogramsPerCubicMeter = pm10MicrogramsPerCubicMeter,
    )

    private fun fullSnapshot() = WidgetSensorSnapshot(
        sensorId = "AA:BB:CC:DD:EE:FF",
        displayName = "Test sensor",
        timestampEpochMillis = 1_000L,
        temperatureCelsius = 20.0,
        relativeHumidityPercent = 50.0,
        pressurePascal = 100_000.0,
        movementCount = 3,
        voltageVolt = 3.0,
        rssiDbm = -60,
        measurementSequenceNumber = 42,
        accelerationXG = 0.1,
        accelerationYG = 0.2,
        accelerationZG = 0.3,
        soundAverageDba = 40.0,
        soundPeakDba = 50.0,
        luminosityLux = 123.0,
        co2Ppm = 420,
        vocIndex = 10,
        noxIndex = 20,
        pm1MicrogramsPerCubicMeter = 1.0,
        pm2_5MicrogramsPerCubicMeter = 2.5,
        pm4MicrogramsPerCubicMeter = 4.0,
        pm10MicrogramsPerCubicMeter = 10.0,
    )

    private fun stubFullSnapshotFormatting() {
        whenever(
            unitsConverter.getTemperatureStringWithoutUnit(
                20.0,
                UnitType.TemperatureUnit.Celsius,
                Accuracy.Accuracy2,
            )
        ).thenReturn(" temperature-c ")
        whenever(
            unitsConverter.getTemperatureStringWithoutUnit(
                20.0,
                UnitType.TemperatureUnit.Fahrenheit,
                Accuracy.Accuracy2,
            )
        ).thenReturn(" temperature-f ")
        whenever(
            unitsConverter.getTemperatureStringWithoutUnit(
                20.0,
                UnitType.TemperatureUnit.Kelvin,
                Accuracy.Accuracy2,
            )
        ).thenReturn(" temperature-k ")
        whenever(
            unitsConverter.getHumidityStringWithoutUnit(
                50.0,
                20.0,
                UnitType.HumidityUnit.Relative,
            )
        ).thenReturn(" humidity-relative ")
        whenever(
            unitsConverter.getHumidityStringWithoutUnit(
                50.0,
                20.0,
                UnitType.HumidityUnit.Absolute,
            )
        ).thenReturn(" humidity-absolute ")
        whenever(
            unitsConverter.getPressureStringWithoutUnit(
                100_000.0,
                UnitType.PressureUnit.HectoPascal,
                Accuracy.Accuracy2,
            )
        ).thenReturn(" pressure-hpa ")
        whenever(
            unitsConverter.getPressureStringWithoutUnit(
                100_000.0,
                UnitType.PressureUnit.Pascal,
                Accuracy.Accuracy2,
            )
        ).thenReturn(" pressure-pa ")
        whenever(
            unitsConverter.getPressureStringWithoutUnit(
                100_000.0,
                UnitType.PressureUnit.MmHg,
                Accuracy.Accuracy2,
            )
        ).thenReturn(" pressure-mmhg ")
        whenever(
            unitsConverter.getPressureStringWithoutUnit(
                100_000.0,
                UnitType.PressureUnit.InchHg,
                Accuracy.Accuracy2,
            )
        ).thenReturn(" pressure-inhg ")
        whenever(
            unitsConverter.getValueWithoutUnit(
                anyOrNull(),
                eq(Accuracy.Accuracy2),
            ),
        ).thenAnswer {
            when (it.getArgument<Double>(0)) {
                in 0.0..30.0 -> " dew-c "
                in 30.0..100.0 -> " dew-f "
                else -> " dew-k "
            }
        }
        whenever(
            unitsConverter.getVoltageEnvironmentValue(3.0)
        ).thenReturn(environmentValue(" voltage ", 3.0, UnitType.BatteryVoltageUnit.Volt))
        whenever(
            unitsConverter.getSignalEnvironmentValue(-60)
        ).thenReturn(environmentValue(" signal ", -60.0, UnitType.SignalStrengthUnit.SignalDbm))
        whenever(
            accelerationConverter.getAccelerationStringWithoutUnit(0.1)
        ).thenReturn(" acceleration-x ")
        whenever(
            accelerationConverter.getAccelerationStringWithoutUnit(0.2)
        ).thenReturn(" acceleration-y ")
        whenever(
            accelerationConverter.getAccelerationStringWithoutUnit(0.3)
        ).thenReturn(" acceleration-z ")
        whenever(
            unitsConverter.getSoundEnvironmentValue(40.0, UnitType.SoundAvg.SoundDba)
        ).thenReturn(environmentValue(" sound-average ", 40.0, UnitType.SoundAvg.SoundDba))
        whenever(
            unitsConverter.getSoundEnvironmentValue(50.0, UnitType.SoundPeak.SoundDba)
        ).thenReturn(environmentValue(" sound-peak ", 50.0, UnitType.SoundPeak.SoundDba))
        whenever(
            unitsConverter.getLuminosityEnvironmentValue(123.0)
        ).thenReturn(environmentValue(" luminosity ", 123.0, UnitType.Luminosity.Lux))
        whenever(
            unitsConverter.getCo2EnvironmentValue(420)
        ).thenReturn(environmentValue(" co2 ", 420.0, UnitType.CO2.Ppm))
        whenever(
            unitsConverter.getVocEnvironmentValue(10)
        ).thenReturn(environmentValue(" voc ", 10.0, UnitType.VOC.VocIndex))
        whenever(
            unitsConverter.getNoxEnvironmentValue(20)
        ).thenReturn(environmentValue(" nox ", 20.0, UnitType.NOX.NoxIndex))
        whenever(
            unitsConverter.getPmEnvironmentValue(1.0, UnitType.PM.PM10)
        ).thenReturn(environmentValue(" pm1 ", 1.0, UnitType.PM.PM10))
        whenever(
            unitsConverter.getPmEnvironmentValue(2.5, UnitType.PM.PM25)
        ).thenReturn(environmentValue(" pm2.5 ", 2.5, UnitType.PM.PM25))
        whenever(
            unitsConverter.getPmEnvironmentValue(4.0, UnitType.PM.PM40)
        ).thenReturn(environmentValue(" pm4 ", 4.0, UnitType.PM.PM40))
        whenever(
            unitsConverter.getPmEnvironmentValue(10.0, UnitType.PM.PM100)
        ).thenReturn(environmentValue(" pm10 ", 10.0, UnitType.PM.PM100))
    }

    private fun expectedUnit(type: WidgetType): String {
        val resourceId = when (type) {
            WidgetType.DEW_POINT_C -> UnitType.TemperatureUnit.Celsius.unit
            WidgetType.DEW_POINT_F -> UnitType.TemperatureUnit.Fahrenheit.unit
            WidgetType.DEW_POINT_K -> UnitType.TemperatureUnit.Kelvin.unit
            else -> type.unitType.unit
        }
        return if (resourceId == R.string.empty) "" else "unit-$resourceId"
    }

    private fun environmentValue(
        valueWithoutUnit: String,
        original: Double,
        unitType: UnitType,
    ) = EnvironmentValue(
        original = original,
        value = original,
        accuracy = unitType.defaultAccuracy,
        valueWithUnit = valueWithoutUnit,
        valueWithoutUnit = valueWithoutUnit,
        unitString = "",
        unitType = unitType,
    )
}
