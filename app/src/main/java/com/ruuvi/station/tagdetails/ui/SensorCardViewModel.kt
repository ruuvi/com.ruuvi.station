package com.ruuvi.station.tagdetails.ui

import android.net.Uri
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost
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
import com.ruuvi.station.network.domain.NetworkDataSyncInteractor
import com.ruuvi.station.nfc.domain.NfcResultInteractor
import com.ruuvi.station.settings.domain.AppSettingsInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.TagInteractor
import com.ruuvi.station.tag.domain.isAir
import com.ruuvi.station.tagdetails.domain.TagDetailsInteractor
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.domain.aqi.AQI
import com.ruuvi.station.units.model.UnitType
import com.ruuvi.station.units.model.UnitType.*
import com.ruuvi.station.util.Period
import com.ruuvi.station.vico.model.ChartData
import com.ruuvi.station.vico.model.Segment
import com.ruuvi.station.vico.model.SegmentType
import com.ruuvi.station.history.*
import com.ruuvi.station.graph.HistoryChartData
import com.ruuvi.station.export.HistoryRecordExporter
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
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
    private val progressiveHistory: ProgressiveHistoryRepository? = null,
    private val historyExporter: HistoryRecordExporter? = null
    ): ViewModel() {

    private val historyController = HistoryController(viewModelScope, progressiveHistory?.previews)
    val historyState = historyController.state
    private val _historyDays = MutableStateFlow(0)
    val historyDays: StateFlow<Int> = _historyDays
    private val customHistory = MutableStateFlow<HistoryWindow?>(null)
    val customHistorySelection: StateFlow<HistoryWindow?> = customHistory
    private val _historyExporting = MutableStateFlow(false)
    val historyExporting: StateFlow<Boolean> = _historyExporting
    private val historyWake = Channel<Unit>(Channel.CONFLATED)
    private var historyExportJob: Job? = null
    private var historyAccess: ProgressiveHistoryRepository.Access? = null
    private var activeHistorySensor: String? = null
    private var historySetupFailed = MutableStateFlow(false)
    val historySetupError: StateFlow<Boolean> = historySetupFailed
    fun usesLongerHistory(sensor: RuuviTag) = progressiveHistory?.enabled() == true && sensor.networkSensor
    fun useHistoryDurationPresets() = progressiveHistory?.presets() ?: true

    /** Collected only by the selected, RESUMED history screen. Cancels on navigation/background. */
    suspend fun observeLongerHistory(sensorId: String) {
        val repository = progressiveHistory ?: return
        activeHistorySensor = sensorId
        var firstAccess = true
        try {
            while (true) {
                currentCoroutineContext().ensureActive()
                var waitMillis = 1000L
                try {
                    val sensor = withContext(Dispatchers.IO) { tagDetailsInteractor.getTagById(sensorId) }
                        ?: throw HistoryFailure.Access()
                    if (!usesLongerHistory(sensor)) throw HistoryFailure.Access()
                    // Display locally authorized history before a periodic allowance check.
                    val access = repository.access(sensorId, cachedFirst = firstAccess)
                    firstAccess = false
                    if (historyAccess != access) historyExportJob?.cancel()
                    historyAccess = access
                    _historyDays.value = access.days
                    val selectableHours = (if (access.days > 0) access.days else GlobalSettings.historyLengthDays) * 24
                    if (chartViewPeriod.value.value > selectableHours) setViewPeriod(selectableHours)
                    historySetupFailed.value = false
                    if (access.days > 0) {
                        val now = System.currentTimeMillis() / 1000
                        val allowance = HistoryWindow(maxOf(0, now - access.days * 86400L), now)
                        val selected = customHistory.value?.intersect(allowance)
                        if (customHistory.value != null && selected == null) customHistory.value = null
                        val hours = if (chartViewPeriod.value is Period.All) access.days * 24
                            else chartViewPeriod.value.value.coerceIn(1, access.days * 24)
                        val window = selected ?: HistoryWindow(maxOf(0, now - hours * 3600L), now)
                        val units = sensor.displayOrder.filter { it !is MovementUnit }
                        val metrics = units.map { it.javaClass.name }
                        // Dew point follows the temperature preference even when temperature is hidden.
                        val valueRevision = if (units.any { it is HumidityUnit.DewPoint }) unitsConverter.getTemperatureUnit().javaClass.name else ""
                        val selectionKey = customHistory.value?.toString() ?: "rolling:$hours"
                        val key = "${access.scope}|$sensorId|${access.days}|${access.revision}|$valueRevision|$selectionKey|${metrics.joinToString()}"
                        historyController.start(sensorId, key, window, rolling = selected == null, previewScope = access.scope) { bounds, refresh ->
                            repository.snapshots(access, bounds, metrics, refresh, previewKey = key.takeIf { bounds.seconds == window.seconds }, valueRevision = valueRevision) { reading ->
                                units.mapNotNull { unit -> getUnitValue(reading, unit)?.takeIf { it.isFinite() }?.let { unit.javaClass.name to it } }.toMap()
                            }
                        }
                    } else historyController.stop(clear = true)
                } catch (error: Exception) {
                    currentCoroutineContext().ensureActive()
                    historySetupFailed.value = true
                    if (error is HistoryFailure.Access || error is HistoryFailure.Http && error.status in listOf(401, 403)) {
                        historyAccess?.let { repository.remove(it) }
                        historyController.stop(clear = true)
                        historyExportJob?.cancel()
                        historyAccess = null; _historyDays.value = 0
                    }
                    waitMillis = 30000L
                }
                withTimeoutOrNull(waitMillis) { historyWake.receive() }
            }
        } finally {
            if (activeHistorySensor == sensorId) {
                activeHistorySensor = null
                historyController.stop()
                historyExportJob?.cancel()
            }
        }
    }

    fun selectCustomHistory(window: HistoryWindow) {
        historyController.stop(clear = true)
        customHistory.value = window
        historyWake.trySend(Unit)
        historyExportJob?.cancel()
    }
    fun retryLongerHistory() { historySetupFailed.value = false; historyWake.trySend(Unit); historyController.refresh() }
    fun cancelHistoryExport() { historyExportJob?.cancel() }

    private fun progressiveChartData(sensorId: String): Flow<MutableList<ChartContainer>> = historyState
        .filter { it.sensor == sensorId && it.timeline != null }
        .map { state ->
        val sensor = tagDetailsInteractor.getTagById(sensorId) ?: return@map mutableListOf<ChartContainer>()
        val timeline = state.timeline!!
        val origin = timeline.since * 1000
        val alarms = getActiveAlarms(sensorId)
        sensor.displayOrder.filter { it !is MovementUnit }.map { unit ->
            val metric = unit.javaClass.name
            val groups = state.groups(metric).map { group -> group.map { point ->
                Entry((point.timestamp * 1000 - origin).toFloat(), point.value.toFloat(), point)
            } }
            val model = HistoryChartData(state, metric, groups,
                onInteraction = { historyController.interaction(it) },
                onViewport = { from, until -> historyController.move(from / 1000, until / 1000) })
            ChartContainer(unit, null, groups.flatten().toMutableList(), when (unit) {
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
                        }, origin,
                timeline.until * 1000, model)
        }.toMutableList()
    }.flowOn(Dispatchers.IO)

    val sensorsFlow: StateFlow<List<RuuviTag>> = flow {
        while (true) {
            emit(tagInteractor.getTags())
            delay(1000)
        }
    }.flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(1000),
            initialValue = tagInteractor.getCachedTags().let { sensors ->
                if (sensors.any { it.id == arguments.sensorId }) {
                    sensors
                } else {
                    emptyList()
                }
            },
        )

    val initialSensorId: String? get() = arguments.sensorId

    private val _selectedSensor = MutableStateFlow<String?>(arguments.sensorId)
    val selectedSensor: StateFlow<String?> = _selectedSensor

    private val _chartViewPeriod = MutableStateFlow<Period>(getGraphViewPeriod())
    val chartViewPeriod: StateFlow<Period> = _chartViewPeriod

    private val _chartCleared = MutableSharedFlow<String>()
    private val chartCleared: SharedFlow<String> = _chartCleared

    val syncInProgress = networkDataSyncInteractor.syncInProgressFlow

    val graphDrawDots = preferencesRepository.graphDrawDots()

    val sensorDataTimeoutSeconds: Long
        get() = preferencesRepository.getDashboardCardTimeoutSeconds()

    private val _showChartStats = MutableStateFlow<Boolean>(preferencesRepository.getShowChartStats())
    val showChartStats: StateFlow<Boolean> = _showChartStats

    private val _newChartsUI = MutableStateFlow<Boolean>(preferencesRepository.isNewChartsUI())
    val newChartsUI: StateFlow<Boolean> = _newChartsUI

    private val _chartSizeLevel = MutableStateFlow<Int>(preferencesRepository.getChartSizeLevel())
    val chartSizeLevel: StateFlow<Int> = _chartSizeLevel

    private val _scrollToChartEvent = Channel<UnitType>(Channel.BUFFERED)
    val scrollToChartEvent = _scrollToChartEvent.receiveAsFlow()


    fun getChartData(sensorId: String, unitType: UnitType, hours: Int): Flow<ChartData> =
        flow<ChartData> {
            val history = tagDetailsInteractor.getTagReadings(sensorId, hours)
            val segments = mutableListOf<Segment>()
            var solidValues = mutableListOf<Double>()
            var solidTimestamps = mutableListOf<Long>()

            var firstPoint = true
            var previousTimestamp: Long? = null
            var previousValue: Double? = null

            var minValue = Double.MAX_VALUE
            var maxValue = Double.MIN_VALUE

            for (item in history) {
                val entryValue = getUnitValue(item, unitType)

                if (entryValue == null) {
                    continue
                }

                minValue = minValue.fastCoerceAtMost(entryValue)
                maxValue = maxValue.fastCoerceAtLeast(entryValue)

                val timestamp = item.createdAt.time

                if (firstPoint) {
                    solidValues += entryValue
                    solidTimestamps += timestamp
                    firstPoint = false
                } else {

                    if (timestamp - (previousTimestamp ?: timestamp) > 60 * 60 * 1000) {
                        if (solidValues.size > 0) {
                            val segmentType = if (solidValues.size == 1) SegmentType.Single else SegmentType.Solid
                            segments.add(
                                Segment(
                                    timestamps = solidTimestamps.toList(),
                                    values = solidValues.toList(),
                                    segmentType = segmentType
                                )
                            )
                        }
                        solidTimestamps = mutableListOf()
                        solidValues = mutableListOf()

                        segments.add(
                            Segment(
                                timestamps = listOf(previousTimestamp ?: timestamp, timestamp),
                                values = listOf(previousValue ?: entryValue, entryValue),
                                segmentType = SegmentType.Dotted
                            )
                        )
                    }
                    solidValues += entryValue
                    solidTimestamps += timestamp

                }

                previousTimestamp = timestamp
                previousValue = entryValue
            }
            if (solidValues.size > 0) {
                val segmentType = if (solidValues.size == 1) SegmentType.Single else SegmentType.Solid
                segments.add(Segment (
                    timestamps = solidTimestamps.toList(),
                    values = solidValues.toList(),
                    segmentType = segmentType
                ))
            }

            emit(ChartData(
                segments = segments,
                minValue = minValue,
                maxValue = maxValue
            ))
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

    fun historyUpdater(sensorId: String): Flow<MutableList<ChartContainer>> = historyDays.flatMapLatest { days ->
        val sensor = tagDetailsInteractor.getTagById(sensorId)
        if (days > 0 && sensor != null && usesLongerHistory(sensor)) progressiveChartData(sensorId)
        else legacyHistoryUpdater(sensorId)
    }

    private fun legacyHistoryUpdater(sensorId: String): Flow<MutableList<ChartContainer>> =
        flow<MutableList<ChartContainer>> {
            delay(200)
            while (true) {
                val history = tagDetailsInteractor.getTagReadings(sensorId)

                val ruuviTag = tagDetailsInteractor.getTagById(sensorId)


                if (history.isEmpty() || ruuviTag == null) {
                    emit(mutableListOf<ChartContainer>())
                    delay(1000)
                    continue
                }

                val from = if (chartViewPeriod.value is Period.All) {
                    history[0].createdAt.time
                } else {
                    Date().time - chartViewPeriod.value.value.coerceIn(1, 240) * 60 * 60 * 1000
                }
                Timber.d("historyUpdater $from")
                val to = Date().time
                val alarms = getActiveAlarms(sensorId)

//            if (history.isEmpty() ||
//                (tempChart?.uiComponent != null && tempChart.uiComponent.highestVisibleX >= (tempChart.uiComponent.data?.xMax ?: Float.MIN_VALUE))) {
//                history = freshHistory

                val displayOrder = ruuviTag.displayOrder.filter { it !is  MovementUnit }
                val datasetsByUnit: Map<UnitType, MutableList<Entry>> = displayOrder.associateWith { mutableListOf<Entry>() }

                history.forEach { item ->

                    val timestamp = (item.createdAt.time - from).toFloat()

                    for (unit in displayOrder) {
                        if (unit is MovementUnit) continue

                        val dataset = datasetsByUnit[unit]
                        dataset?.let {

                            val entryValue = when (unit) {
                                is TemperatureUnit -> item.temperature?.let { temperature ->
                                    unitsConverter.getTemperatureValue(temperature, unit)
                                }
                                is HumidityUnit -> item.humidity?.let { humidity ->
                                    unitsConverter.getHumidityValue(humidity, item.temperature, unit)
                                }
                                is PressureUnit -> item.pressure?.let { pressure ->
                                    unitsConverter.getPressureValue(pressure, unit)
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
                                else -> null
                            }

                            if (entryValue != null) {
                                dataset.add(Entry(timestamp, entryValue.toFloat()))
                            }

                        }
                    }
                }

                val chartContainers = mutableListOf<ChartContainer>()

                for (unit in displayOrder) {
                    val dataset = datasetsByUnit[unit]

                    if (!dataset.isNullOrEmpty()) {
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

                        chartContainers.add(
                            ChartContainer(
                                unitType = unit,
                                data = dataset,
                                limits = alarmLimit,
                                from = from,
                                to = to,
                                uiComponent = null
                            )
                        )
                    }
                }

                emit(chartContainers)
                Timber.d("historyUpdater emited ${chartContainers.size} containers")
                delay(1000)
            }
        }.flowOn(Dispatchers.IO)

    fun getChartCleared(sensorId: String):Flow<String> = chartCleared.filter { it == sensorId }

    fun changeShowChartStats() {
        preferencesRepository.setShowChartStats(!preferencesRepository.getShowChartStats())
        _showChartStats.value = preferencesRepository.getShowChartStats()
    }

    fun scrollToChart(type: UnitType) {
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
        gattInteractor.getSyncStatusFlow(sensorId).filterNotNull().collect{ status ->
            Timber.d("getGattEvents sensor $sensorId $status")
            status.let { gattStatus ->
                if (gattStatus.sensorId == sensorId) {
                    when (gattStatus.syncProgress) {
                        SyncProgress.STILL, SyncProgress.DONE -> {
                            emit(
                                SyncStatus(
                                    sensorId = gattStatus.sensorId,
                                    syncProgress = gattStatus.syncProgress,
                                    syncInProgress = gattStatus.syncProgress.syncInProgress,
                                    statusMessage = UiText.EmptyString,
                                    manualSync = gattStatus.manualSync
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
                                    statusMessage = UiText.EmptyString,
                                    manualSync = gattStatus.manualSync
                                )
                            )
                            resetGattStatus(sensorId, gattStatus.sessionId)
                        }

                        SyncProgress.CONNECTING -> {
                            emit(
                                SyncStatus(
                                    sensorId = gattStatus.sensorId,
                                    syncProgress = gattStatus.syncProgress,
                                    syncInProgress = gattStatus.syncProgress.syncInProgress,
                                    statusMessage = UiText.StringResource(R.string.connecting),
                                    manualSync = gattStatus.manualSync
                                )
                            )
                        }

                        SyncProgress.CONNECTED, SyncProgress.READING_INFO -> {
                            emit(
                                SyncStatus(
                                    sensorId = gattStatus.sensorId,
                                    syncProgress = gattStatus.syncProgress,
                                    syncInProgress = gattStatus.syncProgress.syncInProgress,
                                    statusMessage = UiText.StringResource(R.string.connected_reading_info),
                                    manualSync = gattStatus.manualSync
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
                                    statusMessage = message,
                                    manualSync = gattStatus.manualSync
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
                                    statusMessage = message,
                                    manualSync = gattStatus.manualSync
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

    fun resetGattStatus(sensorId: String, sessionId: Long? = null) {
        gattInteractor.resetGattStatus(sensorId, sessionId)
    }

    fun syncGatt(sensorId: String, manual: Boolean = true) {
        if (isGattSyncInProgress(sensorId)) {
            Timber.d("Ignoring duplicate GATT download for $sensorId")
            return
        }
        val sensor = tagDetailsInteractor.getTagById(sensorId)
        sensor?.let { sensor ->
            var syncFrom = sensor.lastSync
            val historyLength = Date(Date().time - 1000 * 60 * 60 * 24 * GlobalSettings.historyLengthDays)
            if (syncFrom == null || syncFrom.before(historyLength)) {
                syncFrom = historyLength
            }
            Timber.d("sync logs from: %s", syncFrom)
            if (gattInteractor.readLogs(sensorId, syncFrom, manualSync = manual)) {
                if (!manual) {
                    preferencesRepository.setLastAutomaticGattSyncAttempt(
                        sensorId,
                        System.currentTimeMillis()
                    )
                }
            }
        }
    }

    fun isGattSyncInProgress(sensorId: String? = null): Boolean =
        gattInteractor.isSyncInProgress(sensorId)

    fun autoSyncGattHistory(sensor: RuuviTag, selected: Boolean) {
        if (selected && sensor.isAir() && !sensor.networkSensor) {
            if (isGattSyncInProgress(sensor.id)) return

            val lastSyncTime = sensor.lastSync?.time ?: 0L
            val lastAttemptTime = preferencesRepository.getLastAutomaticGattSyncAttempt(sensor.id)
            val latestTime = maxOf(lastSyncTime, lastAttemptTime)
            val currentTime = System.currentTimeMillis()

            if (currentTime - latestTime >= AUTO_SYNC_COOLDOWN_MS) {
                syncGatt(sensor.id, manual = false)
            }
        }
    }

    fun setViewPeriod(periodDays: Int) {
        historyController.stop(clear = true)
        historyWake.trySend(Unit)
        customHistory.value = null
        historyExportJob?.cancel()
        appSettingsInteractor.setGraphViewPeriod(periodDays)
        _chartViewPeriod.value = Period.getInstance(periodDays)
    }

    suspend fun exportToCsv(sensorId: String): Uri? = exportHistory(sensorId, false)

    suspend fun exportToXlsx(sensorId: String): Uri? = exportHistory(sensorId, true)

    private suspend fun exportHistory(sensorId: String, xlsx: Boolean): Uri? {
        if (_historyExporting.value) return null
        val job = currentCoroutineContext()[Job]
        historyExportJob = job
        _historyExporting.value = true
        try {
            val access = historyAccess
            val window = historyState.value.timeline
            return if (access?.sensor == sensorId && window != null && access.days > 0 && progressiveHistory?.enabled() == true) {
                historyExporter?.export(access, window, xlsx)
            } else withContext(Dispatchers.IO) {
                if (progressiveHistory?.enabled() == true && tagDetailsInteractor.getTagById(sensorId)?.networkSensor == true &&
                    !(access?.sensor == sensorId && access.days == 0)) throw HistoryFailure.Access()
                if (xlsx) xlsxExporter.exportToXlsx(sensorId) else csvExporter.toCsv(sensorId)
            }
        } finally {
            if (historyExportJob === job) { historyExportJob = null; _historyExporting.value = false }
        }
    }

    private fun getGraphViewPeriod() = Period.getInstance(appSettingsInteractor.getGraphViewPeriod())

    fun shouldSkipGattSyncDialog() = preferencesRepository.getDontShowGattSync()

    fun removeTagData(sensorId: String) {
        historyExportJob?.cancel()
        historyController.stop(clear = true)
        historyAccess?.takeIf { it.sensor == sensorId }?.let { access -> viewModelScope.launch { progressiveHistory?.remove(access) } }
        sensorHistoryRepository.removeForSensor(sensorId)
        tagDetailsInteractor.clearLastSync(sensorId)
        preferencesRepository.clearLastAutomaticGattSyncAttempt(sensorId)
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

    fun getNfcScanResponse(scanInfo: SensorNfсScanInfo) = nfcResultInteractor.getNfcScanResponse(scanInfo)

    fun addSensor(sensorId: String) {
        tagInteractor.makeSensorFavorite(sensorId)
    }

    fun getActiveAlarms(sensorId: String) = alarmRepository.getActiveAlarms(sensorId)

    fun saveSelected(sensorId: String) {
        _selectedSensor.value = sensorId
    }

    companion object {
        private const val AUTO_SYNC_COOLDOWN_MS = 5 * 60 * 1000L
    }

}

data class SensorCardViewModelArguments(
    val sensorId: String? = null,
)

data class SyncStatus (
    val sensorId: String,
    val syncProgress: SyncProgress = SyncProgress.STILL,
    val syncInProgress: Boolean,
    val statusMessage: UiText,
    val manualSync: Boolean = false,
)
