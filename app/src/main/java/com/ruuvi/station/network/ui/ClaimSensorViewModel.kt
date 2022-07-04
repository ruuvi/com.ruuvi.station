package com.ruuvi.station.network.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ruuvi.station.database.domain.AlarmRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class ClaimSensorViewModel (
    val sensorId: String,
    private val ruuviNetworkInteractor: RuuviNetworkInteractor,
    private val interactor: TagSettingsInteractor,
    private val alarmRepository: AlarmRepository,
    private val sensorSettingsRepository: SensorSettingsRepository
    ): ViewModel() {

    private val claimResult = MutableLiveData<Pair<Boolean, String>?> (null)
    val claimResultObserve: LiveData<Pair<Boolean, String>?> = claimResult

    private val _claimInProgress = MutableLiveData<Boolean> (false)
    val claimInProgress: LiveData<Boolean> = _claimInProgress

    fun claimSensor() {
        CoroutineScope(Dispatchers.Main).launch {
            _claimInProgress.value = true
            try {
                withContext(Dispatchers.IO) {
                    val settings = interactor.getSensorSettings(sensorId)
                    settings?.let {
                        ruuviNetworkInteractor.claimSensor(settings) {
                            if (it?.isSuccess() == true) {
                                saveSensorCalibration()
                                saveAlarmsToNetwork()
                                saveUserBackground()
                            }
                            claimResult.value = Pair(it?.isSuccess() ?: false, it?.error ?: "")
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.d(e)
            } finally {
                _claimInProgress.value = false
            }
        }
    }

    private fun saveAlarmsToNetwork() {
        val alarms = alarmRepository.getForSensor(sensorId)
        for (alarm in alarms) {
            ruuviNetworkInteractor.setAlert(alarm)
        }
    }

    private fun saveUserBackground() {
        val sensorSettings = sensorSettingsRepository.getSensorSettings(sensorId)
        val userBackground = sensorSettings?.userBackground
        if (userBackground?.isNotEmpty() == true) {
            ruuviNetworkInteractor.uploadImage(sensorId, userBackground)
        }
    }

    private fun saveSensorCalibration() {
        ruuviNetworkInteractor.updateSensorCalibration(sensorId)
    }
}