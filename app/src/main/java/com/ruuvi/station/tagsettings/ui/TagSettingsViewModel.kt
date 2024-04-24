package com.ruuvi.station.tagsettings.ui

import android.net.Uri
import androidx.compose.material.SnackbarDuration
import androidx.lifecycle.*
import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlarmCheckInteractor
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.app.ui.UiEvent
import com.ruuvi.station.app.ui.UiText
import com.ruuvi.station.bluetooth.domain.SensorInfoInteractor
import com.ruuvi.station.database.domain.AlarmRepository
import com.ruuvi.station.database.domain.SensorShareListRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.network.domain.OperationStatus
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.isLowBattery
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor
import com.ruuvi.station.units.domain.AccelerationConverter
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.HumidityUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber

class TagSettingsViewModel(
    val sensorId: String,
    var newSensor: Boolean,
    private val interactor: TagSettingsInteractor,
    private val alarmCheckInteractor: AlarmCheckInteractor,
    private val networkInteractor: RuuviNetworkInteractor,
    private val alarmRepository: AlarmRepository,
    private val sensorFwInteractor: SensorInfoInteractor,
    private val unitsConverter: UnitsConverter,
    private val accelerationConverter: AccelerationConverter,
    private val shareListRepository: SensorShareListRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    var file: Uri? = null

    private val _uiEvent = MutableSharedFlow<UiEvent> (1)
    val uiEvent: SharedFlow<UiEvent> = _uiEvent

    private val _sensorState = MutableStateFlow<RuuviTag>(requireNotNull(interactor.getFavouriteSensorById(sensorId)))
    val sensorState: StateFlow<RuuviTag> = _sensorState

    private val _userLoggedIn = MutableStateFlow<Boolean> (networkInteractor.signedIn)
    val userLoggedIn: StateFlow<Boolean> = _userLoggedIn

    private val _sensorShared = MutableStateFlow<UiText> (UiText.EmptyString)
    val sensorShared: StateFlow<UiText> = _sensorShared

    private val operationStatus = MutableLiveData<String> ("")
    val operationStatusObserve: LiveData<String> = operationStatus

    val isLowBattery: Flow<Boolean> = sensorState.mapNotNull {
        Timber.d("isLowBattery")
        it.isLowBattery()
    }

    val sensorOwnedByUser: Flow<Boolean> = sensorState.mapNotNull {
        it.owner?.isNotEmpty() == true && it.owner == networkInteractor.getEmail()
    }

    val sensorOwnedOrOffline: Flow<Boolean> = sensorState.mapNotNull {
        !it.networkSensor || it.owner.isNullOrEmpty() || it.owner == networkInteractor.getEmail()
    }

    private val _askToClaim =  MutableSharedFlow<Boolean>()
    val askToClaim: SharedFlow<Boolean> = _askToClaim

    val firmware: Flow<UiText?>  = sensorState.mapNotNull {
        getFirmware(it.firmware)
    }

    init {
        if (!_sensorState.value.networkSensor && sensorState.value.owner.isNullOrEmpty()) {
            Timber.d("checkSensorOwner")
            viewModelScope.launch {
                networkInteractor.getSensorOwner(sensorId) {
                    if (newSensor && it?.isSuccess() == true && networkInteractor.signedIn && it.data?.email.isNullOrEmpty()) {
                        viewModelScope.launch {
                            _askToClaim.emit(true)
                        }
                    }
                }
            }
        }
    }

    private fun getFirmware(firmware: String?): UiText? {
        Timber.d("getFirmware")
        if (firmware.isNullOrEmpty() &&
            sensorState.value.latestMeasurement != null &&
            sensorState.value.latestMeasurement?.dataFormat != 5
        ) {
            return UiText.StringResource(R.string.firmware_very_old)
        }
        return firmware?.let { UiText.DynamicString(firmware) }
    }

    fun getTagInfo() {
        Timber.d("getTagInfo")
        CoroutineScope(Dispatchers.IO).launch {
            val sensorState = interactor.getFavouriteSensorById(sensorId)
            if (sensorState != null) {
                _sensorState.value = sensorState
            }
        }
    }

    fun updateSensorFirmwareVersion() {
        Timber.d("updateSensorFirmwareVersion")
        CoroutineScope(Dispatchers.IO).launch {
            val storredFw = sensorState.value.firmware
            if (storredFw.isNullOrEmpty() && sensorState.value.latestMeasurement?.connectable == true) {
                val fwResult = sensorFwInteractor.getSensorFirmwareVersion(sensorId)
                if (fwResult.isSuccess && fwResult.fw.isNotEmpty()) {
                    interactor.setSensorFirmware(sensorId, fwResult.fw)
                }
            }
        }
    }

    fun checkIfSensorShared() {
        Timber.d("checkIfSensorShared")
        try {
            getSensorSharedEmails()
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    fun getSensorSharedEmails() {
        val shareCount = shareListRepository.getShareListForSensor(sensorId).size
        val shareText = if (shareCount > 0) {
            UiText.StringResourceWithArgs(R.string.shared_to_x,
                arrayOf(shareCount, preferencesRepository.getSubscriptionMaxSharesPerSensor()) )
        } else {
            UiText.StringResource(R.string.sensor_not_shared)
        }
        _sensorShared.value = shareText
        Timber.d("getSensorSharedEmails $shareCount")
    }

    fun getTagById(tagId: String): RuuviTagEntity? =
        interactor.getTagById(tagId)

    fun removeNotificationById(notificationId: Int) {
        alarmCheckInteractor.removeNotificationById(notificationId)
    }

    fun statusProcessed() { operationStatus.value = "" }

    fun setName(name: String?) {
        Timber.d("setName")
        interactor.updateTagName(sensorId, name)
        getTagInfo()
        val status = networkInteractor.updateSensorNameWithStatus(sensorId)
        viewModelScope.launch {
            status?.collect{ status ->
                Timber.d("setName status $status")
                when (status) {
                    OperationStatus.Success -> {
                        (Dispatchers.Main) {
                            _uiEvent.emit(UiEvent.ShowSnackbar(UiText.StringResource(R.string.saving_success), SnackbarDuration.Short))
                        }
                    }
                    is OperationStatus.Fail -> {
                        (Dispatchers.Main) {
                            _uiEvent.emit(UiEvent.ShowSnackbar(UiText.StringResource(R.string.saving_fail), SnackbarDuration.Short))
                        }
                    }
                    OperationStatus.InProgress -> {
                        (Dispatchers.Main) {
                            _uiEvent.emit(UiEvent.ShowSnackbar(UiText.StringResource(R.string.saving_to_cloud), SnackbarDuration.Indefinite))
                        }
                    }
                    OperationStatus.Skipped -> {}
                }
            }
        }
    }

    fun getTemperatureOffsetString(value: Double) = unitsConverter.getTemperatureOffsetString(value)

    fun getHumidityOffsetString(value: Double) =
        unitsConverter.getHumidityString(value,0.0, HumidityUnit.PERCENT, Accuracy.Accuracy2)

    fun getPressureOffsetString(value: Double) =
        unitsConverter.getPressureString(value, Accuracy.Accuracy2)

    fun getAccelerationString(value: Double?) =
        accelerationConverter.getAccelerationString(value, null)

    fun getSignalString(rssi: Int) = unitsConverter.getSignalString(rssi)
}