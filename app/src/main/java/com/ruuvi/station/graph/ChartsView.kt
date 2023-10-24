package com.ruuvi.station.graph

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
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
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.LoadingAnimation3dots
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.ruuviStationFonts
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.graph.model.ChartSensorType
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.PressureUnit
import com.ruuvi.station.util.Period
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
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
    chartCleared: Flow<String>,
    viewPeriod: Period,
    getHistory: (String) -> List<TagSensorReading>
) {
    Timber.d("ChartView - top $sensorId $selected")
    val context = LocalContext.current

    var history by remember {
        mutableStateOf<List<TagSensorReading>>(listOf())
    }

    var from by remember {
        mutableStateOf(0L)
    }

    var to by remember {
        mutableStateOf(0L)
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

    var isLoading by remember {
        mutableStateOf(true)
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

    LaunchedEffect(key1 = selected) {
        Timber.d("ChartView - LaunchedEffect $sensorId")
        while (selected) {
            Timber.d("ChartView - get history $sensorId")
            delay(300)
            val freshHistory = getHistory.invoke(sensorId)

            if (history.isEmpty() ||
                temperatureChart.highestVisibleX >= (temperatureChart.data?.xMax ?: Float.MIN_VALUE)) {
                history = freshHistory

                if (history.isNotEmpty()) {
                    Timber.d("ChartView - prepare datasets $sensorId pointsCount = ${history.size}")
                    if (viewPeriod is Period.All) {
                        from = history[0].createdAt.time
                    } else {
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
            } else {

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
                            from,
                            to
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
                            from,
                            to
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
                            from,
                            to
                        )
                    }
                }
            }
        } else {
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
                            from,
                            to
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
                                from,
                                to
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
                                from,
                                to
                            )
                        }
                    }
                }
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
        chart.axisLeft.valueFormatter = GraphView.AxisLeftValueFormatter("#.##")
        chart.axisLeft.granularity = 0.01f
    } else {
        if (unitsConverter.getPressureUnit() == PressureUnit.PA) {
            chart.axisLeft.valueFormatter = GraphView.AxisLeftValueFormatter("#")
            chart.axisLeft.granularity = 1f
        } else {
            chart.axisLeft.valueFormatter = GraphView.AxisLeftValueFormatter("#.##")
            chart.axisLeft.granularity = 0.01f
        }
    }
}