package com.ruuvi.station.network.data

data class NetworkSyncStatus (
    val syncInProgress: Boolean,
    val lastSync: Long
)