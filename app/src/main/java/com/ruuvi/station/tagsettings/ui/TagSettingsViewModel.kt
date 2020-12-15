package com.ruuvi.station.tagsettings.ui

import android.net.Uri
import androidx.lifecycle.*
import com.ruuvi.station.alarm.domain.AlarmCheckInteractor
import com.ruuvi.station.database.tables.Alarm
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.network.data.response.SensorDataResponse
import com.ruuvi.station.network.domain.NetworkDataSyncInteractor
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class TagSettingsViewModel(
    val tagId: String,
    private val interactor: TagSettingsInteractor,
    private val alarmCheckInteractor: AlarmCheckInteractor,
    private val networkInteractor: RuuviNetworkInteractor,
    private val networkDataSyncInteractor: NetworkDataSyncInteractor
) : ViewModel() {

    var tagAlarms: List<Alarm> = ArrayList()
    var alarmItems: MutableList<TagSettingsActivity.AlarmItem> = ArrayList()
    var file: Uri? = null

    private var networkStatus = MutableLiveData<SensorDataResponse?>(networkInteractor.getSensorNetworkStatus(tagId))

    private val tagState = MutableLiveData<RuuviTagEntity?>(getTagById(tagId))
    val tagObserve: LiveData<RuuviTagEntity?> = tagState

    private val userLoggedIn = MutableLiveData<Boolean> (networkInteractor.signedIn)
    val userLoggedInObserve: LiveData<Boolean> = userLoggedIn

    val sensorOwnedByUserObserve: LiveData<Boolean> = Transformations.map(networkStatus) {
        it?.owner == networkInteractor.getEmail()
    }

    val isNetworkTagObserve: LiveData<Boolean> = Transformations.map(networkStatus) {
        it != null
    }

    val sensorOwnerObserve: LiveData<String> = Transformations.map(networkStatus) {
        it?.owner
    }

    private val operationStatus = MutableLiveData<String> ("")
    val operationStatusObserve: LiveData<String> = operationStatus

    private val handler = CoroutineExceptionHandler() { _, exception ->
        CoroutineScope(Dispatchers.Main).launch {
            operationStatus.value = exception.message
        }
    }

    fun getTagById(tagId: String): RuuviTagEntity? =
        interactor.getTagById(tagId)

    fun updateNetworkStatus() {
        networkStatus.value = networkInteractor.getSensorNetworkStatus(tagId)
    }

    fun updateTag() {
        val tag = tagState.value
        tag?.let {
            interactor.updateTag(tag)
        }
        tagState.value = getTagById(tagId)
    }

    fun updateTag(tag: RuuviTagEntity) =
        interactor.updateTag(tag)

    fun deleteTag(tag: RuuviTagEntity) {
        interactor.deleteTagsAndRelatives(tag)
        if (networkStatus.value?.owner == networkInteractor.getEmail()) {
            networkInteractor.unclaimSensor(tagId)
        } else if (!networkStatus.value?.owner.isNullOrEmpty()) {
            networkInteractor.getEmail()?.let { email ->
                networkInteractor.unshareSensor(email, tagId, handler) {response->
                    if (response?.result == "error") {
                        operationStatus.value = response.error
                    }
                }
            }
        }
    }

    fun removeNotificationById(notificationId: Int) {
        alarmCheckInteractor.removeNotificationById(notificationId)
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

    fun getSensorData() {
        val tag = tagState.value?.id
        if (tag != null) {
            viewModelScope.launch {
                networkDataSyncInteractor.syncSensorDataForPeriod(tag, 72)
            }
        }
    }

    fun setUserBackground(path: String) {
        tagObserve.value?.userBackground = path
        updateTag()
    }

    fun setName(name: String?) {
        tagObserve.value?.name = name
        updateTag()
    }
}