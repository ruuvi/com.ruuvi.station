package com.ruuvi.station.calibration.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ruuvi.station.calibration.domain.CalibrationInteractor
import com.ruuvi.station.calibration.model.CalibrationInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CalibrateTemperatureViewModel (
    val sensorId: String,
    val calibrationInteractor: CalibrationInteractor
) : ViewModel() {
    private val calibrationInfo = MutableLiveData<CalibrationInfo>()
    val calibrationInfoObserve: LiveData<CalibrationInfo> = calibrationInfo

    fun refreshSensorData() {
        CoroutineScope(Dispatchers.IO).launch {
            val data = calibrationInteractor.getCalibrationInfo(sensorId)
            withContext(Dispatchers.Main) {
                calibrationInfo.value = data
            }
        }
    }

    fun getTemperatureString(temperature: Double?) = calibrationInteractor.getTemperatureString(temperature)

    fun calibrateTo(targetValue: Double) {
        calibrationInteractor.calibrateTemperature(sensorId, targetValue)
    }

    fun getTemperatureUnit(): String = calibrationInteractor.getTemperatureUnit()

    fun clearTemperatureCalibration() {
        calibrationInteractor.clearTemperatureCalibration(sensorId)
    }
}