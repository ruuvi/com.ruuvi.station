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
) : ViewModel(), ICalibrationViewModel {
    private val calibrationInfo = MutableLiveData<CalibrationInfo>()
    override val calibrationInfoObserve: LiveData<CalibrationInfo> = calibrationInfo

    override fun refreshSensorData() {
        CoroutineScope(Dispatchers.IO).launch {
            val data = calibrationInteractor.getTemperatureCalibrationInfo(sensorId)
            withContext(Dispatchers.Main) {
                calibrationInfo.value = data
            }
        }
    }

    override fun getStringForValue(value: Double): String  = calibrationInteractor.getTemperatureString(value)

    override fun calibrateTo(targetValue: Double) {
        calibrationInteractor.calibrateTemperature(sensorId, targetValue)
    }

    override fun getUnit(): String = calibrationInteractor.getTemperatureUnit()

    override fun clearCalibration() = calibrationInteractor.clearTemperatureCalibration(sensorId)
}