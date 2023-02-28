package com.ruuvi.station.network.data

sealed class NetworkSyncEvent {
    object Idle: NetworkSyncEvent()
    object InProgress: NetworkSyncEvent()
    class Error(val message: String): NetworkSyncEvent()
    object Success : NetworkSyncEvent()
    object Unauthorised: NetworkSyncEvent()
}