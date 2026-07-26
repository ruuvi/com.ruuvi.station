package com.ruuvi.station.widgets.domain

import android.content.Context
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.network.data.response.RuuviNetworkResponse
import com.ruuvi.station.network.data.response.SensorDenseResponse
import com.ruuvi.station.network.data.response.SensorsDenseResponseBody
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.ruuviTagPreview
import com.ruuvi.station.units.domain.AccelerationConverter
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.widgets.data.WidgetType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.Date

class WidgetInteractorCancellationTest {
    private val context = mockk<Context>(relaxed = true)
    private val tagRepository = mockk<TagRepository>()
    private val cloudInteractor = mockk<RuuviNetworkInteractor>()
    private val unitsConverter = mockk<UnitsConverter>(relaxed = true)
    private val accelerationConverter = mockk<AccelerationConverter>(relaxed = true)

    @Test
    fun `complex widget cloud cancellation is propagated`() {
        val sensor = cloudSensor()
        everyCloudSensor(sensor)
        coEvery {
            cloudInteractor.getSensorDenseLastData(any())
        } throws CancellationException("Cancelled")

        val interactor = createInteractor()

        assertThrows(CancellationException::class.java) {
            runBlocking {
                interactor.getComplexWidgetData(sensor.id, settings = null)
            }
        }
    }

    @Test
    fun `simple widget entry point propagates cloud cancellation`() {
        val sensor = cloudSensor()
        everyCloudSensor(sensor)
        coEvery {
            cloudInteractor.getSensorDenseLastData(any())
        } throws CancellationException("Cancelled")

        val interactor = createInteractor()

        assertThrows(CancellationException::class.java) {
            runBlocking {
                interactor.getSimpleWidgetData(sensor.id, WidgetType.TEMPERATURE)
            }
        }
    }

    @Test
    fun `simple widget cancellation uses neither cached data nor failure backoff`() {
        var now = 0L
        var requestCount = 0
        val cancellation = CancellationException("Cancelled")
        coEvery {
            cloudInteractor.getSensorDenseLastData(any())
        } coAnswers {
            requestCount += 1
            when (requestCount) {
                1, 3 -> successfulEmptyResponse()
                else -> throw cancellation
            }
        }
        val sensor = cloudSensor()
        val interactor = createInteractor { now }

        val initialResult = runBlocking {
            interactor.getSimpleDataFromCloud(sensor, WidgetType.TEMPERATURE)
        }
        assertNotNull(initialResult)

        now = CLOUD_REFRESH_INTERVAL_MILLIS
        assertThrows(CancellationException::class.java) {
            runBlocking {
                interactor.getSimpleDataFromCloud(sensor, WidgetType.TEMPERATURE)
            }
        }
        runBlocking {
            assertNotNull(interactor.getSimpleDataFromCloud(sensor, WidgetType.TEMPERATURE))
        }
        coVerify(exactly = 3) {
            cloudInteractor.getSensorDenseLastData(any())
        }
    }

    @Test
    fun `ordinary cloud failure retains widget fallbacks`() {
        val sensor = cloudSensor()
        everyCloudSensor(sensor)
        coEvery {
            cloudInteractor.getSensorDenseLastData(any())
        } throws IllegalStateException("Unavailable")

        val complexResult = runBlocking {
            createInteractor().getComplexWidgetData(sensor.id, settings = null)
        }
        val simpleResult = runBlocking {
            createInteractor().getSimpleDataFromCloud(sensor, WidgetType.TEMPERATURE)
        }

        assertEquals(sensor.id, complexResult.sensorId)
        assertEquals(Date(0), complexResult.timestamp)
        assertNull(simpleResult)
    }

    @Test
    fun `ordinary cloud failures are retried only after the refresh interval`() {
        var now = 0L
        coEvery {
            cloudInteractor.getSensorDenseLastData(any())
        } throws IllegalStateException("Unavailable")
        val sensor = cloudSensor()
        val interactor = createInteractor { now }

        runBlocking {
            assertNull(interactor.getSimpleDataFromCloud(sensor, WidgetType.TEMPERATURE))
            assertNotNull(interactor.getSimpleDataFromCloud(sensor, WidgetType.TEMPERATURE))
        }
        coVerify(exactly = 1) {
            cloudInteractor.getSensorDenseLastData(any())
        }

        now = CLOUD_REFRESH_INTERVAL_MILLIS
        runBlocking {
            assertNull(interactor.getSimpleDataFromCloud(sensor, WidgetType.TEMPERATURE))
        }
        coVerify(exactly = 2) {
            cloudInteractor.getSensorDenseLastData(any())
        }
    }

    private fun createInteractor(
        monotonicTimeMillis: () -> Long = { 0L },
    ) = WidgetInteractor(
        context = context,
        tagRepository = tagRepository,
        cloudInteractor = cloudInteractor,
        unitsConverter = unitsConverter,
        accelerationConverter = accelerationConverter,
        monotonicTimeMillis = monotonicTimeMillis,
    )

    private fun everyCloudSensor(sensor: RuuviTag) {
        every {
            tagRepository.getFavoriteSensorById(sensor.id)
        } returns sensor
    }

    private fun cloudSensor() = ruuviTagPreview.copy(
        id = SENSOR_ID,
        name = SENSOR_ID,
        displayName = "Cloud sensor",
        networkLastSync = Date(),
        latestMeasurement = null,
    )

    private fun successfulEmptyResponse(): SensorDenseResponse =
        RuuviNetworkResponse(
            result = RuuviNetworkResponse.successResult,
            error = "",
            data = SensorsDenseResponseBody(sensors = emptyList()),
            code = null,
        )

    companion object {
        private const val SENSOR_ID = "AA:BB:CC:DD:EE:FF"
        private const val CLOUD_REFRESH_INTERVAL_MILLIS = 60_000L
    }
}
