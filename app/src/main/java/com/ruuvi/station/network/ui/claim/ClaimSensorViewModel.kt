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
import kotlinx.coroutines.flow.SharedFlow
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

    fun contestSensor(secret: String) {
        Timber.d("contestSensor")
        emitUiEvent(UiEvent.Navigate(ClaimRoutes.CLAIM_IN_PROGRESS, true))
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withContext(Dispatchers.IO) {
                    val settings = interactor.getSensorSettings(sensorId)
                    settings?.let {
                        sensorClaimInteractor.contestSensor(sensorId = sensorId, name = it.displayName, secret = secret) {
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
    private var forceClaimStarted = false

    fun getSensorId() {
        if (forceClaimStarted) {
            Timber.d("getSensorId already in progress")
            return
        }
        forceClaimStarted = true
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.Navigate(ClaimRoutes.FORCE_CLAIM_GETTING_ID, true))
        }

        nfcJob = viewModelScope.launch {
            NfcScanReciever.nfcSensorScanned.collect{ scanInfo ->
                Timber.d("nfc scanned: $scanInfo")
                if (scanInfo != null && scanInfo.mac != sensorId) {
                    viewModelScope.launch {
                        _uiEvent.emit(UiEvent.ShowSnackbar(UiText.StringResource(R.string.claim_wrong_sensor_scanned)))
                    }
                }
                if (scanInfo?.mac == sensorId && !scanInfo.id.isNullOrEmpty()) {
                    startContest(scanInfo.id)
                }
            }
        }
        bluetoothJob = viewModelScope.launch {
            var id: String? = null
            while (id == null) {
                if (!this.isActive) break
                val sensorInfo = sensorInfoInteractor.getSensorFirmwareVersion(sensorId)
                Timber.d("SensorInfo $sensorInfo")
                if (sensorInfo.id == null || sensorInfo.id == "XX:XX:XX:XX:XX:XX:XX:XX") {
                    delay(3000)
                } else {
                    id = sensorInfo.id
                    if (this.isActive) {
                        startContest(id)
                    }
                }
            }
        }
    }

    /**
     * Sends the force claim request once. Both the NFC and Bluetooth jobs are stopped first, so
     * touching the sensor again (or a late Bluetooth result) cannot send a second request.
     */
    private fun startContest(secret: String) {
        if (contestStarted) return
        contestStarted = true
        nfcJob?.cancel()
        bluetoothJob?.cancel()
        contestSensor(secret)
    }

    private fun emitUiEvent (event: UiEvent) {
        viewModelScope.launch {
            _uiEvent.emit(event)
        }
    }
}