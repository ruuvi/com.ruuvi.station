package com.ruuvi.station.bluetooth.domain

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.bluetooth.BluetoothInteractor
import com.ruuvi.station.bluetooth.IRuuviGattListener
import com.ruuvi.station.bluetooth.LogReading
import com.ruuvi.station.bluetooth.model.GattSyncStatus
import com.ruuvi.station.bluetooth.model.SyncProgress
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.dataforwarding.domain.DataForwardingSender
import com.ruuvi.station.firebase.domain.FirebaseInteractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.util.*

class BluetoothGattInteractor (
    private val interactor: BluetoothInteractor,
    private val tagRepository: TagRepository,
    private val sensorSettingsRepository: SensorSettingsRepository,
    private val sensorHistoryRepository: SensorHistoryRepository,
    private val firebaseInteractor: FirebaseInteractor,
    private val dataForwardingSender: DataForwardingSender,
    private val preferencesRepository: PreferencesRepository
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
                if (shouldForwardData()) forwardGattReadings(sensorId, data)
            }

            override fun syncProgress(syncedDataPoints: Int) {
                // Throttle UI updates, this might have to be tweaked.
                // Maybe a time based thing would be better?
                if (syncedDataPoints % 10 == 0) {
                    setSyncStatus(GattSyncStatus(sensorId, SyncProgress.READING_DATA, syncedDataPoints = syncedDataPoints))
                }
            }

            override fun error(errorMessage: String) {
                Timber.d("Error: $errorMessage")
            }

            override fun heartbeat(raw: String) { }
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
        Timber.d("saveGattReadings")
        firebaseInteractor.logGattSync(data.size)
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
        sensorHistoryRepository.bulkInsert(sensorId, tagReadingList)
        updateLastSync(sensorId, Date())
    }

    fun forwardGattReadings(sensorId: String, data: List<LogReading>) {
        Timber.d("forwardGattReadings")
        val sensorSettings = sensorSettingsRepository.getSensorSettings(sensorId)
        if (sensorSettings != null) {
            dataForwardingSender.sendGattSyncData(data, sensorSettings)
        }
    }

    fun updateLastSync(sensorId: String, date: Date?) =
        sensorSettingsRepository.updateLastSync(sensorId, date)

    fun resetGattStatus(sensorId: String) {
        setSyncStatus(GattSyncStatus(sensorId, SyncProgress.STILL))
    }

    private fun shouldForwardData(): Boolean {
        return preferencesRepository.getDataForwardingDuringSyncEnabled() &&
                preferencesRepository.getDataForwardingUrl().isNotEmpty()
    }
}