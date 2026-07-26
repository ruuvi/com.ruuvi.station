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
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WidgetMeasurementFormatterRegistryTest {
    private lateinit var context: Context
    private lateinit var unitsConverter: UnitsConverter
    private lateinit var accelerationConverter: AccelerationConverter
    private lateinit var registry: WidgetMeasurementFormatterRegistry

    @Before
    fun setUp() {
        context = mockk()
        unitsConverter = mockk(relaxed = true)
        accelerationConverter = mockk(relaxed = true)
        every { context.getString(any()) } answers {
            val resourceId = firstArg<Int>()
            if (resourceId == R.string.empty) "" else "unit-$resourceId"
        }
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
        every {
            unitsConverter.getHumidityStringWithoutUnit(
                humidity = 50.0,
                temperature = 20.0,
                humidityUnit = UnitType.HumidityUnit.Absolute,
            )
        } returns " 8.64 "
        every {
            unitsConverter.getHumidityStringWithoutUnit(
                humidity = 0.0,
                temperature = 20.0,
                humidityUnit = UnitType.HumidityUnit.Absolute,
            )
        } returns " 0.00 "

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
        verify(exactly = 1) {
            unitsConverter.getHumidityStringWithoutUnit(
                humidity = 50.0,
                temperature = 20.0,
                humidityUnit = UnitType.HumidityUnit.Absolute,
            )
        }
        verify(exactly = 1) {
            unitsConverter.getHumidityStringWithoutUnit(
                humidity = 0.0,
                temperature = 20.0,
                humidityUnit = UnitType.HumidityUnit.Absolute,
            )
        }
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
        verify(exactly = 0) {
            unitsConverter.getValueWithoutUnit(any(), any())
        }
    }

    @Test
    fun `valid dew point is converted and trimmed`() {
        val dewPoint = slot<Double>()
        every { unitsConverter.getHumidityAccuracy() } returns Accuracy.Accuracy2
        every {
            unitsConverter.getValueWithoutUnit(capture(dewPoint), Accuracy.Accuracy2)
        } returns " 9.26 "

        val formatted = registry.format(
            WidgetType.DEW_POINT_C,
            emptySnapshot(
                relativeHumidityPercent = 50.0,
                temperatureCelsius = 20.0,
            ),
        )

        assertEquals("9.26", formatted.sensorValue)
        assertEquals(9.26, dewPoint.captured, 0.02)
    }

    @Test
    fun `particulate widget types use their explicit persisted field mapping`() {
        every { context.getString(UnitType.PM.PM10.unit) } returns " µg/m³ "
        every {
            unitsConverter.getPmEnvironmentValue(1.0, UnitType.PM.PM10)
        } returns environmentValue(" PM1 ", 1.0, UnitType.PM.PM10)
        every {
            unitsConverter.getPmEnvironmentValue(2.5, UnitType.PM.PM25)
        } returns environmentValue(" PM2.5 ", 2.5, UnitType.PM.PM25)
        every {
            unitsConverter.getPmEnvironmentValue(4.0, UnitType.PM.PM40)
        } returns environmentValue(" PM4 ", 4.0, UnitType.PM.PM40)
        every {
            unitsConverter.getPmEnvironmentValue(10.0, UnitType.PM.PM100)
        } returns environmentValue(" PM10 ", 10.0, UnitType.PM.PM100)

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
        verify(exactly = 1) {
            unitsConverter.getPmEnvironmentValue(1.0, UnitType.PM.PM10)
            unitsConverter.getPmEnvironmentValue(2.5, UnitType.PM.PM25)
            unitsConverter.getPmEnvironmentValue(4.0, UnitType.PM.PM40)
            unitsConverter.getPmEnvironmentValue(10.0, UnitType.PM.PM100)
        }
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
        every {
            unitsConverter.getTemperatureStringWithoutUnit(
                20.0,
                UnitType.TemperatureUnit.Celsius,
                any(),
            )
        } returns " temperature-c "
        every {
            unitsConverter.getTemperatureStringWithoutUnit(
                20.0,
                UnitType.TemperatureUnit.Fahrenheit,
                any(),
            )
        } returns " temperature-f "
        every {
            unitsConverter.getTemperatureStringWithoutUnit(
                20.0,
                UnitType.TemperatureUnit.Kelvin,
                any(),
            )
        } returns " temperature-k "
        every {
            unitsConverter.getHumidityStringWithoutUnit(
                50.0,
                20.0,
                UnitType.HumidityUnit.Relative,
            )
        } returns " humidity-relative "
        every {
            unitsConverter.getHumidityStringWithoutUnit(
                50.0,
                20.0,
                UnitType.HumidityUnit.Absolute,
            )
        } returns " humidity-absolute "
        every {
            unitsConverter.getPressureStringWithoutUnit(
                100_000.0,
                UnitType.PressureUnit.HectoPascal,
                any(),
            )
        } returns " pressure-hpa "
        every {
            unitsConverter.getPressureStringWithoutUnit(
                100_000.0,
                UnitType.PressureUnit.Pascal,
                any(),
            )
        } returns " pressure-pa "
        every {
            unitsConverter.getPressureStringWithoutUnit(
                100_000.0,
                UnitType.PressureUnit.MmHg,
                any(),
            )
        } returns " pressure-mmhg "
        every {
            unitsConverter.getPressureStringWithoutUnit(
                100_000.0,
                UnitType.PressureUnit.InchHg,
                any(),
            )
        } returns " pressure-inhg "
        every { unitsConverter.getHumidityAccuracy() } returns Accuracy.Accuracy2
        every {
            unitsConverter.getValueWithoutUnit(any(), Accuracy.Accuracy2)
        } answers {
            when (firstArg<Double>()) {
                in 0.0..30.0 -> " dew-c "
                in 30.0..100.0 -> " dew-f "
                else -> " dew-k "
            }
        }
        every {
            unitsConverter.getVoltageEnvironmentValue(3.0)
        } returns environmentValue(" voltage ", 3.0, UnitType.BatteryVoltageUnit.Volt)
        every {
            unitsConverter.getSignalEnvironmentValue(-60)
        } returns environmentValue(" signal ", -60.0, UnitType.SignalStrengthUnit.SignalDbm)
        every {
            accelerationConverter.getAccelerationStringWithoutUnit(0.1)
        } returns " acceleration-x "
        every {
            accelerationConverter.getAccelerationStringWithoutUnit(0.2)
        } returns " acceleration-y "
        every {
            accelerationConverter.getAccelerationStringWithoutUnit(0.3)
        } returns " acceleration-z "
        every {
            unitsConverter.getSoundEnvironmentValue(40.0, UnitType.SoundAvg.SoundDba)
        } returns environmentValue(" sound-average ", 40.0, UnitType.SoundAvg.SoundDba)
        every {
            unitsConverter.getSoundEnvironmentValue(50.0, UnitType.SoundPeak.SoundDba)
        } returns environmentValue(" sound-peak ", 50.0, UnitType.SoundPeak.SoundDba)
        every {
            unitsConverter.getLuminosityEnvironmentValue(123.0)
        } returns environmentValue(" luminosity ", 123.0, UnitType.Luminosity.Lux)
        every {
            unitsConverter.getCo2EnvironmentValue(420)
        } returns environmentValue(" co2 ", 420.0, UnitType.CO2.Ppm)
        every {
            unitsConverter.getVocEnvironmentValue(10)
        } returns environmentValue(" voc ", 10.0, UnitType.VOC.VocIndex)
        every {
            unitsConverter.getNoxEnvironmentValue(20)
        } returns environmentValue(" nox ", 20.0, UnitType.NOX.NoxIndex)
        every {
            unitsConverter.getPmEnvironmentValue(1.0, UnitType.PM.PM10)
        } returns environmentValue(" pm1 ", 1.0, UnitType.PM.PM10)
        every {
            unitsConverter.getPmEnvironmentValue(2.5, UnitType.PM.PM25)
        } returns environmentValue(" pm2.5 ", 2.5, UnitType.PM.PM25)
        every {
            unitsConverter.getPmEnvironmentValue(4.0, UnitType.PM.PM40)
        } returns environmentValue(" pm4 ", 4.0, UnitType.PM.PM40)
        every {
            unitsConverter.getPmEnvironmentValue(10.0, UnitType.PM.PM100)
        } returns environmentValue(" pm10 ", 10.0, UnitType.PM.PM100)
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
