package com.ruuvi.station.calibration.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ruuvi.station.calibration.domain.CalibrationInteractor
import com.ruuvi.station.database.tables.RuuviTagEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CalibrateTemperatureViewModel (
    val sensorId: String,
    val calibrationInteractor: CalibrationInteractor
) : ViewModel() {

    private val sensorData = MutableLiveData<RuuviTagEntity?>()
    val sensorDataObserve: LiveData<RuuviTagEntity?> = sensorData

    fun refreshSensorData() {
        CoroutineScope(Dispatchers.IO).launch {
            val data = calibrationInteractor.getSensorData(sensorId)
            withContext(Dispatchers.Main) {
                sensorData.value = data
            }
        }
    }

    fun getTemperatureString(temperature: Double?) = calibrationInteractor.getTemperatureString(temperature)

    fun getOriginalTemperature(temperature: Double) =
        calibrationInteractor.getTemperatureString(
            calibrationInteractor.getOriginalTemperature(sensorId, temperature)
        )

    fun getTemperatureOffset() = calibrationInteractor.getTemperatureOffset(sensorId)

    fun calibrateTo(targetValue: Double) {
        sensorData.value?.let {
            it.temperature?.let {temperature ->
                calibrationInteractor.calibrateTemperature(sensorId, it.temperature, targetValue)
            }
        }
    }

    fun getTemperatureUnit(): String = calibrationInteractor.getTemperatureUnit()

    fun clearTemperatureCalibration() {
        calibrationInteractor.clearTemperatureCalibration(sensorId)
    }
}