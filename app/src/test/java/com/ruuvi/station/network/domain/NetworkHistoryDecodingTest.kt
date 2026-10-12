package com.ruuvi.station.network.domain

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.bluetooth.BluetoothLibrary
import com.ruuvi.station.bluetooth.contract.FoundRuuviTag
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.history.HistoryRange
import com.ruuvi.station.network.data.response.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** Decode coverage retained from the former account-sync history downloader. */
class NetworkHistoryDecodingTest {
    @Test fun `history skips malformed and legacy payloads but checkpoints all returned timestamps`() = runBlocking {
        val now = 1_800_000_000_000L
        val range = HistoryRange(now - 10_000, now)
        val network = mockk<RuuviNetworkInteractor>(relaxed = true)
        val settings = mockk<SensorSettingsRepository>()
        val history = mockk<SensorHistoryRepository>(relaxed = true)
        val preferences = mockk<PreferencesRepository>(relaxed = true)
        every { network.signedIn } returns true
        every { network.getEmail() } returns "account"
        every { settings.getSensorSettings(SYNC_SENSOR_ID) } returns
            SensorSettings(id = SYNC_SENSOR_ID, networkSensor = true, cloudHistoryDays = 1095)
        every { history.saveCloudPage(any(), any(), any(), any(), any(), any()) } returns true
        val start = range.startMillis / 1000
        fun response(values: List<SensorDataMeasurementResponse>): GetSensorDataResponse = RuuviNetworkResponse(
            "success", "", GetSensorDataResponseBody(SYNC_SENSOR_ID, 0.0, 0.0, 0.0, values.size, values), null)
        val data = listOf("valid", "", "invalid", "legacy-6", "legacy-f0")
        coEvery { network.getSensorData(any()) } returnsMany listOf(response(data.mapIndexed { i, payload ->
            SensorDataMeasurementResponse("", "", payload, start + i, -60)
        }), response(emptyList()))
        mockkObject(BluetoothLibrary)
        try {
            every { BluetoothLibrary.decode(any(), "valid", any()) } returns FoundRuuviTag(dataFormat = 5)
            every { BluetoothLibrary.decode(any(), "invalid", any()) } throws IllegalArgumentException("Invalid data")
            every { BluetoothLibrary.decode(any(), "legacy-6", any()) } returns FoundRuuviTag(dataFormat = 0x06)
            every { BluetoothLibrary.decode(any(), "legacy-f0", any()) } returns FoundRuuviTag(dataFormat = 0xF0)
            val sync = NetworkHistoryInteractor(network, settings, history, preferences) { now }
            sync.syncHistory(SYNC_SENSOR_ID, range)
            val pages = mutableListOf<List<TagSensorReading>>()
            val coverage = mutableListOf<HistoryRange>()
            verify(exactly = 2) { history.saveCloudPage(eq(SYNC_SENSOR_ID), capture(pages), capture(coverage), any(), any(), any()) }
            assertEquals(listOf(range.startMillis), pages.flatten().map { it.createdAt.time })
            assertEquals(range.startMillis + 5000, coverage.first().endExclusiveMillis)
            assertEquals(range.endExclusiveMillis, coverage.last().endExclusiveMillis)
            assertTrue(pages.last().isEmpty())
            assertFalse(sync.state.value.failed)
            verify(exactly = 0) { BluetoothLibrary.decode(any(), "", any()) }
        } finally { unmockkObject(BluetoothLibrary) }
    }
}
