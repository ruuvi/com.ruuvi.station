package com.ruuvi.station.tagsettings.ui

import android.net.Uri
import androidx.lifecycle.*
import com.ruuvi.station.alarm.domain.AlarmCheckInteractor
import com.ruuvi.station.alarm.domain.AlarmElement
import com.ruuvi.station.alarm.domain.AlarmType
import com.ruuvi.station.database.tables.Alarm
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.network.data.response.SensorDataResponse
import com.ruuvi.station.network.domain.NetworkDataSyncInteractor
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*

class TagSettingsViewModel(
    val tagId: String,
    private val interactor: TagSettingsInteractor,
    private val alarmCheckInteractor: AlarmCheckInteractor,
    private val networkInteractor: RuuviNetworkInteractor,
    private val networkDataSyncInteractor: NetworkDataSyncInteractor
) : ViewModel() {
    var alarmElements: MutableList<AlarmElement> = ArrayList()
    var file: Uri? = null

    private var networkStatus = MutableLiveData<SensorDataResponse?>(networkInteractor.getSensorNetworkStatus(tagId))

    private val tagState = MutableLiveData<RuuviTagEntity?>(getTagById(tagId))
    val tagObserve: LiveData<RuuviTagEntity?> = tagState

    private val userLoggedIn = MutableLiveData<Boolean> (networkInteractor.signedIn)
    val userLoggedInObserve: LiveData<Boolean> = userLoggedIn

    private val operationStatus = MutableLiveData<String> ("")
    val operationStatusObserve: LiveData<String> = operationStatus

    val sensorOwnedByUserObserve: LiveData<Boolean> = Transformations.map(networkStatus) {
        it?.owner == networkInteractor.getEmail()
    }

    val sensorOwnerObserve: LiveData<String> = Transformations.map(networkStatus) {
        it?.owner
    }

    val isNetworkTagObserve: LiveData<Boolean> = Transformations.map(networkStatus) {
        it != null
    }

    fun getTagInfo() {
        CoroutineScope(Dispatchers.IO).launch {
            val tagInfo = getTagById(tagId)
            withContext(Dispatchers.Main) {
                tagState.value = tagInfo
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
        networkStatus.value = networkInteractor.getSensorNetworkStatus(tagId)
    }

    fun deleteTag(tag: RuuviTagEntity) {
        interactor.deleteTagsAndRelatives(tag)
        if (networkStatus.value?.owner == networkInteractor.getEmail()) {
            networkInteractor.unclaimSensor(tagId)
        } else if (!networkStatus.value?.owner.isNullOrEmpty()) {
            networkInteractor.getEmail()?.let { email ->
                networkInteractor.unshareSensor(email, tagId, handler) {response->
                    if (response?.isError() == true) {
                        operationStatus.value = response.error
                    }
                }
            }
        }
    }

    fun removeNotificationById(notificationId: Int) {
        alarmCheckInteractor.removeNotificationById(notificationId)
    }

    fun updateTagBackground(userBackground: String?, defaultBackground: Int?) = CoroutineScope(Dispatchers.IO).launch {
        interactor.updateTagBackground(tagId, userBackground, defaultBackground)
        networkStatus.value?.let {
            if (userBackground.isNullOrEmpty() == false) {
                Timber.d("Upload image filename: $userBackground")
                networkInteractor.uploadImage(tagId, userBackground, handler) { response ->
                    if (response?.isSuccess() == true && !response.data?.guid.isNullOrEmpty()) {
                        interactor.updateNetworkBackground(tagId, response.data?.guid)
                    }
                }
            } else if (it.picture.isNotEmpty()){
                networkInteractor.resetImage(tagId, handler) {
                }
            }
        }
    }

    fun saveOrUpdateAlarmItems() {
        for (alarmItem in alarmElements) {
            if (alarmItem.isEnabled || alarmItem.low != alarmItem.min || alarmItem.high != alarmItem.max) {
                if (alarmItem.alarm == null) {
                    alarmItem.alarm = Alarm(alarmItem.low, alarmItem.high, alarmItem.type.value, tagId, alarmItem.customDescription, alarmItem.mutedTill)
                    alarmItem.alarm?.enabled = alarmItem.isEnabled
                    alarmItem.alarm?.save()
                } else {
                    alarmItem.alarm?.enabled = alarmItem.isEnabled
                    alarmItem.alarm?.low = alarmItem.low
                    alarmItem.alarm?.high = alarmItem.high
                    alarmItem.alarm?.customDescription = alarmItem.customDescription
                    alarmItem.alarm?.mutedTill = alarmItem.mutedTill
                    alarmItem.alarm?.update()
                }
            } else if (alarmItem.alarm != null) {
                alarmItem.alarm?.enabled = false
                alarmItem.alarm?.mutedTill = alarmItem.mutedTill
                alarmItem.alarm?.update()
            }
            if (!alarmItem.isEnabled) {
                val notificationId = alarmItem.alarm?.id ?: -1
                removeNotificationById(notificationId)
            }
        }
    }

    fun claimSensor() {
        val tag = tagState.value
        if (tag != null) {
            networkInteractor.claimSensor(tag) {
                updateNetworkStatus()
                if (it == null || it.error.isNullOrEmpty() == false) {
                    operationStatus.value = "Failed to claim tag: ${it?.error}"
                } else {
                    operationStatus.value = "Tag successfully claimed"
                }
            }
        }
    }

    fun statusProcessed() { operationStatus.value = "" }

    fun setName(name: String?) {
        interactor.updateTagName(tagId, name)
        getTagInfo()
        networkStatus.value?.let {
            networkInteractor.updateSensor(tagId, name ?: "", handler) {response->
                if (response?.isError() == true) {
                    operationStatus.value = response.error
                }
            }
        }
    }

    fun setupAlarmElements() {
        alarmElements.clear()

        with(alarmElements) {
            add(AlarmElement(
                AlarmType.TEMPERATURE,
                false,
                -40,
                85
            ))
            add(AlarmElement(
                AlarmType.HUMIDITY,
                false,
                0,
                100
            ))
            add(AlarmElement(
                AlarmType.PRESSURE,
                false,
                30000,
                110000
            ))
            add(AlarmElement(
                AlarmType.RSSI,
                false,
                -105,
                0
            ))
            add(AlarmElement(
                AlarmType.MOVEMENT,
                false,
                0,
                0
            ))
        }

        val dbAlarms = Alarm.getForTag(tagId)
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