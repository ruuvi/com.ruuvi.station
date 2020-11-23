package com.ruuvi.station.tagdetails.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tagdetails.domain.TagDetailsInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class TagViewModel(
    private val tagDetailsInteractor: TagDetailsInteractor,
    val tagId: String
) : ViewModel() {
    private val tagEntry = MutableLiveData<RuuviTag?>(null)
    val tagEntryObserve: LiveData<RuuviTag?> = tagEntry

    private val tagReadings = MutableLiveData<List<TagSensorReading>?>(null)
    val tagReadingsObserve: LiveData<List<TagSensorReading>?> = tagReadings

    private val ioScope = CoroutineScope(Dispatchers.IO)

    private var showGraph = false

    private var selected = false

    init {
        Timber.d("TagViewModel initialized")
        getTagInfo()
    }

    fun isShowGraph(isShow: Boolean) {
        showGraph = isShow
    }

    fun tagSelected(selectedTag: RuuviTag?) {
        selected = tagId == selectedTag?.id
    }

    fun getTagInfo() {
        ioScope.launch {
                Timber.d("getTagInfo $tagId")
                getTagEntryData(tagId)
                if (showGraph && selected) getGraphData(tagId)
        }
    }

    private fun getGraphData(tagId: String) {
        Timber.d("Get graph data for tagId = $tagId")
        ioScope.launch {
            tagDetailsInteractor
                .getTagReadings(tagId)
                ?.let {
                    withContext(Dispatchers.Main){
                        tagReadings.value = it
                    }
                }
        }
    }

    private fun getTagEntryData(tagId: String) {
        Timber.d("getTagEntryData for tagId = $tagId")
        ioScope.launch {
            tagDetailsInteractor
                .getTagById(tagId)
                ?.let {
                    withContext(Dispatchers.Main){
                        tagEntry.value = it
                    }
                }
        }
    }

    fun getTemperatureString(tag: RuuviTag): String =
        tagDetailsInteractor.getTemperatureString(tag)

    fun getTemperatureStringWithoutUnit(tag: RuuviTag): String =
        tagDetailsInteractor.getTemperatureStringWithoutUnit(tag)

    fun getTemperatureUnitString(): String =
        tagDetailsInteractor.getTemperatureUnitString()

    fun getHumidityString(tag: RuuviTag): String =
        tagDetailsInteractor.getHumidityString(tag)

    fun getPressureString(tag: RuuviTag): String =
        tagDetailsInteractor.getPressureString(tag)

    fun getSignalString(tag: RuuviTag): String =
        tagDetailsInteractor.getSignalString(tag)

    override fun onCleared() {
        super.onCleared()
        Timber.d("TagViewModel cleared!")
    }
}