package com.ruuvi.station.widgets.domain

import com.ruuvi.station.bluetooth.contract.FoundRuuviTag
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.network.data.response.RuuviNetworkResponse
import com.ruuvi.station.network.data.response.SensorDataMeasurementResponse
import com.ruuvi.station.network.data.response.SensorDenseResponse
import com.ruuvi.station.network.data.response.SensorSubscription
import com.ruuvi.station.network.data.response.SensorsDenseInfo
import com.ruuvi.station.network.data.response.SensorsDenseResponseBody
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.ruuviTagPreview
import com.ruuvi.station.tag.domain.sensorMeasurementsPreview
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.units.model.UnitType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Date

class WidgetSensorSnapshotProviderTest {
    private val tagRepository = mock<TagRepository>()
    private val cloudInteractor = mock<RuuviNetworkInteractor>()
    private var now = 0L
    private val decoded = decodedSensor()

    @Test
    fun `favorite sensor access delegates to the repository`() {
        val sensor = localSensor()
        whenever(tagRepository.getFavoriteSensors()).thenReturn(listOf(sensor))
        whenever(tagRepository.getFavoriteSensorById(SENSOR_ID)).thenReturn(sensor)
        val provider = createProvider()

        assertEquals(listOf(sensor), provider.getFavoriteSensors())
        assertSame(sensor, provider.getFavoriteSensor(SENSOR_ID))
    }

    @Test
    fun `cloud must be strictly newer than local snapshot`() {
        val sensor = cloudSensor(localTimestampMillis = 10_000L, localTemperature = 10.0)
        whenever(tagRepository.getFavoriteSensorById(SENSOR_ID)).thenReturn(sensor)
        stubCloudResponse(successfulResponse(timestampEpochSeconds = 10L))
        val provider = createProvider()

        val snapshot = runBlocking { provider.getLatestSnapshot(SENSOR_ID) }

        assertEquals(10.0, snapshot?.temperatureCelsius ?: Double.NaN, 0.0)
    }

    @Test
    fun `newer cloud snapshot wins and receives calibration offsets`() {
        val sensor = cloudSensor(localTimestampMillis = 10_000L, localTemperature = 10.0)
        stubCloudResponse(
            successfulResponse(
                timestampEpochSeconds = 11L,
                temperatureOffset = 1.25,
            ),
        )
        val provider = createProvider()

        val snapshot = runBlocking { provider.getLatestSnapshot(sensor) }

        assertEquals(21.25, snapshot?.temperatureCelsius ?: Double.NaN, 0.0)
        assertEquals(11_000L, snapshot?.timestampEpochMillis ?: Long.MIN_VALUE)
    }

    @Test
    fun `non-cloud sensor uses local snapshot without requesting cloud data`() {
        val sensor = localSensor(timestampMillis = 1_000L, temperature = 10.0)
        val provider = createProvider()

        val snapshot = runBlocking { provider.getLatestSnapshot(sensor) }

        assertEquals(10.0, snapshot?.temperatureCelsius ?: Double.NaN, 0.0)
        verifyCloudRequests(0)
    }

    @Test
    fun `newest cloud measurement is decoded with its sensor id data and rssi`() {
        val sensor = cloudSensor(localTimestampMillis = 1_000L, localTemperature = 10.0)
        stubCloudResponse(
            successfulResponse(
                cloudMeasurements = listOf(
                    measurement(timestampEpochSeconds = 2L, data = "older", rssi = -80),
                    measurement(timestampEpochSeconds = 3L, data = "newer", rssi = -40),
                ),
            ),
        )
        var decoderInput: Triple<String, String, Int>? = null
        val provider = createProvider { sensorId, data, rssi ->
            decoderInput = Triple(sensorId, data, rssi)
            decoded
        }

        val snapshot = runBlocking { provider.getLatestSnapshot(sensor) }

        assertEquals(3_000L, snapshot?.timestampEpochMillis ?: Long.MIN_VALUE)
        assertEquals(Triple(SENSOR_ID, "newer", -40), decoderInput)
    }

    @Test
    fun `ordinary cloud failure falls back locally and retains cache until retry interval`() {
        val sensor = cloudSensor(localTimestampMillis = 1_000L, localTemperature = 10.0)
        var requests = 0
        stubCloudResponse {
            requests += 1
            when (requests) {
                1, 3 -> successfulResponse(timestampEpochSeconds = requests.toLong() + 1L)
                else -> throw IllegalStateException("Unavailable")
            }
        }
        val provider = createProvider()

        runBlocking {
            assertEquals(
                20.0,
                provider.getLatestSnapshot(sensor)?.temperatureCelsius ?: Double.NaN,
                0.0,
            )
            now = CLOUD_REFRESH_INTERVAL_MILLIS
            assertEquals(
                20.0,
                provider.getLatestSnapshot(sensor)?.temperatureCelsius ?: Double.NaN,
                0.0,
            )
            assertEquals(
                20.0,
                provider.getLatestSnapshot(sensor)?.temperatureCelsius ?: Double.NaN,
                0.0,
            )
            now += CLOUD_REFRESH_INTERVAL_MILLIS
            assertEquals(
                20.0,
                provider.getLatestSnapshot(sensor)?.temperatureCelsius ?: Double.NaN,
                0.0,
            )
        }

        verifyCloudRequests(3)
    }

    @Test
    fun `ordinary cloud failure without a cache falls back locally and starts cooldown`() {
        val sensor = cloudSensor(localTimestampMillis = 1_000L, localTemperature = 10.0)
        stubCloudResponse { throw IllegalStateException("Unavailable") }
        val provider = createProvider()

        runBlocking {
            repeat(2) {
                assertEquals(
                    10.0,
                    provider.getLatestSnapshot(sensor)?.temperatureCelsius ?: Double.NaN,
                    0.0,
                )
            }
        }

        verifyCloudRequests(1)
    }

    @Test
    fun `normal unsuccessful response replaces cache and falls back locally during cooldown`() {
        val sensor = cloudSensor(localTimestampMillis = 1_000L, localTemperature = 10.0)
        var requests = 0
        stubCloudResponse {
            requests += 1
            if (requests == 1) {
                successfulResponse(timestampEpochSeconds = 2L)
            } else {
                unsuccessfulResponse()
            }
        }
        val provider = createProvider()

        runBlocking {
            assertEquals(
                20.0,
                provider.getLatestSnapshot(sensor)?.temperatureCelsius ?: Double.NaN,
                0.0,
            )
            now = CLOUD_REFRESH_INTERVAL_MILLIS
            assertEquals(
                10.0,
                provider.getLatestSnapshot(sensor)?.temperatureCelsius ?: Double.NaN,
                0.0,
            )
            assertEquals(
                10.0,
                provider.getLatestSnapshot(sensor)?.temperatureCelsius ?: Double.NaN,
                0.0,
            )
        }

        verifyCloudRequests(2)
    }

    @Test
    fun `normal null response replaces cache and falls back locally during cooldown`() {
        val sensor = cloudSensor(localTimestampMillis = 1_000L, localTemperature = 10.0)
        var requests = 0
        stubCloudResponse {
            requests += 1
            if (requests == 1) {
                successfulResponse(timestampEpochSeconds = 2L)
            } else {
                null
            }
        }
        val provider = createProvider()

        runBlocking {
            assertEquals(
                20.0,
                provider.getLatestSnapshot(sensor)?.temperatureCelsius ?: Double.NaN,
                0.0,
            )
            now = CLOUD_REFRESH_INTERVAL_MILLIS
            assertEquals(
                10.0,
                provider.getLatestSnapshot(sensor)?.temperatureCelsius ?: Double.NaN,
                0.0,
            )
            assertEquals(
                10.0,
                provider.getLatestSnapshot(sensor)?.temperatureCelsius ?: Double.NaN,
                0.0,
            )
        }

        verifyCloudRequests(2)
    }

    @Test
    fun `cancellation uses neither stale cache nor failure cooldown`() {
        val sensor = cloudSensor(localTimestampMillis = 1_000L, localTemperature = 10.0)
        var requests = 0
        stubCloudResponse {
            requests += 1
            when (requests) {
                1, 3 -> successfulResponse(timestampEpochSeconds = requests.toLong() + 1L)
                else -> throw CancellationException("Cancelled")
            }
        }
        val provider = createProvider()

        runBlocking {
            provider.getLatestSnapshot(sensor)
            now = CLOUD_REFRESH_INTERVAL_MILLIS
        }
        assertThrows(CancellationException::class.java) {
            runBlocking { provider.getLatestSnapshot(sensor) }
        }
        runBlocking {
            assertEquals(
                20.0,
                provider.getLatestSnapshot(sensor)?.temperatureCelsius ?: Double.NaN,
                0.0,
            )
        }

        verifyCloudRequests(3)
    }

    @Test
    fun `missing sensor or measurements produce no snapshot`() {
        whenever(tagRepository.getFavoriteSensorById(SENSOR_ID)).thenReturn(null)
        val provider = createProvider()

        assertNull(runBlocking { provider.getLatestSnapshot(SENSOR_ID) })
        assertNull(runBlocking { provider.getLatestSnapshot(localSensor(latestMeasurement = false)) })
    }

    private fun createProvider(
        decoder: (String, String, Int) -> FoundRuuviTag = { _, _, _ -> decoded },
    ) = WidgetSensorSnapshotProvider(
        tagRepository = tagRepository,
        cloudInteractor = cloudInteractor,
        decoder = decoder,
        monotonicTimeMillis = { now },
    )

    private fun stubCloudResponse(response: SensorDenseResponse?) {
        runBlocking {
            whenever(cloudInteractor.getSensorDenseLastData(any())).thenReturn(response)
        }
    }

    private fun stubCloudResponse(response: () -> SensorDenseResponse?) {
        runBlocking {
            whenever(cloudInteractor.getSensorDenseLastData(any())).thenAnswer { response() }
        }
    }

    private fun verifyCloudRequests(expectedCount: Int) {
        runBlocking {
            if (expectedCount == 0) {
                verify(cloudInteractor, never()).getSensorDenseLastData(any())
            } else {
                verify(cloudInteractor, times(expectedCount)).getSensorDenseLastData(any())
            }
        }
    }

    private fun localSensor(
        latestMeasurement: Boolean = true,
        timestampMillis: Long = 1_000L,
        temperature: Double = 10.0,
    ): RuuviTag = ruuviTagPreview.copy(
        id = SENSOR_ID,
        displayName = "Sensor",
        networkLastSync = null,
        latestMeasurement = if (latestMeasurement) {
            sensorMeasurementsPreview.copy(
                temperature = environment(temperature),
                updatedAt = Date(timestampMillis),
            )
        } else {
            null
        },
    )

    private fun cloudSensor(
        localTimestampMillis: Long,
        localTemperature: Double,
    ): RuuviTag = localSensor(
        timestampMillis = localTimestampMillis,
        temperature = localTemperature,
    ).copy(networkLastSync = Date())

    private fun environment(value: Double) = EnvironmentValue(
        original = value,
        value = value,
        accuracy = Accuracy.Accuracy2,
        valueWithUnit = value.toString(),
        valueWithoutUnit = value.toString(),
        unitString = "°C",
        unitType = UnitType.TemperatureUnit.Celsius,
    )

    private fun successfulResponse(
        timestampEpochSeconds: Long,
        temperatureOffset: Double = 0.0,
    ): SensorDenseResponse = successfulResponse(
        cloudMeasurements = listOf(measurement(timestampEpochSeconds)),
        temperatureOffset = temperatureOffset,
    )

    private fun successfulResponse(
        cloudMeasurements: List<SensorDataMeasurementResponse>,
        temperatureOffset: Double = 0.0,
    ): SensorDenseResponse {
        val sensorInfo = sensorInfo(
            measurements = cloudMeasurements,
            temperatureOffset = temperatureOffset,
        )
        return RuuviNetworkResponse(
            result = RuuviNetworkResponse.successResult,
            error = "",
            data = SensorsDenseResponseBody(sensors = listOf(sensorInfo)),
            code = null,
        )
    }

    private fun measurement(
        timestampEpochSeconds: Long,
        data: String = "encoded",
        rssi: Int = -50,
    ) = SensorDataMeasurementResponse(
        coordinates = "",
        gwmac = "",
        data = data,
        timestamp = timestampEpochSeconds,
        rssi = rssi,
    )

    private fun unsuccessfulResponse(): SensorDenseResponse = RuuviNetworkResponse(
        result = RuuviNetworkResponse.errorResult,
        error = "Unavailable",
        data = null,
        code = null,
    )

    private fun sensorInfo(
        measurements: List<SensorDataMeasurementResponse>,
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
        measurements = measurements,
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

    private fun decodedSensor() = FoundRuuviTag(
        rssi = null,
        temperature = 20.0,
        humidity = null,
        pressure = null,
        accelX = null,
        accelY = null,
        accelZ = null,
        voltage = null,
        movementCounter = null,
        measurementSequenceNumber = null,
        pm1 = null,
        pm25 = null,
        pm4 = null,
        pm10 = null,
        co2 = null,
        voc = null,
        nox = null,
        luminosity = null,
        dBaAvg = null,
        dBaPeak = null,
    )

    companion object {
        private const val SENSOR_ID = "AA:BB:CC:DD:EE:FF"
        private const val CLOUD_REFRESH_INTERVAL_MILLIS = 60_000L
    }
}
