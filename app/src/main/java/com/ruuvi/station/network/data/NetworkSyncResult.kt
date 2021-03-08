package com.ruuvi.station.network.data

data class NetworkSyncResult (
    val type: NetworkSyncResultType,
    val errorMessage: String = ""
)

enum class NetworkSyncResultType {
    NONE,
    SUCCESS,
    EXCEPTION,
    NOT_LOGGED
}