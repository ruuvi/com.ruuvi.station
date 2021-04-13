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

class CalibratePressureViewModel (
    val sensorId: String,
    val calibrationInteractor: CalibrationInteractor
) : ViewModel(),ICalibrationViewModel {

    private val calibrationInfo = MutableLiveData<CalibrationInfo>()
    override val calibrationInfoObserve: LiveData<CalibrationInfo> = calibrationInfo


    override fun getUnit(): String = calibrationInteractor.getPressureUnit()

    override fun calibrateTo(targetValue: Double) {
        calibrationInteractor.calibratePressure(sensorId, targetValue)
    }

    override fun clearCalibration() {
        calibrationInteractor.clearPressureCalibration(sensorId)
    }

    override fun getStringForValue(value: Double): String = calibrationInteractor.getPressureString(value)

    override fun refreshSensorData() {
        CoroutineScope(Dispatchers.IO).launch {
            val data = calibrationInteractor.getPressureCalibrationInfo(sensorId)
            withContext(Dispatchers.Main) {
                calibrationInfo.value = data
            }
        }
    }
}