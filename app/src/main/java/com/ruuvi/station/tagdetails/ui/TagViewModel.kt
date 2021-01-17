package com.ruuvi.station.tagdetails.ui

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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

    enum class SyncProgress {
        STILL, CONNECTING, CONNECTED, DISCONNECTED, READING_INFO, READING_DATA, SAVING_DATA, NOT_SUPPORTED, NOT_FOUND, ERROR, DONE
    }

    class SyncStatus {
        var syncProgress = SyncProgress.STILL
        var deviceInfoModel = ""
        var deviceInfoFw = ""
        var readDataSize = 0
    }

    private val syncStatusObj = MutableLiveData<SyncStatus>()
    val syncStatusObserve: LiveData<SyncStatus> = syncStatusObj

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

    fun syncGatt() {
        syncStatusObj.value = SyncStatus()
        syncStatusObj.value?.syncProgress = SyncProgress.CONNECTING
        syncStatusObj.postValue(syncStatusObj.value)
        tagEntryObserve.value?.let { tag ->
            var syncFrom = tag.lastSync
            val threeDaysAgo = Date(Date().time - 1000 * 60 * 60 * 24 * 3)
            if (syncFrom == null) {
                syncFrom = threeDaysAgo
            } else if (syncFrom.before(threeDaysAgo)) {
                syncFrom = threeDaysAgo
            }
            Timber.d("sync logs from: %s", syncFrom)
            val found = gattInteractor.readLogs(tag.id, syncFrom, object : IRuuviGattListener {
                override fun connected(state: Boolean) {
                    if (state) {
                        syncStatusObj.value?.syncProgress = SyncProgress.CONNECTED
                        syncStatusObj.value?.syncProgress = SyncProgress.READING_INFO
                    } else {
                        if (syncStatusObj.value?.syncProgress == SyncProgress.SAVING_DATA) {
                            syncStatusObj.value?.syncProgress = SyncProgress.DONE
                        } else {
                            syncStatusObj.value?.syncProgress = SyncProgress.DISCONNECTED
                        }
                    }
                    syncStatusObj.postValue(syncStatusObj.value)
                }

                override fun deviceInfo(model: String, fw: String, canReadLogs: Boolean) {
                    syncStatusObj.value?.deviceInfoModel = model
                    syncStatusObj.value?.deviceInfoFw = fw
                    if (canReadLogs) {
                        syncStatusObj.value?.syncProgress = SyncProgress.READING_DATA
                    } else {
                        syncStatusObj.value?.syncProgress = SyncProgress.NOT_SUPPORTED
                    }
                    syncStatusObj.postValue(syncStatusObj.value)
                }

                override fun dataReady(data: List<LogReading>) {
                    syncStatusObj.value?.readDataSize = data.size
                    syncStatusObj.value?.syncProgress = SyncProgress.SAVING_DATA
                    syncStatusObj.postValue(syncStatusObj.value)
                    saveGattReadings(tag, data)
                }

                override fun heartbeat(raw: String) {
                }
            })
            if (!found) {
                syncStatusObj.value?.syncProgress = SyncProgress.NOT_FOUND
                syncStatusObj.postValue(syncStatusObj.value)
            }
        } ?: kotlin.run {
            Handler(Looper.getMainLooper()).post {
                syncStatusObj.value?.syncProgress = SyncProgress.ERROR
                syncStatusObj.postValue(syncStatusObj.value)
            }
        }
    }

    fun removeTagData() {
        TagSensorReading.removeForTag(tagId)
        tagDetailsInteractor.clearLastSync(tagId)
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