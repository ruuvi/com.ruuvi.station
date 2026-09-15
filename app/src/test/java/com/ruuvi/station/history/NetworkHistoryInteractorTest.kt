package com.ruuvi.station.history

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.network.data.request.GetSensorDataRequest
import com.ruuvi.station.network.data.response.*
import com.ruuvi.station.network.domain.NetworkHistoryInteractor
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class NetworkHistoryInteractorTest {
    private val network = mock<RuuviNetworkInteractor>()
    private val settings = mock<SensorSettingsRepository>()
    private val history = mock<SensorHistoryRepository>()
    private val preferences = mock<PreferencesRepository>()
    private val now = 1_800_000_000_000L
    private val sensorId = "AA:BB:CC:DD:EE:FF"
    private val selected = HistoryRange(now - 7 * 86_400_000L, now)
    private val interactor = NetworkHistoryInteractor(network, settings, history, preferences) { now }

    @Before fun prepare() = runBlocking<Unit> {
        whenever(network.signedIn).thenReturn(true)
        whenever(network.getEmail()).thenReturn("user@example.com")
        whenever(settings.getSensorSettings(sensorId)).thenReturn(SensorSettings(id = sensorId, networkSensor = true, cloudHistoryDays = 365))
        whenever(history.coverage(any(), any(), any(), any())).thenReturn(emptyList())
        whenever(history.saveCloudPage(any(), any(), any(), any(), any(), any())).thenReturn(true)
        whenever(network.getSensorData(any())).thenReturn(response())
    }

    @Test fun `download is restricted to one sensor and selected seven days`() = runBlocking<Unit> {
        interactor.syncHistory(sensorId, selected)
        val requests = argumentCaptor<GetSensorDataRequest>()
        verify(network).getSensorData(requests.capture())
        val request = requests.firstValue
        assertEquals(sensorId, request.sensor)
        assertEquals(selected.startMillis, request.since?.time)
        assertEquals(selected.endExclusiveMillis - 1000, request.until?.time)
        assertEquals(5000, request.limit)
        assertEquals("asc", request.sort?.code)
        assertEquals("mixed", request.mode?.code)
        verify(history).saveCloudPage(eq(sensorId), eq(emptyList()), eq(selected), eq("user@example.com"), eq("production"), eq(0L))
        assertFalse(interactor.state.value.failed)
    }

    @Test fun `a single record page continues until the bounded range is exhausted`() = runBlocking<Unit> {
        val start = selected.startMillis / 1000
        whenever(network.getSensorData(any())).thenReturn(response(start), response(start + 1), response())
        interactor.syncHistory(sensorId, selected)
        val requests = argumentCaptor<GetSensorDataRequest>()
        verify(network, times(3)).getSensorData(requests.capture())
        assertEquals(listOf(start, start + 1, start + 2), requests.allValues.map { it.since!!.time / 1000 })
        assertEquals(1, requests.allValues.map { it.until }.distinct().size)
        verify(history, times(3)).saveCloudPage(any(), any(), any(), any(), any(), any())
    }

    @Test fun `cached older empty interval does not download again`() = runBlocking<Unit> {
        val older = HistoryRange(now - 80 * 86_400_000L, now - 70 * 86_400_000L)
        whenever(history.coverage(any(), any(), any(), any())).thenReturn(listOf(older))
        interactor.syncHistory(sensorId, older)
        verify(network, never()).getSensorData(any())
    }

    @Test fun `expanding an older range only downloads uncovered portions`() = runBlocking<Unit> {
        val older = HistoryRange(now - 80 * 86_400_000L, now - 70 * 86_400_000L)
        val middle = HistoryRange(older.startMillis + 86_400_000L, older.endExclusiveMillis - 86_400_000L)
        whenever(history.coverage(any(), any(), any(), any())).thenReturn(listOf(middle))
        interactor.syncHistory(sensorId, older)
        val requests = argumentCaptor<GetSensorDataRequest>()
        verify(network, times(2)).getSensorData(requests.capture())
        assertEquals(older.startMillis, requests.firstValue.since?.time)
        assertEquals(middle.startMillis - 1000, requests.firstValue.until?.time)
        assertEquals(middle.endExclusiveMillis, requests.secondValue.since?.time)
    }

    @Test fun `live cached history refreshes only its last minute`() = runBlocking<Unit> {
        whenever(history.coverage(any(), any(), any(), any())).thenReturn(listOf(selected))
        interactor.syncHistory(sensorId, selected)
        val requests = argumentCaptor<GetSensorDataRequest>()
        verify(network).getSensorData(requests.capture())
        assertEquals(now - 60_000, requests.firstValue.since?.time)
    }

    @Test fun `subscription allowance clips cloud requests without changing selected range`() = runBlocking<Unit> {
        whenever(settings.getSensorSettings(sensorId)).thenReturn(SensorSettings(id = sensorId, networkSensor = true, cloudHistoryDays = 2))
        interactor.syncHistory(sensorId, selected)
        val requests = argumentCaptor<GetSensorDataRequest>()
        verify(network).getSensorData(requests.capture())
        assertEquals(now - 2 * 86_400_000L, requests.firstValue.since?.time)
        assertTrue(interactor.state.value.restricted)
    }

    @Test fun `zero allowance and local only sensors never request history`() = runBlocking<Unit> {
        whenever(settings.getSensorSettings(sensorId)).thenReturn(SensorSettings(id = sensorId, networkSensor = true, cloudHistoryDays = 0))
        interactor.syncHistory(sensorId, selected)
        assertTrue(interactor.state.value.restricted)
        whenever(settings.getSensorSettings(sensorId)).thenReturn(SensorSettings(id = sensorId, networkSensor = false))
        interactor.syncHistory(sensorId, selected)
        verify(network, never()).getSensorData(any())
    }

    @Test fun `failed page preserves completed page but cannot claim remaining coverage`() = runBlocking<Unit> {
        whenever(network.getSensorData(any())).thenReturn(response(selected.startMillis / 1000), null)
        interactor.syncHistory(sensorId, selected)
        verify(history, times(1)).saveCloudPage(any(), any(), any(), any(), any(), any())
        assertTrue(interactor.state.value.failed)
        assertFalse(interactor.state.value.loading)
    }

    @Test fun `non advancing response fails without looping or marking a gap covered`() = runBlocking<Unit> {
        whenever(network.getSensorData(any())).thenReturn(response(selected.startMillis / 1000 - 1))
        interactor.syncHistory(sensorId, selected)
        verify(network, times(1)).getSensorData(any())
        verify(history, never()).saveCloudPage(any(), any(), any(), any(), any(), any())
        assertTrue(interactor.state.value.failed)
    }

    @Test fun `deletion during download rejects page and stops pagination`() = runBlocking<Unit> {
        whenever(history.saveCloudPage(any(), any(), any(), any(), any(), any())).thenReturn(false)
        whenever(network.getSensorData(any())).thenReturn(response(selected.startMillis / 1000))
        interactor.syncHistory(sensorId, selected)
        verify(network, times(1)).getSensorData(any())
        assertTrue(interactor.state.value.failed)
    }

    @Test fun `cancel interrupts active request and does not save coverage`() = runBlocking<Unit> {
        val entered = CompletableDeferred<Unit>()
        whenever(network.getSensorData(any())).doSuspendableAnswer {
            entered.complete(Unit)
            awaitCancellation()
        }
        val job = launch { interactor.syncHistory(sensorId, selected) }
        entered.await()
        interactor.cancelAndJoin()
        assertTrue(job.isCancelled)
        verify(history, never()).saveCloudPage(any(), any(), any(), any(), any(), any())
    }

    @Test fun `cache keys isolate backend and account and stale coverage is revalidated`() = runBlocking<Unit> {
        whenever(preferences.isDevServerEnabled()).thenReturn(true)
        interactor.syncHistory(sensorId, selected)
        verify(history).coverage(sensorId, "user@example.com", "testnet", now - SensorHistoryRepository.CACHE_FRESHNESS_MILLIS)
    }

    private fun response(vararg seconds: Long): GetSensorDataResponse = RuuviNetworkResponse(
        "success", "", GetSensorDataResponseBody(sensorId, 0.0, 0.0, 0.0, seconds.size,
            seconds.map { SensorDataMeasurementResponse("", "", "", it, -60) }), null
    )
}
