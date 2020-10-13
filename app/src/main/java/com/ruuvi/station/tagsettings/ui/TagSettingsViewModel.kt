package com.ruuvi.station.tagsettings.ui

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ruuvi.station.alarm.domain.AlarmCheckInteractor
import com.ruuvi.station.database.tables.Alarm
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor
import java.util.ArrayList

class TagSettingsViewModel(
    val tagId: String,
    private val interactor: TagSettingsInteractor,
    private val alarmCheckInteractor: AlarmCheckInteractor,
    private val networkInteractor: RuuviNetworkInteractor
) : ViewModel() {

    var tagAlarms: List<Alarm> = ArrayList()
    var alarmItems: MutableList<TagSettingsActivity.AlarmItem> = ArrayList()
    var file: Uri? = null

    private val tagState = MutableLiveData<RuuviTagEntity?>(getTagById(tagId))
    val tagObserve: LiveData<RuuviTagEntity?> = tagState

    private val userLoggedIn = MutableLiveData<Boolean> (networkInteractor.signedIn)
    val userLoggedInObserve: LiveData<Boolean> = userLoggedIn

    private val tagClaimed = MutableLiveData<Boolean> (networkInteractor.tagIsClaimed(tagId))
    val tagClaimedObserve: LiveData<Boolean> = tagClaimed

    private val operationStatus = MutableLiveData<String> ("")
    val operationStatusObserve: LiveData<String> = operationStatus

    fun getTagById(tagId: String): RuuviTagEntity? =
        interactor.getTagById(tagId)

    fun updateTag() {
        tagState.value?.update()
    }

    fun updateTag(tag: RuuviTagEntity) =
        interactor.updateTag(tag)

    fun deleteTag(tag: RuuviTagEntity) =
        interactor.deleteTagsAndRelatives(tag)

    fun removeNotificationById(notificationId: Int) {
        alarmCheckInteractor.removeNotificationById(notificationId)
    }

    fun claimSensor() {
        val tag = tagState.value
        if (tag != null) {
            networkInteractor.claimSensor(tag) {
                tagClaimed.value = networkInteractor.tagIsClaimed(tagId)
                if (it == null || it.error.isNullOrEmpty() == false) {
                    operationStatus.value = "Failed to claim tag"
                } else {
                    operationStatus.value = "Tag successfully claimed"
                }
            }
        }
    }

    fun shareSensor(recipientEmail: String) {
        if (recipientEmail.isNotEmpty()) {
            networkInteractor.shareSensor(recipientEmail, tagId) {
                if (it == null || it.error.isNullOrEmpty() == false) {
                    operationStatus.value = "Failed to share tag to $recipientEmail"
                } else {
                    operationStatus.value = "Tag shared with $recipientEmail"
                }
            }
        }
    }

    fun statusProcessed() { operationStatus.value = "" }

    fun getSensorData() {
        val tag = tagState.value?.id
        if (tag != null) {
            networkInteractor.getSensorData(tag) {
                if (it == null || it.error.isNullOrEmpty() == false) {
                    operationStatus.value = "Failed to get tag data"
                } else {

                }
            }
        }
    }
}