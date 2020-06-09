package com.ruuvi.station.tagdetails.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ruuvi.station.alarm.AlarmChecker
import com.ruuvi.station.app.preferences.Preferences
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.tagdetails.domain.TagDetailsInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Timer
import kotlin.concurrent.scheduleAtFixedRate

class TagDetailsViewModel(
    private val tagDetailsInteractor: TagDetailsInteractor,
    val preferences: Preferences
) : ViewModel() {

    private var tags = MutableLiveData<List<RuuviTagEntity>>()
    private var selectedTag = MutableLiveData<RuuviTagEntity>()
    private var showGraph = MutableLiveData<Boolean>()
    private var alarmStatus = MutableLiveData<Int>()
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private var timer = Timer("timer", true)

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

    fun observeTags(): LiveData<List<RuuviTagEntity>> = tags

    fun observeSelectedTag(): LiveData<RuuviTagEntity> = selectedTag

    fun observeAlarmStatus(): LiveData<Int> = alarmStatus

    fun observeShowGraph(): LiveData<Boolean> = showGraph

    fun pageSelected(pageIndex: Int) {
        selectedTag.value = tags.value?.get(pageIndex)
        ioScope.launch {
            checkForAlarm()
        }
    }

    fun switchGraphVisibility() {
        showGraph.value = !(showGraph.value ?: false)
    }

    fun refreshTags() {
        ioScope.launch {
            val list = tagDetailsInteractor.getAllTags()
            uiScope.launch {
                tags.value = list
            }
        }
    }

    private fun checkForAlarm() {
        ioScope.launch {
            selectedTag.value?.id?.let { tagId ->
                val tagEntry = tagDetailsInteractor.getTag(tagId)
                tagEntry?.let {
                    uiScope.launch {
                        alarmStatus.value = AlarmChecker.getStatus(it)
                    }
                }
            }
        }
    }

    fun getCurrentShowGraph() = showGraph.value ?: false

    override fun onCleared() {
        timer.cancel()
        super.onCleared()
    }
}