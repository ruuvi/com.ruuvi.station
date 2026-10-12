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
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.dataforwarding.domain.DataForwardingSender
import com.ruuvi.station.firebase.domain.FirebaseInteractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

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
    private val sessionIdGenerator = AtomicLong()
    private val activeSessions = ConcurrentHashMap<String, Long>()
    private val sessionLock = Any()

    fun readLogs(sensorId: String, from: Date?): Boolean {
        val sessionId = synchronized(sessionLock) {
            if (activeSessions.containsKey(sensorId)) {
                Timber.d("Ignoring duplicate GATT download for $sensorId")
                return false
            }
            sessionIdGenerator.incrementAndGet().also { activeSessions[sensorId] = it }
        }
        setSessionSyncStatus(sensorId, sessionId, SyncProgress.CONNECTING)
        val found= interactor.readLogs(sensorId, from, object : IRuuviGattListener {
            override fun connected(state: Boolean) {
                Timber.d("connected $state syncProgress = ${syncStatus.value?.syncProgress}")
                if (state) {
                    setSessionSyncStatus(sensorId, sessionId, SyncProgress.CONNECTED)
                    setSessionSyncStatus(sensorId, sessionId, SyncProgress.READING_INFO)
                } else {
                    if (syncStatus.value?.syncProgress == SyncProgress.SAVING_DATA || syncStatus.value?.syncProgress == SyncProgress.DONE) {
                        setSessionSyncStatus(sensorId, sessionId, SyncProgress.DONE)
                    } else {
                        setSessionSyncStatus(sensorId, sessionId, SyncProgress.DISCONNECTED)
                    }
                }
            }

            override fun deviceInfo(model: String, fw: String, canReadLogs: Boolean, serialNumber: String?) {
                if (canReadLogs) {
                    setSessionSyncStatus(
                        sensorId,
                        sessionId,
                        SyncProgress.READING_DATA,
                        deviceInfoModel = model,
                        deviceInfoFw = fw
                    )
                } else {
                    setSessionSyncStatus(
                        sensorId,
                        sessionId,
                        SyncProgress.NOT_SUPPORTED,
                        deviceInfoModel = model,
                        deviceInfoFw = fw
                    )
                }
            }

            override fun dataReady(data: List<LogReading>) {
                setSessionSyncStatus(
                    sensorId,
                    sessionId,
                    SyncProgress.SAVING_DATA,
                    readDataSize = data.size
                )
                if (!isCurrentSession(sensorId, sessionId)) return
                saveGattReadings(sensorId, data)
                if (isCurrentSession(sensorId, sessionId) && shouldForwardData()) {
                    forwardGattReadings(sensorId, data)
                }
            }

            override fun syncProgress(syncedDataPoints: Int) {
                // Throttle UI updates, this might have to be tweaked.
                // Maybe a time based thing would be better?
                if (syncedDataPoints % 10 == 0) {
                    setSessionSyncStatus(
                        sensorId,
                        sessionId,
                        SyncProgress.READING_DATA,
                        syncedDataPoints = syncedDataPoints
                    )
                }
            }

            override fun error(errorMessage: String) {
                Timber.d("Error: $errorMessage")
                setSessionSyncStatus(sensorId, sessionId, SyncProgress.ERROR)
            }

            override fun heartbeat(raw: String) { }
        })
        if (!found)
            setSessionSyncStatus(sensorId, sessionId, SyncProgress.NOT_FOUND)
        return true
    }

    fun disconnect(id: String): Boolean {
        return interactor.disconnect(id)
    }

    fun isSyncInProgress(sensorId: String? = null): Boolean = synchronized(sessionLock) {
        sensorId?.let(activeSessions::containsKey) ?: activeSessions.isNotEmpty()
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
            val reading = TagSensorReading(logReading)
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

    fun resetGattStatus(sensorId: String, sessionId: Long? = null) {
        synchronized(sessionLock) {
            val currentStatus = syncStatus.value
            if (sessionId != null &&
                (currentStatus?.sensorId != sensorId || currentStatus.sessionId != sessionId)
            ) {
                return
            }
            activeSessions.remove(sensorId)
            setSyncStatus(GattSyncStatus(sensorId, SyncProgress.STILL, sessionId = sessionId ?: 0))
        }
    }

    private fun setSessionSyncStatus(
        sensorId: String,
        sessionId: Long,
        syncProgress: SyncProgress,
        deviceInfoModel: String = "",
        deviceInfoFw: String = "",
        readDataSize: Int = 0,
        syncedDataPoints: Int = 0
    ) {
        synchronized(sessionLock) {
            if (!isCurrentSession(sensorId, sessionId)) {
                Timber.d("Ignoring stale GATT callback for $sensorId session $sessionId")
                return
            }
            setSyncStatus(
                GattSyncStatus(
                    sensorId = sensorId,
                    syncProgress = syncProgress,
                    deviceInfoModel = deviceInfoModel,
                    deviceInfoFw = deviceInfoFw,
                    readDataSize = readDataSize,
                    syncedDataPoints = syncedDataPoints,
                    sessionId = sessionId
                )
            )
            if (!syncProgress.syncInProgress) {
                activeSessions.remove(sensorId, sessionId)
            }
        }
    }

    private fun isCurrentSession(sensorId: String, sessionId: Long): Boolean =
        activeSessions[sensorId] == sessionId

    private fun shouldForwardData(): Boolean {
        return preferencesRepository.getDataForwardingDuringSyncEnabled() &&
                preferencesRepository.getDataForwardingUrl().isNotEmpty()
    }
}