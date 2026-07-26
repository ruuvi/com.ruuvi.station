package com.ruuvi.station.widgets.domain

import com.ruuvi.station.bluetooth.contract.FoundRuuviTag
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.network.data.response.SensorSubscription
import com.ruuvi.station.network.data.response.SensorsDenseInfo
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tag.domain.ruuviTagPreview
import com.ruuvi.station.tag.domain.sensorMeasurementsPreview
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.units.model.UnitType
import com.ruuvi.station.widgets.data.WidgetSensorSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.mock
import java.util.Date

class WidgetSensorSnapshotMappingTest {
    private val provider = WidgetSensorSnapshotProvider(
        tagRepository = mock<TagRepository>(),
        cloudInteractor = mock<RuuviNetworkInteractor>(),
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
        val sensorInfo = sensorInfo(
            temperatureOffset = 1.5,
            humidityOffset = -2.0,
            pressureOffset = 25.0,
        )
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
        val sensorInfo = sensorInfo(
            temperatureOffset = Double.NaN,
            pressureOffset = Double.POSITIVE_INFINITY,
        )
        val decoded = decodedSensor(
            temperature = 20.0,
            humidity = Double.NaN,
            pressure = 100_000.0,
            voltage = Double.NaN,
            accelerationX = Double.NaN,
            accelerationY = Double.POSITIVE_INFINITY,
            accelerationZ = Double.NEGATIVE_INFINITY,
            soundAverage = Double.NaN,
            soundPeak = Double.POSITIVE_INFINITY,
            luminosity = Double.NEGATIVE_INFINITY,
            pm1 = Double.NaN,
            pm2_5 = Double.POSITIVE_INFINITY,
            pm4 = Double.NEGATIVE_INFINITY,
            pm10 = Double.NaN,
        )

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

    private fun sensorInfo(
        temperatureOffset: Double = 0.0,
        humidityOffset: Double = 0.0,
        pressureOffset: Double = 0.0,
    ) = SensorsDenseInfo(
        sensor = SENSOR_ID,
        owner = "",
        name = "",
        picture = "",
        `public` = false,
        canShare = false,
        offsetTemperature = temperatureOffset,
        offsetHumidity = humidityOffset,
        offsetPressure = pressureOffset,
        measurements = emptyList(),
        alerts = emptyList(),
        lastUpdated = 0L,
        subscription = SensorSubscription(
            maxHistoryDays = 0,
            maxResolutionMinutes = 0,
            emailAlertAllowed = false,
            pushAlertAllowed = false,
            subscriptionName = "",
        ),
        settings = null,
        sharedTo = emptyList(),
    )

    private fun decodedSensor(
        temperature: Double? = 20.0,
        humidity: Double? = 50.0,
        pressure: Double? = 100_000.0,
        movementCounter: Int? = 8,
        voltage: Double? = 2.9,
        rssi: Int? = -55,
        measurementSequenceNumber: Int? = 42,
        accelerationX: Double? = 0.1,
        accelerationY: Double? = 0.2,
        accelerationZ: Double? = 0.3,
        soundAverage: Double? = 40.5,
        soundPeak: Double? = 70.5,
        luminosity: Double? = 100.0,
        co2: Int? = 600,
        voc: Int? = 80,
        nox: Int? = 90,
        pm1: Double? = 1.0,
        pm2_5: Double? = 2.5,
        pm4: Double? = 4.0,
        pm10: Double? = 10.0,
    ) = FoundRuuviTag(
        rssi = rssi,
        temperature = temperature,
        humidity = humidity,
        pressure = pressure,
        accelX = accelerationX,
        accelY = accelerationY,
        accelZ = accelerationZ,
        voltage = voltage,
        movementCounter = movementCounter,
        measurementSequenceNumber = measurementSequenceNumber,
        pm1 = pm1,
        pm25 = pm2_5,
        pm4 = pm4,
        pm10 = pm10,
        co2 = co2,
        voc = voc,
        nox = nox,
        luminosity = luminosity,
        dBaAvg = soundAverage,
        dBaPeak = soundPeak,
    )

    companion object {
        private const val SENSOR_ID = "AA:BB:CC:DD:EE:FF"
    }
}
