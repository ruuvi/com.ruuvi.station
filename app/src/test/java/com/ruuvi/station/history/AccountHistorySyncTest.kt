package com.ruuvi.station.history

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.network.data.NetworkSyncEvent
import com.ruuvi.station.network.data.response.*
import com.ruuvi.station.network.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*

class AccountHistorySyncTest {
    @Test fun `account refresh with 50 paid sensors succeeds without downloading or saving history`() = runBlocking<Unit> {
        val network = mock<RuuviNetworkInteractor>()
        val preferences = mock<PreferencesRepository>()
        val settings = mock<SensorSettingsRepository>()
        val tags = mock<TagRepository>()
        val historySync = mock<NetworkHistoryInteractor>()
        val history = mock<com.ruuvi.station.database.domain.SensorHistoryRepository>()
        val sensorId = "AA:BB:CC:DD:EE:FF"
        val owner = "owner@example.com"
        val local = SensorSettings(id = sensorId, networkSensor = true, owner = owner, canShare = true,
            subscriptionName = "Pro", cloudHistoryDays = 365)
        val remote = SensorsDenseInfo(sensorId, owner, "Sensor", "", false, true,
            0.0, 0.0, 0.0, emptyList(), emptyList(), 0L,
            SensorSubscription(365, 1, true, true, "Pro"), null, emptyList(), emptyList())
        val localSensors = (0 until 50).map { local.copy(id = "AA:BB:CC:DD:EE:${it.toString(16).padStart(2, '0')}") }
        val remoteSensors = localSensors.map { remote.copy(sensor = it.id) }
        whenever(network.signedIn).thenReturn(true)
        whenever(network.getEmail()).thenReturn(owner)
        whenever(network.getSensorDenseLastData(any())).thenReturn(
            RuuviNetworkResponse("success", "", SensorsDenseResponseBody(remoteSensors), null))
        whenever(settings.getSensorSettingsOrCreate(any())).thenAnswer { call -> localSensors.single { it.id == call.getArgument<String>(0) } }
        whenever(settings.getSensorSettings(any())).thenAnswer { call -> localSensors.single { it.id == call.getArgument<String>(0) } }
        whenever(settings.getSensorSettings()).thenReturn(localSensors)
        val sync = NetworkDataSyncInteractor(preferences, tags, network, mock(), settings, history, mock(),
            mock(), mock(), mock(), mock(), mock(), mock(), mock(), mock(), historySync)

        sync.syncNetworkData().join()

        assertTrue(sync.lastResult is NetworkSyncEvent.Success)
        assertFalse(sync.syncInProgressFlow.value)
        verify(network).getSensorDenseLastData(any())
        verify(preferences).setLastSyncDate(any())
        verify(network, never()).getSensorData(any())
        verifyNoInteractions(historySync)
        verifyNoInteractions(history)
    }
}
