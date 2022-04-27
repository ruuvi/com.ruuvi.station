package com.ruuvi.station.calibration.ui

import com.ruuvi.station.calibration.model.CalibrationInfo
import kotlinx.coroutines.flow.Flow

interface ICalibrationViewModel {
    val calibrationInfoFlow: Flow<CalibrationInfo>
    fun getUnit(): String
    fun calibrateTo(targetValue: Double)
    fun clearCalibration()
    fun getStringForValue(value: Double): String
}