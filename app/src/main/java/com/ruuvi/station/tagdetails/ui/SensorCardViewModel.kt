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
import com.ruuvi.station.graph.model.ChartContainer
import com.ruuvi.station.network.domain.NetworkDataSyncInteractor
import com.ruuvi.station.nfc.domain.NfcResultInteractor
import com.ruuvi.station.settings.domain.AppSettingsInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.TagInteractor
import com.ruuvi.station.tagdetails.domain.TagDetailsInteractor
import com.ruuvi.station.tagsettings.domain.CsvExporter
import com.ruuvi.station.tagsettings.domain.XlsxExporter
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.domain.aqi.AQI
import com.ruuvi.station.units.model.UnitType
import com.ruuvi.station.units.model.UnitType.*
import com.ruuvi.station.util.Period
import com.ruuvi.station.vico.model.ChartData
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
    private val xlsxExporter: XlsxExporter,
    private val nfcResultInteractor: NfcResultInteractor,
    private val alarmRepository: AlarmRepository,
    private val unitsConverter: UnitsConverter
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

    private val _increasedChartSize = MutableStateFlow<Boolean>(preferencesRepository.isIncreasedChartSize())
    val increasedChartSize: StateFlow<Boolean> = _increasedChartSize


    fun getChartData(sensorId: String, unitType: UnitType, hours: Int): Flow<ChartData> =
        flow<ChartData> {
            val history = tagDetailsInteractor.getTagReadings(sensorId, 48)
            val values = mutableListOf<Double>()
            val timestamps = mutableListOf<Long>()

            history.forEach { item ->


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
                    is AirQuality -> AQI.getAQI(item.pm25, item.co2, item.voc, item.nox).score
                    is CO2 -> item.co2
                    is VOC -> item.voc
                    is NOX -> item.nox
                    is PM1 -> item.pm1
                    is PM25 -> item.pm25
                    is PM4 -> item.pm4
                    is PM10 -> item.pm10
                    is Luminosity -> item.luminosity
                    is SoundAvg -> item.dBaAvg
                    is SoundPeak -> item.dBaPeak
                    is MovementUnit -> item.movementCounter
                    else -> null
                }

                if (entryValue != null) {
                    values.add(entryValue.toDouble())
                    timestamps.add(item.createdAt.time)
                }
            }

            emit(ChartData(timestamps = timestamps, values = values))
        }.flowOn(Dispatchers.IO)

    fun historyUpdater(sensorId: String): Flow<MutableList<ChartContainer>> =
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
                    Date().time - chartViewPeriod.value.value * 60 * 60 * 1000
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
                                is AirQuality -> AQI.getAQI(item.pm25, item.co2, item.voc, item.nox).score
                                is CO2 -> item.co2
                                is VOC -> item.voc
                                is NOX -> item.nox
                                is PM1 -> item.pm1
                                is PM25 -> item.pm25
                                is PM4 -> item.pm4
                                is PM10 -> item.pm10
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
                            is PressureUnit -> alarms.firstOrNull { it.alarmType == AlarmType.PRESSURE }
                                ?.let {
                                    unitsConverter.getPressureValue(it.min) to unitsConverter.getPressureValue(it.max)
                                }
                            is CO2 -> alarms.firstOrNull { it.alarmType == AlarmType.CO2 }
                                ?.let { it.min to it.max }
                            is VOC -> alarms.firstOrNull { it.alarmType == AlarmType.VOC }
                                ?.let { it.min to it.max }
                            is NOX -> alarms.firstOrNull { it.alarmType == AlarmType.NOX }
                                ?.let { it.min to it.max }
                            is PM1 -> alarms.firstOrNull { it.alarmType == AlarmType.PM1 }
                                ?.let { it.min to it.max }
                            is PM25 -> alarms.firstOrNull { it.alarmType == AlarmType.PM25 }
                                ?.let { it.min to it.max }
                            is PM4 -> alarms.firstOrNull { it.alarmType == AlarmType.PM4 }
                                ?.let { it.min to it.max }
                            is PM10 -> alarms.firstOrNull { it.alarmType == AlarmType.PM10 }
                                ?.let { it.min to it.max }
                            is Luminosity -> alarms.firstOrNull { it.alarmType == AlarmType.LUMINOSITY }
                                ?.let { it.min to it.max }
                            is SoundAvg -> alarms.firstOrNull { it.alarmType == AlarmType.SOUND }
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

    fun changeIncreaseChartSize() {
        preferencesRepository.setIncreasedChartSize(!preferencesRepository.isIncreasedChartSize())
        _increasedChartSize.value = preferencesRepository.isIncreasedChartSize()
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
        _chartViewPeriod.value = Period.getInstance(periodDays)
    }

    fun exportToCsv(sensorId: String): Uri? = csvExporter.toCsv(sensorId)

    fun exportToXlsx(sensorId: String): Uri? = xlsxExporter.exportToXlsx(sensorId)

    private fun getGraphViewPeriod() = Period.getInstance(appSettingsInteractor.getGraphViewPeriod())

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