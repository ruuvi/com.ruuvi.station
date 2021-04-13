package com.ruuvi.station.calibration.ui

import androidx.lifecycle.LiveData
import com.ruuvi.station.calibration.model.CalibrationInfo

interface ICalibrationViewModel {
    val calibrationInfoObserve: LiveData<CalibrationInfo>
    fun getUnit(): String
    fun calibrateTo(value: Double)
    fun clearCalibration()
    fun getStringForValue(value: Double): String
    fun refreshSensorData()
}