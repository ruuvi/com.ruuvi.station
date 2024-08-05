package com.ruuvi.station.graph

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlarmType
import com.ruuvi.station.app.ui.components.LoadingAnimation3dots
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.ruuviStationFonts
import com.ruuvi.station.database.tables.Alarm
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.graph.model.ChartContainer
import com.ruuvi.station.graph.model.ChartSensorType
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.HumidityUnit
import com.ruuvi.station.units.model.PressureUnit
import com.ruuvi.station.util.Period
import com.ruuvi.station.util.ui.scrollbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList

private val chartHeight = 220.dp

@Composable
fun ChartsView(
    modifier: Modifier,
    sensorId: String,
    temperatureChart: LineChart,
    humidityChart: LineChart,
    pressureChart: LineChart,
    batteryChart: LineChart,
    accelerationChart: LineChart,
    rssiChart: LineChart,
    movementsChart: LineChart,
    unitsConverter: UnitsConverter,
    selected: Boolean,
    graphDrawDots: Boolean,
    showChartStats: Boolean,
    chartCleared: Flow<String>,
    viewPeriod: Period,
    newChartsUI: Boolean,
    getHistory: (String) -> List<TagSensorReading>,
    getActiveAlarms: (String) -> List<Alarm>
) {
    Timber.d("ChartView - top $sensorId $selected viewPeriod = ${viewPeriod.value}")
    val context = LocalContext.current

    var viewPeriodLocal by remember { mutableStateOf<Period?>(null) }

    var chartContainers by remember {
        mutableStateOf<List<ChartContainer>>(ArrayList())
    }

    var temperatureData by remember {
        mutableStateOf<MutableList<Entry>>(ArrayList())
    }
    var humidityData by remember {
        mutableStateOf<MutableList<Entry>>(ArrayList())
    }
    var pressureData by remember {
        mutableStateOf<MutableList<Entry>>(ArrayList())
    }

    var history by remember {
        mutableStateOf<List<TagSensorReading>>(listOf())
    }

    var from by remember {
        mutableStateOf(0L)
    }

    var to by remember {
        mutableStateOf(0L)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    if (viewPeriod != viewPeriodLocal) {
        viewPeriodLocal = viewPeriod
        temperatureChart.fitScreen()
        humidityChart.fitScreen()
        pressureChart.fitScreen()
    }

    LaunchedEffect(key1 = sensorId) {
        Timber.d("ChartView - initial setup $sensorId")
        chartsInitialSetup(
            charts = listOf(
                ChartSensorType.TEMPERATURE to temperatureChart,
                ChartSensorType.HUMIDITY to humidityChart,
                ChartSensorType.PRESSURE to pressureChart,
                ChartSensorType.BATTERY to batteryChart,
                ChartSensorType.RSSI to rssiChart,
                ChartSensorType.ACCELERATION to accelerationChart,
                ChartSensorType.MOVEMENTS to movementsChart
            ),
            unitsConverter = unitsConverter,
            context = context
        )

        chartCleared.collect{
            Timber.d("ChartView - chart cleared $it")
            temperatureData.clear()
            humidityData.clear()
            pressureData.clear()
            temperatureChart.fitScreen()
            humidityChart.fitScreen()
            pressureChart.fitScreen()
        }
    }

    var temperatureLimits by remember {mutableStateOf<Pair<Double, Double>?> (null)}
    var humidityLimits by remember {mutableStateOf<Pair<Double, Double>?> (null)}
    var pressureLimits by remember {mutableStateOf<Pair<Double, Double>?> (null)}

    LaunchedEffect(key1 = selected, viewPeriod) {
        Timber.d("ChartView - LaunchedEffect $sensorId")
        while (selected) {
            val container = mutableListOf<ChartContainer>()

            Timber.d("ChartView - get history $sensorId")
            delay(300)
            val freshHistory = getHistory.invoke(sensorId)
            val alarms = getActiveAlarms(sensorId)

            temperatureLimits = alarms.firstOrNull { it.alarmType == AlarmType.TEMPERATURE }?.let {
                unitsConverter.getTemperatureValue(it.min) to unitsConverter.getTemperatureValue(it.max)
            }
            humidityLimits = if (unitsConverter.getHumidityUnit() == HumidityUnit.PERCENT) {
                alarms.firstOrNull { it.alarmType == AlarmType.HUMIDITY }?.let {it.min to it.max }
            } else null
            pressureLimits = alarms.firstOrNull { it.alarmType == AlarmType.PRESSURE }?.let {
                unitsConverter.getPressureValue(it.min) to unitsConverter.getPressureValue(it.max)
            }

            Timber.d("ChartView - temperatureLimits $temperatureLimits")
            Timber.d("ChartView - humidityLimits $humidityLimits")
            Timber.d("ChartView - pressureLimits $pressureLimits")

            if (history.isEmpty() ||
                temperatureChart.highestVisibleX >= (temperatureChart.data?.xMax ?: Float.MIN_VALUE)) {
                history = freshHistory

                if (history.isNotEmpty()) {
                    Timber.d("ChartView - prepare datasets $sensorId pointsCount = ${history.size} FROM = $from")
                    if (viewPeriod is Period.All) {
                        Timber.d("ChartView - VIEW ALL")
                        from = history[0].createdAt.time
                    } else {
                        Timber.d("ChartView - VIEW ${viewPeriod.value}")
                        from = Date().time - viewPeriod.value * 60 * 60 * 1000
                    }
                    to = Date().time

                    val temperatureDataTemp = mutableListOf<Entry>()
                    val humidityDataTemp = mutableListOf<Entry>()
                    val pressureDataTemp = mutableListOf<Entry>()
                    val batteryDataTemp = mutableListOf<Entry>()
                    val accelerationDataTemp = mutableListOf<Entry>()
                    val rssiDataTemp = mutableListOf<Entry>()
                    val movementDataTemp = mutableListOf<Entry>()

                    history.forEach { item ->
                        val timestamp = (item.createdAt.time - from).toFloat()
                        temperatureDataTemp.add(Entry(timestamp, unitsConverter.getTemperatureValue(item.temperature).toFloat()))
                        item.humidity?.let { humidity ->
                            humidityDataTemp.add(Entry(timestamp, unitsConverter.getHumidityValue(humidity, item.temperature).toFloat()))
                        }
                        item.pressure?.let {pressure ->
                            pressureDataTemp.add(Entry(timestamp, unitsConverter.getPressureValue(pressure).toFloat()))
                        }
                        item.voltage?.let {voltage ->
                            batteryDataTemp.add(Entry(timestamp, voltage.toFloat()))
                        }
                        item.accelX?.let {accelX ->
                            accelerationDataTemp.add(Entry(timestamp, accelX.toFloat()))
                        }
                        item.rssi?.let {rssi ->
                            rssiDataTemp.add(Entry(timestamp, rssi.toFloat()))
                        }
                        item.movementCounter?.let {movements ->
                            movementDataTemp.add(Entry(timestamp, movements.toFloat()))
                        }
                    }
                    container.add(
                        ChartContainer(
                            chartSensorType = ChartSensorType.TEMPERATURE,
                            uiComponent = temperatureChart,
                            data = temperatureDataTemp,
                            limits = temperatureLimits,
                            from = from,
                            to = to
                        )
                    )

                    if (humidityDataTemp.isNotEmpty()) {
                        container.add(
                            ChartContainer(
                                chartSensorType = ChartSensorType.HUMIDITY,
                                uiComponent = humidityChart,
                                data = humidityDataTemp,
                                limits = humidityLimits,
                                from = from,
                                to = to
                            )
                        )
                    }

                    if (pressureDataTemp.isNotEmpty()) {
                        container.add(
                            ChartContainer(
                                chartSensorType = ChartSensorType.PRESSURE,
                                uiComponent = pressureChart,
                                data = pressureDataTemp,
                                limits = pressureLimits,
                                from = from,
                                to = to
                            )
                        )
                    }


                    if (batteryDataTemp.isNotEmpty() && newChartsUI) {
                        container.add(
                            ChartContainer(
                                chartSensorType = ChartSensorType.BATTERY,
                                uiComponent = batteryChart,
                                data = batteryDataTemp,
                                limits = null,
                                from = from,
                                to = to
                            )
                        )
                    }

                    if (accelerationDataTemp.isNotEmpty() && newChartsUI) {
                        container.add(
                            ChartContainer(
                                chartSensorType = ChartSensorType.ACCELERATION,
                                uiComponent = accelerationChart,
                                data = accelerationDataTemp,
                                limits = null,
                                from = from,
                                to = to
                            )
                        )
                    }

                    if (rssiDataTemp.isNotEmpty() && newChartsUI) {
                        container.add(
                            ChartContainer(
                                chartSensorType = ChartSensorType.RSSI,
                                uiComponent = rssiChart,
                                data = rssiDataTemp,
                                limits = null,
                                from = from,
                                to = to
                            )
                        )
                    }

                    if (movementDataTemp.isNotEmpty() && newChartsUI) {
                        container.add(
                            ChartContainer(
                                chartSensorType = ChartSensorType.MOVEMENTS,
                                uiComponent = movementsChart,
                                data = movementDataTemp,
                                limits = null,
                                from = from,
                                to = to
                            )
                        )
                    }

                    temperatureData = temperatureDataTemp
                    humidityData = humidityDataTemp
                    pressureData = pressureDataTemp
                    chartContainers = container
                }
            }
            isLoading = false
            delay(1000)
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            LoadingAnimation3dots()
        }
    } else {
        if (temperatureData.isEmpty() && humidityData.isEmpty() && pressureData.isEmpty()) {
            EmptyCharts()
        } else if (isLandscape) {
            LandscapeChartsPrototype(
                modifier,
                chartContainers,
                unitsConverter,
                graphDrawDots,
                showChartStats
            )
//                LandscapeCharts(
//                    modifier,
//                    temperatureChart,
//                    humidityChart,
//                    pressureChart,
//                    temperatureData,
//                    pressureData,
//                    humidityData,
//                    unitsConverter,
//                    graphDrawDots,
//                    showChartStats,
//                    temperatureLimits,
//                    humidityLimits,
//                    pressureLimits,
//                    from,
//                    to
//                )
        } else {
            VerticalChartsPrototype(
                modifier,
                chartContainers,
                unitsConverter,
                graphDrawDots,
                showChartStats
            )
//                VerticalCharts(
//                    modifier,
//                    temperatureChart,
//                    humidityChart,
//                    pressureChart,
//                    temperatureData,
//                    pressureData,
//                    humidityData,
//                    unitsConverter,
//                    graphDrawDots,
//                    showChartStats,
//                    temperatureLimits,
//                    humidityLimits,
//                    pressureLimits,
//                    from,
//                    to
//                )
        }
    }
}

@Composable
fun VerticalChartsPrototype(
    modifier: Modifier,
    chartContainers: List<ChartContainer>,
    unitsConverter: UnitsConverter,
    graphDrawDots: Boolean,
    showChartStats: Boolean
) {
    val clearMarker = {
        for (chartContainer in chartContainers) {
            chartContainer.uiComponent.highlightValue(null)
        }
    }

    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = modifier
            .scrollbar(state = listState, horizontal = false)
    ) {
        if (chartContainers.firstOrNull()?.data.isNullOrEmpty()) {
            item {
                EmptyCharts()
            }
        } else {
            for (chartContainer in chartContainers) {
                item {
                    ChartViewPrototype(
                        chartContainer.uiComponent,
                        Modifier.fillMaxWidth(),
                        chartContainer.data,
                        unitsConverter,
                        chartContainer.chartSensorType,
                        graphDrawDots,
                        showChartStats,
                        limits = chartContainer.limits,
                        chartContainer.from,
                        chartContainer.to,
                        chartHeight,
                        clearMarker
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LandscapeChartsPrototype(
    modifier: Modifier,
    chartContainers: List<ChartContainer>,
    unitsConverter: UnitsConverter,
    graphDrawDots: Boolean,
    showChartStats: Boolean
) {
    val clearMarker = {
        for (chartContainer in chartContainers) {
            chartContainer.uiComponent.highlightValue(null)
        }
    }

    val pagerState = rememberPagerState {
        return@rememberPagerState chartContainers.size
    }

    VerticalPager(
        modifier = modifier.fillMaxSize(),
        state = pagerState,
        beyondBoundsPageCount = 3
    ) { page ->
        val chartContainer = chartContainers[page]

        ChartViewPrototype(
            chartContainer.uiComponent,
            Modifier.fillMaxWidth(),
            chartContainer.data,
            unitsConverter,
            chartContainer.chartSensorType,
            graphDrawDots,
            showChartStats,
            limits = chartContainer.limits,
            chartContainer.from,
            chartContainer.to,
            chartHeight,
            clearMarker
        )
    }
}

@Composable
fun EmptyCharts() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = RuuviStationTheme.dimensions.extended),
            color = Color.White,
            fontFamily = ruuviStationFonts.mulishRegular,
            fontSize = RuuviStationTheme.fontSizes.extended,
            text = stringResource(id = R.string.empty_chart_message),
            textAlign = TextAlign.Center
        )
    }
}

fun setupChart(
    chart: LineChart,
    unitsConverter: UnitsConverter,
    chartSensorType: ChartSensorType
) {
    if (chartSensorType == ChartSensorType.TEMPERATURE || chartSensorType == ChartSensorType.HUMIDITY) {
        chart.axisLeft.valueFormatter = AxisLeftValueFormatter("#.##")
        chart.axisLeft.granularity = 0.01f
    } else {
        if (unitsConverter.getPressureUnit() == PressureUnit.PA) {
            chart.axisLeft.valueFormatter = AxisLeftValueFormatter("#")
            chart.axisLeft.granularity = 1f
        } else {
            chart.axisLeft.valueFormatter = AxisLeftValueFormatter("#.##")
            chart.axisLeft.granularity = 0.01f
        }
    }
}

class AxisLeftValueFormatter(private val formatPattern: String) : IAxisValueFormatter {
    override fun getFormattedValue(value: Double, p1: AxisBase?): String {
        val decimalFormat = DecimalFormat(formatPattern)
        return decimalFormat.format(value)
    }
}

@Composable
fun VerticalCharts(
    modifier: Modifier,
    temperatureChart: LineChart,
    humidityChart: LineChart,
    pressureChart: LineChart,
    temperatureData: MutableList<Entry>,
    pressureData: MutableList<Entry>,
    humidityData: MutableList<Entry>,
    unitsConverter: UnitsConverter,
    graphDrawDots: Boolean,
    showChartStats: Boolean,
    temperatureLimits: Pair<Double, Double>?,
    humidityLimits: Pair<Double, Double>?,
    pressureLimits: Pair<Double, Double>?,
    from: Long,
    to: Long
) {
    val clearMarker = {
        temperatureChart.highlightValue(null)
        pressureChart.highlightValue(null)
        humidityChart.highlightValue(null)
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (temperatureData.isEmpty() && humidityData.isEmpty() && pressureData.isEmpty()) {
            EmptyCharts()
        } else {
            val onlyOneChart = humidityData.isEmpty() && pressureData.isEmpty()
            if (onlyOneChart) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(0.5f)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                ChartView(
                    temperatureChart,
                    Modifier.fillMaxSize(),
                    temperatureData,
                    unitsConverter,
                    ChartSensorType.TEMPERATURE,
                    graphDrawDots,
                    showChartStats,
                    limits = temperatureLimits,
                    from,
                    to,
                    clearMarker
                )
            }
            if (onlyOneChart) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(0.5f)
                )
            }
            if (humidityData.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    ChartView(
                        humidityChart,
                        Modifier.fillMaxSize(),
                        humidityData,
                        unitsConverter,
                        ChartSensorType.HUMIDITY,
                        graphDrawDots,
                        showChartStats,
                        limits = humidityLimits,
                        from,
                        to,
                        clearMarker
                    )
                }
            }
            if (pressureData.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    ChartView(
                        pressureChart,
                        Modifier.fillMaxSize(),
                        pressureData,
                        unitsConverter,
                        ChartSensorType.PRESSURE,
                        graphDrawDots,
                        showChartStats,
                        limits = pressureLimits,
                        from,
                        to,
                        clearMarker
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LandscapeCharts(
    modifier: Modifier,
    temperatureChart: LineChart,
    humidityChart: LineChart,
    pressureChart: LineChart,
    temperatureData: MutableList<Entry>,
    pressureData: MutableList<Entry>,
    humidityData: MutableList<Entry>,
    unitsConverter: UnitsConverter,
    graphDrawDots: Boolean,
    showChartStats: Boolean,
    temperatureLimits: Pair<Double, Double>?,
    humidityLimits: Pair<Double, Double>?,
    pressureLimits: Pair<Double, Double>?,
    from: Long,
    to: Long
) {
    val clearMarker = {
        temperatureChart.highlightValue(null)
        humidityChart.highlightValue(null)
        pressureChart.highlightValue(null)
    }
    val chartTypes = mutableListOf<ChartSensorType>(ChartSensorType.TEMPERATURE)
    if (humidityData.isNotEmpty()) chartTypes.add(ChartSensorType.HUMIDITY)
    if (pressureData.isNotEmpty()) chartTypes.add(ChartSensorType.PRESSURE)

    val pagerState = rememberPagerState {
        return@rememberPagerState chartTypes.size
    }

    VerticalPager(
        modifier = modifier.fillMaxSize(),
        state = pagerState,
        beyondBoundsPageCount = 3
    ) { page ->
        val chartSensorType = chartTypes[page]

        when (chartSensorType) {
            ChartSensorType.TEMPERATURE -> {
                ChartView(
                    temperatureChart,
                    Modifier.fillMaxSize(),
                    temperatureData,
                    unitsConverter,
                    ChartSensorType.TEMPERATURE,
                    graphDrawDots,
                    showChartStats,
                    limits = temperatureLimits,
                    from,
                    to,
                    clearMarker
                )
            }
            ChartSensorType.HUMIDITY -> {
                ChartView(
                    humidityChart,
                    Modifier.fillMaxSize(),
                    humidityData,
                    unitsConverter,
                    ChartSensorType.HUMIDITY,
                    graphDrawDots,
                    showChartStats,
                    limits = humidityLimits,
                    from,
                    to,
                    clearMarker
                )
            }
            ChartSensorType.PRESSURE -> {
                ChartView(
                    pressureChart,
                    Modifier.fillMaxSize(),
                    pressureData,
                    unitsConverter,
                    ChartSensorType.PRESSURE,
                    graphDrawDots,
                    showChartStats,
                    limits = pressureLimits,
                    from,
                    to,
                    clearMarker
                )
            }
            else -> {}
        }
    }
}