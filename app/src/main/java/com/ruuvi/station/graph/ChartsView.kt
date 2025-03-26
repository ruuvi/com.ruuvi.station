package com.ruuvi.station.graph

import android.content.res.Configuration
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
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.LoadingAnimation3dots
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.ruuviStationFonts
import com.ruuvi.station.database.tables.Alarm
import com.ruuvi.station.graph.model.ChartContainer
import com.ruuvi.station.graph.model.ChartSensorType
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.UnitType
import com.ruuvi.station.util.Period
import com.ruuvi.station.util.ui.pxToDp
import com.ruuvi.station.util.ui.scrollbar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber
import java.text.DecimalFormat
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
    size: Size,
    increasedChartSize: Boolean,
    historyUpdater: (String) -> Flow<MutableList<ChartContainer>>,
    getActiveAlarms: (String) -> List<Alarm>
) {
    Timber.d("ChartView - top ${sensor.id} $selected viewPeriod = ${viewPeriod.value}")
    val context = LocalContext.current

    val chartUIComponents = remember {
        mutableMapOf<ChartSensorType, LineChart>()
    }

    var chartContainers by remember {
        mutableStateOf(listOf<ChartContainer>())
    }

    var chartsInitialized by remember { mutableStateOf(false) }

    var chartsPerScreen by remember {
        mutableIntStateOf(3)
    }

    var needsScroll by remember {
        mutableStateOf(true)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(key1 = sensor.id) {
        Timber.d("ChartView - chart containers fill ${sensor.id}")
        chartsInitialized = false

        historyUpdater(sensor.id).collectLatest { data ->
            Timber.d("ChartView - new data collected ${data.size}")
            for (newContainer in data) {
                var uiComponent = chartUIComponents.get(newContainer.chartSensorType)
                if (uiComponent == null) {
                    uiComponent = LineChart(context)
                    chartUIComponents.put(newContainer.chartSensorType, uiComponent)
                }
                newContainer.uiComponent = uiComponent
            }
            chartContainers = data

            if (!chartsInitialized) {
                Timber.d("ChartView - initial setup ${sensor.id}")
                if (chartContainers.isNotEmpty()) {
                    chartsInitialSetup(
                        charts = chartContainers.mapNotNull { container -> container.uiComponent?.let { container.chartSensorType to it } },
                        unitsConverter = unitsConverter,
                        context = context
                    )
                    chartsInitialized = true
                }
                isLoading = false
            }
        }
    }

    LaunchedEffect(key1 = chartContainers.size, increasedChartSize) {
        chartsPerScreen = if (increasedChartSize) {
            min(2, chartContainers.size)
        } else {
            min(3, chartContainers.size)
        }
        needsScroll = chartsPerScreen < chartContainers.size
    }

    LaunchedEffect(key1 = sensor.id) {
        chartCleared.collect{
            Timber.d("ChartView - chart cleared $it")
            for (container in chartContainers) {
                container.data?.clear()
                container.uiComponent?.fitScreen()
            }
        }
    }

    var viewPeriodLocal by remember { mutableStateOf<Period?>(viewPeriod) }

    if (viewPeriod != viewPeriodLocal) {
        Timber.d("ChartView - viewPeriod changed ${sensor.id} $viewPeriod $viewPeriodLocal")
        viewPeriodLocal = viewPeriod
        for (container in chartContainers) {
            container.uiComponent?.fitScreen()
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
            Box (modifier = modifier.fillMaxSize()) {
                val height = (size.height / chartsPerScreen).pxToDp()

                if (!size.isEmpty())
                    VerticalChartsPrototype(
                        modifier = Modifier,
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
            chartContainer.uiComponent?.highlightValue(null)
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
                                chartContainer.uiComponent!!,
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
                            chartContainer.uiComponent!!,
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
            chartContainer.uiComponent!!.highlightValue(null)
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
                chartContainer.uiComponent!!,
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
        if (unitsConverter.getPressureUnit() == UnitType.PressureUnit.Pascal) {
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