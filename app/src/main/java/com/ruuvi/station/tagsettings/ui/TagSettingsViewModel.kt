package com.ruuvi.station.tagsettings.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruuvi.station.database.TagRepository
import com.ruuvi.station.database.tables.Alarm
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.ArrayList
import java.util.Timer
import kotlin.concurrent.scheduleAtFixedRate

class TagSettingsViewModel(
    private val interactor: TagSettingsInteractor,
    val tagId: String
) : ViewModel() {

    var tag: RuuviTagEntity? = null
    var tempUnit = "C"
    var tagAlarms: List<Alarm> = ArrayList()
    var alarmItems: MutableList<TagSettingsActivity.AlarmItem> = ArrayList()
    var file: Uri? = null

    private val tagState = MutableStateFlow<RuuviTagEntity?>(null)
    val tagFlow: StateFlow<RuuviTagEntity?> = tagState

    private val timer = Timer("TagSettingsViewModelTimer", true)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            timer.scheduleAtFixedRate(0, 1000) {
                tag = getTagById(tagId)
                viewModelScope.launch {
                    tagState.value = getTagById(tagId)
                }
            }
        }

        tempUnit = interactor.getTemperatureUnit()
    }

    fun getRepositoryInstance(): TagRepository =
        interactor.getRepositoryInstance()

    fun getTagById(tagId: String): RuuviTagEntity? =
        interactor.getTagById(tagId)

    fun updateTag() {
        tagState.value?.update()
    }

    fun updateTag(tag: RuuviTagEntity) =
        interactor.updateTag(tag)

    fun deleteTag(tag: RuuviTagEntity) =
        interactor.deleteTagsAndRelatives(tag)

    override fun onCleared() {
        super.onCleared()
        timer.cancel()
    }
}