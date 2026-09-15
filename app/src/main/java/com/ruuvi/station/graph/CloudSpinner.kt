package com.ruuvi.station.graph

import com.ruuvi.station.network.domain.HistorySyncState

fun cloudSpinnerVisible(accountRefreshing: Boolean, activeHistorySensor: String?, history: HistorySyncState): Boolean =
    accountRefreshing || (activeHistorySensor != null && history.sensorId == activeHistorySensor && history.loading)
