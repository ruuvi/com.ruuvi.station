package com.ruuvi.station.bluetooth.model

data class SensorFirmwareResult(
    val isSuccess: Boolean,
    val fw: String,
    val error: String,
    val id: String? = null
)