package com.ruuvi.station.widgets.domain

import com.ruuvi.station.bluetooth.contract.FoundRuuviTag
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.network.data.response.SensorsDenseInfo
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tag.domain.ruuviTagPreview
import com.ruuvi.station.tag.domain.sensorMeasurementsPreview
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.units.model.UnitType
import com.ruuvi.station.widgets.data.WidgetSensorSnapshot
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

class WidgetSensorSnapshotMappingTest {
    private val provider = WidgetSensorSnapshotProvider(
        tagRepository = mockk<TagRepository>(),
        cloudInteractor = mockk<RuuviNetworkInteractor>(),
        decoder = { _, _, _ -> error("Decoder is not used by mapping tests") },
        monotonicTimeMillis = { 0L },
    )

    @Test
    fun `local mapping uses raw canonical values instead of formatted values`() {
        val timestamp = 123_456L
        val sensor = ruuviTagPreview.copy(
            id = SENSOR_ID,
            displayName = "Storage",
            latestMeasurement = sensorMeasurementsPreview.copy(
                temperature = environment(21.25, UnitType.TemperatureUnit.Fahrenheit),
                humidity = environment(48.5, UnitType.HumidityUnit.Absolute),
                pressure = environment(100_125.0, UnitType.PressureUnit.MmHg),
                movement = environment(7.0, UnitType.MovementUnit.MovementsCount),
                voltage = environment(2.95, UnitType.BatteryVoltageUnit.Volt),
                rssi = environment(-62.0, UnitType.SignalStrengthUnit.SignalDbm),
                accelerationX = -0.125,
                accelerationY = 0.0,
                accelerationZ = 0.98,
                measurementSequenceNumber = 65_535,
                pm10 = environment(1.1, UnitType.PM.PM10),
                pm25 = environment(2.2, UnitType.PM.PM25),
                pm40 = environment(3.3, UnitType.PM.PM40),
                pm100 = environment(4.4, UnitType.PM.PM100),
                co2 = environment(500.0, UnitType.CO2.Ppm),
                voc = environment(60.0, UnitType.VOC.VocIndex),
                nox = environment(70.0, UnitType.NOX.NoxIndex),
                luminosity = environment(80.0, UnitType.Luminosity.Lux),
                dBaAvg = environment(41.5, UnitType.SoundAvg.SoundDba),
                dBaPeak = environment(72.5, UnitType.SoundPeak.SoundDba),
                updatedAt = Date(timestamp),
            ),
        )

        assertEquals(
            WidgetSensorSnapshot(
                sensorId = SENSOR_ID,
                displayName = "Storage",
                timestampEpochMillis = timestamp,
                temperatureCelsius = 21.25,
                relativeHumidityPercent = 48.5,
                pressurePascal = 100_125.0,
                movementCount = 7,
                voltageVolt = 2.95,
                rssiDbm = -62,
                measurementSequenceNumber = 65_535,
                accelerationXG = -0.125,
                accelerationYG = 0.0,
                accelerationZG = 0.98,
                soundAverageDba = 41.5,
                soundPeakDba = 72.5,
                luminosityLux = 80.0,
                co2Ppm = 500,
                vocIndex = 60,
                noxIndex = 70,
                pm1MicrogramsPerCubicMeter = 1.1,
                pm2_5MicrogramsPerCubicMeter = 2.2,
                pm4MicrogramsPerCubicMeter = 3.3,
                pm10MicrogramsPerCubicMeter = 4.4,
            ),
            provider.mapLocalSnapshot(sensor),
        )
    }

    @Test
    fun `local mapping returns null without a measurement and drops invalid numbers`() {
        assertNull(provider.mapLocalSnapshot(ruuviTagPreview.copy(latestMeasurement = null)))

        val sensor = ruuviTagPreview.copy(
            latestMeasurement = sensorMeasurementsPreview.copy(
                temperature = environment(Double.NaN, UnitType.TemperatureUnit.Celsius),
                humidity = environment(
                    Double.POSITIVE_INFINITY,
                    UnitType.HumidityUnit.Relative,
                ),
                pressure = environment(
                    Double.NEGATIVE_INFINITY,
                    UnitType.PressureUnit.Pascal,
                ),
                movement = environment(1.5, UnitType.MovementUnit.MovementsCount),
                voltage = environment(Double.NaN, UnitType.BatteryVoltageUnit.Volt),
                rssi = environment(
                    Int.MAX_VALUE.toDouble() + 1.0,
                    UnitType.SignalStrengthUnit.SignalDbm,
                ),
                accelerationX = Double.NaN,
                accelerationY = Double.POSITIVE_INFINITY,
                accelerationZ = Double.NEGATIVE_INFINITY,
                pm10 = environment(Double.NaN, UnitType.PM.PM10),
                pm25 = environment(Double.POSITIVE_INFINITY, UnitType.PM.PM25),
                pm40 = environment(Double.NEGATIVE_INFINITY, UnitType.PM.PM40),
                pm100 = environment(Double.NaN, UnitType.PM.PM100),
                co2 = environment(Int.MAX_VALUE.toDouble() + 1.0, UnitType.CO2.Ppm),
                voc = environment(Int.MIN_VALUE.toDouble() - 1.0, UnitType.VOC.VocIndex),
                nox = environment(0.5, UnitType.NOX.NoxIndex),
                luminosity = environment(
                    Double.POSITIVE_INFINITY,
                    UnitType.Luminosity.Lux,
                ),
                dBaAvg = environment(Double.NaN, UnitType.SoundAvg.SoundDba),
                dBaPeak = environment(
                    Double.NEGATIVE_INFINITY,
                    UnitType.SoundPeak.SoundDba,
                ),
            ),
        )

        val snapshot = provider.mapLocalSnapshot(sensor)

        assertNull(snapshot?.temperatureCelsius)
        assertNull(snapshot?.relativeHumidityPercent)
        assertNull(snapshot?.pressurePascal)
        assertNull(snapshot?.movementCount)
        assertNull(snapshot?.voltageVolt)
        assertNull(snapshot?.rssiDbm)
        assertNull(snapshot?.accelerationXG)
        assertNull(snapshot?.accelerationYG)
        assertNull(snapshot?.accelerationZG)
        assertNull(snapshot?.soundAverageDba)
        assertNull(snapshot?.soundPeakDba)
        assertNull(snapshot?.luminosityLux)
        assertNull(snapshot?.co2Ppm)
        assertNull(snapshot?.vocIndex)
        assertNull(snapshot?.noxIndex)
        assertNull(snapshot?.pm1MicrogramsPerCubicMeter)
        assertNull(snapshot?.pm2_5MicrogramsPerCubicMeter)
        assertNull(snapshot?.pm4MicrogramsPerCubicMeter)
        assertNull(snapshot?.pm10MicrogramsPerCubicMeter)
    }

    @Test
    fun `cloud mapping applies offsets once and maps every raw field`() {
        val sensorInfo = mockk<SensorsDenseInfo>()
        every { sensorInfo.offsetTemperature } returns 1.5
        every { sensorInfo.offsetHumidity } returns -2.0
        every { sensorInfo.offsetPressure } returns 25.0
        val decoded = decodedSensor()
        val sensor = ruuviTagPreview.copy(
            id = SENSOR_ID,
            displayName = "Cloud storage",
        )

        assertEquals(
            WidgetSensorSnapshot(
                sensorId = SENSOR_ID,
                displayName = "Cloud storage",
                timestampEpochMillis = 123_000L,
                temperatureCelsius = 21.5,
                relativeHumidityPercent = 48.0,
                pressurePascal = 100_025.0,
                movementCount = 8,
                voltageVolt = 2.9,
                rssiDbm = -55,
                measurementSequenceNumber = 42,
                accelerationXG = 0.1,
                accelerationYG = 0.2,
                accelerationZG = 0.3,
                soundAverageDba = 40.5,
                soundPeakDba = 70.5,
                luminosityLux = 100.0,
                co2Ppm = 600,
                vocIndex = 80,
                noxIndex = 90,
                pm1MicrogramsPerCubicMeter = 1.0,
                pm2_5MicrogramsPerCubicMeter = 2.5,
                pm4MicrogramsPerCubicMeter = 4.0,
                pm10MicrogramsPerCubicMeter = 10.0,
            ),
            provider.mapCloudSnapshot(
                sensor = sensor,
                decoded = decoded,
                sensorInfo = sensorInfo,
                timestampEpochSeconds = 123L,
            ),
        )
    }

    @Test
    fun `cloud mapping drops non-finite decoded values and offsets`() {
        val sensorInfo = mockk<SensorsDenseInfo>()
        every { sensorInfo.offsetTemperature } returns Double.NaN
        every { sensorInfo.offsetHumidity } returns 0.0
        every { sensorInfo.offsetPressure } returns Double.POSITIVE_INFINITY
        val decoded = mockk<FoundRuuviTag>(relaxed = true) {
            every { temperature } returns 20.0
            every { humidity } returns Double.NaN
            every { pressure } returns 100_000.0
            every { voltage } returns Double.NaN
            every { accelX } returns Double.NaN
            every { accelY } returns Double.POSITIVE_INFINITY
            every { accelZ } returns Double.NEGATIVE_INFINITY
            every { dBaAvg } returns Double.NaN
            every { dBaPeak } returns Double.POSITIVE_INFINITY
            every { luminosity } returns Double.NEGATIVE_INFINITY
            every { pm1 } returns Double.NaN
            every { pm25 } returns Double.POSITIVE_INFINITY
            every { pm4 } returns Double.NEGATIVE_INFINITY
            every { pm10 } returns Double.NaN
        }

        val snapshot = provider.mapCloudSnapshot(
            sensor = ruuviTagPreview,
            decoded = decoded,
            sensorInfo = sensorInfo,
            timestampEpochSeconds = 123L,
        )

        assertNull(snapshot.temperatureCelsius)
        assertNull(snapshot.relativeHumidityPercent)
        assertNull(snapshot.pressurePascal)
        assertNull(snapshot.voltageVolt)
        assertNull(snapshot.accelerationXG)
        assertNull(snapshot.accelerationYG)
        assertNull(snapshot.accelerationZG)
        assertNull(snapshot.soundAverageDba)
        assertNull(snapshot.soundPeakDba)
        assertNull(snapshot.luminosityLux)
        assertNull(snapshot.pm1MicrogramsPerCubicMeter)
        assertNull(snapshot.pm2_5MicrogramsPerCubicMeter)
        assertNull(snapshot.pm4MicrogramsPerCubicMeter)
        assertNull(snapshot.pm10MicrogramsPerCubicMeter)
    }

    private fun environment(original: Double, unitType: UnitType) = EnvironmentValue(
        original = original,
        value = 999.0,
        accuracy = Accuracy.Accuracy2,
        valueWithUnit = "localized with unit",
        valueWithoutUnit = "localized",
        unitString = "localized unit",
        unitType = unitType,
    )

    private fun decodedSensor() = mockk<FoundRuuviTag>(relaxed = true) {
        every { temperature } returns 20.0
        every { humidity } returns 50.0
        every { pressure } returns 100_000.0
        every { movementCounter } returns 8
        every { voltage } returns 2.9
        every { rssi } returns -55
        every { measurementSequenceNumber } returns 42
        every { accelX } returns 0.1
        every { accelY } returns 0.2
        every { accelZ } returns 0.3
        every { dBaAvg } returns 40.5
        every { dBaPeak } returns 70.5
        every { luminosity } returns 100.0
        every { co2 } returns 600
        every { voc } returns 80
        every { nox } returns 90
        every { pm1 } returns 1.0
        every { pm25 } returns 2.5
        every { pm4 } returns 4.0
        every { pm10 } returns 10.0
    }

    companion object {
        private const val SENSOR_ID = "AA:BB:CC:DD:EE:FF"
    }
}
