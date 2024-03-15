package com.ruuvi.station.graph

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
import com.ruuvi.station.graph.model.ChartSensorType
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.HumidityUnit
import com.ruuvi.station.units.model.PressureUnit
import com.ruuvi.station.util.Period
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.text.DecimalFormat
import java.util.*

@Composable
fun ChartsView(
    modifier: Modifier,
    sensorId: String,
    temperatureChart: LineChart,
    humidityChart: LineChart,
    pressureChart: LineChart,
    unitsConverter: UnitsConverter,
    selected: Boolean,
    graphDrawDots: Boolean,
    showChartStats: Boolean,
    chartCleared: Flow<String>,
    viewPeriod: Period,
    getHistory: (String) -> List<TagSensorReading>,
    getActiveAlarms: (String) -> List<Alarm>
) {
    Timber.d("ChartView - top $sensorId $selected viewPeriod = ${viewPeriod.value}")
    val context = LocalContext.current

    var viewPeriodLocal by remember { mutableStateOf<Period?>(null) }

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
            temperatureChart = temperatureChart,
            humidityChart = humidityChart,
            pressureChart = pressureChart,
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
                    history.forEach { item ->
                        val timestamp = (item.createdAt.time - from).toFloat()
                        temperatureDataTemp.add(Entry(timestamp, unitsConverter.getTemperatureValue(item.temperature).toFloat()))
                        item.humidity?.let { humidity ->
                            humidityDataTemp.add(Entry(timestamp, unitsConverter.getHumidityValue(humidity, item.temperature).toFloat()))
                        }
                        item.pressure?.let {pressure ->
                            pressureDataTemp.add(Entry(timestamp, unitsConverter.getPressureValue(pressure).toFloat()))
                        }
                    }
                    temperatureData = temperatureDataTemp
                    humidityData = humidityDataTemp
                    pressureData = pressureDataTemp
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
            LandscapeCharts(
                modifier,
                temperatureChart,
                humidityChart,
                pressureChart,
                temperatureData,
                pressureData,
                humidityData,
                unitsConverter,
                graphDrawDots,
                showChartStats,
                temperatureLimits,
                humidityLimits,
                pressureLimits,
                from,
                to
            )
        } else {
            VerticalChartsPrototype(
                modifier,
                temperatureChart,
                humidityChart,
                pressureChart,
                temperatureData,
                pressureData,
                humidityData,
                unitsConverter,
                graphDrawDots,
                showChartStats,
                temperatureLimits,
                humidityLimits,
                pressureLimits,
                from,
                to
            )
        }
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

@Composable
fun VerticalChartsPrototype(
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

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        if (temperatureData.isEmpty() && humidityData.isEmpty() && pressureData.isEmpty()) {
            EmptyCharts()
        } else {

            ChartViewPrototype(
                temperatureChart,
                Modifier.fillMaxWidth(),
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

            if (humidityData.isNotEmpty()) {
                ChartViewPrototype(
                    humidityChart,
                    Modifier.fillMaxWidth(),
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
            if (pressureData.isNotEmpty()) {
                ChartViewPrototype(
                    pressureChart,
                    Modifier.fillMaxWidth(),
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
        }
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