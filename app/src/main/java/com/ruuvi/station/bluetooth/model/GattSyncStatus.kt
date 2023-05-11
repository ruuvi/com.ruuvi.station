package com.ruuvi.station.bluetooth.model

data class GattSyncStatus (
    val sensorId: String,
    val syncProgress: SyncProgress = SyncProgress.STILL,
    val deviceInfoModel: String = "",
    val deviceInfoFw: String = "",
    val readDataSize: Int = 0,
    var syncedDataPoints: Int = 0
)

enum class SyncProgress (val syncInProgress: Boolean) {
    STILL(false),
    CONNECTING(true),
    CONNECTED(true),
    DISCONNECTED(false),
    READING_INFO(true),
    READING_DATA(true),
    SAVING_DATA(true),
    NOT_SUPPORTED(false),
    NOT_FOUND(false),
    ERROR(false),
    DONE(false)
}