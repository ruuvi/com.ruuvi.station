package com.ruuvi.station.tagdetails.ui

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.app.TaskStackBuilder
import androidx.core.view.WindowCompat
import androidx.lifecycle.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ruuvi.gateway.tester.nfc.model.SensorNfсScanInfo
import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlarmSensorStatus
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.app.ui.components.BlinkingEffect
import com.ruuvi.station.app.ui.theme.*
import com.ruuvi.station.dashboard.DashboardTapAction
import com.ruuvi.station.dashboard.ui.DashboardActivity
import com.ruuvi.station.database.tables.Alarm
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.graph.ChartControlElement2
import com.ruuvi.station.graph.ChartsView
import com.ruuvi.station.graph.model.ChartContainer
import com.ruuvi.station.graph.model.ChartSensorType
import com.ruuvi.station.nfc.domain.NfcScanResponse
import com.ruuvi.station.nfc.ui.NfcInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.isLowBattery
import com.ruuvi.station.tagsettings.ui.TagSettingsActivity
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.util.Period
import com.ruuvi.station.util.base.NfcActivity
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import com.ruuvi.station.util.extensions.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import org.kodein.di.generic.instance
import timber.log.Timber

class SensorCardActivity : NfcActivity(), KodeinAware {

    override val kodein by closestKodein()

    private val unitsConverter: UnitsConverter by instance()

    private val viewModel: SensorCardViewModel by viewModel {
        val preferences: PreferencesRepository by kodein.instance()
        val showChart = when (intent.getSerializableExtra(ARGUMENT_OPEN_TYPE) as? SensorCardOpenType ?: SensorCardOpenType.DEFAULT) {
            SensorCardOpenType.DEFAULT -> preferences.getDashboardTapAction() == DashboardTapAction.SHOW_CHART
            SensorCardOpenType.CARD -> false
            SensorCardOpenType.HISTORY -> true
        }

        SensorCardViewModelArguments(
            intent.getStringExtra(ARGUMENT_SENSOR_ID),
            showChart,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            RuuviTheme {
                val sensors by viewModel.sensorsFlow.collectAsStateWithLifecycle(initialValue = listOf())
                val selectedSensor by viewModel.selectedSensor.collectAsStateWithLifecycle()
                val viewPeriod by viewModel.chartViewPeriod.collectAsState()
                val showCharts by viewModel.showCharts.collectAsStateWithLifecycle(false)
                val syncInProcess by viewModel.syncInProgress.collectAsStateWithLifecycle()
                val showChartStats by viewModel.showChartStats.collectAsStateWithLifecycle()
                val newChartsUI by viewModel.newChartsUI.collectAsStateWithLifecycle()
                val increasedChartSize by viewModel.increasedChartSize.collectAsStateWithLifecycle()

                if (sensors.isNotEmpty()) {
                    SensorsPager(
                        selectedSensor = selectedSensor,
                        sensors = sensors,
                        showCharts = showCharts,
                        showChartStats = showChartStats,
                        graphDrawDots = viewModel.graphDrawDots,
                        syncInProgress = syncInProcess,
                        setShowCharts = viewModel::setShowCharts,
                        getActiveAlarms = viewModel::getActiveAlarms,
                        getHistory = viewModel::getSensorHistory,
                        historyUpdater = viewModel::historyUpdater,
                        unitsConverter = unitsConverter,
                        viewPeriod = viewPeriod,
                        newChartsUI = newChartsUI,
                        getSyncStatusFlow = viewModel::getGattEvents,
                        getChartClearedFlow = viewModel::getChartCleared,
                        disconnectGattAction = viewModel::disconnectGatt,
                        shouldSkipGattSyncDialog = viewModel::shouldSkipGattSyncDialog,
                        syncGatt = viewModel::syncGatt,
                        setViewPeriod = viewModel::setViewPeriod,
                        exportToCsv = viewModel::exportToCsv ,
                        exportToXlsx = viewModel::exportToXlsx ,
                        removeTagData= viewModel::removeTagData,
                        refreshStatus = viewModel::refreshStatus,
                        increasedChartSize = increasedChartSize,
                        changeIncreasedChartSize = viewModel::changeIncreaseChartSize,
                        dontShowGattSyncDescription = viewModel::dontShowGattSyncDescription,
                        getNfcScanResponse = viewModel::getNfcScanResponse,
                        addSensor = viewModel::addSensor,
                        changeShowStats = viewModel::changeShowChartStats,
                        saveSelected = viewModel::saveSelected,
                        getIndex = viewModel::getIndex
                    )
                }
            }
        }
    }

    companion object {
        const val ARGUMENT_SENSOR_ID = "ARGUMENT_SENSOR_ID"
        const val ARGUMENT_OPEN_TYPE = "ARGUMENT_OPEN_TYPE"

        fun start(
            context: Context,
            sensorId: String,
            openType: SensorCardOpenType = SensorCardOpenType.DEFAULT
        ) {
            val intent = Intent(context, SensorCardActivity::class.java)
            intent.putExtra(ARGUMENT_SENSOR_ID, sensorId)
            intent.putExtra(ARGUMENT_OPEN_TYPE, openType)
            context.startActivity(intent)
        }

        fun startWithDashboard(
            context: Context,
            sensorId: String,
            openType: SensorCardOpenType = SensorCardOpenType.DEFAULT
        ) {
            val intent = Intent(context, SensorCardActivity::class.java)
            intent.putExtra(ARGUMENT_SENSOR_ID, sensorId)
            intent.putExtra(ARGUMENT_OPEN_TYPE, openType)

            val stackBuilder = TaskStackBuilder.create(context)
            val intentDashboardActivity = Intent(context, DashboardActivity::class.java)
            stackBuilder.addNextIntent(intentDashboardActivity)
            stackBuilder.addNextIntent(intent)

            stackBuilder.startActivities()
        }

        fun createPendingIntent(
            context: Context,
            sensorId: String,
            requestCode: Int,
            openType: SensorCardOpenType = SensorCardOpenType.DEFAULT
        ): PendingIntent? {
            val intent = Intent(context, SensorCardActivity::class.java)
            intent.putExtra(ARGUMENT_SENSOR_ID, sensorId)
            intent.putExtra(ARGUMENT_OPEN_TYPE, openType)

            val stackBuilder = TaskStackBuilder.create(context)
            val intentDashboardActivity = Intent(context, DashboardActivity::class.java)
            stackBuilder.addNextIntent(intentDashboardActivity)
            stackBuilder.addNextIntent(intent)

            return stackBuilder
                .getPendingIntent(requestCode, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}

enum class SensorCardOpenType {
    DEFAULT,
    CARD,
    HISTORY
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SensorsPager(
    selectedSensor: String?,
    sensors: List<RuuviTag>,
    showCharts: Boolean,
    showChartStats: Boolean,
    syncInProgress: Boolean,
    graphDrawDots: Boolean,
    setShowCharts: (Boolean) -> Unit,
    getActiveAlarms: (String) -> List<Alarm>,
    getHistory: (String) -> List<TagSensorReading>,
    historyUpdater: (String) -> Flow<MutableList<ChartContainer>>,
    unitsConverter: UnitsConverter,
    viewPeriod: Period,
    newChartsUI: Boolean,
    increasedChartSize: Boolean,
    getSyncStatusFlow: (String) -> Flow<SyncStatus>,
    getChartClearedFlow: (String) -> Flow<String>,
    disconnectGattAction: (String) -> Unit,
    shouldSkipGattSyncDialog: () -> Boolean,
    syncGatt: (String) -> Unit,
    setViewPeriod: (Int) -> Unit,
    exportToCsv: (String) -> Uri?,
    exportToXlsx: (String) -> Uri?,
    removeTagData: (String) -> Unit,
    refreshStatus: () -> Unit,
    dontShowGattSyncDescription: () -> Unit,
    getNfcScanResponse: (SensorNfсScanInfo) -> NfcScanResponse,
    addSensor: (String) -> Unit,
    changeShowStats: () -> Unit,
    changeIncreasedChartSize: () -> Unit,
    saveSelected: (String) -> Unit,
    getIndex: (String) -> Int
) {
    Timber.d("SensorsPager selected $selectedSensor sensors count ${sensors.size}")
    val systemUiController = rememberSystemUiController()
    val isDarkTheme = isSystemInDarkTheme()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = sensors.firstOrNull { sensor -> sensor.id == selectedSensor }
            ?.let { sensors.indexOf(it) } ?: 0,
        initialPageOffsetFraction = 0f
    ) {
        return@rememberPagerState sensors.size
    }

    var pagerSensor by remember {
        mutableStateOf(sensors.firstOrNull{it.id == selectedSensor})
    }

    Surface(
        color = DefaultSensorBackgroundDark,
        modifier = Modifier.fillMaxSize()
        ) {

    }

    pagerSensor = sensors.getOrNull(pagerState.currentPage)
    LaunchedEffect(pagerState, sensors.size) {
        snapshotFlow { pagerState.currentPage }.distinctUntilChanged().collectLatest { page ->
            Timber.d("page changed to $page")
            delay(100)
            pagerSensor = sensors.getOrNull(page)
        }
    }

    Timber.d("page sensor $pagerSensor bg= ${pagerSensor?.userBackground}")

    pagerSensor?.let { sensor ->
        if (sensor.userBackground != null) {
            val uri = Uri.parse(sensor.userBackground)
            if (uri.path != null) {
                if (showCharts) {
                    SensorCardImage(uri, showCharts)
                } else {
                    Crossfade(targetState = uri, label = "switch background") {
                        Timber.d("image for sensor ${sensor.displayName}")
                        SensorCardImage(it, showCharts)
                    }
                }
            }
        }
    }

    NfcInteractor(
        addSensor = addSensor,
        getNfcScanResponse = getNfcScanResponse
    )

    Box(modifier = Modifier.systemBarsPadding()) {
        Column() {
            SensorCardTopAppBar(
                navigationCallback = {
                    (context as Activity).onBackPressed()
                },
                chartsEnabled = showCharts,
                alarmStatus = pagerSensor?.alarmSensorStatus ?: AlarmSensorStatus.NoAlarms,
                alarmAction = {
                    if (pagerSensor != null) {
                        TagSettingsActivity.start(context, pagerSensor?.id)
                    }
                },
                chartsAction = { setShowCharts(!showCharts) },
                settingsAction = {
                    if (pagerSensor != null) {
                        TagSettingsActivity.start(context, pagerSensor?.id)
                    }
                }
            )

            pagerSensor?.let {
                SensorTitle(
                    sensor = it,
                    pagerState = pagerState
                )
            }

            HorizontalPager(
                modifier = Modifier.fillMaxSize(),
                state = pagerState,
                userScrollEnabled = !showCharts,
            ) { page ->
                val sensor = sensors.getOrNull(page)
                if (sensor != null) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Top
                    ) {
                        if (showCharts) {
                            val hideIncreaseChartSize = sensor.latestMeasurement?.humidityValue == null || sensor.latestMeasurement.pressureValue == null
                            ChartControlElement2(
                                sensorId = sensor.id,
                                showChartStats = showChartStats,
                                viewPeriod = viewPeriod,
                                syncStatus = getSyncStatusFlow.invoke(sensor.id),
                                disconnectGattAction = disconnectGattAction,
                                shouldSkipGattSyncDialog = shouldSkipGattSyncDialog,
                                syncGatt = syncGatt,
                                setViewPeriod = setViewPeriod,
                                exportToCsv = exportToCsv,
                                exportToXlsx = exportToXlsx,
                                removeTagData = removeTagData,
                                refreshStatus = refreshStatus,
                                dontShowGattSyncDescription = dontShowGattSyncDescription,
                                changeShowStats = changeShowStats,
                                increasedChartSize = increasedChartSize,
                                hideIncreaseChartSize = hideIncreaseChartSize,
                                changeIncreasedChartSize = changeIncreasedChartSize
                            )
                            var size by remember { mutableStateOf(Size.Zero)}
                            ChartsView(
                                modifier = Modifier
                                    .weight(1f)
                                    .onGloballyPositioned { coordinates ->
                                        size = coordinates.size.toSize()
                                        Timber.d("ChartsView size $size")
                                    },
                                sensor = sensor,
                                getHistory = getHistory,
                                unitsConverter = unitsConverter,
                                graphDrawDots = graphDrawDots,
                                selected = pagerSensor?.id == sensor.id,
                                viewPeriod = viewPeriod,
                                newChartsUI = newChartsUI,
                                chartCleared = getChartClearedFlow(sensor.id),
                                showChartStats = showChartStats,
                                historyUpdater = historyUpdater,
                                getActiveAlarms = getActiveAlarms,
                                increasedChartSize = increasedChartSize,
                                size = size
                            )
                        } else {
                            SensorCard(
                                sensor = sensor,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        SensorCardBottom(
                            sensor = sensor,
                            syncInProgress = syncInProgress,
                            modifier = Modifier
                                .height(intrinsicSize = IntrinsicSize.Min)
                        )
                    }
                }
            }
        }
    }

    SideEffect {
        systemUiController.setStatusBarColor(
            color = Color.Transparent,
            darkIcons = false
        )
        systemUiController.setNavigationBarColor(
            color = Color.Transparent,
            navigationBarContrastEnforced = false,
            darkIcons = false
        )
    }

    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> {

                }
                Lifecycle.Event.ON_START -> {
                    pagerSensor?.let {
                        val index = getIndex(it.id)
                        Timber.d("SensorsPager onStart selectedSensor = ${it.id} index = $index")
                        coroutineScope.launch {
                            pagerState.scrollToPage(index)
                        }
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                }
                Lifecycle.Event.ON_PAUSE -> {
                }
                Lifecycle.Event.ON_STOP -> {
                    pagerSensor?.let {sensor ->
                        saveSelected(sensor.id)
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                }
                else -> {}
            }
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // When the effect leaves the Composition, remove the observer
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SensorTitle(
    sensor: RuuviTag,
    pagerState: PagerState
) {
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()) {
        Box(modifier = Modifier
            .align(Alignment.TopStart)
            .padding(start = 6.dp)
        ) {
            if (pagerState.canScrollBackward && pagerState.currentPage != 0) {
                IconButton(modifier = Modifier.size(RuuviStationTheme.dimensions.buttonHeightSmall), onClick = {
                    if (pagerState.canScrollBackward && pagerState.currentPage != 0) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.arrow_back_16),
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .padding(horizontal = RuuviStationTheme.dimensions.huge)
                .align(Alignment.Center)
                .width(IntrinsicSize.Max),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                fontSize =  RuuviStationTheme.fontSizes.big,
                fontFamily = RuuviStationTheme.fonts.mulishExtraBold,
                text = sensor.displayName,
                textAlign = TextAlign.Center,
                color = Color.White,
                maxLines = 2
            )
        }
        Box(modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(end = 6.dp)
        ) {
            if (pagerState.canScrollForward && pagerState.currentPage != pagerState.pageCount - 1) {
                IconButton(modifier = Modifier.size(RuuviStationTheme.dimensions.buttonHeightSmall), onClick = {
                    if (pagerState.canScrollForward && pagerState.currentPage != pagerState.pageCount -1) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.arrow_forward_16),
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun SensorCard(
    modifier: Modifier = Modifier,
    sensor: RuuviTag
) {
    ConstraintLayout(
        modifier = modifier
            .fillMaxSize()
    ) {
        val (temperatureValue, temperatureUnit, otherValues, lowBattery) = createRefs()

        if (sensor.latestMeasurement?.temperatureValue != null) {
            Text(
                modifier = Modifier
                    .constrainAs(temperatureValue) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                    .padding(top = 48.dp),
                fontSize = 72.sp,
                fontFamily = ruuviStationFonts.oswaldBold,
                text = sensor.latestMeasurement.temperatureValue.valueWithoutUnit,
                lineHeight = 10.sp,
                color = Color.White
            )

            Text(
                modifier = Modifier
                    .constrainAs(temperatureUnit) {
                        top.linkTo(temperatureValue.top)
                        start.linkTo(temperatureValue.end)
                    }
                    .padding(
                        top = 48.dp + 18.dp * LocalDensity.current.fontScale,
                        start = 2.dp
                    ),
                fontSize = 36.sp,
                fontFamily = ruuviStationFonts.oswaldRegular,
                text = sensor.latestMeasurement.temperatureValue.unitString,
                color = Color.White
            )
        }

        SensorValues(
            modifier = Modifier
                .constrainAs(otherValues) {
                    start.linkTo(parent.start)
                    bottom.linkTo(parent.bottom)
                    top.linkTo(parent.top)
                }
                .padding(
                    start = RuuviStationTheme.dimensions.extended
                ),
            sensor = sensor
        )

        if (sensor.isLowBattery()) {
            SensorCardLowBattery(
                modifier = Modifier
                    .constrainAs(lowBattery) {
                        end.linkTo(parent.end)
                        bottom.linkTo(parent.bottom)
                    }
            )
        }
    }
}

@Composable
fun SensorValues(
    modifier: Modifier,
    sensor: RuuviTag
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.Start
    ) {
        sensor.latestMeasurement?.humidityValue?.let {
            SensorValueItem(R.drawable.icon_measure_humidity, it.valueWithoutUnit, it.unitString)
            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
        }

        sensor.latestMeasurement?.pressureValue?.let {
            SensorValueItem(R.drawable.icon_measure_pressure, it.valueWithoutUnit, it.unitString)
            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
        }

        sensor.latestMeasurement?.movementValue?.let {
            SensorValueItem(R.drawable.ic_icon_measure_movement, it.valueWithoutUnit, it.unitString)
            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))

        }
    }
}

@Composable
fun SensorValueItem(
    icon: Int,
    value: String,
    unit: String
) {
    Row (
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
            ) {
        Icon(
            modifier = Modifier.size(48.dp),
            painter = painterResource(id = icon),
            tint = Color.White,
            contentDescription = ""
        )
        Row() {
            Text(
                modifier = Modifier
                    .alignByBaseline()
                    .padding(
                        start = RuuviStationTheme.dimensions.extended
                    ),
                fontSize = RuuviStationTheme.fontSizes.extended,
                style = RuuviStationTheme.typography.dashboardBigValueUnit,
                fontFamily = ruuviStationFonts.mulishBold,
                fontWeight = FontWeight.Bold,
                text = value,
                color = Color.White
            )

            Text(
                modifier = Modifier
                    .alignByBaseline()
                    .padding(
                        start = RuuviStationTheme.dimensions.small
                    ),
                style = RuuviStationTheme.typography.dashboardSecondary,
                color = White80,
                fontSize = RuuviStationTheme.fontSizes.compact,
                text = unit,
            )
        }
    }
}

@Composable
fun SensorCardTopAppBar(
    navigationCallback: () -> Unit,
    chartsEnabled: Boolean,
    alarmStatus: AlarmSensorStatus = AlarmSensorStatus.NoAlarms,
    alarmAction: () -> Unit,
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
            IconButton(onClick = { alarmAction.invoke() }) {
                when (alarmStatus) {
                    AlarmSensorStatus.NoAlarms ->
                        Icon(
                        painter = painterResource(id = R.drawable.ic_notifications_off_24px),
                        contentDescription = "")
                    AlarmSensorStatus.NotTriggered ->
                        Icon(
                            painter = painterResource(id = R.drawable.ic_notifications_on_24px),
                            tint = Color.White,
                            contentDescription = "")
                    is AlarmSensorStatus.Triggered ->
                        BlinkingEffect() {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_notifications_active_24px),
                                contentDescription = null,
                                tint = RuuviStationTheme.colors.activeAlert
                            )
                        }
                }
            }

            IconButton(onClick = { chartsAction.invoke() }) {
                val chartIconRes = if (chartsEnabled) {
                    R.drawable.icon_temperature
                } else {
                    R.drawable.ic_ruuvi_graphs_icon
                }
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

@Composable
fun SensorCardLowBattery(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = modifier.padding(end = RuuviStationTheme.dimensions.medium)
    ) {
        Text(
            color = White50,
            style = RuuviStationTheme.typography.dashboardSecondary,
            textAlign = TextAlign.Right,
            text = stringResource(id = R.string.low_battery),
        )
        Spacer(modifier = Modifier.width(RuuviStationTheme.dimensions.medium))
        Image(
            modifier = Modifier
                .height(12.dp)
                .width(24.dp)
                .align(Alignment.CenterVertically),
            painter = painterResource(id = R.drawable.icon_battery_low),
            contentDescription = null
        )
    }
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

@Composable
fun SensorCardBottom(
    sensor: RuuviTag,
    syncInProgress: Boolean,
    modifier: Modifier = Modifier
) {
    if (sensor.latestMeasurement != null) {
        val context = LocalContext.current
        val lifecycle = LocalLifecycleOwner.current

        var updatedText by remember {
            mutableStateOf(sensor.latestMeasurement.updatedAt?.describingTimeSince(context) ?: "")
        }

        var syncText by remember { mutableStateOf("") }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .padding(RuuviStationTheme.dimensions.medium)
                .fillMaxWidth()
        ) {
            val icon = sensor.getSource().getIconResource()
            val fontScale = LocalContext.current.resources.configuration.fontScale

            if (syncInProgress) {
                Text(
                    style = RuuviStationTheme.typography.dashboardSecondary,
                    color = White50,
                    textAlign = TextAlign.Left,
                    text = syncText,
                )
            }

            Text(
                modifier = Modifier.weight(1f),
                style = RuuviStationTheme.typography.dashboardSecondary,
                color = White50,
                textAlign = TextAlign.Right,
                text = updatedText,
            )

            Spacer(modifier = Modifier.width(RuuviStationTheme.dimensions.medium))

            Icon(
                modifier = Modifier
                    .height(RuuviStationTheme.dimensions.mediumPlus * fontScale)
                    .width((24 * fontScale).dp),
                painter = painterResource(id = icon),
                tint = White50,
                contentDescription = null,
            )
        }

        LaunchedEffect(key1 = lifecycle, key2 = sensor.latestMeasurement.updatedAt) {
            lifecycle.whenStarted {
                while (true) {
                    updatedText =
                        sensor.latestMeasurement.updatedAt?.describingTimeSince(context) ?: ""
                    delay(500)
                }
            }
        }

        val baseSyncText = stringResource(id = R.string.synchronizing)
        LaunchedEffect(key1 = syncInProgress) {
            var dotsCount = 0
            while (syncInProgress) {
                var syncTextTemp = baseSyncText

                for (j in 1..dotsCount) {
                    syncTextTemp = syncTextTemp + "."
                }

                syncText = syncTextTemp
                dotsCount++
                if (dotsCount > 3) dotsCount = 0
                delay(700)
            }
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = modifier
                .padding(RuuviStationTheme.dimensions.medium)
                .fillMaxWidth()
        ) {
            Text(
                modifier = Modifier.weight(1f),
                style = RuuviStationTheme.typography.dashboardSecondary,
                color = White50,
                fontSize = RuuviStationTheme.fontSizes.compact,
                textAlign = TextAlign.Center,
                text = stringResource(id = R.string.no_data_10_days),
            )
        }
    }
}