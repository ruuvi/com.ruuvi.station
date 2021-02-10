package com.ruuvi.station.tagsettings.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.ruuvi.station.alarm.domain.AlarmCheckInteractor
import com.ruuvi.station.database.tables.Alarm
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.ArrayList

@ExperimentalCoroutinesApi
class TagSettingsViewModel(
    val tagId: String,
    private val interactor: TagSettingsInteractor,
    private val alarmCheckInteractor: AlarmCheckInteractor
) : ViewModel() {

    var tagAlarms: List<Alarm> = Alarm.getForTag(tagId)
    var alarmItems: MutableList<TagSettingsActivity.AlarmItem> = ArrayList()
    var file: Uri? = null

    private val tagState = MutableStateFlow<RuuviTagEntity?>(null)
    val tagFlow: StateFlow<RuuviTagEntity?> = tagState

    init {
        getTagInfo()
    }

    fun getTagInfo() {
        CoroutineScope(Dispatchers.IO).launch {
            tagState.value = getTagById(tagId)
        }
    }

    fun getTagById(tagId: String): RuuviTagEntity? =
        interactor.getTagById(tagId)

    fun updateTag(tag: RuuviTagEntity) =
        interactor.updateTag(tag)

    fun deleteTag(tag: RuuviTagEntity) =
        interactor.deleteTagsAndRelatives(tag)

    fun removeNotificationById(notificationId: Int) {
        alarmCheckInteractor.removeNotificationById(notificationId)
    }

    fun updateTagName(name: String?) = CoroutineScope(Dispatchers.IO).launch {
        interactor.updateTagName(tagId, name)
    }

    fun updateTagBackground(userBackground: String?, defaultBackground: Int?) = CoroutineScope(Dispatchers.IO).launch {
        interactor.updateTagBackground(tagId, userBackground, defaultBackground)
    }

    fun saveOrUpdateAlarmItems() {
        for (alarmItem in alarmItems) {
            if (alarmItem.isChecked || alarmItem.low != alarmItem.min || alarmItem.high != alarmItem.max) {
                if (alarmItem.alarm == null) {
                    alarmItem.alarm = Alarm(alarmItem.low, alarmItem.high, alarmItem.type, tagId, alarmItem.customDescription, alarmItem.mutedTill)
                    alarmItem.alarm?.enabled = alarmItem.isChecked
                    alarmItem.alarm?.save()
                } else {
                    alarmItem.alarm?.enabled = alarmItem.isChecked
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
            if (!alarmItem.isChecked) {
                val notificationId = alarmItem.alarm?.id ?: -1
                removeNotificationById(notificationId)
            }
        }
    }
}