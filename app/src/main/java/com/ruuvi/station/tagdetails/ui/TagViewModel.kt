package com.ruuvi.station.tagdetails.ui

import android.app.Dialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ruuvi.station.R
import com.ruuvi.station.bluetooth.IRuuviGattListener
import com.ruuvi.station.bluetooth.LogReading
import com.ruuvi.station.bluetooth.domain.BluetoothGattInteractor
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tagdetails.domain.TagDetailsInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*


class TagViewModel(
        private val tagDetailsInteractor: TagDetailsInteractor,
        private val gattInteractor: BluetoothGattInteractor,
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

    fun syncGatt(context: Context) {
        val builder = AlertDialog.Builder(context)
        var text = "${context.getString(R.string.connecting)}.."
        builder.setMessage(text)
        builder.setPositiveButton(context.getText(R.string.ok)) { p0, _ -> p0.dismiss() }
        val ad = builder.show()
        ad.setCanceledOnTouchOutside(false)
        ad.getButton(Dialog.BUTTON_POSITIVE).isEnabled = false
        var completed = false
        tagEntryObserve.value?.let { tag ->
            Timber.d("sync logs from: " + tag.lastSync)
            val found = gattInteractor.readLogs(tag.id, tag.lastSync, object : IRuuviGattListener {
                override fun connected(state: Boolean) {
                    if (state) {
                        Handler(Looper.getMainLooper()).post(Runnable {
                            text += "\n${context.getString(R.string.connected_reading_info)}.."
                            ad.setMessage(text)
                        })
                    } else {
                        if (completed) {
                            if (!text.contains(context.getString(R.string.sync_complete))) {
                                text += "\n${context.getString(R.string.sync_complete)}"
                            }
                        } else {
                            text += "\n${context.getString(R.string.disconnected)}"
                        }
                        Handler(Looper.getMainLooper()).post(Runnable {
                            ad.getButton(Dialog.BUTTON_POSITIVE).isEnabled = true
                            ad.setMessage(text)
                        })
                    }
                }

                override fun deviceInfo(model: String, fw: String, canReadLogs: Boolean) {
                    Handler(Looper.getMainLooper()).post(Runnable {
                        text += if (canReadLogs) {
                            "\n$model, $fw\n${context.getString(R.string.reading_history)}.."
                        } else {
                            "\n$model, $fw\n${context.getString(R.string.reading_history_not_supported)}"
                        }
                        ad.setMessage(text)
                    })
                }

                override fun dataReady(data: List<LogReading>) {
                    Handler(Looper.getMainLooper()).post(Runnable {
                        text += if (data.isNotEmpty()) {
                            "\n${context.getString(R.string.data_points_read, data.size * 3)}"
                        } else {
                            "\n${context.getString(R.string.no_new_data_points)}"
                        }
                        ad.setMessage(text)
                    })
                    saveGattReadings(tag, data)
                    completed = true
                }

                override fun heartbeat(raw: String) {
                }
            })
            if (!found) {
                Handler(Looper.getMainLooper()).post(Runnable {
                    ad.setMessage(context.getString(R.string.tag_not_in_range))
                    ad.getButton(Dialog.BUTTON_POSITIVE).isEnabled = true
                })
            }
        } ?: kotlin.run {
            Handler(Looper.getMainLooper()).post(Runnable {
                ad.setMessage(context.getString(R.string.something_went_wrong))
                ad.getButton(Dialog.BUTTON_POSITIVE).isEnabled = true
            })
        }
    }

    fun removeTagData() {
        TagSensorReading.removeForTag(tagId)
        updateLastSync(null)
    }

    fun saveGattReadings(tag: RuuviTag, data: List<LogReading>) {
        val tagReadingList = mutableListOf<TagSensorReading>()
        data.forEach { logReading ->
            val reading = TagSensorReading()
            reading.ruuviTagId = tag.id
            reading.temperature = logReading.temperature
            reading.humidity = logReading.humidity
            reading.pressure = logReading.pressure
            reading.createdAt = logReading.date
            tagReadingList.add(reading)
        }
        TagSensorReading.saveList(tagReadingList)
        updateLastSync(Date())
    }

    fun updateLastSync(date: Date?) {
        tagDetailsInteractor.updateLastSync(tagId, date)
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
                        withContext(Dispatchers.Main) {
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
                        withContext(Dispatchers.Main) {
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