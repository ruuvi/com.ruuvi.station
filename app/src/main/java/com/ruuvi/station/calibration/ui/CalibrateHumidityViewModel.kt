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

class CalibrateHumidityViewModel (
    val sensorId: String,
    val calibrationInteractor: CalibrationInteractor
) : ViewModel(), ICalibrationViewModel {

    private val calibrationInfo = MutableLiveData<CalibrationInfo>()
    override val calibrationInfoObserve: LiveData<CalibrationInfo> = calibrationInfo

    override fun getUnit(): String = calibrationInteractor.getHumidityUnit()

    override fun calibrateTo(targetValue: Double) = calibrationInteractor.calibrateHumidity(sensorId, targetValue)

    override fun clearCalibration() = calibrationInteractor.clearHumidityCalibration(sensorId)

    override fun getStringForValue(value: Double): String = calibrationInteractor.getHumidityString(value)

    override fun refreshSensorData() {
        CoroutineScope(Dispatchers.IO).launch {
            val data = calibrationInteractor.getHumidityCalibrationInfo(sensorId)
            withContext(Dispatchers.Main) {
                calibrationInfo.value = data
            }
        }
    }
}