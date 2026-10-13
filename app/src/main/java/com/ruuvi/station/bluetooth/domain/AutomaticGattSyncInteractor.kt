package com.ruuvi.station.bluetooth.domain

import com.ruuvi.station.app.preferences.GlobalSettings
import com.ruuvi.station.app.preferences.PreferencesRepository
import java.util.Date

class AutomaticGattSyncInteractor(
    private val gattInteractor: BluetoothGattInteractor,
    private val preferencesRepository: PreferencesRepository,
) {
    fun syncIfEligible(
        sensorId: String,
        lastSync: Date?,
        isAir: Boolean,
        networkSensor: Boolean,
    ) {
        if (!isAir || networkSensor || gattInteractor.isSyncInProgress(sensorId)) return

        val now = System.currentTimeMillis()
        val lastAttempt = preferencesRepository.getLastAutomaticGattSyncAttempt(sensorId)
        val lastSyncedOrAttempted = maxOf(lastSync?.time ?: 0L, lastAttempt)
        if (now - lastSyncedOrAttempted < AUTO_SYNC_COOLDOWN_MS) return

        val earliestHistory = Date(now - HISTORY_LENGTH_MILLIS)
        val syncFrom = lastSync?.takeIf { it >= earliestHistory } ?: earliestHistory
        if (gattInteractor.readLogs(sensorId, syncFrom)) {
            preferencesRepository.setLastAutomaticGattSyncAttempt(sensorId, now)
        }
    }

    private companion object {
        private const val AUTO_SYNC_COOLDOWN_MS = 5 * 60 * 1000L
        private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L
        private const val HISTORY_LENGTH_MILLIS =
            GlobalSettings.historyLengthDays * MILLIS_PER_DAY
    }
}
