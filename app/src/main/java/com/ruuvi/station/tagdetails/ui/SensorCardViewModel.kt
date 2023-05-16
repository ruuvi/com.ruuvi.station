package com.ruuvi.station.tagdetails.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruuvi.station.R
import com.ruuvi.station.app.preferences.GlobalSettings
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.app.ui.UiText
import com.ruuvi.station.bluetooth.domain.BluetoothGattInteractor
import com.ruuvi.station.bluetooth.model.SyncProgress
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.network.domain.NetworkDataSyncInteractor
import com.ruuvi.station.settings.domain.AppSettingsInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.TagInteractor
import com.ruuvi.station.tagdetails.domain.TagDetailsInteractor
import com.ruuvi.station.tagsettings.domain.CsvExporter
import com.ruuvi.station.util.Days
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

class SensorCardViewModel(
    private val arguments: SensorCardViewModelArguments,
    private val tagInteractor: TagInteractor,
    private val tagDetailsInteractor: TagDetailsInteractor,
    private val networkDataSyncInteractor: NetworkDataSyncInteractor,
    private val appSettingsInteractor: AppSettingsInteractor,
    private val preferencesRepository: PreferencesRepository,
    private val gattInteractor: BluetoothGattInteractor,
    private val sensorHistoryRepository: SensorHistoryRepository,
    private val csvExporter: CsvExporter,
    ): ViewModel() {

    val sensorsFlow: Flow<List<RuuviTag>> = flow {
        while (true) {
            emit(tagInteractor.getTags())
            delay(1000)
        }
    }.flowOn(Dispatchers.IO)

    private val _selectedIndex = MutableStateFlow<Int>(0)
    val selectedIndex: StateFlow<Int> = _selectedIndex

    private val _chartViewPeriod = MutableStateFlow<Days>(getGraphViewPeriod())
    val chartViewPeriod: StateFlow<Days> = _chartViewPeriod

    private val _chartCleared = MutableSharedFlow<String>()
    private val chartCleared: SharedFlow<String> = _chartCleared

    private val _showCharts = MutableStateFlow<Boolean> (arguments.showChart)
    val showCharts: SharedFlow<Boolean> = _showCharts

    val syncInProgress = networkDataSyncInteractor.syncInProgressFlow

    fun getSensorHistory(sensorId: String): List<TagSensorReading> {
        return tagDetailsInteractor.getTagReadings(sensorId)
    }

    fun getChartCleared(sensorId: String):Flow<String> = chartCleared.filter { it == sensorId }

    fun getGattEvents(sensorId: String): Flow<SyncStatus> = flow{
        gattInteractor.syncStatusFlow.filter { it?.sensorId == sensorId }.collect{ status ->
            Timber.d("getGattEvents gattInteractor.syncStatusFlow $status")
            status?.let { gattStatus ->
                if (gattStatus.sensorId == sensorId) {
                    when (gattStatus.syncProgress) {
                        SyncProgress.STILL, SyncProgress.DONE -> {
                            emit(
                                SyncStatus(
                                    sensorId = gattStatus.sensorId,
                                    syncProgress = gattStatus.syncProgress,
                                    syncInProgress = gattStatus.syncProgress.syncInProgress,
                                    statusMessage = UiText.EmptyString
                                )
                            )
                        }
                        SyncProgress.NOT_SUPPORTED, SyncProgress.NOT_FOUND,
                        SyncProgress.ERROR, SyncProgress.DISCONNECTED -> {
                            emit(
                                SyncStatus(
                                    sensorId = gattStatus.sensorId,
                                    syncProgress = gattStatus.syncProgress,
                                    syncInProgress = gattStatus.syncProgress.syncInProgress,
                                    statusMessage = UiText.EmptyString
                                )
                            )
                            resetGattStatus(sensorId)
                        }
                        SyncProgress.CONNECTING -> {
                            emit(
                                SyncStatus(
                                    sensorId = gattStatus.sensorId,
                                    syncProgress = gattStatus.syncProgress,
                                    syncInProgress = gattStatus.syncProgress.syncInProgress,
                                    statusMessage = UiText.StringResource(R.string.connecting)
                                )
                            )
                        }
                        SyncProgress.CONNECTED, SyncProgress.READING_INFO -> {
                            emit(
                                SyncStatus(
                                    sensorId = gattStatus.sensorId,
                                    syncProgress = gattStatus.syncProgress,
                                    syncInProgress = gattStatus.syncProgress.syncInProgress,
                                    statusMessage = UiText.StringResource(R.string.connected_reading_info)
                                )
                            )
                        }
                        SyncProgress.READING_DATA -> {
                            val message = if (gattStatus.syncedDataPoints > 0) {
                                UiText.StringResourceWithArgs(R.string.reading_history_x, arrayOf(gattStatus.syncedDataPoints))
                            } else {
                                UiText.StringResource(R.string.reading_history)
                            }
                            emit(
                                SyncStatus(
                                    sensorId = gattStatus.sensorId,
                                    syncProgress = gattStatus.syncProgress,
                                    syncInProgress = gattStatus.syncProgress.syncInProgress,
                                    statusMessage = message
                                )
                            )
                        }
                        SyncProgress.SAVING_DATA -> {
                            val message = if (gattStatus.readDataSize > 0) {
                                UiText.StringResourceWithArgs(R.string.data_points_read, arrayOf(gattStatus.readDataSize))
                            } else {
                                UiText.StringResource(R.string.no_new_data_points)
                            }
                            emit(
                                SyncStatus(
                                    sensorId = gattStatus.sensorId,
                                    syncProgress = gattStatus.syncProgress,
                                    syncInProgress = gattStatus.syncProgress.syncInProgress,
                                    statusMessage = message
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun disconnectGatt(sensorId: String) {
        gattInteractor.disconnect(sensorId)
    }

    fun resetGattStatus(sensorId: String) {
        gattInteractor.resetGattStatus(sensorId)
    }

    fun syncGatt(sensorId: String) {
        val sensor = tagDetailsInteractor.getTagById(sensorId)
        sensor?.let { sensor ->
            var syncFrom = sensor.lastSync
            val historyLength = Date(Date().time - 1000 * 60 * 60 * 24 * GlobalSettings.historyLengthDays)
            if (syncFrom == null || syncFrom.before(historyLength)) {
                syncFrom = historyLength
            }
            Timber.d("sync logs from: %s", syncFrom)
            gattInteractor.readLogs(sensorId, syncFrom)
        }
    }

    fun setViewPeriod(periodDays: Int) {
        appSettingsInteractor.setGraphViewPeriod(periodDays)
        _chartViewPeriod.value = getGraphViewPeriod()
    }

    fun exportToCsv(sensorId: String): Uri? = csvExporter.toCsv(sensorId)

    private fun getGraphViewPeriod() = Days.getInstance(appSettingsInteractor.getGraphViewPeriod())

    fun shouldSkipGattSyncDialog() = preferencesRepository.getDontShowGattSync()

    fun removeTagData(sensorId: String) {
        sensorHistoryRepository.removeForSensor(sensorId)
        tagDetailsInteractor.clearLastSync(sensorId)
        viewModelScope.launch {
            _chartCleared.emit(sensorId)
        }
    }

    fun refreshStatus() {
        Timber.d("refreshStatus")
        _chartViewPeriod.value = getGraphViewPeriod()
    }

    fun dontShowGattSyncDescription() {
        preferencesRepository.setDontShowGattSync(true)
    }

    fun setShowCharts(showCharts: Boolean) {
        _showCharts.value = showCharts
    }

    init {
        if (arguments.sensorId != null) {
            val sensors = tagInteractor.getTags()
            _selectedIndex.value = sensors.indexOfFirst { it.id == arguments.sensorId }
        }
    }

}

data class SensorCardViewModelArguments(
    val sensorId: String? = null,
    val showChart: Boolean = false
)

data class SyncStatus (
    val sensorId: String,
    val syncProgress: SyncProgress = SyncProgress.STILL,
    val syncInProgress: Boolean,
    val statusMessage: UiText
)