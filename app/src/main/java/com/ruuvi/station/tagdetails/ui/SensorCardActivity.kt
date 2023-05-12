package com.ruuvi.station.tagdetails.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.github.mikephil.charting.charts.LineChart
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.Paragraph
import com.ruuvi.station.app.ui.components.Subtitle
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.app.ui.theme.White
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.graph.ChartControlElement2
import com.ruuvi.station.graph.ChartsView
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tagsettings.ui.TagSettingsActivity
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.util.Days
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import com.ruuvi.station.util.extensions.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import org.kodein.di.generic.instance
import timber.log.Timber

class SensorCardActivity : AppCompatActivity(), KodeinAware {

    override val kodein by closestKodein()

    private val unitsConverter: UnitsConverter by instance()

    private val viewModel: SensorCardViewModel by viewModel {
        SensorCardViewModelArguments(
            intent.getStringExtra(ARGUMENT_SENSOR_ID),
            intent.getBooleanExtra(ARGUMENT_SHOW_CHART, false),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            RuuviTheme {
                val sensors by viewModel.sensorsFlow.collectAsStateWithLifecycle(initialValue = listOf())
                val selectedIndex by viewModel.selectedIndex.collectAsStateWithLifecycle()
                val viewPeriod by viewModel.chartViewPeriod.collectAsState(Days.Day10())
                val showCharts by viewModel.showCharts.collectAsStateWithLifecycle(false)

                SensorsPager(
                    selectedIndex = selectedIndex,
                    sensors = sensors,
                    showCharts = showCharts,
                    setShowCharts = viewModel::setShowCharts,
                    getHistory = viewModel::getSensorHistory,
                    unitsConverter = unitsConverter,
                    viewPeriod = viewPeriod,
                    getSyncStatusFlow = viewModel::getGattEvents,
                    getChartClearedFlow = viewModel::getChartCleared,
                    disconnectGattAction = viewModel::disconnectGatt,
                    shouldSkipGattSyncDialog = viewModel::shouldSkipGattSyncDialog,
                    syncGatt = viewModel::syncGatt,
                    setViewPeriod = viewModel::setViewPeriod,
                    exportToCsv = viewModel::exportToCsv ,
                    removeTagData= viewModel::removeTagData,
                    refreshStatus = viewModel::refreshStatus,
                    dontShowGattSyncDescription = viewModel::dontShowGattSyncDescription
                )
            }
        }
    }

    companion object {
        const val ARGUMENT_SENSOR_ID = "ARGUMENT_SENSOR_ID"
        const val ARGUMENT_SHOW_CHART = "ARGUMENT_SHOW_CHART"

        fun start(context: Context, sensorId: String, showChart: Boolean = false) {
            val intent = Intent(context, SensorCardActivity::class.java)
            intent.putExtra(ARGUMENT_SENSOR_ID, sensorId)
            intent.putExtra(ARGUMENT_SHOW_CHART, showChart)
            context.startActivity(intent)
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun SensorsPager(
    selectedIndex: Int,
    sensors: List<RuuviTag>,
    showCharts: Boolean,
    setShowCharts: (Boolean) -> Unit,
    getHistory: (String) -> List<TagSensorReading>,
    unitsConverter: UnitsConverter,
    viewPeriod: Days,
    getSyncStatusFlow: (String) -> Flow<SyncStatus>,
    getChartClearedFlow: (String) -> Flow<String>,
    disconnectGattAction: (String) -> Unit,
    shouldSkipGattSyncDialog: () -> Boolean,
    syncGatt: (String) -> Unit,
    setViewPeriod: (Int) -> Unit,
    exportToCsv: (String) -> Uri?,
    removeTagData: (String) -> Unit,
    refreshStatus: () -> Unit,
    dontShowGattSyncDescription: () -> Unit
) {
    Timber.d("SensorsPager selected $selectedIndex")
    val systemUiController = rememberSystemUiController()
    val isDarkTheme = isSystemInDarkTheme()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(selectedIndex)

    var pagerSensor by remember {
        mutableStateOf(sensors.getOrNull(selectedIndex))
    }

    LaunchedEffect(pagerState, sensors.size) {
        snapshotFlow { pagerState.currentPage }.collectLatest { page ->
            Timber.d("page changed to $page")
            delay(100)
            pagerSensor = sensors.getOrNull(page)
        }
    }

    Timber.d("page sensor $pagerSensor sensors = $sensors")

    pagerSensor?.let { sensor ->
        if (sensor.userBackground != null) {
            val uri = Uri.parse(sensor.userBackground)
            if (uri.path != null) {
                Crossfade(targetState = uri) {
                    Timber.d("image for sensor ${sensor.displayName}")
                    SensorCardImage(it, showCharts)
                }
            }
        }
    }

    Box(modifier = Modifier.systemBarsPadding()) {
        Column() {
            SensorCardTopAppBar(
                navigationCallback = {
                    (context as Activity).onBackPressed()
                },
                chartsEnabled = showCharts,
                alertAction = {},
                chartsAction = { setShowCharts(!showCharts) },
                settingsAction = {
                    if (pagerSensor != null) {
                        TagSettingsActivity.start(context, pagerSensor?.id)
                    }
                }
            )

            HorizontalPager(
                modifier = Modifier.fillMaxSize(),
                state = pagerState,
                userScrollEnabled = !showCharts,
                count = sensors.size
            ) { page ->
                val sensor = sensors.getOrNull(page)
                if (sensor != null) {

                    val temperatureChart by remember {
                        mutableStateOf(LineChart(context))
                    }

                    val humidityChart by remember {
                        mutableStateOf(LineChart(context))
                    }

                    val pressureChart by remember {
                        mutableStateOf(LineChart(context))
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Top
                    ) {
                        SensorTitle(sensor = sensor)
                        if (showCharts) {
                            ChartControlElement2(
                                sensorId = sensor.id,
                                viewPeriod = viewPeriod,
                                syncStatus = getSyncStatusFlow.invoke(sensor.id),
                                disconnectGattAction = disconnectGattAction,
                                shouldSkipGattSyncDialog = shouldSkipGattSyncDialog,
                                syncGatt = syncGatt,
                                setViewPeriod = setViewPeriod,
                                exportToCsv = exportToCsv,
                                removeTagData = removeTagData,
                                refreshStatus = refreshStatus,
                                dontShowGattSyncDescription = dontShowGattSyncDescription
                            )

                            ChartsView(
                                sensorId = sensor.id,
                                temperatureChart = temperatureChart,
                                humidityChart = humidityChart,
                                pressureChart = pressureChart,
                                getHistory = getHistory,
                                unitsConverter = unitsConverter,
                                selected = pagerSensor?.id == sensor.id,
                                chartCleared = getChartClearedFlow(sensor.id)
                            )
                        } else {
                            SensorCard(sensor = sensor)
                        }
                    }
                }
            }
        }
    }

    SideEffect {
        systemUiController.setStatusBarColor(
            color = Color.Transparent,
            darkIcons = !isDarkTheme
        )
        systemUiController.setNavigationBarColor(
            color = Color.Transparent,
            darkIcons = !isDarkTheme
        )
    }
}

@Composable
fun SensorTitle(sensor: RuuviTag) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Subtitle(
            text = sensor.displayName
        )
    }
}

@Composable
fun SensorCard(sensor: RuuviTag) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 64.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Bottom
    ) {
        Paragraph(text = sensor.temperatureString)
        Paragraph(text = sensor.humidityString)
        Paragraph(text = sensor.pressureString)
    }
}

@Composable
fun SensorCardTopAppBar(
    navigationCallback: () -> Unit,
    chartsEnabled: Boolean,
    alertAction: () -> Unit,
    chartsAction: () -> Unit,
    settingsAction: () -> Unit
) {
    TopAppBar(
        title = {
            Image(
                modifier = Modifier.height(40.dp),
                painter = painterResource(id = R.drawable.logo_2021),
                contentDescription = "",
                colorFilter = ColorFilter.tint(Color.White)
            )
        },
        navigationIcon = {
            IconButton(
                onClick = { navigationCallback.invoke() }) {
                Icon(Icons.Default.ArrowBack, stringResource(id = R.string.back))
            }
        },
        actions = {
            IconButton(onClick = { }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_notifications_on_24px),
                    contentDescription = ""
                )
            }
            val chartIconRes = if (chartsEnabled) {
                R.drawable.icon_temperature
            } else {
                R.drawable.ic_ruuvi_graphs_icon
            }
            IconButton(onClick = { chartsAction.invoke() }) {
                Icon(
                    painter = painterResource(id = chartIconRes),
                    tint = White,
                    contentDescription = ""
                )
            }
            IconButton(onClick = { settingsAction.invoke() }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_settings_24px),
                    tint = White,
                    contentDescription = stringResource(id = R.string.sensor_settings)
                )
            }

        },
        backgroundColor = Color.Transparent,
        contentColor = RuuviStationTheme.colors.topBarText,
        elevation = 0.dp
    )
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun SensorCardImage(
    userBackground: Uri,
    chartsEnabled: Boolean
) {
    Timber.d("Image path $userBackground")

    GlideImage(
        modifier = Modifier.fillMaxSize(),
        model = userBackground,
        contentDescription = null,
        contentScale = ContentScale.Crop
    )

    Image(
        modifier = Modifier.fillMaxSize(),
        painter = painterResource(id = R.drawable.tag_bg_layer),
        contentDescription = null,
        contentScale = ContentScale.Crop
    )

    if (chartsEnabled) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color(0xCC001D1B))
        )
    }

}