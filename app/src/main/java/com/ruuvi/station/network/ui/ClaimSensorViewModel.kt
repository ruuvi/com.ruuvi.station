package com.ruuvi.station.network.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ruuvi.station.R
import com.ruuvi.station.database.domain.AlarmRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    private val _claimState = MutableStateFlow<ClaimSensorState> (
        ClaimSensorState.InProgress(R.string.claim_sensor, R.string.check_claim_state)
    )
    val claimState: StateFlow<ClaimSensorState> = _claimState

    private val claimResult = MutableLiveData<Pair<Boolean, String>?> (null)
    val claimResultObserve: LiveData<Pair<Boolean, String>?> = claimResult

    private val _claimInProgress = MutableLiveData<Boolean> (false)
    val claimInProgress: LiveData<Boolean> = _claimInProgress

    fun checkClaimState() {
        _claimState.value = ClaimSensorState.InProgress(R.string.claim_sensor, R.string.check_claim_state)

        CoroutineScope(Dispatchers.IO).launch {
            ruuviNetworkInteractor.getSensorOwner(sensorId) {
                if (it?.isSuccess() == true) {
                    if (it.data?.email == "") {
                        _claimState.value = ClaimSensorState.FreeToClaim
                        return@getSensorOwner
                    }

                    if (it.data?.email == ruuviNetworkInteractor.getEmail()) {
                        _claimState.value = ClaimSensorState.ClaimFinished
                        return@getSensorOwner
                    }

                    _claimState.value = ClaimSensorState.ForceClaimInit
                    return@getSensorOwner
                } else {
                    _claimState.value = ClaimSensorState.ErrorWhileChecking(it?.error ?: "")
                }
            }
        }
    }

    fun claimSensor() {
        CoroutineScope(Dispatchers.Main).launch {
            _claimState.value = ClaimSensorState.InProgress(R.string.claim_sensor, R.string.claim_in_progress)
            try {
                withContext(Dispatchers.IO) {
                    val settings = interactor.getSensorSettings(sensorId)
                    settings?.let {
                        ruuviNetworkInteractor.claimSensor(settings) {
                            if (it?.isSuccess() == true) {
                                saveSensorCalibration()
                                saveAlarmsToNetwork()
                                saveUserBackground()
                                _claimState.value = ClaimSensorState.ClaimFinished
                            } else {
                                _claimState.value = ClaimSensorState.ErrorWhileChecking(it?.error ?: "")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.d(e)
                _claimState.value = ClaimSensorState.ErrorWhileChecking(e.message.toString())
            }
        }
    }

    init {
        checkClaimState()
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

    fun getSensorId() {
        _claimState.value = ClaimSensorState.ForceClaimGettingId
    }
}

sealed class ClaimSensorState {
    class InProgress(val title: Int, val status: Int): ClaimSensorState()
    object FreeToClaim: ClaimSensorState()
    object ForceClaimInit: ClaimSensorState()
    object ForceClaimGettingId: ClaimSensorState()
    class ErrorWhileChecking(val error: String): ClaimSensorState()
    object ClaimFinished: ClaimSensorState()
}