package com.ruuvi.station.calibration.model

import java.util.*

data class CalibrationInfo(
    val sensorId: String,
    val updateAt: Date?,
    val rawValue: Double,
    val calibratedValue: Double,
    val isCalibrated: Boolean,
    val currentOffset: Double? = null,
    val currentOffsetString: String,
    val lastOffset: Double? = null,
)