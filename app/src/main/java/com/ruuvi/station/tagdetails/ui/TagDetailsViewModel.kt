package com.ruuvi.station.tagdetails.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruuvi.station.alarm.AlarmChecker
import com.ruuvi.station.app.preferences.Preferences
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.tagdetails.domain.TagDetailsInteractor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Timer
import kotlin.concurrent.scheduleAtFixedRate

@ExperimentalCoroutinesApi
class TagDetailsViewModel(
    private val tagDetailsInteractor: TagDetailsInteractor,
    val preferences: Preferences
) : ViewModel() {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private var timer = Timer("timer", true)

    private val isShowGraph = MutableStateFlow<Boolean>(false)
    val isShowGraphFlow: StateFlow<Boolean> = isShowGraph

    private val selectedTag = MutableStateFlow<RuuviTagEntity?>(null)
    val selectedTagFlow: StateFlow<RuuviTagEntity?> = selectedTag

    private val tags = MutableStateFlow<List<RuuviTagEntity>?>(null)
    val tagsFlow: StateFlow<List<RuuviTagEntity>?> = tags

    private val alarmStatus = MutableStateFlow<Int>(-1)
    val alarmStatusFlow: StateFlow<Int> = alarmStatus

    var dashboardEnabled = preferences.dashboardEnabled
    var tag: RuuviTagEntity? = null

    init {
        refreshTags()

        ioScope.launch {
            timer.scheduleAtFixedRate(0, 1000) {
                checkForAlarm()
            }
        }
    }

    fun pageSelected(pageIndex: Int) {
        viewModelScope.launch {
            tags.value?.let {
                selectedTag.value = it[pageIndex]
            }
            checkForAlarm()
        }
    }

    fun switchShowGraphChannel() {
        isShowGraph.value = !isShowGraph.value
    }


    fun refreshTags() {
        ioScope.launch {
            val list = tagDetailsInteractor.getAllTags()
            withContext(Dispatchers.Main) {
                tags.value = list
            }
        }
    }

    private fun checkForAlarm() {
        ioScope.launch {
            selectedTag.value?.id?.let { tagId ->
                val tagEntry = tagDetailsInteractor.getTag(tagId)
                tagEntry?.let {
                    withContext(Dispatchers.Main) {
                        alarmStatus.value = AlarmChecker.getStatus(it)
                    }
                }
            }
        }
    }

    override fun onCleared() {
        timer.cancel()
        super.onCleared()
    }
}