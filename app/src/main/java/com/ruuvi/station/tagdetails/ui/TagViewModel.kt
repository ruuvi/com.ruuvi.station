package com.ruuvi.station.tagdetails.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.tagdetails.domain.TagDetailsInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Timer
import kotlin.concurrent.scheduleAtFixedRate

class TagViewModel(
    private val tagDetailsInteractor: TagDetailsInteractor,
    tagId: String
) : ViewModel() {
    val tagEntry = Channel<RuuviTagEntity>()

    val tagReadings = Channel<List<TagSensorReading>>()

    private val ioScope = CoroutineScope(Dispatchers.IO)

    private var showGraph = false

    private val timer = Timer("tagViewModelTimer", true)

    init {
        Timber.d("TagViewModel initialized")
        getTagInfo(tagId)
    }

    fun startShowGraph() {
        showGraph = true
    }

    fun stopShowGraph() {
        showGraph = false
    }

    private fun getTagInfo(tagId: String) {
        Timber.d("getTagInfo $tagId")
        ioScope.launch {
            timer.scheduleAtFixedRate(0, 1000) {
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
                    tagReadings.send(it)
                }
        }
    }

    private fun getTagEntryData(tagId: String) {
        Timber.d("getTagEntryData for tagId = $tagId")
        ioScope.launch {
            tagDetailsInteractor
                .getTag(tagId)
                ?.let {
                    tagEntry.send(it)
                }
        }
    }

    fun getTemperatureString(context: Context, tag: RuuviTagEntity): String =
        tagDetailsInteractor.getTemperatureString(context, tag)

    fun getHumidityString(context: Context, tag: RuuviTagEntity): String =
        tagDetailsInteractor.getHumidityString(context, tag)

    override fun onCleared() {
        super.onCleared()

        cancelTimerAndChannels()
        Timber.d("TagViewModel cleared!")
    }

    private fun cancelTimerAndChannels() {
        timer.cancel()
        tagReadings.cancel()
        tagEntry.cancel()
    }
}