package com.ruuvi.station.tagsettings.ui

import android.net.Uri
import androidx.lifecycle.*
import com.ruuvi.station.alarm.domain.AlarmCheckInteractor
import com.ruuvi.station.alarm.domain.AlarmElement
import com.ruuvi.station.database.domain.AlarmRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.network.data.response.SensorDataResponse
import com.ruuvi.station.network.domain.NetworkDataSyncInteractor
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*

class TagSettingsViewModel(
    val sensorId: String,
    private val interactor: TagSettingsInteractor,
    private val alarmCheckInteractor: AlarmCheckInteractor,
    private val networkInteractor: RuuviNetworkInteractor,
    private val networkDataSyncInteractor: NetworkDataSyncInteractor,
    private val alarmRepository: AlarmRepository
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

    private val operationStatus = MutableLiveData<String> ("")
    val operationStatusObserve: LiveData<String> = operationStatus

    val sensorOwnedByUserObserve: LiveData<Boolean> = Transformations.map(sensorSettings) {
        it?.owner == networkInteractor.getEmail()
    }

    val canCalibrate = MediatorLiveData<Boolean>()

    init {
        canCalibrate.addSource(sensorOwnedByUserObserve) { canCalibrate.value = getCanCalibrateValue()}
        canCalibrate.addSource(userLoggedIn) { canCalibrate.value = getCanCalibrateValue() }
        canCalibrate.addSource(sensorSettings) { canCalibrate.value = getCanCalibrateValue() }
    }

    private fun getCanCalibrateValue(): Boolean? {
        val ownedByUser = sensorOwnedByUserObserve.value ?: false
        val loggedIn = userLoggedIn.value ?: false
        val owner: String? = sensorSettings.value?.owner
        return !loggedIn || ownedByUser || owner.isNullOrEmpty()
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

    private val handler = CoroutineExceptionHandler() { _, exception ->
        CoroutineScope(Dispatchers.Main).launch {
            operationStatus.value = exception.message
            Timber.d("CoroutineExceptionHandler: ${exception.message}")
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


    fun saveOrUpdateAlarmItems() {
        for (alarmItem in alarmElements) {
            if (alarmItem.shouldBeSaved()) {
                alarmRepository.saveAlarmElement(alarmItem)
                networkInteractor.setAlert(sensorId, alarmItem)
            }
            if (!alarmItem.isEnabled) {
                val notificationId = alarmItem.alarm?.id ?: -1
                removeNotificationById(notificationId)
            }
        }
    }

    fun claimSensor() {
        val sensorSettings = sensorSettings.value
        if (sensorSettings != null) {
            networkInteractor.claimSensor(sensorSettings) {
                updateNetworkStatus()
                if (it == null || it.error.isNullOrEmpty() == false) {
                    //TODO LOCALIZE
                    operationStatus.value = "Failed to claim tag: ${it?.error}"
                } else {
                    operationStatus.value = "Tag successfully claimed"
                }
            }
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
                item.customDescription = alarm.customDescription ?: ""
                item.mutedTill = alarm.mutedTill
                item.alarm = alarm
                item.normalizeValues()
            }
        }
    }
}