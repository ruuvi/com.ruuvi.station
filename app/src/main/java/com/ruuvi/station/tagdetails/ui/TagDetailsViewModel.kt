package com.ruuvi.station.tagdetails.ui

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.os.Handler
import com.ruuvi.station.alarm.AlarmChecker
import com.ruuvi.station.app.preferences.Preferences
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.tagdetails.domain.TagDetailsInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TagDetailsViewModel (
        val tagDetailsInteractor: TagDetailsInteractor,
        val preferences: Preferences
        ): ViewModel() {

    private var tags = MutableLiveData<List<RuuviTagEntity>>()
    private var selectedTag = MutableLiveData<RuuviTagEntity>()
    private var showGraph = MutableLiveData<Boolean>()
    private var alarmStatus = MutableLiveData<Int>()
    private val handler = Handler()
    private val uiScope = CoroutineScope(Dispatchers.Main)
    var dashboardEnabled = preferences.dashboardEnabled

    init {
        refreshData()
        handler.removeCallbacksAndMessages(null)
        handler.post(object : Runnable {
            override fun run() {
                checkForAlarm()
                handler.postDelayed(this, 1000)
            }
        })
    }

    fun observeTags(): LiveData<List<RuuviTagEntity>> = tags

    fun observeSelectedTag(): LiveData<RuuviTagEntity> = selectedTag

    fun observeAlarmStatus(): LiveData<Int> = alarmStatus

    fun observeShowGraph() : LiveData<Boolean> = showGraph

    fun pageSelected(pageIndex: Int) {
        selectedTag.value = tags.value?.get(pageIndex)
        checkForAlarm()
    }

    fun switchGraphVisibility() {
        showGraph.value = !(showGraph.value ?: false)
    }

    fun refreshData() {
        tags.value = tagDetailsInteractor.getAllTags()
    }

    private fun checkForAlarm() {
        CoroutineScope(Dispatchers.IO).launch {
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
        super.onCleared()
        handler.removeCallbacksAndMessages(null)
    }
}