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
    @Test fun `account refresh updates paid sensor metadata and succeeds without downloading history`() = runBlocking<Unit> {
        val network = mock<RuuviNetworkInteractor>()
        val preferences = mock<PreferencesRepository>()
        val settings = mock<SensorSettingsRepository>()
        val tags = mock<TagRepository>()
        val historySync = mock<NetworkHistoryInteractor>()
        val sensorId = "AA:BB:CC:DD:EE:FF"
        val owner = "owner@example.com"
        val local = SensorSettings(id = sensorId, networkSensor = true, owner = owner, canShare = true,
            subscriptionName = "Pro", cloudHistoryDays = 365)
        val remote = SensorsDenseInfo(sensorId, owner, "Sensor", "", false, true,
            0.0, 0.0, 0.0, emptyList(), emptyList(), 0L,
            SensorSubscription(365, 1, true, true, "Pro"), null, emptyList(), emptyList())
        whenever(network.signedIn).thenReturn(true)
        whenever(network.getEmail()).thenReturn(owner)
        whenever(network.getSensorDenseLastData(any())).thenReturn(
            RuuviNetworkResponse("success", "", SensorsDenseResponseBody(listOf(remote)), null))
        whenever(settings.getSensorSettingsOrCreate(sensorId)).thenReturn(local)
        whenever(settings.getSensorSettings(sensorId)).thenReturn(local)
        whenever(settings.getSensorSettings()).thenReturn(listOf(local))
        val sync = NetworkDataSyncInteractor(preferences, tags, network, mock(), settings, mock(), mock(),
            mock(), mock(), mock(), mock(), mock(), mock(), mock(), mock(), historySync)

        sync.syncNetworkData().join()

        assertTrue(sync.lastResult is NetworkSyncEvent.Success)
        assertFalse(sync.syncInProgressFlow.value)
        verify(network).getSensorDenseLastData(any())
        verify(preferences).setLastSyncDate(any())
        verify(network, never()).getSensorData(any())
        verifyNoInteractions(historySync)
    }
}
