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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.isAir
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.HumidityUnit
import com.ruuvi.station.units.model.PressureUnit
import com.ruuvi.station.util.Period
import com.ruuvi.station.util.ui.pxToDp
import com.ruuvi.station.util.ui.scrollbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.text.DecimalFormat
import java.util.*
import kotlin.math.min

@Composable
fun ChartsView(
    modifier: Modifier,
    sensor: RuuviTag,
    unitsConverter: UnitsConverter,
    selected: Boolean,
    graphDrawDots: Boolean,
    showChartStats: Boolean,
    chartCleared: Flow<String>,
    viewPeriod: Period,
    newChartsUI: Boolean,
    size: Size,
    increasedChartSize: Boolean,
    getHistory: (String) -> List<TagSensorReading>,
    getActiveAlarms: (String) -> List<Alarm>
) {
    Timber.d("ChartView - top ${sensor.id} $selected viewPeriod = ${viewPeriod.value}")
    val context = LocalContext.current

    val chartContainers by remember {
        mutableStateOf<MutableList<ChartContainer>>(mutableListOf())
    }

    LaunchedEffect(key1 = sensor.id) {
        Timber.d("ChartView - chart containers fill ${sensor.id}")

        val temperatureContainer = ChartContainer(ChartSensorType.TEMPERATURE, LineChart(context))
        chartContainers.add(temperatureContainer)

        if (sensor.latestMeasurement?.humidityValue != null) {
            val humidityContainer = ChartContainer(ChartSensorType.HUMIDITY, LineChart(context))
            chartContainers.add(humidityContainer)
        }

        if (sensor.latestMeasurement?.pressureValue != null) {
            val pressureContainer = ChartContainer(ChartSensorType.PRESSURE, LineChart(context))
            chartContainers.add(pressureContainer)
        }

        if (sensor.isAir()) {
            sensor.latestMeasurement?.co2.let {
                val co2Container = ChartContainer(ChartSensorType.CO2, LineChart(context))
                chartContainers.add(co2Container)
            }

            sensor.latestMeasurement?.voc.let {
                val vocContainer = ChartContainer(ChartSensorType.VOC, LineChart(context))
                chartContainers.add(vocContainer)
            }

            sensor.latestMeasurement?.nox.let {
                val noxContainer = ChartContainer(ChartSensorType.NOX, LineChart(context))
                chartContainers.add(noxContainer)
            }

            sensor.latestMeasurement?.pm25.let {
                val pm25Container = ChartContainer(ChartSensorType.PM25, LineChart(context))
                chartContainers.add(pm25Container)
            }

            sensor.latestMeasurement?.luminosity.let {
                val luminosityContainer = ChartContainer(ChartSensorType.LUMINOSITY, LineChart(context))
                chartContainers.add(luminosityContainer)
            }

            sensor.latestMeasurement?.dBaAvg.let {
                val soundContainer = ChartContainer(ChartSensorType.SOUND, LineChart(context))
                chartContainers.add(soundContainer)
            }
        }

        Timber.d("ChartView - initial setup ${sensor.id}")
        chartsInitialSetup(
            charts = chartContainers.map { it.chartSensorType to it.uiComponent },
            unitsConverter = unitsConverter,
            context = context
        )

        chartCleared.collect{
            Timber.d("ChartView - chart cleared $it")
            for (container in chartContainers) {
                container.data?.clear()
                container.uiComponent.fitScreen()
            }
        }
    }

    var viewPeriodLocal by remember { mutableStateOf<Period?>(null) }

    var needsScroll by remember {
        mutableStateOf(true)
    }

    var chartsPerScreen by remember {
        mutableStateOf(3)
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
        Timber.d("ChartView - viewPeriod changed ${sensor.id}")
        viewPeriodLocal = viewPeriod
        for (container in chartContainers) {
            container.uiComponent.fitScreen()
        }
    }

    var temperatureLimits by remember {mutableStateOf<Pair<Double, Double>?> (null)}
    var humidityLimits by remember {mutableStateOf<Pair<Double, Double>?> (null)}
    var pressureLimits by remember {mutableStateOf<Pair<Double, Double>?> (null)}

    LaunchedEffect(key1 = selected, viewPeriod, increasedChartSize) {
        Timber.d("ChartView - LaunchedEffect ${sensor.id}")
        while (selected) {
            val container = mutableListOf<ChartContainer>()

            Timber.d("ChartView - get history ${sensor.id}")
            delay(300)
            val freshHistory = getHistory.invoke(sensor.id)
            val alarms = getActiveAlarms(sensor.id)

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

            val tempChart = chartContainers.firstOrNull { it.chartSensorType == ChartSensorType.TEMPERATURE }
            if (history.isEmpty() ||
                (tempChart?.uiComponent != null && tempChart.uiComponent.highestVisibleX >= (tempChart.uiComponent.data?.xMax ?: Float.MIN_VALUE))) {
                history = freshHistory

                if (history.isNotEmpty()) {
                    Timber.d("ChartView - prepare datasets ${sensor.id} pointsCount = ${history.size} FROM = $from")
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
                    val co2DataTemp = mutableListOf<Entry>()
                    val vocDataTemp = mutableListOf<Entry>()
                    val noxDataTemp = mutableListOf<Entry>()
                    val pm25DataTemp = mutableListOf<Entry>()
                    val luminosityDataTemp = mutableListOf<Entry>()
                    val soundDataTemp = mutableListOf<Entry>()

                    history.forEach { item ->
                        val timestamp = (item.createdAt.time - from).toFloat()
                        item.temperature?.let { temperature ->
                            temperatureDataTemp.add(Entry(timestamp, unitsConverter.getTemperatureValue(temperature).toFloat()))
                        }
                        item.humidity?.let { humidity ->
                            val humidityValue = unitsConverter.getHumidityValue(humidity, item.temperature)
                            if (humidityValue != null) {
                                humidityDataTemp.add(Entry(timestamp, humidityValue.toFloat()))
                            }
                        }
                        item.pressure?.let {pressure ->
                            pressureDataTemp.add(Entry(timestamp, unitsConverter.getPressureValue(pressure).toFloat()))
                        }
                        if (newChartsUI) {
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
                    val temperatureContainer = chartContainers.firstOrNull { it.chartSensorType == ChartSensorType.TEMPERATURE }
                    temperatureContainer?.let {
                        it.data = temperatureDataTemp
                        it.limits = temperatureLimits
                        it.from = from
                        it.to = to
                    }

                    if (humidityDataTemp.isNotEmpty()) {
                        val humidityContainer = chartContainers.firstOrNull { it.chartSensorType == ChartSensorType.HUMIDITY }
                        humidityContainer?.let {
                            it.data = humidityDataTemp
                            it.limits = humidityLimits
                            it.from = from
                            it.to = to
                        }
                    }

                    if (pressureDataTemp.isNotEmpty()) {
                        val pressureContainer = chartContainers.firstOrNull { it.chartSensorType == ChartSensorType.PRESSURE }
                        pressureContainer?.let {
                            it.data = pressureDataTemp
                            it.limits = pressureLimits
                            it.from = from
                            it.to = to
                        }
                    }

                    if (co2DataTemp.isNotEmpty()) {
                        val co2Container = chartContainers.firstOrNull { it.chartSensorType == ChartSensorType.CO2}
                        co2Container?.let {
                            it.data = co2DataTemp
                            it.from = from
                            it.to = to
                        }
                    }

                    if (vocDataTemp.isNotEmpty()) {
                        val vocContainer = chartContainers.firstOrNull { it.chartSensorType == ChartSensorType.VOC}
                        vocContainer?.let {
                            it.data = vocDataTemp
                            it.from = from
                            it.to = to
                        }
                    }

                    if (noxDataTemp.isNotEmpty()) {
                        val noxContainer = chartContainers.firstOrNull { it.chartSensorType == ChartSensorType.NOX}
                        noxContainer?.let {
                            it.data = noxDataTemp
                            it.from = from
                            it.to = to
                        }
                    }

                    if (pm25DataTemp.isNotEmpty()) {
                        val pm25Container = chartContainers.firstOrNull { it.chartSensorType == ChartSensorType.PM25}
                        pm25Container?.let {
                            it.data = pm25DataTemp
                            it.from = from
                            it.to = to
                        }
                    }

                    if (luminosityDataTemp.isNotEmpty()) {
                        val luminosityContainer = chartContainers.firstOrNull { it.chartSensorType == ChartSensorType.LUMINOSITY}
                        luminosityContainer?.let {
                            it.data = luminosityDataTemp
                            it.from = from
                            it.to = to
                        }
                    }

                    if (soundDataTemp.isNotEmpty()) {
                        val soundContainer = chartContainers.firstOrNull { it.chartSensorType == ChartSensorType.SOUND}
                        soundContainer?.let {
                            it.data = soundDataTemp
                            it.from = from
                            it.to = to
                        }
                    }
                }
            }
            isLoading = false
            chartsPerScreen = if (increasedChartSize) {
                min(2, chartContainers.size)
            } else {
                min(3, chartContainers.size)
            }
            needsScroll = chartsPerScreen < chartContainers.size
            Timber.d("needsScroll $needsScroll increaseChartSize = $increasedChartSize chartsPerScreen=$chartsPerScreen containers = ${chartContainers.size}")

            delay(1000)
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            LoadingAnimation3dots()
        }
    } else {
       if (isLandscape) {
           LandscapeChartsPrototype(
                modifier,
                chartContainers,
                unitsConverter,
                graphDrawDots,
                showChartStats
            )
        } else {
            Box (modifier = modifier.fillMaxSize()){
                val height = (size.height / chartsPerScreen).pxToDp()

                if (!size.isEmpty())
                VerticalChartsPrototype(
                    modifier =  Modifier,
                    chartContainers = chartContainers,
                    unitsConverter = unitsConverter,
                    graphDrawDots = graphDrawDots,
                    showChartStats = showChartStats,
                    height = height,
                    needsScroll = needsScroll
                )
            }
        }
    }
}

@Composable
fun VerticalChartsPrototype(
    modifier: Modifier,
    chartContainers: List<ChartContainer>,
    unitsConverter: UnitsConverter,
    graphDrawDots: Boolean,
    showChartStats: Boolean,
    height: Dp,
    needsScroll: Boolean
) {
    val clearMarker = {
        for (chartContainer in chartContainers) {
            chartContainer.uiComponent.highlightValue(null)
        }
    }

    if (chartContainers.firstOrNull()?.data.isNullOrEmpty()) {
        EmptyCharts(modifier)
    } else {

        Timber.d("chart height $height $needsScroll")
        val listState = rememberLazyListState()

        if (needsScroll) {
            val columnModifier = modifier
                .fillMaxSize()
                .scrollbar(state = listState, horizontal = false)

            LazyColumn(
                state = listState,
                modifier = columnModifier
            ) {
                for (chartContainer in chartContainers) {
                    val data = chartContainer.data
                    val from = chartContainer.from
                    val to  = chartContainer.to
                    if (data != null && to != null && from != null) {
                        item {
                            ChartViewPrototype(
                                chartContainer.uiComponent,
                                Modifier
                                    .height(height)
                                    .fillMaxWidth(),
                                data,
                                unitsConverter,
                                chartContainer.chartSensorType,
                                graphDrawDots,
                                showChartStats,
                                limits = chartContainer.limits,
                                from,
                                to,
                                clearMarker
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
            ) {
                for (chartContainer in chartContainers) {
                    val data = chartContainer.data
                    val from = chartContainer.from
                    val to  = chartContainer.to
                    if (data != null && to != null && from != null) {
                        ChartViewPrototype(
                            chartContainer.uiComponent,
                            Modifier
                                .height(height)
                                .fillMaxWidth(),
                            data,
                            unitsConverter,
                            chartContainer.chartSensorType,
                            graphDrawDots,
                            showChartStats,
                            limits = chartContainer.limits,
                            from,
                            to,
                            clearMarker
                        )
                    }
                }
            }
        }
    }
}

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
        beyondViewportPageCount = 3
    ) { page ->
        val chartContainer = chartContainers[page]
        val data = chartContainer.data
        val from = chartContainer.from
        val to  = chartContainer.to
        if (data != null && to != null && from != null) {
            ChartViewPrototype(
                chartContainer.uiComponent,
                Modifier.fillMaxSize(),
                data,
                unitsConverter,
                chartContainer.chartSensorType,
                graphDrawDots,
                showChartStats,
                limits = chartContainer.limits,
                from,
                to,
                clearMarker
            )
        }
    }
}

@Composable
fun EmptyCharts(modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
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
            EmptyCharts(modifier)
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
        beyondViewportPageCount = 3
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