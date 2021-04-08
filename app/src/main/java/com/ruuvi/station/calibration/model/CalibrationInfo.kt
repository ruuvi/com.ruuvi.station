package com.ruuvi.station.calibration.model

import java.util.*

data class CalibrationInfo(
    val sensorId: String,
    val updateAt: Date?,
    val temperatureRaw: Double,
    val temperatureCalibrated: Double,
    val isTemperatureCalibrated: Boolean,
    val currentTemperatureOffset: Double? = null,
    val currentTemperatureOffsetString: String,
    val lastTemperatureOffset: Double? = null,
)