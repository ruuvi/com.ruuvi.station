package com.ruuvi.station.tagdetails.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.mikephil.charting.data.Entry
import com.ruuvi.gateway.tester.nfc.model.SensorNfсScanInfo
import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlarmType
import com.ruuvi.station.app.preferences.GlobalSettings
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.app.ui.UiText
import com.ruuvi.station.bluetooth.domain.BluetoothGattInteractor
import com.ruuvi.station.bluetooth.model.SyncProgress
import com.ruuvi.station.database.domain.AlarmRepository
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.export.CsvExporter
import com.ruuvi.station.export.XlsxExporter
import com.ruuvi.station.graph.model.ChartContainer
import com.ruuvi.station.history.HistoryViewportController
import com.ruuvi.station.history.HistorySelection
import com.ruuvi.station.history.HistoryRange
import com.ruuvi.station.network.domain.NetworkHistoryInteractor
import com.ruuvi.station.network.domain.NetworkDataSyncInteractor
import com.ruuvi.station.nfc.domain.NfcResultInteractor
import com.ruuvi.station.settings.domain.AppSettingsInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.TagInteractor
import com.ruuvi.station.tagdetails.domain.TagDetailsInteractor
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.domain.aqi.AQI
import com.ruuvi.station.units.model.UnitType
import com.ruuvi.station.units.model.UnitType.*
import com.ruuvi.station.util.Period
import com.ruuvi.station.vico.model.ChartData
import com.ruuvi.station.vico.model.Segment
import com.ruuvi.station.vico.model.SegmentType
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
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
    private val xlsxExporter: XlsxExporter,
    private val nfcResultInteractor: NfcResultInteractor,
    private val alarmRepository: AlarmRepository,
    private val unitsConverter: UnitsConverter,
    private val networkHistoryInteractor: NetworkHistoryInteractor,
    private val now: () -> Long = System::currentTimeMillis
    ): ViewModel() {

    val sensorsFlow: Flow<List<RuuviTag>> = flow {
        while (true) {
            emit(tagInteractor.getTags())
            delay(1000)
        }
    }.flowOn(Dispatchers.IO)

    private val _selectedSensor = MutableStateFlow<String?>(null)
    val selectedSensor: StateFlow<String?> = _selectedSensor

    private val _chartViewPeriod = MutableStateFlow<Period>(getGraphViewPeriod())
    val chartViewPeriod: StateFlow<Period> = _chartViewPeriod

    private val _historySelection = MutableStateFlow<HistorySelection>(HistorySelection.Rolling(_chartViewPeriod.value.value))
    val historySelection = _historySelection.asStateFlow()
    private val resolvedWindow = MutableStateFlow(_historySelection.value to _historySelection.value.resolve(now()))
    val historySyncState = networkHistoryInteractor.state
    private val retryHistory = MutableStateFlow(0)
    private var visibleHistoryJob: Job? = null
    private val viewportController = HistoryViewportController()

    fun setHistoryViewport(sensorId: String, range: HistoryRange?, finished: Boolean) {
        viewportController.update(sensorId, _historySelection.value, range, finished)
    }

    /** The pager owns this collection and runs it only while the selected history view is resumed. */
    suspend fun observeHistory(sensorId: String) {
        combine(_historySelection, retryHistory) { selection, retry -> selection to retry }
            .collectLatest { (selection, _) ->
                coroutineScope {
                    visibleHistoryJob = currentCoroutineContext().job
                    val now = now()
                    val initial = selection.resolve(now)
                    resolvedWindow.value = selection to initial
                    val live = initial.endExclusiveMillis == now
                    launch {
                        var first = true
                        do {
                            networkHistoryInteractor.syncHistory(sensorId, resolvedWindow.value.second, revalidateHistorical = first)
                            first = false
                            if (live) delay(NetworkHistoryInteractor.LIVE_REFRESH_MILLIS)
                        } while (live && isActive)
                    }
                    if (live) {
                        while (isActive) {
                            delay(1000)
                            resolvedWindow.value = selection to selection.resolve(now())
                        }
                    } else {
                        awaitCancellation()
                    }
                }
            }
    }

    fun setHistoryDates(startDateUtc: Long, endDateUtc: Long) {
        _historySelection.value = HistorySelection.Custom(startDateUtc, endDateUtc)
    }

    fun retryCloudHistory() { retryHistory.value += 1 }


    private val _chartCleared = MutableSharedFlow<String>()
    private val chartCleared: SharedFlow<String> = _chartCleared

    private val _showCharts = MutableStateFlow<Boolean> (arguments.showChart)
    val showCharts: SharedFlow<Boolean> = _showCharts

    val syncInProgress = networkDataSyncInteractor.syncInProgressFlow

    val graphDrawDots = preferencesRepository.graphDrawDots()

    private val _showChartStats = MutableStateFlow<Boolean>(preferencesRepository.getShowChartStats())
    val showChartStats: StateFlow<Boolean> = _showChartStats

    private val _newChartsUI = MutableStateFlow<Boolean>(preferencesRepository.isNewChartsUI())
    val newChartsUI: StateFlow<Boolean> = _newChartsUI

    private val _chartSizeLevel = MutableStateFlow<Int>(preferencesRepository.getChartSizeLevel())
    val chartSizeLevel: StateFlow<Int> = _chartSizeLevel

    private val _scrollToChartEvent = Channel<UnitType>(Channel.BUFFERED)
    val scrollToChartEvent = _scrollToChartEvent.receiveAsFlow()


    fun getChartData(sensorId: String, unitType: UnitType, hours: Int): Flow<ChartData> = flow {
        val range = HistorySelection.Rolling(hours).resolve(now())
        val sampled = tagDetailsInteractor.sampleHistory(sensorId, range, listOf(unitType), ::getUnitValue).getValue(unitType)
        val segments = sampled.points.groupBy { it.segment }.values.map { points ->
            Segment(points.map { it.timestamp }, points.map { it.value },
                if (points.size == 1) SegmentType.Single else SegmentType.Solid)
        }
        emit(ChartData(segments, sampled.statistics?.minimum ?: 0.0, sampled.statistics?.maximum ?: 0.0))
    }.flowOn(Dispatchers.IO)

    fun getUnitValue(item: TagSensorReading, unitType: UnitType): Double? {
        val entryValue = when (unitType) {
            is TemperatureUnit -> item.temperature?.let { temperature ->
                unitsConverter.getTemperatureValue(temperature, unitType)
            }
            is HumidityUnit -> item.humidity?.let { humidity ->
                unitsConverter.getHumidityValue(humidity, item.temperature, unitType)
            }
            is PressureUnit -> item.pressure?.let { pressure ->
                unitsConverter.getPressureValue(pressure, unitType)
            }
            is BatteryVoltageUnit -> item.voltage
            is Acceleration.GForceX -> item.accelX
            is Acceleration.GForceY -> item.accelY
            is Acceleration.GForceZ -> item.accelZ
            is SignalStrengthUnit -> item.rssi
            is AirQuality -> AQI.getAQI(item.pm25, item.co2).score
            is CO2 -> item.co2
            is VOC -> item.voc
            is NOX -> item.nox
            is PM.PM10 -> item.pm1
            is PM.PM25 -> item.pm25
            is PM.PM40 -> item.pm4
            is PM.PM100 -> item.pm10
            is Luminosity -> item.luminosity
            is SoundAvg -> item.dBaAvg
            is SoundPeak -> item.dBaPeak
            is MovementUnit -> item.movementCounter
            else -> null
        }
        return entryValue?.toDouble()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun historyUpdater(sensorId: String): Flow<MutableList<ChartContainer>> = flow {
        var originSelection: HistorySelection? = null
        var origin = 0L
        val knownUnits = mutableSetOf<UnitType>()
        emitAll(combine(
            resolvedWindow.map { it.first }.distinctUntilChanged(),
            viewportController.updates
        ) { selection, viewport ->
            selection to viewport.range.takeIf { viewport.sensorId == sensorId && viewport.selection == selection }
        }.distinctUntilChanged().transformLatest { (selection, viewport) ->
            if (selection != originSelection) {
                originSelection = selection
                origin = resolvedWindow.value.second.startMillis
                knownUnits.clear()
            }
            var previousKey: Any? = null
            while (currentCoroutineContext().isActive) {
                val window = resolvedWindow.value
                if (window.first != selection) return@transformLatest
                val range = viewport?.intersect(window.second) ?: window.second
                val sensor = tagDetailsInteractor.getTagById(sensorId)
                val units = sensor?.displayOrder?.filter { it !is MovementUnit }.orEmpty()
                val alarms = getActiveAlarms(sensorId)
                // A rolling axis advances once a minute even when no measurements arrive.
                val rollingMinute = if (viewport == null && window.second.endExclusiveMillis >= now() - 2000)
                    window.second.endExclusiveMillis / 60_000 else null
                val key = listOf(sensorHistoryRepository.revision(sensorId), tagDetailsInteractor.readingOptions(sensorId),
                    units, alarms, rollingMinute)
                if (key != previousKey) {
                    val sampled = tagDetailsInteractor.sampleHistory(sensorId, range, units, ::getUnitValue)
                    currentCoroutineContext().ensureActive()
                    val chartContainers = mutableListOf<ChartContainer>()
                    for (unit in units) {
                        val series = sampled.getValue(unit)
                        if (series.points.isEmpty() && unit !in knownUnits) continue
                        knownUnits.add(unit)
                        val alarmLimit = when (unit) {
                            is TemperatureUnit -> alarms.firstOrNull{ it -> it.alarmType == AlarmType.TEMPERATURE }?.let {
                                unitsConverter.getTemperatureValue(it.min, unit) to unitsConverter.getTemperatureValue(it.max, unit)
                            }
                            is HumidityUnit.Relative -> alarms.firstOrNull { it.alarmType == AlarmType.HUMIDITY }
                                ?.let { it.min to it.max }
                            is HumidityUnit.Absolute -> alarms.firstOrNull { it.alarmType == AlarmType.ABSOLUTE_HUMIDITY }
                                ?.let { it.min to it.max }
                            is HumidityUnit.DewPoint -> alarms.firstOrNull { it.alarmType == AlarmType.DEW_POINT }
                                ?.let {
                                    unitsConverter.getTemperatureValue(it.min) to unitsConverter.getTemperatureValue(it.max)
                                }
                            is PressureUnit -> alarms.firstOrNull { it.alarmType == AlarmType.PRESSURE }
                                ?.let {
                                    unitsConverter.getPressureValue(it.min, unit) to unitsConverter.getPressureValue(it.max, unit)
                                }
                            is CO2 -> alarms.firstOrNull { it.alarmType == AlarmType.CO2 }
                                ?.let { it.min to it.max }
                            is VOC -> alarms.firstOrNull { it.alarmType == AlarmType.VOC }
                                ?.let { it.min to it.max }
                            is NOX -> alarms.firstOrNull { it.alarmType == AlarmType.NOX }
                                ?.let { it.min to it.max }
                            is PM.PM10 -> alarms.firstOrNull { it.alarmType == AlarmType.PM10 }
                                ?.let { it.min to it.max }
                            is PM.PM25 -> alarms.firstOrNull { it.alarmType == AlarmType.PM25 }
                                ?.let { it.min to it.max }
                            is PM.PM40 -> alarms.firstOrNull { it.alarmType == AlarmType.PM40 }
                                ?.let { it.min to it.max }
                            is PM.PM100 -> alarms.firstOrNull { it.alarmType == AlarmType.PM100 }
                                ?.let { it.min to it.max }
                            is Luminosity -> alarms.firstOrNull { it.alarmType == AlarmType.LUMINOSITY }
                                ?.let { it.min to it.max }
                            is SoundAvg -> alarms.firstOrNull { it.alarmType == AlarmType.SOUND }
                                ?.let { it.min to it.max }
                            is SignalStrengthUnit -> alarms.firstOrNull { it.alarmType == AlarmType.RSSI }
                                ?.let { it.min to it.max }
                            is AirQuality -> alarms.firstOrNull { it.alarmType == AlarmType.AQI }
                                ?.let { it.min to it.max }
                            is BatteryVoltageUnit -> alarms.firstOrNull { it.alarmType == AlarmType.BATTERY_VOLTAGE }
                                ?.let { it.min to it.max }
                            else -> null
                        }

                        chartContainers.add(ChartContainer(
                            unitType = unit,
                            data = series.points.map { Entry((it.timestamp - origin).toFloat(), it.value.toFloat(), it) }.toMutableList(),
                            limits = alarmLimit, from = origin, to = window.second.endExclusiveMillis,
                            axisStart = window.second.startMillis, statistics = series.statistics, uiComponent = null
                        ))
                    }
                    emit(chartContainers)
                    previousKey = key
                }
                delay(1000)
            }
        })
    }.flowOn(Dispatchers.IO)

    fun getChartCleared(sensorId: String):Flow<String> = chartCleared.filter { it == sensorId }

    fun changeShowChartStats() {
        preferencesRepository.setShowChartStats(!preferencesRepository.getShowChartStats())
        _showChartStats.value = preferencesRepository.getShowChartStats()
    }

    fun scrollToChart(type: UnitType) {
        _showCharts.value = true
        viewModelScope.launch {
            _scrollToChartEvent.send(type)
        }
    }

    fun increaseChartSize() {
        val nextLevel = when (preferencesRepository.getChartSizeLevel()) {
            PreferencesRepository.CHART_SIZE_LEVEL_NORMAL -> PreferencesRepository.CHART_SIZE_LEVEL_INCREASED
            PreferencesRepository.CHART_SIZE_LEVEL_INCREASED -> PreferencesRepository.CHART_SIZE_LEVEL_MAX
            else -> PreferencesRepository.CHART_SIZE_LEVEL_MAX
        }
        preferencesRepository.setChartSizeLevel(nextLevel)
        _chartSizeLevel.value = nextLevel
    }

    fun decreaseChartSize() {
        val nextLevel = when (preferencesRepository.getChartSizeLevel()) {
            PreferencesRepository.CHART_SIZE_LEVEL_MAX -> PreferencesRepository.CHART_SIZE_LEVEL_INCREASED
            PreferencesRepository.CHART_SIZE_LEVEL_INCREASED -> PreferencesRepository.CHART_SIZE_LEVEL_NORMAL
            else -> PreferencesRepository.CHART_SIZE_LEVEL_NORMAL
        }
        preferencesRepository.setChartSizeLevel(nextLevel)
        _chartSizeLevel.value = nextLevel
    }

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
            val historyLength = Date(Date().time - GlobalSettings.historyLengthMillis)
            if (syncFrom == null || syncFrom.before(historyLength)) {
                syncFrom = historyLength
            }
            Timber.d("sync logs from: %s", syncFrom)
            gattInteractor.readLogs(sensorId, syncFrom)
        }
    }

    fun setViewPeriod(periodDays: Int) {
        appSettingsInteractor.setGraphViewPeriod(periodDays)
        _chartViewPeriod.value = Period.getInstance(periodDays)
        _historySelection.value = HistorySelection.Rolling(periodDays)
    }

    fun exportToCsv(sensorId: String): Uri? = csvExporter.toCsv(sensorId)

    fun exportToXlsx(sensorId: String): Uri? = xlsxExporter.exportToXlsx(sensorId)

    private fun getGraphViewPeriod() = Period.getInstance(appSettingsInteractor.getGraphViewPeriod())

    fun shouldSkipGattSyncDialog() = preferencesRepository.getDontShowGattSync()

    fun removeTagData(sensorId: String) {
        viewModelScope.launch {
            visibleHistoryJob?.cancelAndJoin()
            withContext(Dispatchers.IO) {
                sensorHistoryRepository.removeForSensor(sensorId)
                tagDetailsInteractor.clearLastSync(sensorId)
            }
            _chartCleared.emit(sensorId)
        }
    }

    fun refreshStatus() {
        Timber.d("refreshStatus")
        _chartViewPeriod.value = getGraphViewPeriod()
        if (_historySelection.value is HistorySelection.Rolling) {
            _historySelection.value = HistorySelection.Rolling(_chartViewPeriod.value.value)
        }
    }

    fun dontShowGattSyncDescription() {
        preferencesRepository.setDontShowGattSync(true)
    }

    fun setShowCharts(showCharts: Boolean) {
        _showCharts.value = showCharts
    }

    fun getNfcScanResponse(scanInfo: SensorNfсScanInfo) = nfcResultInteractor.getNfcScanResponse(scanInfo)

    fun addSensor(sensorId: String) {
        tagInteractor.makeSensorFavorite(sensorId)
    }

    fun getActiveAlarms(sensorId: String) = alarmRepository.getActiveAlarms(sensorId)

    fun saveSelected(sensorId: String) {
        _selectedSensor.value = sensorId
    }

    fun getIndex(sensorId: String): Int {
        val sensors = tagInteractor.getTags()
        val index = sensors.indexOfFirst { it.id == sensorId }
        return if (index == - 1) 0 else index
    }

    init {
        if (arguments.sensorId != null) {
            val sensors = tagInteractor.getTags()
            _selectedSensor.value = sensors.firstOrNull { it.id == arguments.sensorId }?.id
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
