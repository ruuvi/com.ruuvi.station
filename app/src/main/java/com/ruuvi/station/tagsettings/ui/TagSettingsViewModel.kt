package com.ruuvi.station.tagsettings.ui

import android.net.Uri
import androidx.lifecycle.*
import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlarmCheckInteractor
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.app.ui.UiText
import com.ruuvi.station.bluetooth.domain.SensorInfoInteractor
import com.ruuvi.station.database.domain.AlarmRepository
import com.ruuvi.station.database.domain.SensorShareListRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.isLowBattery
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor
import com.ruuvi.station.units.domain.AccelerationConverter
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.HumidityUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull
import timber.log.Timber

class TagSettingsViewModel(
    val sensorId: String,
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

    val firmware: Flow<UiText?>  = sensorState.mapNotNull {
        getFirmware(it.firmware)
    }

    init {
        if (!_sensorState.value.networkSensor && sensorState.value.owner.isNullOrEmpty()) {
            Timber.d("checkSensorOwner")
            viewModelScope.launch {
                networkInteractor.getSensorOwner(sensorId) {}
            }
        }
    }

    private fun getFirmware(firmware: String?): UiText? {
        Timber.d("getFirmware")
        if (firmware.isNullOrEmpty() && sensorState.value.dataFormat != 5) {
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
            if (storredFw.isNullOrEmpty() && sensorState.value.connectable == true) {
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

    private val handler = CoroutineExceptionHandler() { _, exception ->
        Timber.d("CoroutineExceptionHandler: ${exception.message}")

//        CoroutineScope(Dispatchers.Main).launch {
//            operationStatus.value = exception.message
//            Timber.d("CoroutineExceptionHandler: ${exception.message}")
//        }
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

    fun deleteSensor() {
        Timber.d("deleteSensor")
        interactor.deleteTagsAndRelatives(sensorId)
    }

    fun removeNotificationById(notificationId: Int) {
        alarmCheckInteractor.removeNotificationById(notificationId)
    }

    fun statusProcessed() { operationStatus.value = "" }

    fun setName(name: String?) {
        Timber.d("setName")
        interactor.updateTagName(sensorId, name)
        getTagInfo()
        networkInteractor.updateSensorName(sensorId)
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