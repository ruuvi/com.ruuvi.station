package com.ruuvi.station.bluetooth.domain

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.bluetooth.BluetoothInteractor
import com.ruuvi.station.bluetooth.contract.LogReading
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.dataforwarding.domain.DataForwardingSender
import com.ruuvi.station.firebase.domain.FirebaseInteractor
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class BluetoothGattInteractorTest {
    private val sensorHistoryRepository = mockk<SensorHistoryRepository>(relaxed = true)
    private val sensorSettingsRepository = mockk<SensorSettingsRepository>(relaxed = true)
    private val firebaseInteractor = mockk<FirebaseInteractor>(relaxed = true)
    private val interactor = BluetoothGattInteractor(
        interactor = mockk<BluetoothInteractor>(relaxed = true),
        tagRepository = mockk<TagRepository>(relaxed = true),
        sensorSettingsRepository = sensorSettingsRepository,
        sensorHistoryRepository = sensorHistoryRepository,
        firebaseInteractor = firebaseInteractor,
        dataForwardingSender = mockk<DataForwardingSender>(relaxed = true),
        preferencesRepository = mockk<PreferencesRepository>(relaxed = true)
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
}
