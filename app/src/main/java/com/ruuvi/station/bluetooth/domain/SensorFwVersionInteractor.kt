package com.ruuvi.station.bluetooth.domain

import com.ruuvi.station.bluetooth.BluetoothInteractor
import com.ruuvi.station.bluetooth.IRuuviGattListener
import com.ruuvi.station.bluetooth.LogReading
import com.ruuvi.station.bluetooth.model.SensorFirmwareResult
import kotlinx.coroutines.delay
import timber.log.Timber

class SensorFwVersionInteractor (
    private val interactor: BluetoothInteractor
) {
    suspend fun getSensorFirmwareVersion(sensorId: String): SensorFirmwareResult {
        var result: SensorFirmwareResult? = null
        interactor.getFwVersion(sensorId, object : IRuuviGattListener {
            override fun connected(state: Boolean) {
                Timber.d("Connected state = $state")
            }

            override fun deviceInfo(model: String, fw: String, canReadLogs: Boolean) {
                Timber.d("deviceInfo model = $model fw = $fw")
                result = SensorFirmwareResult(true, fw, "")
            }

            override fun dataReady(data: List<LogReading>) {
                Timber.d("data ready")
            }

            override fun heartbeat(raw: String) {
                Timber.d("heartbeat")
            }

            override fun syncProgress(syncedDataPoints: Int) {
                Timber.d("syncProgress")
            }

            override fun error(errorMessage: String) {
                Timber.d("error: $errorMessage")
                result = SensorFirmwareResult(false, "", errorMessage)
            }
        })

        for (i in 1..CHECK_ITERATION_COUNT) {
            if (result == null) {
                delay(ITERATION_DELAY)
            } else {
                return result as SensorFirmwareResult
            }
        }
        return result ?: SensorFirmwareResult(false, "", "Failed to get sensor FW")
    }

    companion object {
        private const val CHECK_FW_TIMEOUT = 15000L
        private const val CHECK_ITERATION_COUNT = 30L
        private const val ITERATION_DELAY = CHECK_FW_TIMEOUT / CHECK_ITERATION_COUNT
    }
}