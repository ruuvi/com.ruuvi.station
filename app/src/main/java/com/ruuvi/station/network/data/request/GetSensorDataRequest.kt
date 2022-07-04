package com.ruuvi.station.network.data.request

import java.util.*

data class GetSensorDataRequest(
    val sensor: String,
    val since: Date? = null,
    val until: Date? = null,
    val sort: SortMode? = null,
    val limit: Int? = null,
    val mode: SensorDataMode? = SensorDataMode.MIXED
)

enum class SensorDataMode(val code: String) {
    DENSE("dense"),
    SPARSE("sparse"),
    MIXED("mixed")
}

enum class SortMode(val code: String) {
    DESCENDING("desc"),
    ASCENDING ("asc")
}