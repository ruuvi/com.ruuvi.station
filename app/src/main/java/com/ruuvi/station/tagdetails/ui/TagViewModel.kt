package com.ruuvi.station.tagdetails.ui

import android.net.Uri
import androidx.lifecycle.*
import com.ruuvi.station.R
import com.ruuvi.station.app.preferences.GlobalSettings
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.app.ui.UiText
import com.ruuvi.station.bluetooth.domain.BluetoothGattInteractor
import com.ruuvi.station.bluetooth.model.SyncProgress
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.network.data.response.SensorDataResponse
import com.ruuvi.station.network.domain.NetworkDataSyncInteractor
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.settings.domain.AppSettingsInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tagdetails.domain.TagDetailsInteractor
import com.ruuvi.station.tagsettings.domain.CsvExporter
import com.ruuvi.station.util.Days
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*

class TagViewModel(
    private val tagDetailsInteractor: TagDetailsInteractor,
    private val gattInteractor: BluetoothGattInteractor,
    private val sensorHistoryRepository: SensorHistoryRepository,
    private val networkInteractor: RuuviNetworkInteractor,
    private val networkDataSyncInteractor: NetworkDataSyncInteractor,
    private val preferencesRepository: PreferencesRepository,
    private val sensorSettingsRepository: SensorSettingsRepository,
    private val appSettingsInteractor: AppSettingsInteractor,
    private val csvExporter: CsvExporter,
    val sensorId: String
) : ViewModel() {
    private val _tagEntry = MutableLiveData<RuuviTag?>(null)
    val tagEntry: LiveData<RuuviTag?> = _tagEntry

    private val tagReadings = MutableLiveData<List<TagSensorReading>?>(null)
    val tagReadingsObserve: LiveData<List<TagSensorReading>?> = tagReadings

    private val networkSyncInProgress = MutableLiveData<Boolean>(false)

    private var networkStatus = MutableLiveData<SensorDataResponse?>(networkInteractor.getSensorNetworkStatus(sensorId))

    val isNetworkTagObserve: LiveData<Boolean> = Transformations.map(networkStatus) {
        it != null
    }

    val canUseGattSync: LiveData<Boolean> = Transformations.map(_tagEntry) {
        it?.connectable == true
    }

    private val _gattSyncInProgress = MutableStateFlow<Boolean>(false)
    val gattSyncInProgress: StateFlow<Boolean> = _gattSyncInProgress

    private val _gattSyncStatus = MutableStateFlow<UiText>(UiText.EmptyString)
    val gattSyncStatus: StateFlow<UiText> = _gattSyncStatus

    private val _event = MutableSharedFlow<SyncProgress>()
    val event: SharedFlow<SyncProgress> = _event

    private val _clearChart = MutableSharedFlow<Boolean>()
    val clearChart: SharedFlow<Boolean> = _clearChart

    val syncStatus:MediatorLiveData<Boolean>  = MediatorLiveData<Boolean>()

    private val _chartViewPeriod = MutableStateFlow<Days>(getGraphViewPeriod())
    val chartViewPeriod: StateFlow<Days> = _chartViewPeriod

    private val ioScope = CoroutineScope(Dispatchers.IO)

    private var showGraph = false

    private var selected = false

    init {
        Timber.d("TagViewModel initialized")
        getTagInfo()
        collectGattSyncStatus()

        viewModelScope.launch {
            networkDataSyncInteractor.syncInProgressFlow.collect {
                networkSyncInProgress.value = it
            }
        }

        syncStatus.addSource(networkSyncInProgress) { syncStatus.value = isSyncInProgress() }
        syncStatus.addSource(isNetworkTagObserve) { syncStatus.value = isSyncInProgress() }
    }

    private fun isSyncInProgress(): Boolean = networkSyncInProgress.value ?: false && isNetworkTagObserve.value ?: false

    private fun collectGattSyncStatus() {
        viewModelScope.launch {
            gattInteractor.syncStatusFlow.collect {
                Timber.d("syncStatusFlow collected $it")
                it?.let { gattStatus ->
                    if (gattStatus.sensorId == sensorId) {
                        when (gattStatus.syncProgress) {
                            SyncProgress.STILL, SyncProgress.DONE -> {
                                _gattSyncInProgress.value = false
                                _gattSyncStatus.value = UiText.EmptyString
                            }
                            SyncProgress.CONNECTING -> {
                                _gattSyncInProgress.value = true
                                _gattSyncStatus.value = UiText.StringResource(R.string.connecting)
                            }
                            SyncProgress.CONNECTED, SyncProgress.READING_INFO -> {
                                _gattSyncInProgress.value = true
                                _gattSyncStatus.value = UiText.StringResource(R.string.connected_reading_info)
                            }
                            SyncProgress.DISCONNECTED -> {
                                _gattSyncInProgress.value = false
                                _gattSyncStatus.value = UiText.EmptyString
                                _event.emit(SyncProgress.DISCONNECTED)
                                resetGattStatus()
                            }
                            SyncProgress.READING_DATA -> {
                                _gattSyncInProgress.value = true
                                if (gattStatus.syncedDataPoints > 0) {
                                    _gattSyncStatus.value = UiText.StringResourceWithArgs(R.string.reading_history_x, arrayOf(gattStatus.syncedDataPoints))
                                } else {
                                    _gattSyncStatus.value = UiText.StringResource(R.string.reading_history)
                                }
                            }
                            SyncProgress.SAVING_DATA -> {
                                _gattSyncInProgress.value = true
                                _gattSyncStatus.value =  if (gattStatus.readDataSize > 0) {
                                    UiText.StringResourceWithArgs(R.string.data_points_read, arrayOf(gattStatus.readDataSize))
                                } else {
                                    UiText.StringResource(R.string.no_new_data_points)
                                }
                            }
                            SyncProgress.NOT_SUPPORTED -> {
                                _gattSyncInProgress.value = false
                                _gattSyncStatus.value = UiText.EmptyString
                                _event.emit(SyncProgress.NOT_SUPPORTED)
                                resetGattStatus()
                            }
                            SyncProgress.NOT_FOUND -> {
                                _gattSyncInProgress.value = false
                                _gattSyncStatus.value = UiText.EmptyString
                                _event.emit(SyncProgress.NOT_FOUND)
                                resetGattStatus()
                            }
                            SyncProgress.ERROR -> {
                                _gattSyncInProgress.value = false
                                _gattSyncStatus.value = UiText.EmptyString
                                _event.emit(SyncProgress.ERROR)
                                resetGattStatus()
                            }
                        }
                    }
                }
            }
        }
    }

    fun isShowGraph(isShow: Boolean) {
        showGraph = isShow
    }

    fun disconnectGatt() {
        tagEntry.value?.let { tag ->
            gattInteractor.disconnect(tag.id)
        }
    }

    fun syncGatt() {
        tagEntry.value?.let { tag ->
            var syncFrom = tag.lastSync
            val historyLength = Date(Date().time - 1000 * 60 * 60 * 24 * GlobalSettings.historyLengthDays)
            if (syncFrom == null || syncFrom.before(historyLength)) {
                syncFrom = historyLength
            }
            Timber.d("sync logs from: %s", syncFrom)
            gattInteractor.readLogs(tag.id, syncFrom)
        }
    }

    fun resetGattStatus() {
        gattInteractor.resetGattStatus(sensorId)
    }

    fun removeTagData() {
        sensorHistoryRepository.removeForSensor(sensorId)
        tagDetailsInteractor.clearLastSync(sensorId)
        viewModelScope.launch {
            _clearChart.emit(true)
        }
    }

    fun tagSelected(selectedTag: RuuviTag?) {
        selected = sensorId == selectedTag?.id
    }

    fun getTagInfo() {
        ioScope.launch {
            Timber.d("getTagInfo $sensorId")
            getTagEntryData(sensorId)
            if (showGraph && selected) getGraphData(sensorId)
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
            val status = networkInteractor.getSensorNetworkStatus(tagId)
            tagDetailsInteractor
                .getTagById(tagId)
                ?.let {
                    withContext(Dispatchers.Main) {
                        _tagEntry.value = it
                        networkStatus.value = status
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

    fun exportToCsv(): Uri? = csvExporter.toCsv(sensorId)

    fun setViewPeriod(periodDays: Int) {
        appSettingsInteractor.setGraphViewPeriod(periodDays)
        _chartViewPeriod.value = getGraphViewPeriod()
    }

    private fun getGraphViewPeriod() = Days.getInstance(appSettingsInteractor.getGraphViewPeriod())

    override fun onCleared() {
        super.onCleared()
        Timber.d("TagViewModel cleared!")
    }

    fun refreshStatus() {
        Timber.d("refreshStatus")
        _chartViewPeriod.value = getGraphViewPeriod()
    }
}