package com.ruuvi.station.bluetooth.model

data class GattSyncStatus (
    val sensorId: String,
    val syncProgress: SyncProgress = SyncProgress.STILL,
    val deviceInfoModel: String = "",
    val deviceInfoFw: String = "",
    val readDataSize: Int = 0,
    var syncedDataPoints: Int = 0
)

enum class SyncProgress {
    STILL, CONNECTING, CONNECTED, DISCONNECTED, READING_INFO, READING_DATA, SAVING_DATA, NOT_SUPPORTED, NOT_FOUND, ERROR, DONE
}