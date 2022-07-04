package com.ruuvi.station.calibration.ui

import androidx.lifecycle.ViewModel
import com.ruuvi.station.calibration.domain.CalibrationInteractor
import com.ruuvi.station.calibration.model.CalibrationInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class CalibrateTemperatureViewModel (
    val sensorId: String,
    val calibrationInteractor: CalibrationInteractor
) : ViewModel(), ICalibrationViewModel {
    override val calibrationInfoFlow: Flow<CalibrationInfo>
        get() = flow {
            while (true) {
                val data = calibrationInteractor.getTemperatureCalibrationInfo(sensorId)
                if (data != null) emit(data)
                delay(500)
            }
        }.flowOn(Dispatchers.IO)

    override fun getStringForValue(value: Double): String  = calibrationInteractor.getTemperatureString(value)

    override fun calibrateTo(targetValue: Double) {
        calibrationInteractor.calibrateTemperature(sensorId, targetValue)
    }

    override fun getUnit(): String = calibrationInteractor.getTemperatureUnit()

    override fun clearCalibration() = calibrationInteractor.clearTemperatureCalibration(sensorId)
}