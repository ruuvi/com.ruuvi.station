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

    var tempUnit = "C"
    var tagAlarms: List<Alarm> = ArrayList()
    var alarmItems: MutableList<TagSettingsActivity.AlarmItem> = ArrayList()
    var file: Uri? = null

    private val tagState = MutableStateFlow<RuuviTagEntity?>(null)
    val tagFlow: StateFlow<RuuviTagEntity?> = tagState

    init {
        CoroutineScope(Dispatchers.IO).launch {
            tagState.value = getTagById(tagId)
        }

        tempUnit = interactor.getTemperatureUnit()
    }

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
}