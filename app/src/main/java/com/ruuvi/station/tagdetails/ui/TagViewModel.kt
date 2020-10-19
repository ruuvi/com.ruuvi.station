package com.ruuvi.station.tagdetails.ui

import androidx.lifecycle.ViewModel
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tagdetails.domain.TagDetailsInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

@ExperimentalCoroutinesApi
class TagViewModel(
    private val tagDetailsInteractor: TagDetailsInteractor,
    val tagId: String
) : ViewModel() {
    private val tagEntry = MutableStateFlow<RuuviTag?>(null)
    val tagEntryFlow: StateFlow<RuuviTag?> = tagEntry

    private val tagReadings = MutableStateFlow<List<TagSensorReading>?>(null)
    val tagReadingsFlow: StateFlow<List<TagSensorReading>?> = tagReadings

    private val ioScope = CoroutineScope(Dispatchers.IO)

    private var showGraph = false

    private val timer = Timer("tagViewModelTimer", true)

    init {
        Timber.d("TagViewModel initialized")
        getTagInfo(tagId)
    }

    fun isShowGraph(isShow: Boolean) {
        showGraph = isShow
    }

    fun removeTagData() {
        TagSensorReading.removeForTag(tagId)
        updateLastSync(null)
    }

    fun updateLastSync(date: Date?) {
        tagDetailsInteractor.updateLastSync(tagId, date)
    }

    private fun getTagInfo(tagId: String) {
        ioScope.launch {
            timer.scheduleAtFixedRate(0, 1000) {
                Timber.d("getTagInfo $tagId")
                getTagEntryData(tagId)
                if (showGraph) getGraphData(tagId)
            }
        }
    }

    private fun getGraphData(tagId: String) {
        Timber.d("Get graph data for tagId = $tagId")
        ioScope.launch {
            tagDetailsInteractor
                .getTagReadings(tagId)
                ?.let {
                    tagReadings.value = it
                }
        }
    }

    private fun getTagEntryData(tagId: String) {
        Timber.d("getTagEntryData for tagId = $tagId")
        ioScope.launch {
            tagDetailsInteractor
                .getTagById(tagId)
                ?.let { tagEntry.value = it }
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

        cancelTimerAndChannels()
        Timber.d("TagViewModel cleared!")
    }

    private fun cancelTimerAndChannels() {
        timer.cancel()
    }
}