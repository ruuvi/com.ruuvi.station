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
import com.ruuvi.station.graph.model.ChartContainer
import com.ruuvi.station.graph.model.ChartSensorType
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
import com.ruuvi.station.units.model.HumidityUnit
import com.ruuvi.station.util.Period
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

    fun getSensorHistory(sensorId: String): List<TagSensorReading> {
        return tagDetailsInteractor.getTagReadings(sensorId)
    }

    fun historyUpdater(sensorId: String): Flow<MutableList<ChartContainer>> =
        flow<MutableList<ChartContainer>> {
            delay(200)
            while (true) {
                val history = tagDetailsInteractor.getTagReadings(sensorId)

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

                val temperatureDataTemp = mutableListOf<Entry>()
                val humidityDataTemp = mutableListOf<Entry>()
                val pressureDataTemp = mutableListOf<Entry>()
                val batteryDataTemp = mutableListOf<Entry>()
                val accelerationDataTemp = mutableListOf<Entry>()
                val rssiDataTemp = mutableListOf<Entry>()
                val movementDataTemp = mutableListOf<Entry>()
                val co2DataTemp = mutableListOf<Entry>()
                val vocDataTemp = mutableListOf<Entry>()
                val noxDataTemp = mutableListOf<Entry>()
                val pm25DataTemp = mutableListOf<Entry>()
                val luminosityDataTemp = mutableListOf<Entry>()
                val soundDataTemp = mutableListOf<Entry>()
                val aqiDataTemp = mutableListOf<Entry>()

                history.forEach { item ->
                    val timestamp = (item.createdAt.time - from).toFloat()
                    item.temperature?.let { temperature ->
                        temperatureDataTemp.add(
                            Entry(
                                timestamp,
                                unitsConverter.getTemperatureValue(temperature).toFloat()
                            )
                        )
                    }
                    item.humidity?.let { humidity ->
                        val humidityValue =
                            unitsConverter.getHumidityValue(humidity, item.temperature)
                        if (humidityValue != null) {
                            humidityDataTemp.add(Entry(timestamp, humidityValue.toFloat()))
                        }
                    }
                    item.pressure?.let { pressure ->
                        pressureDataTemp.add(
                            Entry(
                                timestamp,
                                unitsConverter.getPressureValue(pressure).toFloat()
                            )
                        )
                    }
                    if (_newChartsUI.value) {
                        item.voltage?.let { voltage ->
                            batteryDataTemp.add(Entry(timestamp, voltage.toFloat()))
                        }
                        item.accelX?.let { accelX ->
                            accelerationDataTemp.add(Entry(timestamp, accelX.toFloat()))
                        }
                        item.rssi?.let { rssi ->
                            rssiDataTemp.add(Entry(timestamp, rssi.toFloat()))
                        }
                        item.movementCounter?.let { movements ->
                            movementDataTemp.add(Entry(timestamp, movements.toFloat()))
                        }
                    }
                    val aqi = AQI.getAQI(item.pm25, item.co2, item.voc, item.nox)
                    aqi.score?.let {
                        aqiDataTemp.add(Entry(timestamp, it.toFloat()))
                    }
                    item.co2?.let { co2 ->
                        co2DataTemp.add(Entry(timestamp, co2.toFloat()))
                    }
                    item.voc?.let { voc ->
                        vocDataTemp.add(Entry(timestamp, voc.toFloat()))
                    }
                    item.nox?.let { nox ->
                        noxDataTemp.add(Entry(timestamp, nox.toFloat()))
                    }
                    item.pm25?.let { pm25 ->
                        pm25DataTemp.add(Entry(timestamp, pm25.toFloat()))
                    }
                    item.luminosity?.let { luminosity ->
                        luminosityDataTemp.add(Entry(timestamp, luminosity.toFloat()))
                    }
                    item.dBaAvg?.let { dBaAvg ->
                        soundDataTemp.add(Entry(timestamp, dBaAvg.toFloat()))
                    }
                }

                val chartContainers = mutableListOf<ChartContainer>()

                if (temperatureDataTemp.isNotEmpty()) {
                    chartContainers.add(
                        ChartContainer(
                            chartSensorType = ChartSensorType.TEMPERATURE,
                            data = temperatureDataTemp,
                            limits = alarms.firstOrNull { it -> it.alarmType == AlarmType.TEMPERATURE }
                                ?.let {
                                    unitsConverter.getTemperatureValue(it.min) to unitsConverter.getTemperatureValue(
                                        it.max
                                    )
                                },
                            from = from,
                            to = to,
                            uiComponent = null
                        )
                    )
                }

                if (humidityDataTemp.isNotEmpty()) {
                    chartContainers.add(ChartContainer(
                        chartSensorType = ChartSensorType.HUMIDITY,
                        data = humidityDataTemp,
                        limits = if (unitsConverter.getHumidityUnit() == HumidityUnit.PERCENT) {
                            alarms.firstOrNull { it.alarmType == AlarmType.HUMIDITY }
                                ?.let { it.min to it.max }
                        } else null,
                        from = from,
                        to = to,
                        uiComponent = null
                    ))
                }

                if (pressureDataTemp.isNotEmpty()) {
                    chartContainers.add(
                        ChartContainer(
                            chartSensorType = ChartSensorType.PRESSURE,
                            data = pressureDataTemp,
                            limits = alarms.firstOrNull { it.alarmType == AlarmType.PRESSURE }
                                ?.let {
                                    unitsConverter.getPressureValue(it.min) to unitsConverter.getPressureValue(it.max)
                                },
                            from = from,
                            to = to,
                            uiComponent = null
                        )
                    )
                }

                if (aqiDataTemp.isNotEmpty()) {
                    chartContainers.add(ChartContainer(
                        chartSensorType = ChartSensorType.AQI,
                        data = aqiDataTemp,
                        limits = null,
                        from = from,
                        to = to,
                        uiComponent = null
                    ))
                }

                if (co2DataTemp.isNotEmpty()) {
                    chartContainers.add(ChartContainer(
                        chartSensorType = ChartSensorType.CO2,
                        data = co2DataTemp,
                        limits = alarms.firstOrNull { it.alarmType == AlarmType.CO2 }
                            ?.let { it.min to it.max },
                        from = from,
                        to = to,
                        uiComponent = null
                    ))
                }

                if (vocDataTemp.isNotEmpty()) {
                    chartContainers.add(ChartContainer(
                        chartSensorType = ChartSensorType.VOC,
                        data = vocDataTemp,
                        limits = alarms.firstOrNull { it.alarmType == AlarmType.VOC }
                            ?.let { it.min to it.max },
                        from = from,
                        to = to,
                        uiComponent = null
                    ))
                }

                if (noxDataTemp.isNotEmpty()) {
                    chartContainers.add(ChartContainer(
                        chartSensorType = ChartSensorType.NOX,
                        data = noxDataTemp,
                        limits = alarms.firstOrNull { it.alarmType == AlarmType.NOX }
                            ?.let { it.min to it.max },
                        from = from,
                        to = to,
                        uiComponent = null
                    ))
                }

                if (pm25DataTemp.isNotEmpty()) {
                    chartContainers.add(ChartContainer(
                        chartSensorType = ChartSensorType.PM25,
                        data = pm25DataTemp,
                        limits = alarms.firstOrNull { it.alarmType == AlarmType.PM25 }
                            ?.let { it.min to it.max },
                        from = from,
                        to = to,
                        uiComponent = null
                    ))
                }

                if (luminosityDataTemp.isNotEmpty()) {
                    chartContainers.add(ChartContainer(
                        chartSensorType = ChartSensorType.LUMINOSITY,
                        data = luminosityDataTemp,
                        limits = alarms.firstOrNull { it.alarmType == AlarmType.LUMINOSITY }
                            ?.let { it.min to it.max },
                        from = from,
                        to = to,
                        uiComponent = null
                    ))
                }

                if (soundDataTemp.isNotEmpty()) {
                    chartContainers.add(ChartContainer(
                        chartSensorType = ChartSensorType.SOUND,
                        data = soundDataTemp,
                        limits = alarms.firstOrNull { it.alarmType == AlarmType.SOUND }
                            ?.let { it.min to it.max },
                        from = from,
                        to = to,
                        uiComponent = null
                    ))
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
        _chartViewPeriod.value = getGraphViewPeriod()
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