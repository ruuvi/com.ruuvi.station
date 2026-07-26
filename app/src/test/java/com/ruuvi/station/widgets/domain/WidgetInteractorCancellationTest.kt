package com.ruuvi.station.widgets.domain

import android.content.Context
import com.ruuvi.station.tag.domain.ruuviTagPreview
import com.ruuvi.station.widgets.data.WidgetType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Test

class WidgetInteractorCancellationTest {
    private val context = mockk<Context>(relaxed = true)
    private val snapshotProvider = mockk<WidgetSensorSnapshotProvider>()
    private val measurementFormatter = mockk<WidgetMeasurementFormatterRegistry>()
    private val sensor = ruuviTagPreview.copy(
        id = SENSOR_ID,
        displayName = "Cloud sensor",
    )
    private val interactor = WidgetInteractor(
        context = context,
        snapshotProvider = snapshotProvider,
        measurementFormatter = measurementFormatter,
    )

    @Test
    fun `complex widget cancellation is propagated`() {
        every { snapshotProvider.getFavoriteSensor(SENSOR_ID) } returns sensor
        coEvery {
            snapshotProvider.getLatestSnapshot(sensor)
        } throws CancellationException("Cancelled")

        assertThrows(CancellationException::class.java) {
            runBlocking {
                interactor.getComplexWidgetData(SENSOR_ID, settings = null)
            }
        }
    }

    @Test
    fun `simple widget cancellation is propagated`() {
        every { snapshotProvider.getFavoriteSensor(SENSOR_ID) } returns sensor
        coEvery {
            snapshotProvider.getLatestSnapshot(sensor)
        } throws CancellationException("Cancelled")

        assertThrows(CancellationException::class.java) {
            runBlocking {
                interactor.getSimpleWidgetData(SENSOR_ID, WidgetType.TEMPERATURE)
            }
        }
    }

    companion object {
        private const val SENSOR_ID = "AA:BB:CC:DD:EE:FF"
    }
}
