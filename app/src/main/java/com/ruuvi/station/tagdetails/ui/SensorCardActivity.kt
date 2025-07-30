package com.ruuvi.station.tagdetails.ui

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.unit.toSize
import androidx.core.app.TaskStackBuilder
import androidx.core.view.WindowCompat
import androidx.lifecycle.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ruuvi.gateway.tester.nfc.model.SensorNfсScanInfo
import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlarmSensorStatus
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.app.ui.components.BlinkingEffect
import com.ruuvi.station.app.ui.components.CircularIndicator
import com.ruuvi.station.app.ui.components.limitScaleTo
import com.ruuvi.station.app.ui.components.modifier.fadingEdge
import com.ruuvi.station.app.ui.components.scaleUpTo
import com.ruuvi.station.app.ui.theme.*
import com.ruuvi.station.dashboard.DashboardTapAction
import com.ruuvi.station.dashboard.ui.DashboardActivity
import com.ruuvi.station.feature.data.FeatureFlag
import com.ruuvi.station.feature.domain.RuntimeBehavior
import com.ruuvi.station.graph.ChartControlElement2
import com.ruuvi.station.graph.ChartsView
import com.ruuvi.station.graph.model.ChartContainer
import com.ruuvi.station.nfc.domain.NfcScanResponse
import com.ruuvi.station.nfc.ui.NfcInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.UpdateSource
import com.ruuvi.station.tag.domain.isLowBattery
import com.ruuvi.station.tagdetails.ui.elements.BigValueDisplay
import com.ruuvi.station.tagdetails.ui.elements.CircularAQIDisplay
import com.ruuvi.station.tagdetails.ui.elements.SensorCardLegacy
import com.ruuvi.station.tagdetails.ui.elements.SensorValueItem
import com.ruuvi.station.tagdetails.ui.elements.ValueBottomSheet
import com.ruuvi.station.tagsettings.ui.TagSettingsActivity
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.units.model.UnitType
import com.ruuvi.station.util.Period
import com.ruuvi.station.util.base.NfcActivity
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import com.ruuvi.station.util.extensions.*
import com.ruuvi.station.util.ui.pxToDp
import com.ruuvi.station.vico.model.ChartData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import org.kodein.di.generic.instance
import timber.log.Timber
import kotlin.math.ceil

class SensorCardActivity : NfcActivity(), KodeinAware {

    override val kodein by closestKodein()

    private val unitsConverter: UnitsConverter by instance()
    private val runtimeBehavior: RuntimeBehavior by instance()

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

        val newSensorCard = runtimeBehavior.isFeatureEnabled(FeatureFlag.NEW_SENSOR_CARD)

        setContent {
            RuuviTheme {
                val sensors by viewModel.sensorsFlow.collectAsStateWithLifecycle(initialValue = listOf())
                val selectedSensor by viewModel.selectedSensor.collectAsStateWithLifecycle()
                val viewPeriod by viewModel.chartViewPeriod.collectAsStateWithLifecycle()
                val showCharts by viewModel.showCharts.collectAsStateWithLifecycle(false)
                val syncInProcess by viewModel.syncInProgress.collectAsStateWithLifecycle()
                val showChartStats by viewModel.showChartStats.collectAsStateWithLifecycle()
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
                        historyUpdater = viewModel::historyUpdater,
                        unitsConverter = unitsConverter,
                        viewPeriod = viewPeriod,
                        newSensorCard = newSensorCard,
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
                        getIndex = viewModel::getIndex,
                        getChartData = viewModel::getChartData
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

@Composable
fun SensorsPager(
    selectedSensor: String?,
    sensors: List<RuuviTag>,
    showCharts: Boolean,
    showChartStats: Boolean,
    syncInProgress: Boolean,
    graphDrawDots: Boolean,
    setShowCharts: (Boolean) -> Unit,
    historyUpdater: (String) -> Flow<MutableList<ChartContainer>>,
    unitsConverter: UnitsConverter,
    viewPeriod: Period,
    increasedChartSize: Boolean,
    newSensorCard: Boolean,
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
    getIndex: (String) -> Int,
    getChartData: (String, UnitType, Int) -> Flow<ChartData>
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
                syncInProgress = syncInProgress,
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
                            val hideIncreaseChartSize = sensor.latestMeasurement?.humidity == null || sensor.latestMeasurement.pressure == null
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
                                unitsConverter = unitsConverter,
                                graphDrawDots = graphDrawDots,
                                selected = pagerSensor?.id == sensor.id,
                                viewPeriod = viewPeriod,
                                chartCleared = getChartClearedFlow(sensor.id),
                                showChartStats = showChartStats,
                                historyUpdater = historyUpdater,
                                increasedChartSize = increasedChartSize,
                                size = size
                            )
                        } else {
                            if (newSensorCard) {
                                SensorCard(
                                    sensor = sensor,
                                    modifier = Modifier.weight(1f),
                                    getChartData = getChartData
                                )
                            } else {
                                SensorCardLegacy(
                                    sensor = sensor,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        SensorCardBottom(
                            sensor = sensor,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorCard(
    modifier: Modifier = Modifier,
    sensor: RuuviTag,
    getChartData: (String, UnitType, Int) -> Flow<ChartData>,
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    var sheetValue by remember { mutableStateOf<EnvironmentValue?>(null) }
    val itemHeight = RuuviStationTheme.dimensions.sensorCardValueItemHeight.scaleUpTo(1.5f)
    var size by remember { mutableStateOf(IntSize.Zero) }
    var topSize by remember { mutableStateOf(IntSize.Zero) }
    val halfSize = (size.height / 2).pxToDp()
    val scrollState = rememberScrollState()
    val valuesWithoutFirst = if (sensor.valuesToDisplay.isNotEmpty()) {
        sensor.valuesToDisplay.subList(1, sensor.valuesToDisplay.size)
    } else {
        listOf()
    }
    val padding = if (halfSize < 200.dp) 8.dp else 48.dp

    val columnModifier = modifier.fadingEdge(scrollState)

    Column(
        modifier = columnModifier
            .fillMaxSize()
            .onGloballyPositioned { layoutCoordinates ->
                size = layoutCoordinates.size
            }
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {

        if (size.height > 0) {
            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = halfSize)
                    .padding(padding)
                    .onGloballyPositioned { layoutCoordinates ->
                        topSize = layoutCoordinates.size
                    }
            ) {
                val firstValue = sensor.valuesToDisplay.firstOrNull()
                if (firstValue != null) {
                    if (firstValue.unitType is UnitType.AirQuality) {
                        if (sensor.latestMeasurement != null) {
                            CircularAQIDisplay(
                                value = firstValue,
                                aqi = sensor.latestMeasurement.aqiScore,
                                alertActive = firstValue.unitType.alarmType?.let {
                                    sensor.alarmSensorStatus.triggered(it)
                                } ?: false
                            ) {
                                showBottomSheet = true
                                sheetValue = firstValue
                            }
                        }
                    } else {
                        BigValueDisplay(
                            value = firstValue,
                            showName = true,
                            alertActive = firstValue.unitType.alarmType?.let {
                                sensor.alarmSensorStatus.triggered(it)
                            } ?: false
                        ) {
                            showBottomSheet = true
                            sheetValue = firstValue
                        }
                    }
                }
            }

            val configuration = LocalConfiguration.current
            val columnCount = if (configuration.screenWidthDp > 650) {
                3
            } else {
                2
            }

            if (ceil(valuesWithoutFirst.size / columnCount.toFloat()) * (itemHeight + 8.dp) - 8.dp <= (size.height / 2).pxToDp()) {
                Timber.d("Measure ${size.height} - ${topSize.height}")

                Column(
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .height(halfSize)
                        //.height((size.height - max(halfSize.dpToPx(),topSize.height + padding.dpToPx())).pxToDp())
                        .fillMaxWidth()
                ) {
                    SensorValues(
                        modifier = Modifier,
                        sensor = sensor,
                        itemHeight = itemHeight
                    ) {
                        showBottomSheet = true
                        sheetValue = it
                    }
                }
            } else {
                SensorValues(
                    modifier = Modifier,
                    sensor = sensor,
                    itemHeight = itemHeight,
                ) {
                    showBottomSheet = true
                    sheetValue = it
                }
            }
        }
    }

    if (showBottomSheet) {
        sheetValue?.let { value ->

            val chartHistory = getChartData(sensor.id, value.unitType, 48).collectAsState(null)

            ValueBottomSheet(
                sheetValue = value,
                chartHistory = chartHistory.value,
                modifier = Modifier
            ) {
                showBottomSheet = false
            }
        }
    }
}

fun <T> distributeRoundRobin(list: List<T>, n: Int): List<List<T>> {
    require(n > 0) { "Number of groups must be > 0" }

    // Create n empty mutable lists
    val result = List(n) { mutableListOf<T>() }

    // Distribute each item to the appropriate sublist
    list.forEachIndexed { index, item ->
        result[index % n].add(item)
    }

    return result
}

@Composable
fun SensorValues(
    modifier: Modifier,
    sensor: RuuviTag,
    itemHeight: Dp,
    onValueClick: (EnvironmentValue) -> Unit
) {
    if (sensor.valuesToDisplay.size <= 1) return
    val valuesWithoutFirst = sensor.valuesToDisplay.subList(1, sensor.valuesToDisplay.size)

    val configuration = LocalConfiguration.current
    val columnCount = if (configuration.screenWidthDp > 650) {
        3
    } else {
        2
    }

    val valuesDistributed = distributeRoundRobin(valuesWithoutFirst, columnCount)
    val columnMaxWidth = 200.dp

    Row(
        modifier = modifier
            .widthIn(max = columnMaxWidth * columnCount)
            .padding(horizontal = RuuviStationTheme.dimensions.screenPadding),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        for (columnValues in valuesDistributed) {
            Column(
                modifier = Modifier
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
                horizontalAlignment = Alignment.Start
            ) {
                if (columnValues.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth()) {}
                }
                for (value in columnValues) {
                    SensorValueItem(
                        icon = value.unitType.iconRes,
                        value = value.valueWithoutUnit,
                        unit = value.unitString,
                        itemHeight = itemHeight,
                        modifier = Modifier.fillMaxWidth(),
                        alertActive = value.unitType.alarmType?.let {
                            sensor.alarmSensorStatus.triggered(it)
                        } ?: false,
                        name = value.unitType.measurementTitle.let { stringResource(it) }
                    )
                    {
                        onValueClick.invoke(value)
                    }
                }
            }
        }
    }
}

@Composable
fun VerticalScrollbarOverlay(
    scrollState: ScrollState,
    scrollbarProportion: Float,
    modifier: Modifier = Modifier
) {

    var boxHeightPx by remember { mutableStateOf(0) }
    val scrollBarHeight = boxHeightPx.pxToDp() * scrollbarProportion
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(4.dp)
            .background(Color.LightGray.copy(alpha = 0.3f))
            .onGloballyPositioned { coordinates ->
                boxHeightPx = coordinates.size.height
            }

    ) {
        val proportion = scrollState.value.toFloat() / scrollState.maxValue.toFloat()

        val offset = (boxHeightPx.pxToDp() - scrollBarHeight) * proportion

        Box(
            modifier = Modifier
                .offset(y = offset)
                .width(4.dp)
                .height(scrollBarHeight)
                .background(Color.White.copy(alpha = 0.75f), shape = RoundedCornerShape(2.dp))
        )
    }
}

@Composable
fun SensorCardTopAppBar(
    navigationCallback: () -> Unit,
    chartsEnabled: Boolean,
    syncInProgress: Boolean,
    alarmStatus: AlarmSensorStatus = AlarmSensorStatus.NoAlarms,
    alarmAction: () -> Unit,
    chartsAction: () -> Unit,
    settingsAction: () -> Unit
) {
    Box {
        TopAppBar(
            modifier = Modifier.height(RuuviStationTheme.dimensions.topAppBarHeight),
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
                        R.drawable.icon_menu_temperature
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
        if (syncInProgress) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(RuuviStationTheme.dimensions.topAppBarHeight),
                contentAlignment = Alignment.Center
            ) {
                CircularIndicator(color = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun SensorCardLowBattery(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = modifier
    ) {
        Image(
            modifier = Modifier
                .height(12.dp.scaleUpTo(1.5f))
                .width(24.dp.scaleUpTo(1.5f))
                .align(Alignment.CenterVertically),
            painter = painterResource(id = R.drawable.icon_battery_low),
            contentDescription = null
        )
        Text(
            color = White50,
            style = RuuviStationTheme.typography.dashboardSecondary,
            textAlign = TextAlign.Right,
            text = stringResource(id = R.string.low_battery),
            fontSize = ruuviStationFontsSizes.petite.limitScaleTo(1.5f)
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
    modifier: Modifier = Modifier
) {
    if (sensor.latestMeasurement != null) {
        val context = LocalContext.current
        val lifecycle = LocalLifecycleOwner.current

        var updatedText by remember {
            mutableStateOf(sensor.latestMeasurement.updatedAt?.describingTimeSince(context) ?: "")
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = modifier
                .padding(
                    horizontal = RuuviStationTheme.dimensions.screenPadding,
                    vertical = RuuviStationTheme.dimensions.mediumPlus
                )
                .fillMaxWidth()
        ) {
            val icon = sensor.getSource().getIconResource()
            Row (
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier
                        .height(RuuviStationTheme.dimensions.mediumPlus.scaleUpTo(1.5f))
                        .width(24.dp.scaleUpTo(1.5f)),
                    painter = painterResource(id = icon),
                    tint = White50,
                    contentDescription = null,
                )
                if (sensor.getSource() == UpdateSource.Cloud) {
                    Spacer(modifier = Modifier.width(RuuviStationTheme.dimensions.medium))
                }
                Text(
                    modifier = Modifier,
                    style = RuuviStationTheme.typography.dashboardSecondary,
                    color = White50,
                    fontSize = ruuviStationFontsSizes.petite.limitScaleTo(1.5f),
                    textAlign = TextAlign.Right,
                    text = updatedText,
                )
            }

            if (sensor.isLowBattery()) {
                SensorCardLowBattery()
            }
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