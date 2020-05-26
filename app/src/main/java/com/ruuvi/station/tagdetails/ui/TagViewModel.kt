package com.ruuvi.station.tagdetails.ui

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.os.Handler
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.tagdetails.domain.TagDetailsInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class TagViewModel (
        private val tagDetailsInteractor: TagDetailsInteractor
) : ViewModel() {
    private val tagEntry = MutableLiveData<RuuviTagEntity>()
    private val tagReadings = MutableLiveData<List<TagSensorReading>>()

    private val handler = Handler()
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private var showGraph = false

    init {
        Timber.d("vm initialized")
    }

    fun observeTagEntry(): LiveData<RuuviTagEntity> = tagEntry

    fun observeTagReadings(): LiveData<List<TagSensorReading>> = tagReadings

    fun getTagInfo(tagId: String) {
        Timber.d("getTagInfo $tagId")
        handler.removeCallbacksAndMessages(null)
        tagEntry.value = tagDetailsInteractor.getTag(tagId)
        handler.post( object : Runnable{
            override fun run() {
                Timber.d("handler for $tagId")
                getTagEntryData(tagId)
                if (showGraph) getGraphData(tagId)
                handler.postDelayed(this, 1000)
            }
        })
    }

    fun startShowGraph() {
        showGraph = true
    }

    fun stopShowGraph() {
        showGraph = false
    }

    private fun getGraphData(tagId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val readings = tagDetailsInteractor.getTagReadings(tagId)
            uiScope.launch {
                tagReadings.value = readings
            }
        }
    }

    private fun getTagEntryData(tagId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            var tag = tagDetailsInteractor.getTag(tagId)
            uiScope.launch {
                tagEntry.value = tag
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        handler.removeCallbacksAndMessages(null)
        Timber.d("TagViewModel cleared!")
    }
}