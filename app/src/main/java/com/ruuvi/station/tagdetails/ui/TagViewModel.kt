package com.ruuvi.station.tagdetails.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruuvi.station.app.preferences.GlobalSettings
import com.ruuvi.station.bluetooth.domain.BluetoothGattInteractor
import com.ruuvi.station.bluetooth.model.GattSyncStatus
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tagdetails.domain.TagDetailsInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*

class TagViewModel(
        private val tagDetailsInteractor: TagDetailsInteractor,
        private val gattInteractor: BluetoothGattInteractor,
        private val sensorHistoryRepository: SensorHistoryRepository,
        val tagId: String
) : ViewModel() {
    private val tagEntry = MutableLiveData<RuuviTag?>(null)
    val tagEntryObserve: LiveData<RuuviTag?> = tagEntry

    private val tagReadings = MutableLiveData<List<TagSensorReading>?>(null)
    val tagReadingsObserve: LiveData<List<TagSensorReading>?> = tagReadings

    private val syncStatusObj = MutableLiveData<GattSyncStatus>()
    val syncStatusObserve: LiveData<GattSyncStatus> = syncStatusObj

    private val ioScope = CoroutineScope(Dispatchers.IO)

    private var showGraph = false

    private var selected = false

    init {
        Timber.d("TagViewModel initialized")
        getTagInfo()

        viewModelScope.launch {
            gattInteractor.syncStatusFlow.collect {
                Timber.d("syncStatusFlow collected $it")
                it?.let {
                    if (it.sensorId == tagId) syncStatusObj.value = it
                }
            }
        }
    }

    fun isShowGraph(isShow: Boolean) {
        showGraph = isShow
    }

    fun disconnectGatt() {
        tagEntryObserve.value?.let { tag ->
            gattInteractor.disconnect(tag.id)
        }
    }

    fun syncGatt() {
        tagEntryObserve.value?.let { tag ->
            var syncFrom = tag.lastSync
            val historyLength = Date(Date().time - 1000 * 60 * 60 * 24 * GlobalSettings.historyLengthDays)
            if (syncFrom == null || syncFrom.before(historyLength)) {
                syncFrom = historyLength
            }
            Timber.d("sync logs from: %s", syncFrom)
            gattInteractor.readLogs(tag.id, syncFrom)
        }
    }

    fun removeTagData() {
        sensorHistoryRepository.removeForSensor(tagId)
        tagDetailsInteractor.clearLastSync(tagId)
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