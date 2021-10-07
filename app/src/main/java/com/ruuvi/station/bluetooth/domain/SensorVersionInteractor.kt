package com.ruuvi.station.bluetooth.domain

import com.ruuvi.station.bluetooth.BluetoothInteractor
import com.ruuvi.station.bluetooth.IRuuviGattListener
import com.ruuvi.station.bluetooth.LogReading
import kotlinx.coroutines.delay
import timber.log.Timber

class SensorVersionInteractor (
    private val interactor: BluetoothInteractor
) {
    suspend fun getSensorFirmwareVersion(sensorId: String): GetFwResult {
        var result: GetFwResult? = null
        interactor.getFwVersion(sensorId, object : IRuuviGattListener {
            override fun connected(state: Boolean) {
                Timber.d("Connected state = $state")
            }

            override fun deviceInfo(model: String, fw: String, canReadLogs: Boolean) {
                Timber.d("deviceInfo model = $model fw = $fw")
                result = GetFwResult(true, fw, "")
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
                result = GetFwResult(false, "", errorMessage)
            }
        })
        delay(15000)
        return result ?: GetFwResult(false, "", "Failed to get sensor FW")
    }
}

data class GetFwResult(
    val isSuccess: Boolean,
    val fw: String,
    val error: String
)