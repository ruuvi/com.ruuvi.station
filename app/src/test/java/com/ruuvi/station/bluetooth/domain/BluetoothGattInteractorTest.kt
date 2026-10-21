package com.ruuvi.station.bluetooth.domain

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.bluetooth.BluetoothInteractor
import com.ruuvi.station.bluetooth.contract.IRuuviGattListener
import com.ruuvi.station.bluetooth.contract.LogReading
import com.ruuvi.station.bluetooth.model.GattSyncStatus
import com.ruuvi.station.bluetooth.model.SyncProgress
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.dataforwarding.domain.DataForwardingSender
import com.ruuvi.station.firebase.domain.FirebaseInteractor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class BluetoothGattInteractorTest {
    private val bluetoothInteractor = mockk<BluetoothInteractor>(relaxed = true)
    private val sensorHistoryRepository = mockk<SensorHistoryRepository>(relaxed = true)
    private val sensorSettingsRepository = mockk<SensorSettingsRepository>(relaxed = true)
    private val firebaseInteractor = mockk<FirebaseInteractor>(relaxed = true)
    private val dataForwardingSender = mockk<DataForwardingSender>(relaxed = true)
    private val preferencesRepository = mockk<PreferencesRepository>(relaxed = true)
    private val interactor = BluetoothGattInteractor(
        interactor = bluetoothInteractor,
        tagRepository = mockk<TagRepository>(relaxed = true),
        sensorSettingsRepository = sensorSettingsRepository,
        sensorHistoryRepository = sensorHistoryRepository,
        firebaseInteractor = firebaseInteractor,
        dataForwardingSender = dataForwardingSender,
        preferencesRepository = preferencesRepository
    )

    @Test
    fun `saving GATT readings converts log records before storing them`() {
        val sensorId = "AA:BB:CC:DD:EE:FF"
        val timestamp = Date(1_000L)
        val log = mockk<LogReading>(relaxed = true)
        val savedReadings = slot<List<TagSensorReading>>()
        every { log.id } returns sensorId
        every { log.date } returns timestamp
        every { log.temperature } returns 21.5
        every { sensorHistoryRepository.bulkInsert(sensorId, capture(savedReadings)) } returns Unit

        interactor.saveGattReadings(sensorId, listOf(log))

        val reading = savedReadings.captured.single()
        assertEquals(sensorId, reading.ruuviTagId)
        assertEquals(timestamp, reading.createdAt)
        assertEquals(21.5, reading.temperature)
        verify { firebaseInteractor.logGattSync(1) }
        verify { sensorSettingsRepository.updateLastSync(sensorId, any()) }
    }

    @Test
    fun `GATT callbacks update status forward readings and complete the session`() {
        val sensorId = "AA:BB:CC:DD:EE:FF"
        val listener = slot<IRuuviGattListener>()
        val sensorSettings = mockk<SensorSettings>()
        val log = logReading(sensorId)
        every { bluetoothInteractor.readLogs(sensorId, any(), capture(listener)) } returns true
        every { preferencesRepository.getDataForwardingDuringSyncEnabled() } returns true
        every { preferencesRepository.getDataForwardingUrl() } returns "https://example.com"
        every { sensorSettingsRepository.getSensorSettings(sensorId) } returns sensorSettings

        assertTrue(interactor.readLogs(sensorId, Date()))
        assertEquals(SyncProgress.CONNECTING, interactor.syncStatusFlow.value?.syncProgress)
        assertTrue(interactor.isSyncInProgress(sensorId))

        listener.captured.connected(true)
        assertEquals(SyncProgress.READING_INFO, interactor.syncStatusFlow.value?.syncProgress)
        listener.captured.deviceInfo("Ruuvi Air", "1.0.0", true, null)
        assertEquals(SyncProgress.READING_DATA, interactor.syncStatusFlow.value?.syncProgress)
        listener.captured.syncProgress(9)
        assertEquals(0, interactor.syncStatusFlow.value?.syncedDataPoints)
        listener.captured.syncProgress(10)
        assertEquals(10, interactor.syncStatusFlow.value?.syncedDataPoints)
        listener.captured.dataReady(listOf(log))
        assertEquals(SyncProgress.SAVING_DATA, interactor.syncStatusFlow.value?.syncProgress)
        verify { dataForwardingSender.sendGattSyncData(listOf(log), sensorSettings) }

        listener.captured.connected(false)
        assertEquals(SyncProgress.DONE, interactor.syncStatusFlow.value?.syncProgress)
        assertFalse(interactor.isSyncInProgress(sensorId))

        interactor.resetGattStatus(sensorId)
        assertEquals(SyncProgress.STILL, interactor.syncStatusFlow.value?.syncProgress)
    }

    @Test
    fun `disconnecting before data is saved marks the session disconnected`() {
        val sensorId = "AA:BB:CC:DD:EE:FF"
        val listener = slot<IRuuviGattListener>()
        every { bluetoothInteractor.readLogs(sensorId, any(), capture(listener)) } returns true

        interactor.readLogs(sensorId, Date())
        listener.captured.connected(false)

        assertEquals(SyncProgress.DISCONNECTED, interactor.syncStatusFlow.value?.syncProgress)
        assertFalse(interactor.isSyncInProgress(sensorId))
    }

    @Test
    fun `GATT status retains whether the download is automatic`() {
        val sensorId = "AA:BB:CC:DD:EE:FF"
        val listener = slot<IRuuviGattListener>()
        every { bluetoothInteractor.readLogs(sensorId, any(), capture(listener)) } returns true

        interactor.readLogs(sensorId, Date(), manualSync = false)
        assertFalse(interactor.syncStatusFlow.value?.manualSync ?: true)

        listener.captured.connected(true)
        assertFalse(interactor.syncStatusFlow.value?.manualSync ?: true)

        listener.captured.deviceInfo("Ruuvi Air", "1.0.0", false, null)
        assertFalse(interactor.syncStatusFlow.value?.manualSync ?: true)
    }

    @Test
    fun `unsupported missing and failed GATT sessions are terminal`() {
        val sensorId = "AA:BB:CC:DD:EE:FF"
        val listener = slot<IRuuviGattListener>()
        every { bluetoothInteractor.readLogs(sensorId, any(), capture(listener)) } returns true

        interactor.readLogs(sensorId, Date())
        listener.captured.deviceInfo("Ruuvi Air", "1.0.0", false, null)
        assertEquals(SyncProgress.NOT_SUPPORTED, interactor.syncStatusFlow.value?.syncProgress)
        assertFalse(interactor.isSyncInProgress(sensorId))

        interactor.readLogs(sensorId, Date())
        listener.captured.error("Connection failed")
        assertEquals(SyncProgress.ERROR, interactor.syncStatusFlow.value?.syncProgress)
        assertFalse(interactor.isSyncInProgress(sensorId))

        every { bluetoothInteractor.readLogs(sensorId, any(), any()) } returns false
        interactor.readLogs(sensorId, Date())
        assertEquals(SyncProgress.NOT_FOUND, interactor.syncStatusFlow.value?.syncProgress)
        assertFalse(interactor.isSyncInProgress(sensorId))
    }

    @Test
    fun `duplicate GATT download does not start another session`() {
        val sensorId = "AA:BB:CC:DD:EE:FF"
        every { bluetoothInteractor.readLogs(sensorId, any(), any()) } returns true

        assertTrue(interactor.readLogs(sensorId, Date()))
        assertFalse(interactor.readLogs(sensorId, Date()))

        verify(exactly = 1) { bluetoothInteractor.readLogs(sensorId, any(), any()) }
    }

    @Test
    fun `GATT progress is tracked independently for each sensor`() = runBlocking {
        val firstSensorId = "AA:BB:CC:DD:EE:FF"
        val secondSensorId = "11:22:33:44:55:66"
        interactor.setSyncStatus(GattSyncStatus(firstSensorId, SyncProgress.READING_DATA))
        interactor.setSyncStatus(GattSyncStatus(secondSensorId, SyncProgress.SAVING_DATA))

        assertEquals(
            SyncProgress.READING_DATA,
            interactor.getSyncStatusFlow(firstSensorId).first()?.syncProgress
        )
        assertEquals(
            SyncProgress.SAVING_DATA,
            interactor.getSyncStatusFlow(secondSensorId).first()?.syncProgress
        )
    }

    private fun logReading(sensorId: String): LogReading = mockk<LogReading>(relaxed = true).also {
        every { it.id } returns sensorId
        every { it.date } returns Date(1_000L)
    }
}
