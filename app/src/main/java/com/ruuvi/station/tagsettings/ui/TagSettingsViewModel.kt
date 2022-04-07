package com.ruuvi.station.tagsettings.ui

import android.net.Uri
import androidx.lifecycle.*
import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlarmCheckInteractor
import com.ruuvi.station.alarm.domain.AlarmElement
import com.ruuvi.station.bluetooth.domain.SensorFwVersionInteractor
import com.ruuvi.station.database.domain.AlarmRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.network.data.response.SensorDataResponse
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor
import com.ruuvi.station.util.ui.UiText
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*

class TagSettingsViewModel(
    val sensorId: String,
    private val interactor: TagSettingsInteractor,
    private val alarmCheckInteractor: AlarmCheckInteractor,
    private val networkInteractor: RuuviNetworkInteractor,
    private val alarmRepository: AlarmRepository,
    private val sensorFwInteractor: SensorFwVersionInteractor
) : ViewModel() {
    var alarmElements: MutableList<AlarmElement> = ArrayList()
    var file: Uri? = null

    private var networkStatus = MutableLiveData<SensorDataResponse?>(networkInteractor.getSensorNetworkStatus(sensorId))

    private val tagState = MutableLiveData<RuuviTagEntity?>(getTagById(sensorId))
    val tagObserve: LiveData<RuuviTagEntity?> = tagState

    private val sensorSettings = MutableLiveData<SensorSettings?>()
    val sensorSettingsObserve: LiveData<SensorSettings?> = sensorSettings

    private val userLoggedIn = MutableLiveData<Boolean> (networkInteractor.signedIn)
    val userLoggedInObserve: LiveData<Boolean> = userLoggedIn

    private val sensorShared = MutableLiveData<Boolean> (false)
    val sensorSharedObserve: LiveData<Boolean> = sensorShared

    private val operationStatus = MutableLiveData<String> ("")
    val operationStatusObserve: LiveData<String> = operationStatus

    val sensorOwnedByUserObserve: LiveData<Boolean> = Transformations.map(sensorSettings) {
        it?.owner?.isNotEmpty() == true && it.owner == networkInteractor.getEmail()
    }

    val sensorOwnedOrOfflineObserve: LiveData<Boolean> = Transformations.map(sensorSettings) {
        it?.networkSensor == false || it?.owner.isNullOrEmpty() || it?.owner == networkInteractor.getEmail()
    }

    val firmware: MediatorLiveData<UiText?>  = MediatorLiveData<UiText?>()

    init {
        Timber.d("TagSettingsViewModel $sensorId")
        firmware.addSource(tagState) { firmware.value = getFirmware() }
        firmware.addSource(sensorSettings) { firmware.value = getFirmware() }
    }

    private fun getFirmware(): UiText? {
        val firmware = sensorSettings.value?.firmware

        if (firmware.isNullOrEmpty() && tagState.value?.dataFormat != 5) {
            return UiText.StringResource(R.string.firmware_very_old)
        }
        return firmware?.let { UiText.DynamicString(firmware) }
    }

    fun getTagInfo() {
        CoroutineScope(Dispatchers.IO).launch {
            val tagInfo = getTagById(sensorId)
            val settings = interactor.getSensorSettings(sensorId)
            withContext(Dispatchers.Main) {
                tagState.value = tagInfo
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
            sensorShared.value = response?.sharedTo?.isNotEmpty() == true
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

    fun setupAlarmElements() {
        alarmElements.clear()

        with(alarmElements) {
            add(AlarmElement.getTemperatureAlarmElement(sensorId))
            add(AlarmElement.getHumidityAlarmElement(sensorId))
            add(AlarmElement.getPressureAlarmElement(sensorId))
            add(AlarmElement.getRssiAlarmElement(sensorId))
            add(AlarmElement.getMovementAlarmElement(sensorId))
        }

        val dbAlarms = alarmRepository.getForSensor(sensorId)
        for (alarm in dbAlarms) {
            val item = alarmElements.firstOrNull { it.type.value == alarm.type }
            item?.let {
                item.high = alarm.high
                item.low = alarm.low
                item.isEnabled = alarm.enabled
                item.customDescription = alarm.customDescription
                item.mutedTill = alarm.mutedTill
                item.alarm = alarm
                item.normalizeValues()
            }
        }
    }
}