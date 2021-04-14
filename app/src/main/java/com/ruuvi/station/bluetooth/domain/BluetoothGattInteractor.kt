package com.ruuvi.station.bluetooth.domain

import com.ruuvi.station.bluetooth.BluetoothInteractor
import com.ruuvi.station.bluetooth.IRuuviGattListener
import com.ruuvi.station.bluetooth.LogReading
import com.ruuvi.station.bluetooth.model.GattSyncStatus
import com.ruuvi.station.bluetooth.model.SyncProgress
import com.ruuvi.station.database.SensorSettingsRepository
import com.ruuvi.station.database.TagRepository
import com.ruuvi.station.database.tables.TagSensorReading
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.util.*

class BluetoothGattInteractor (
    private val interactor: BluetoothInteractor,
    private val tagRepository: TagRepository,
    private val sensorSettingsRepository: SensorSettingsRepository
) {
    private val syncStatus = MutableStateFlow<GattSyncStatus?> (null)
    val syncStatusFlow: StateFlow<GattSyncStatus?> = syncStatus

    fun readLogs(sensorId: String, from: Date?) {
        setSyncStatus(GattSyncStatus(sensorId, SyncProgress.CONNECTING))
        val found= interactor.readLogs(sensorId, from, object : IRuuviGattListener {
            override fun connected(state: Boolean) {
                if (state) {
                    setSyncStatus(GattSyncStatus(sensorId, SyncProgress.CONNECTED))
                    setSyncStatus(GattSyncStatus(sensorId, SyncProgress.READING_INFO))
                } else {
                    if (syncStatus.value?.syncProgress == SyncProgress.SAVING_DATA) {
                        setSyncStatus(GattSyncStatus(sensorId, SyncProgress.DONE))
                    } else {
                        setSyncStatus(GattSyncStatus(sensorId, SyncProgress.DISCONNECTED))
                    }
                }
            }

            override fun deviceInfo(model: String, fw: String, canReadLogs: Boolean) {
                if (canReadLogs) {
                    setSyncStatus(GattSyncStatus(sensorId, SyncProgress.READING_DATA, model, fw))
                } else {
                    setSyncStatus(GattSyncStatus(sensorId, SyncProgress.NOT_SUPPORTED, model, fw))
                }
            }

            override fun dataReady(data: List<LogReading>) {
                setSyncStatus(GattSyncStatus(
                    sensorId,
                    SyncProgress.SAVING_DATA,
                    readDataSize = data.size
                ))
                saveGattReadings(sensorId, data)
            }

            override fun heartbeat(raw: String) {
            }
        })
        if (!found)
            setSyncStatus(GattSyncStatus(sensorId, SyncProgress.NOT_FOUND))
    }

    fun disconnect(id: String): Boolean {
        return interactor.disconnect(id)
    }

    fun setSyncStatus(status: GattSyncStatus) {
        Timber.d("syncStatusFlow sent $status")
        syncStatus.value = status
    }

    fun saveGattReadings(sensorId: String, data: List<LogReading>) {
        val tagReadingList = mutableListOf<TagSensorReading>()
        val sensorSettings = sensorSettingsRepository.getSensorSettings(sensorId)
        data.forEach { logReading ->
            val reading = TagSensorReading()
            reading.ruuviTagId = sensorId
            reading.temperature = logReading.temperature
            reading.humidity = logReading.humidity
            reading.pressure = logReading.pressure
            reading.createdAt = logReading.date
            sensorSettings?.calibrateSensor(reading)
            tagReadingList.add(reading)
        }
        TagSensorReading.saveList(tagReadingList)
        updateLastSync(sensorId, Date())
    }

    fun updateLastSync(sensorId: String, date: Date?) =
        tagRepository.getTagById(sensorId)?.let {
            it.lastSync = date
            it.update()
        }
}