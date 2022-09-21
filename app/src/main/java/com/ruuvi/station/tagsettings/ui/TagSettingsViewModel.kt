package com.ruuvi.station.tagsettings.ui

import android.net.Uri
import androidx.lifecycle.*
import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlarmCheckInteractor
import com.ruuvi.station.bluetooth.domain.SensorFwVersionInteractor
import com.ruuvi.station.database.domain.AlarmRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.database.tables.isLowBattery
import com.ruuvi.station.network.data.response.SensorDataResponse
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.HumidityUnit
import com.ruuvi.station.util.ui.UiText
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
    private val sensorFwInteractor: SensorFwVersionInteractor,
    private val unitsConverter: UnitsConverter
) : ViewModel() {
    var file: Uri? = null

    private var networkStatus = MutableLiveData<SensorDataResponse?>(networkInteractor.getSensorNetworkStatus(sensorId))

    private val _sensorState = MutableStateFlow<RuuviTag>(requireNotNull(interactor.getFavouriteSensorById(sensorId)))
    val sensorState: StateFlow<RuuviTag> = _sensorState

    private val _tagState = MutableLiveData<RuuviTagEntity?>(getTagById(sensorId))
    val tagState: LiveData<RuuviTagEntity?> = _tagState

    private val sensorSettings = MutableLiveData<SensorSettings?>()
    val sensorSettingsObserve: LiveData<SensorSettings?> = sensorSettings

    private val _userLoggedIn = MutableStateFlow<Boolean> (networkInteractor.signedIn)
    val userLoggedIn: StateFlow<Boolean> = _userLoggedIn

    private val _sensorShared = MutableStateFlow<Boolean> (false)
    val sensorShared: StateFlow<Boolean> = _sensorShared

    private val operationStatus = MutableLiveData<String> ("")
    val operationStatusObserve: LiveData<String> = operationStatus

    val isLowBattery = Transformations.map(_tagState) {
        it?.isLowBattery() ?: false
    }

    val sensorOwnedByUserObserve: Flow<Boolean> = sensorState.mapNotNull {
        it.owner?.isNotEmpty() == true && it.owner == networkInteractor.getEmail()
    }

    val sensorOwnedOrOfflineObserve: Flow<Boolean> = sensorState.mapNotNull {
        !it.networkSensor || it.owner.isNullOrEmpty() || it.owner == networkInteractor.getEmail()
    }

    val firmware: MediatorLiveData<UiText?>  = MediatorLiveData<UiText?>()

    init {
        Timber.d("TagSettingsViewModel $sensorId")
        firmware.addSource(_tagState) { firmware.value = getFirmware() }
        firmware.addSource(sensorSettings) { firmware.value = getFirmware() }
    }

    private fun getFirmware(): UiText? {
        val firmware = sensorSettings.value?.firmware

        if (firmware.isNullOrEmpty() && _tagState.value?.dataFormat != 5) {
            return UiText.StringResource(R.string.firmware_very_old)
        }
        return firmware?.let { UiText.DynamicString(firmware) }
    }

    fun getTagInfo() {
        CoroutineScope(Dispatchers.IO).launch {
            val sensorState = interactor.getFavouriteSensorById(sensorId)
            if (sensorState != null) {
                _sensorState.value = sensorState
            }

            val tagInfo = getTagById(sensorId)
            val settings = interactor.getSensorSettings(sensorId)
            if (settings?.networkSensor != true && settings?.owner.isNullOrEmpty()) {
                interactor.checkSensorOwner(sensorId)
            }
            withContext(Dispatchers.Main) {
                _tagState.value = tagInfo
                sensorSettings.value = settings
            }
        }
    }

    fun updateSensorFirmwareVersion() {
        CoroutineScope(Dispatchers.IO).launch {
            val tagInfo = getTagById(sensorId)
            val settings = interactor.getSensorSettings(sensorId)
            if (settings?.firmware.isNullOrEmpty() && tagInfo?.connectable == true) {
                val fwResult = sensorFwInteractor.getSensorFirmwareVersion(sensorId)
                if (fwResult.isSuccess && fwResult.fw.isNotEmpty()) {
                    interactor.setSensorFirmware(sensorId, fwResult.fw)
                }
            }
        }
    }

    fun checkIfSensorShared() {
        getSensorSharedEmails()

    }

    private val handler = CoroutineExceptionHandler() { _, exception ->
        CoroutineScope(Dispatchers.Main).launch {
            operationStatus.value = exception.message
            Timber.d("CoroutineExceptionHandler: ${exception.message}")
        }
    }

    fun getSensorSharedEmails() {
        networkInteractor.getSharedInfo(sensorId, handler) { response ->
            Timber.d("getSensorSharedEmails ${response.toString()}")
            _sensorShared.value = response?.sharedTo?.isNotEmpty() == true
        }
    }

    fun getTagById(tagId: String): RuuviTagEntity? =
        interactor.getTagById(tagId)

    fun updateNetworkStatus() {
        networkStatus.value = networkInteractor.getSensorNetworkStatus(sensorId)
    }

    fun deleteTag(tag: RuuviTagEntity) {
        interactor.deleteTagsAndRelatives(tag)
    }

    fun removeNotificationById(notificationId: Int) {
        alarmCheckInteractor.removeNotificationById(notificationId)
    }

    fun updateTagBackground(userBackground: String?, defaultBackground: Int?) {
        interactor.updateTagBackground(sensorId, userBackground, defaultBackground)
        if (userBackground.isNullOrEmpty() == false) {
            networkInteractor.uploadImage(sensorId, userBackground)
        } else if (networkStatus.value?.picture.isNullOrEmpty() == false) {
            networkInteractor.resetImage(sensorId)
        }
    }

    fun statusProcessed() { operationStatus.value = "" }

    fun setName(name: String?) {
        interactor.updateTagName(sensorId, name)
        getTagInfo()
        networkInteractor.updateSensorName(sensorId)
    }

    fun getTemperatureOffsetString(value: Double) = unitsConverter.getTemperatureOffsetString(value)

    fun getHumidityOffsetString(value: Double) =
        unitsConverter.getHumidityString(value,0.0, HumidityUnit.PERCENT, Accuracy.Accuracy2)

    fun getPressureOffsetString(value: Double) =
        unitsConverter.getPressureString(value, Accuracy.Accuracy2)
}