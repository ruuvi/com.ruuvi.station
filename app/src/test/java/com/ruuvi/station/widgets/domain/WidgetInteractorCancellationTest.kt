package com.ruuvi.station.widgets.domain

import android.content.Context
import com.ruuvi.station.tag.domain.ruuviTagPreview
import com.ruuvi.station.widgets.data.WidgetType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class WidgetInteractorCancellationTest {
    private val context = mock<Context>()
    private val snapshotProvider = mock<WidgetSensorSnapshotProvider>()
    private val measurementFormatter = mock<WidgetMeasurementFormatterRegistry>()
    private val timestampFormatter = mock<WidgetTimestampFormatter>()
    private val sensor = ruuviTagPreview.copy(
        id = SENSOR_ID,
        displayName = "Cloud sensor",
    )
    private val interactor = WidgetInteractor(
        context = context,
        snapshotProvider = snapshotProvider,
        measurementFormatter = measurementFormatter,
        timestampFormatter = timestampFormatter,
    )

    @Test
    fun `complex widget cancellation is propagated`() {
        whenever(snapshotProvider.getFavoriteSensor(SENSOR_ID)).thenReturn(sensor)
        runBlocking {
            whenever(snapshotProvider.getLatestSnapshot(sensor))
                .thenThrow(CancellationException("Cancelled"))
        }

        assertThrows(CancellationException::class.java) {
            runBlocking {
                interactor.getComplexWidgetData(SENSOR_ID, settings = null)
            }
        }
    }

    @Test
    fun `simple widget cancellation is propagated`() {
        whenever(snapshotProvider.getFavoriteSensor(SENSOR_ID)).thenReturn(sensor)
        runBlocking {
            whenever(snapshotProvider.getLatestSnapshot(sensor))
                .thenThrow(CancellationException("Cancelled"))
        }

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
