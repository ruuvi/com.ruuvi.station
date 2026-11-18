package com.ruuvi.station.network.ui.claim

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.UiEvent
import com.ruuvi.station.app.ui.UiText
import com.ruuvi.station.bluetooth.domain.SensorInfoInteractor
import com.ruuvi.station.database.tables.isAir
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
    private val sensorInfoInteractor: SensorInfoInteractor,
    private val sensorClaimInteractor: SensorClaimInteractor,
    ): ViewModel() {

    private val _uiEvent = MutableSharedFlow<UiEvent> (1)
    val uiEvent: SharedFlow<UiEvent> = _uiEvent

    val isAir = interactor.getTagById(sensorId)?.isAir()

    private var claimStateChecked = false

    /**
     * Picks the first claim page. Runs once per ViewModel: the Activity calls it from onCreate, and
     * a rotation must not send the user back from the force claim instructions to the start.
     */
    fun checkClaimState() {
        if (claimStateChecked) return
        claimStateChecked = true
        Timber.d("checkClaimState")
        if (!ruuviNetworkInteractor.signedIn) {
            emitUiEvent(UiEvent.Navigate(ClaimRoutes.NOT_SIGNED_IN, true))
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            ruuviNetworkInteractor.getSensorOwner(sensorId) {
                if (it?.isSuccess() == true) {
                    if (it.data?.email == "") {
                        emitUiEvent(UiEvent.Navigate(ClaimRoutes.FREE_TO_CLAIM, true))
                        return@getSensorOwner
                    }

                    if (it.data?.email.equals(ruuviNetworkInteractor.getEmail(), true)) {
                        emitUiEvent(UiEvent.Navigate(ClaimRoutes.UNCLAIM, true))
                        return@getSensorOwner
                    }

                    emitUiEvent(UiEvent.Navigate(ClaimRoutes.FORCE_CLAIM_INIT, true))
                    return@getSensorOwner
                } else {
                    emitUiEvent(UiEvent.Navigate(ClaimRoutes.FORCE_CLAIM_ERROR, true))
                }
            }
        }
    }

    fun claimSensor() {
        Timber.d("claimSensor")
        emitUiEvent(UiEvent.Navigate(ClaimRoutes.CLAIM_IN_PROGRESS, true))
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withContext(Dispatchers.IO) {
                    val settings = interactor.getSensorSettings(sensorId)
                    settings?.let {
                        sensorClaimInteractor.claimSensor(settings.id, settings.displayName) {
                            if (it?.isSuccess() == true) {
                                emitUiEvent(UiEvent.NavigateUp)
                            } else {
                                emitUiEvent(UiEvent.ShowSnackbar(UiText.DynamicString(it?.error ?: "Error")))
                                emitUiEvent(UiEvent.NavigateUp)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.d(e)
                emitUiEvent(UiEvent.ShowSnackbar(UiText.DynamicString(e.message ?: "Error")))
            }
        }
    }

    fun unclaimSensor(deleteData: Boolean) {
        Timber.d("unclaimSensor")
        emitUiEvent(UiEvent.Navigate(ClaimRoutes.CLAIM_IN_PROGRESS, true))
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withContext(Dispatchers.IO) {
                    val settings = interactor.getSensorSettings(sensorId)
                    settings?.let {
                        sensorClaimInteractor.unclaimSensor(settings.id, deleteData) {
                            if (it?.isSuccess() == true) {
                                emitUiEvent(UiEvent.NavigateUp)
                            } else {
                                emitUiEvent(UiEvent.ShowSnackbar(UiText.DynamicString(it?.error ?: "Error")))
                                emitUiEvent(UiEvent.NavigateUp)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.d(e)
                emitUiEvent(UiEvent.ShowSnackbar(UiText.DynamicString(e.message ?: "Error")))
            }
        }
    }

    private var nfcJob: Job? = null
    private var bluetoothJob: Job? = null
    private var contestStarted = false

    private val _bluetoothInProgress = MutableStateFlow(false)
    val bluetoothInProgress: StateFlow<Boolean> = _bluetoothInProgress

    private var nfcTimeoutJob: Job? = null
    private val _nfcInProgress = MutableStateFlow(false)
    /** True after "Use NFC" was tapped while waiting for the sensor to be touched. */
    val nfcInProgress: StateFlow<Boolean> = _nfcInProgress

    private fun contestSensor(secret: String) {
        Timber.d("contestSensor")
        emitUiEvent(UiEvent.Navigate(ClaimRoutes.CLAIM_IN_PROGRESS, true))
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = interactor.getSensorSettings(sensorId)
                if (settings == null) {
                    onContestFailed(null)
                    return@launch
                }
                sensorClaimInteractor.contestSensor(sensorId = sensorId, name = settings.displayName, secret = secret) {
                    if (it?.isSuccess() == true) {
                        emitUiEvent(UiEvent.NavigateUp)
                    } else {
                        onContestFailed(it?.error)
                    }
                }
            } catch (e: Exception) {
                Timber.d(e)
                onContestFailed(e.message)
            }
        }
    }

    private fun onContestFailed(error: String?) {
        viewModelScope.launch {
            contestStarted = false
            openForceClaimInstructions()
            _uiEvent.emit(UiEvent.ShowSnackbar(UiText.DynamicString(error?.takeIf { it.isNotBlank() } ?: "Error")))
        }
    }

    fun getSensorId() {
        viewModelScope.launch { openForceClaimInstructions() }
    }

    private suspend fun openForceClaimInstructions() {
        _uiEvent.emit(UiEvent.Navigate(ClaimRoutes.FORCE_CLAIM_GETTING_ID, true))
        if (nfcJob?.isActive == true) return

        nfcJob = viewModelScope.launch {
            NfcScanReciever.nfcSensorScanned.collect { scanInfo ->
                Timber.d("nfc scanned: $scanInfo")
                if (scanInfo != null && scanInfo.mac != sensorId) {
                    stopWaitingForNfc()
                    _uiEvent.emit(UiEvent.ShowSnackbar(UiText.StringResource(R.string.claim_wrong_sensor_scanned)))
                }
                if (scanInfo?.mac == sensorId && !scanInfo.id.isNullOrEmpty()) {
                    startContest(scanInfo.id)
                }
            }
        }
    }

    /**
     * Reads the sensor ID over Bluetooth. A RuuviTag only exposes its ID for a while after its
     * button is pressed (Ruuvi Air reports a masked ID), so reading is retried until the ID shows
     * up or [BLUETOOTH_TIMEOUT] passes (same timeout as iOS); then the user is asked to try again.
     */
    fun useBluetooth() {
        if (contestStarted || _nfcInProgress.value || bluetoothJob?.isActive == true) return

        _bluetoothInProgress.value = true
        bluetoothJob = viewModelScope.launch {
            try {
                val id = withTimeoutOrNull(BLUETOOTH_TIMEOUT) {
                    var id: String? = null
                    while (id == null) {
                        val sensorInfo = sensorInfoInteractor.getSensorFirmwareVersion(sensorId)
                        Timber.d("SensorInfo $sensorInfo")
                        id = sensorInfo.id?.takeIf { it != UNKNOWN_SENSOR_ID }
                        if (id == null) delay(BLUETOOTH_RETRY_DELAY)
                    }
                    id
                }
                if (id != null) {
                    startContest(id)
                } else if (!contestStarted) {
                    _uiEvent.emit(UiEvent.ShowSnackbar(UiText.StringResource(R.string.sensor_not_found_error)))
                }
            } finally {
                _bluetoothInProgress.value = false
            }
        }
    }

    fun useNfc() {
        if (contestStarted || _nfcInProgress.value || _bluetoothInProgress.value) return

        _nfcInProgress.value = true
        nfcTimeoutJob = viewModelScope.launch {
            delay(NFC_TIMEOUT)
            _nfcInProgress.value = false
            _uiEvent.emit(UiEvent.ShowSnackbar(UiText.StringResource(R.string.sensor_not_found_error)))
        }
    }

    private fun stopWaitingForNfc() {
        nfcTimeoutJob?.cancel()
        _nfcInProgress.value = false
    }

    private fun startContest(secret: String) {
        if (contestStarted) return
        contestStarted = true
        stopWaitingForNfc()
        bluetoothJob?.cancel()
        nfcJob?.cancel()
        contestSensor(secret)
    }

    private fun emitUiEvent (event: UiEvent) {
        viewModelScope.launch {
            _uiEvent.emit(event)
        }
    }

    companion object {
        private const val NFC_TIMEOUT = 60_000L
        /** Matches the iOS GATT timeout for reading the sensor ID. */
        private const val BLUETOOTH_TIMEOUT = 15_000L
        private const val BLUETOOTH_RETRY_DELAY = 1_000L
        private const val UNKNOWN_SENSOR_ID = "XX:XX:XX:XX:XX:XX:XX:XX"
    }
}
