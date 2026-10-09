package com.ruuvi.station.tagdetails.ui

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.app.preferences.GlobalSettings
import com.ruuvi.station.bluetooth.domain.BluetoothGattInteractor
import com.ruuvi.station.bluetooth.model.GattSyncStatus
import com.ruuvi.station.bluetooth.model.SyncProgress
import com.ruuvi.station.database.domain.AlarmRepository
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.export.CsvExporter
import com.ruuvi.station.export.XlsxExporter
import com.ruuvi.station.network.domain.NetworkDataSyncInteractor
import com.ruuvi.station.nfc.domain.NfcResultInteractor
import com.ruuvi.station.settings.domain.AppSettingsInteractor
import com.ruuvi.station.tag.domain.TagInteractor
import com.ruuvi.station.tag.domain.ruuviTagPreview
import com.ruuvi.station.tag.domain.sensorMeasurementsPreview
import com.ruuvi.station.tagdetails.domain.TagDetailsInteractor
import com.ruuvi.station.units.domain.UnitsConverter
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

class SensorCardViewModelTest {
    private val tagInteractor = mockk<TagInteractor>(relaxed = true)
    private val tagDetailsInteractor = mockk<TagDetailsInteractor>()
    private val networkDataSyncInteractor = mockk<NetworkDataSyncInteractor>()
    private val appSettingsInteractor = mockk<AppSettingsInteractor>(relaxed = true)
    private val preferencesRepository = mockk<PreferencesRepository>(relaxed = true)
    private val gattInteractor = mockk<BluetoothGattInteractor>(relaxed = true)
    private val syncStatus = MutableStateFlow<GattSyncStatus?>(null)
    private val viewModel by lazy {
        SensorCardViewModel(
            arguments = SensorCardViewModelArguments(),
            tagInteractor = tagInteractor,
            tagDetailsInteractor = tagDetailsInteractor,
            networkDataSyncInteractor = networkDataSyncInteractor,
            appSettingsInteractor = appSettingsInteractor,
            preferencesRepository = preferencesRepository,
            gattInteractor = gattInteractor,
            sensorHistoryRepository = mockk<SensorHistoryRepository>(relaxed = true),
            csvExporter = mockk<CsvExporter>(relaxed = true),
            xlsxExporter = mockk<XlsxExporter>(relaxed = true),
            nfcResultInteractor = mockk<NfcResultInteractor>(relaxed = true),
            alarmRepository = mockk<AlarmRepository>(relaxed = true),
            unitsConverter = mockk<UnitsConverter>(relaxed = true),
        )
    }

    @Before
    fun setUp() {
        SensorCardViewModel.clearAutoSyncAttempts()
        every { networkDataSyncInteractor.syncInProgressFlow } returns MutableStateFlow(false)
        every { gattInteractor.syncStatusFlow } returns syncStatus
        every { tagDetailsInteractor.getTagById(AIR_SENSOR.id) } returns AIR_SENSOR
    }

    @Test
    fun `selected Air sensor starts GATT history sync when no recent sync or attempt`() {
        viewModel.autoSyncGattHistory(AIR_SENSOR, selected = true)

        verify { gattInteractor.readLogs(AIR_SENSOR.id, OLD_LAST_SYNC) }
    }

    @Test
    fun `selected Air sensor skips GATT history sync if attempted within 5 minutes`() {
        viewModel.autoSyncGattHistory(AIR_SENSOR, selected = true)
        viewModel.autoSyncGattHistory(AIR_SENSOR, selected = true)

        verify(exactly = 1) { gattInteractor.readLogs(AIR_SENSOR.id, any()) }
    }

    @Test
    fun `selected Air sensor skips GATT history sync if synced within 5 minutes`() {
        val recentSync = Date(System.currentTimeMillis() - 1 * 60 * 1000L)
        val recentSyncSensor = AIR_SENSOR.copy(lastSync = recentSync)

        viewModel.autoSyncGattHistory(recentSyncSensor, selected = true)

        verify(exactly = 0) { gattInteractor.readLogs(any(), any()) }
    }

    @Test
    fun `selected Air sensor skips GATT history sync if already downloading`() {
        val syncingStatus = GattSyncStatus(AIR_SENSOR.id, SyncProgress.READING_DATA)
        every { gattInteractor.syncStatusFlow } returns MutableStateFlow(syncingStatus)

        viewModel.autoSyncGattHistory(AIR_SENSOR, selected = true)

        verify(exactly = 0) { gattInteractor.readLogs(any(), any()) }
    }

    @Test
    fun `unselected Air sensor does not start GATT history sync`() {
        viewModel.autoSyncGattHistory(AIR_SENSOR, selected = false)

        verify(exactly = 0) { gattInteractor.readLogs(any(), any()) }
    }

    @Test
    fun `selected non-Air sensor does not start GATT history sync`() {
        viewModel.autoSyncGattHistory(ruuviTagPreview, selected = true)

        verify(exactly = 0) { gattInteractor.readLogs(any(), any()) }
    }

    @Test
    fun `selected cloud Air sensor does not start GATT history sync`() {
        viewModel.autoSyncGattHistory(CLOUD_AIR_SENSOR, selected = true)

        verify(exactly = 0) { gattInteractor.readLogs(any(), any()) }
    }

    @Test
    fun `manual GATT sync starts from the sensor last sync time`() {
        viewModel.syncGatt(AIR_SENSOR.id)

        verify { gattInteractor.readLogs(AIR_SENSOR.id, OLD_LAST_SYNC) }
    }

    @Test
    fun `GATT sync limits old sensor history to configured history length`() {
        val oldSensor = AIR_SENSOR.copy(id = "11:22:33:44:55:66", lastSync = Date(0))
        val syncFrom = slot<Date>()
        every { tagDetailsInteractor.getTagById(oldSensor.id) } returns oldSensor
        val earliestAllowed = System.currentTimeMillis() -
            1000L * 60 * 60 * 24 * GlobalSettings.historyLengthDays

        viewModel.syncGatt(oldSensor.id)

        verify { gattInteractor.readLogs(oldSensor.id, capture(syncFrom)) }
        assertTrue(syncFrom.captured.time >= earliestAllowed)
        assertTrue(syncFrom.captured.time <= System.currentTimeMillis())
    }

    @Test
    fun `GATT sync does not start when sensor is unavailable`() {
        every { tagDetailsInteractor.getTagById("missing-sensor") } returns null

        viewModel.syncGatt("missing-sensor")

        verify(exactly = 0) { gattInteractor.readLogs("missing-sensor", any()) }
    }

    @Test
    fun `automatic GATT failure is marked as non-manual`() = runBlocking {
        viewModel.autoSyncGattHistory(AIR_SENSOR, selected = true)
        syncStatus.value = GattSyncStatus(AIR_SENSOR.id, SyncProgress.ERROR)

        val event = viewModel.getGattEvents(AIR_SENSOR.id).first()

        assertFalse(event.manualSync)
    }

    @Test
    fun `manual GATT failure is marked as manual`() = runBlocking {
        viewModel.syncGatt(AIR_SENSOR.id)
        syncStatus.value = GattSyncStatus(AIR_SENSOR.id, SyncProgress.NOT_FOUND)

        val event = viewModel.getGattEvents(AIR_SENSOR.id).first()

        assertTrue(event.manualSync)
        assertEquals(SyncProgress.NOT_FOUND, event.syncProgress)
    }

    @Test
    fun `manual GATT events preserve progress for every sync state`() = runBlocking {
        viewModel.syncGatt(AIR_SENSOR.id)

        SyncProgress.entries.forEach { progress ->
            syncStatus.value = GattSyncStatus(
                sensorId = AIR_SENSOR.id,
                syncProgress = progress,
                syncedDataPoints = if (progress == SyncProgress.READING_DATA) 10 else 0,
                readDataSize = if (progress == SyncProgress.SAVING_DATA) 2 else 0,
            )

            val event = viewModel.getGattEvents(AIR_SENSOR.id).first()

            assertTrue(event.manualSync)
            assertEquals(progress, event.syncProgress)
        }
    }

    private companion object {
        val OLD_LAST_SYNC = Date(System.currentTimeMillis() - 10 * 60 * 1000L)
        val AIR_SENSOR = ruuviTagPreview.copy(
            id = "AA:BB:CC:DD:EE:FF",
            lastSync = OLD_LAST_SYNC,
            networkSensor = false,
            latestMeasurement = sensorMeasurementsPreview.copy(dataFormat = 0xE0),
        )
        val CLOUD_AIR_SENSOR = AIR_SENSOR.copy(
            networkSensor = true
        )
    }
}
