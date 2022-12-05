package com.ruuvi.station.network.ui.claim

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.UiEvent
import com.ruuvi.station.app.ui.UiText
import com.ruuvi.station.bluetooth.domain.SensorInfoInteractor
import com.ruuvi.station.database.domain.AlarmRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.network.domain.SensorClaimInteractor
import com.ruuvi.station.nfc.NfcScanReciever
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

class ClaimSensorViewModel (
    val sensorId: String,
    private val ruuviNetworkInteractor: RuuviNetworkInteractor,
    private val interactor: TagSettingsInteractor,
    private val alarmRepository: AlarmRepository,
    private val sensorSettingsRepository: SensorSettingsRepository,
    private val sensorInfoInteractor: SensorInfoInteractor,
    private val sensorClaimInteractor: SensorClaimInteractor,
    ): ViewModel() {

    private val _uiEvent = MutableSharedFlow<UiEvent> ()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent

    private val _claimState = MutableStateFlow<ClaimSensorState> (
        ClaimSensorState.InProgress(R.string.claim_sensor, R.string.check_claim_state)
    )
    val claimState: StateFlow<ClaimSensorState> = _claimState

    private val claimResult = MutableLiveData<Pair<Boolean, String>?> (null)
    val claimResultObserve: LiveData<Pair<Boolean, String>?> = claimResult

    private val _claimInProgress = MutableLiveData<Boolean> (false)
    val claimInProgress: LiveData<Boolean> = _claimInProgress

    fun checkClaimState() {
        Timber.d("checkClaimState")
        _claimState.value = ClaimSensorState.InProgress(R.string.claim_sensor, R.string.check_claim_state)

        CoroutineScope(Dispatchers.IO).launch {
            ruuviNetworkInteractor.getSensorOwner(sensorId) {
                if (it?.isSuccess() == true) {
                    if (it.data?.email == "") {
                        _claimState.value = ClaimSensorState.FreeToClaim
                        emitUiEvent(UiEvent.Navigate(ClaimRoutes.FREE_TO_CLAIM, true))
                        return@getSensorOwner
                    }

                    if (it.data?.email == ruuviNetworkInteractor.getEmail()) {
                        _claimState.value = ClaimSensorState.ClaimFinished
                        emitUiEvent(UiEvent.NavigateUp)
                        return@getSensorOwner
                    }

                    _claimState.value = ClaimSensorState.ForceClaimInit
                    emitUiEvent(UiEvent.Navigate(ClaimRoutes.FORCE_CLAIM_INIT, true))
                    return@getSensorOwner
                } else {
                    _claimState.value = ClaimSensorState.ErrorWhileChecking(it?.error ?: "")
                    emitUiEvent(UiEvent.ShowSnackbar(UiText.DynamicString(it?.error ?: "Error")))
                    emitUiEvent(UiEvent.NavigateUp)
                }
            }
        }
    }

    fun claimSensor() {
        Timber.d("claimSensor")
        emitUiEvent(UiEvent.Navigate(ClaimRoutes.CLAIM_IN_PROGRESS, true))
        CoroutineScope(Dispatchers.IO).launch {
            _claimState.value = ClaimSensorState.InProgress(R.string.claim_sensor, R.string.claim_in_progress)
            try {
                withContext(Dispatchers.IO) {
                    val settings = interactor.getSensorSettings(sensorId)
                    settings?.let {
                        sensorClaimInteractor.claimSensor(settings) {
                            if (it?.isSuccess() == true) {
                                saveSensorCalibration()
                                saveAlarmsToNetwork()
                                saveUserBackground()
                                _claimState.value = ClaimSensorState.ClaimFinished
                                emitUiEvent(UiEvent.NavigateUp)
                            } else {
                                _claimState.value = ClaimSensorState.ErrorWhileChecking(it?.error ?: "")
                                emitUiEvent(UiEvent.ShowSnackbar(UiText.DynamicString(it?.error ?: "Error")))
                                emitUiEvent(UiEvent.NavigateUp)
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

    fun contestSensor(secret: String) {
        Timber.d("contestSensor")
        emitUiEvent(UiEvent.Navigate(ClaimRoutes.CLAIM_IN_PROGRESS, true))
        CoroutineScope(Dispatchers.IO).launch {
            _claimState.value = ClaimSensorState.InProgress(R.string.claim_sensor, R.string.claim_in_progress)
            try {
                withContext(Dispatchers.IO) {
                    val settings = interactor.getSensorSettings(sensorId)
                    settings?.let {
                        sensorClaimInteractor.contestSensor(sensorId = sensorId, name = it.displayName, secret = secret) {
                            if (it?.isSuccess() == true) {
                                saveSensorCalibration()
                                saveAlarmsToNetwork()
                                saveUserBackground()
                                _claimState.value = ClaimSensorState.ClaimFinished
                                emitUiEvent(UiEvent.NavigateUp)
                            } else {
                                _claimState.value = ClaimSensorState.ErrorWhileChecking(it?.error ?: "")
                                emitUiEvent(UiEvent.ShowSnackbar(UiText.DynamicString(it?.error ?: "Error")))
                                emitUiEvent(UiEvent.NavigateUp)
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
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.Navigate(ClaimRoutes.FORCE_CLAIM_GETTING_ID, true))
        }
        var bluetoothJob: Job? = null

        val nfcJob = viewModelScope.launch {
            sensorInfoInteractor.getSensorFirmwareVersion(sensorId)

            NfcScanReciever.nfcSensorScanned.collect{ scanInfo ->
                Timber.d("NFC VM $scanInfo")
                if (scanInfo != null && scanInfo.mac != sensorId) {
                    viewModelScope.launch {
                        _uiEvent.emit(UiEvent.ShowSnackbar(UiText.StringResource(R.string.claim_wrong_sensor_scanned)))
                    }
                }
                if (scanInfo?.mac == sensorId && !scanInfo.id.isNullOrEmpty()) {
                    bluetoothJob?.cancel()
                    contestSensor(scanInfo.id)
                }
            }
        }
        bluetoothJob = viewModelScope.launch {
            var id: String? = null
            while (id == null) {
                if (!this.isActive) break
                val sensorInfo = sensorInfoInteractor.getSensorFirmwareVersion(sensorId)
                Timber.d("SensorInfo $sensorInfo")
                if (sensorInfo.id == null) {
                    delay(3000)
                } else {
                    id = sensorInfo.id
                    if (this.isActive) {
                        contestSensor(id)
                        nfcJob.cancel()
                    }
                }
            }
        }
    }

    private fun emitUiEvent (event: UiEvent) {
        viewModelScope.launch {
            _uiEvent.emit(event)
        }
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