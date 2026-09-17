package com.ruuvi.station.tagdetails.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.DragScope
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarHost
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlarmSensorStatus
import com.ruuvi.station.alarm.ui.AlarmItemsViewModel
import com.ruuvi.station.app.ui.RuuviTopAppBar
import com.ruuvi.station.app.ui.theme.DefaultSensorBackgroundDark
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.app.ui.theme.Titan70
import com.ruuvi.station.graph.ChartControlElement2
import com.ruuvi.station.graph.ChartsView
import com.ruuvi.station.nfc.ui.NfcInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.isAir
import com.ruuvi.station.tagdetails.ui.elements.SensorCardLegacy
import com.ruuvi.station.tagsettings.ui.RemoveSensor
import com.ruuvi.station.tagsettings.ui.RemoveSensorViewModel
import com.ruuvi.station.tagsettings.ui.SensorAlertsScreen
import com.ruuvi.station.tagsettings.ui.SensorSettingsRootScreen
import com.ruuvi.station.tagsettings.ui.SensorSettingsRoutes
import com.ruuvi.station.tagsettings.ui.TagSettingsViewModel
import com.ruuvi.station.tagsettings.ui.led_control.LedControlScreen
import com.ruuvi.station.tagsettings.ui.led_control.LedControlViewModel
import com.ruuvi.station.tagsettings.ui.notes.Notes
import com.ruuvi.station.tagsettings.ui.notes.NotesViewModel
import com.ruuvi.station.tagsettings.ui.visible_measurements.VisibleMeasurements
import com.ruuvi.station.tagsettings.ui.visible_measurements.VisibleMeasurementsViewModel
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.util.Period
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val SETTINGS_NAVIGATION_ANIMATION_MILLIS = 400
private const val DESTINATION_CROSSFADE_MILLIS = 200
private const val BACKGROUND_COLOR_ANIMATION_MILLIS = 300
private const val MINIMUM_CHARTS_BEFORE_SIZE_INCREASE = 3
private val HISTORY_STATUS_BAR_COLOR = Color(0xE6001D1B)

internal data class SensorDetailViewModelProvider(
    val settings: (String) -> TagSettingsViewModel,
    val alerts: (String) -> AlarmItemsViewModel,
    val removeSensor: (String) -> RemoveSensorViewModel,
    val visibleMeasurements: (String) -> VisibleMeasurementsViewModel,
    val ledControl: (String) -> LedControlViewModel,
    val notes: (String) -> NotesViewModel,
)

@Composable
internal fun SensorDetailRoute(
    viewModel: SensorCardViewModel,
    startDestination: SensorDetailStartDestination,
    viewModelProvider: SensorDetailViewModelProvider,
    unitsConverter: UnitsConverter,
    useNewSensorCard: Boolean,
    onFinish: () -> Unit,
) {
    val sensors by viewModel.sensorsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val selectedSensorId by viewModel.selectedSensor.collectAsStateWithLifecycle()
    val viewPeriod by viewModel.chartViewPeriod.collectAsStateWithLifecycle()
    val syncInProgress by viewModel.syncInProgress.collectAsStateWithLifecycle()
    val showChartStats by viewModel.showChartStats.collectAsStateWithLifecycle()
    val chartSizeLevel by viewModel.chartSizeLevel.collectAsStateWithLifecycle()

    var destination by rememberSaveable { mutableStateOf(startDestination.root) }
    var settingsRoute by rememberSaveable { mutableStateOf(startDestination.settingsRoute) }
    val scaffoldState = rememberScaffoldState()
    val useDarkTheme = destination.forcesDarkTheme || isSystemInDarkTheme()

    fun selectDestination(newDestination: SensorDetailDestination) {
        if (newDestination == destination) return
        settingsRoute = SensorSettingsRoutes.SENSOR_SETTINGS_ROOT
        destination = newDestination
    }

    fun navigateBackFromNestedSettings() {
        if (startDestination.startsInNestedSettings) {
            onFinish()
        } else {
            settingsRoute = SensorSettingsRoutes.SENSOR_SETTINGS_ROOT
        }
    }

    if (sensors.isNotEmpty()) {
        RuuviTheme(darkTheme = useDarkTheme) {
            SensorDetailScreen(
                sensors = sensors,
                selectedSensorId = selectedSensorId,
                destination = destination,
                settingsRoute = settingsRoute,
                scaffoldState = scaffoldState,
                syncInProgress = syncInProgress,
                showChartStats = showChartStats,
                chartSizeLevel = chartSizeLevel,
                viewPeriod = viewPeriod,
                unitsConverter = unitsConverter,
                useNewSensorCard = useNewSensorCard,
                useDarkTheme = useDarkTheme,
                viewModel = viewModel,
                viewModelProvider = viewModelProvider,
                onDestinationSelected = ::selectDestination,
                onSettingsRouteSelected = { settingsRoute = it },
                onNestedSettingsBack = ::navigateBackFromNestedSettings,
                onFinish = onFinish,
            )
        }
    }
}

@Composable
private fun SensorDetailScreen(
    sensors: List<RuuviTag>,
    selectedSensorId: String?,
    destination: SensorDetailDestination,
    settingsRoute: String,
    scaffoldState: ScaffoldState,
    syncInProgress: Boolean,
    showChartStats: Boolean,
    chartSizeLevel: Int,
    viewPeriod: Period,
    unitsConverter: UnitsConverter,
    useNewSensorCard: Boolean,
    useDarkTheme: Boolean,
    viewModel: SensorCardViewModel,
    viewModelProvider: SensorDetailViewModelProvider,
    onDestinationSelected: (SensorDetailDestination) -> Unit,
    onSettingsRouteSelected: (String) -> Unit,
    onNestedSettingsBack: () -> Unit,
    onFinish: () -> Unit,
) {
    val sensorIds = remember(sensors) { sensors.map(RuuviTag::id) }
    val initialPage = sensorIds.indexOf(selectedSensorId).takeIf { it >= 0 } ?: 0
    val pagerState = rememberPagerState(initialPage = initialPage) { sensors.size }
    val currentSensor = sensors.getOrNull(pagerState.currentPage)
    val latestSensorIds by rememberUpdatedState(sensorIds)
    val nestedSettings = destination == SensorDetailDestination.SETTINGS &&
        settingsRoute != SensorSettingsRoutes.SENSOR_SETTINGS_ROOT
    val useSensorBackground = destination.usesSensorBackground && !nestedSettings
    val saveableStateHolder = rememberSaveableStateHolder()
    val coroutineScope = rememberCoroutineScope()
    val reverseTitleSwipeDirection = LocalLayoutDirection.current == LayoutDirection.Rtl
    val titlePagerDraggableState = remember(pagerState) {
        PagerTitleDraggableState(pagerState)
    }
    val pagerFlingBehavior = PagerDefaults.flingBehavior(state = pagerState)
    val titlePagePosition = pagerState.currentPage + pagerState.currentPageOffsetFraction
    val context = LocalContext.current

    LaunchedEffect(selectedSensorId, sensorIds) {
        val selectedPage = sensorIds.indexOf(selectedSensorId)
        if (selectedPage >= 0 && selectedPage != pagerState.currentPage) {
            pagerState.scrollToPage(selectedPage)
        } else if (selectedPage < 0) {
            sensorIds.getOrNull(pagerState.currentPage)
                ?.let(viewModel::saveSelected)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                latestSensorIds.getOrNull(page)?.let(viewModel::saveSelected)
            }
    }

    LaunchedEffect(currentSensor?.id, destination) {
        scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
    }

    BackHandler(enabled = nestedSettings) {
        onNestedSettingsBack()
    }

    SensorDetailSystemBars(
        useTransparentStatusBar = useSensorBackground,
        useTransparentNavigationBar = useSensorBackground && !destination.forcesDarkTheme,
        useDarkTheme = useDarkTheme,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (useSensorBackground) {
                    if (destination.forcesDarkTheme) {
                        RuuviStationTheme.colors.background
                    } else {
                        DefaultSensorBackgroundDark
                    }
                } else {
                    RuuviStationTheme.colors.background
                },
            ),
    ) {
        if (!useSensorBackground) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(RuuviStationTheme.colors.topBar),
            )
        }

        SensorDetailBackground(
            sensor = currentSensor,
            destination = destination,
            enabled = useSensorBackground,
        )

        if (useSensorBackground && destination.forcesDarkTheme) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsBottomHeight(WindowInsets.navigationBars)
                    .background(RuuviStationTheme.colors.background),
            )
        }

        NfcInteractor(
            addSensor = viewModel::addSensor,
            getNfcScanResponse = viewModel::getNfcScanResponse,
        )

        AnimatedContent(
            targetState = settingsRoute,
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            transitionSpec = {
                val animationSpec = tween<IntOffset>(durationMillis = SETTINGS_NAVIGATION_ANIMATION_MILLIS)
                if (targetState == SensorSettingsRoutes.SENSOR_SETTINGS_ROOT) {
                    slideInHorizontally(animationSpec) { -it } togetherWith
                        slideOutHorizontally(animationSpec) { it }
                } else {
                    slideInHorizontally(animationSpec) { it } togetherWith
                        slideOutHorizontally(animationSpec) { -it }
                }
            },
            label = "sensor settings navigation",
        ) { visibleSettingsRoute ->
            val visibleNestedSettings = destination == SensorDetailDestination.SETTINGS &&
                visibleSettingsRoute != SensorSettingsRoutes.SENSOR_SETTINGS_ROOT

            Column(modifier = Modifier.fillMaxSize()) {
                if (visibleNestedSettings) {
                    RuuviTopAppBar(
                        title = SensorSettingsRoutes.getTitleByRoute(context, visibleSettingsRoute),
                        navigationAction = onNestedSettingsBack,
                    )
                } else {
                    SensorDetailTopAppBar(
                        destination = destination,
                        syncInProgress = syncInProgress,
                        alarmStatus = currentSensor?.alarmSensorStatus ?: AlarmSensorStatus.NoAlarms,
                        useOpaqueBackground = !useSensorBackground,
                        onBack = onFinish,
                        onDestinationSelected = onDestinationSelected,
                    )
                }

                if (!visibleNestedSettings && currentSensor != null) {
                    SensorDetailTitle(
                        sensors = sensors,
                        pagePosition = titlePagePosition,
                        subtitle = destination.subtitleRes?.let { stringResource(id = it) },
                        canSelectPrevious = pagerState.canScrollBackward,
                        canSelectNext = pagerState.canScrollForward,
                        onSelectPrevious = {
                            coroutineScope.launch {
                                pagerState.selectSensorPage(
                                    page = pagerState.currentPage - 1,
                                    destination = destination,
                                )
                            }
                        },
                        onSelectNext = {
                            coroutineScope.launch {
                                pagerState.selectSensorPage(
                                    page = pagerState.currentPage + 1,
                                    destination = destination,
                                )
                            }
                        },
                        modifier = if (useSensorBackground) {
                            Modifier
                        } else {
                            Modifier.background(RuuviStationTheme.colors.topBar)
                        },
                        swipeModifier = if (
                            destination.allowsTitleSensorSwipe && sensors.size > 1
                        ) {
                            Modifier.draggable(
                                state = titlePagerDraggableState,
                                orientation = Orientation.Horizontal,
                                reverseDirection = reverseTitleSwipeDirection,
                                onDragStopped = { velocity ->
                                    pagerState.scroll {
                                        with(pagerFlingBehavior) {
                                            performFling(-velocity)
                                        }
                                    }
                                },
                            )
                        } else {
                            Modifier
                        },
                    )
                }

                if (visibleNestedSettings && currentSensor != null) {
                    key(currentSensor.id, visibleSettingsRoute) {
                        saveableStateHolder.SaveableStateProvider(
                            "${currentSensor.id}:$visibleSettingsRoute",
                        ) {
                            SensorSettingsNestedScreen(
                                sensorId = currentSensor.id,
                                route = visibleSettingsRoute,
                                scaffoldState = scaffoldState,
                                viewModelProvider = viewModelProvider,
                                onSettingsRouteSelected = onSettingsRouteSelected,
                                onFinish = onFinish,
                            )
                        }
                    }
                } else {
                    SensorRootPager(
                        modifier = Modifier.weight(1f),
                        sensors = sensors,
                        currentSensorId = currentSensor?.id,
                        destination = destination,
                        pagerState = pagerState,
                        scaffoldState = scaffoldState,
                        showChartStats = showChartStats,
                        chartSizeLevel = chartSizeLevel,
                        viewPeriod = viewPeriod,
                        unitsConverter = unitsConverter,
                        useNewSensorCard = useNewSensorCard,
                        viewModel = viewModel,
                        viewModelProvider = viewModelProvider,
                        saveableStateHolder = saveableStateHolder,
                        onDestinationSelected = onDestinationSelected,
                        onSettingsRouteSelected = onSettingsRouteSelected,
                    )

                    if (destination.showsSensorFooter && currentSensor != null) {
                        SensorCardBottom(
                            sensor = currentSensor,
                            modifier = Modifier.height(24.dp),
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = scaffoldState.snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        )
    }
}

private suspend fun PagerState.selectSensorPage(
    page: Int,
    destination: SensorDetailDestination,
) {
    if (page !in 0 until pageCount) return

    if (destination.allowsBodySensorSwipe) {
        animateScrollToPage(page)
    } else {
        scrollToPage(page)
    }
}

private class PagerTitleDraggableState(
    private val pagerState: PagerState,
) : DraggableState {
    override suspend fun drag(
        dragPriority: MutatePriority,
        block: suspend DragScope.() -> Unit,
    ) {
        pagerState.scroll(dragPriority) {
            val pagerScrollScope = this
            block(
                object : DragScope {
                    override fun dragBy(pixels: Float) {
                        pagerScrollScope.scrollBy(-pixels)
                    }
                },
            )
        }
    }

    override fun dispatchRawDelta(delta: Float) {
        pagerState.dispatchRawDelta(-delta)
    }
}

@Composable
private fun SensorRootPager(
    modifier: Modifier = Modifier,
    sensors: List<RuuviTag>,
    currentSensorId: String?,
    destination: SensorDetailDestination,
    pagerState: PagerState,
    scaffoldState: ScaffoldState,
    showChartStats: Boolean,
    chartSizeLevel: Int,
    viewPeriod: Period,
    unitsConverter: UnitsConverter,
    useNewSensorCard: Boolean,
    viewModel: SensorCardViewModel,
    viewModelProvider: SensorDetailViewModelProvider,
    saveableStateHolder: SaveableStateHolder,
    onDestinationSelected: (SensorDetailDestination) -> Unit,
    onSettingsRouteSelected: (String) -> Unit,
) {
    val systemDarkTheme = isSystemInDarkTheme()

    HorizontalPager(
        modifier = modifier
            .fillMaxSize(),
        state = pagerState,
        key = { page -> sensors[page].id },
        userScrollEnabled = destination.allowsBodySensorSwipe,
    ) { page ->
        sensors.getOrNull(page)?.let { sensor ->
            Crossfade(
                targetState = destination,
                modifier = Modifier.fillMaxSize(),
                animationSpec = tween(durationMillis = DESTINATION_CROSSFADE_MILLIS),
                label = "sensor detail destination",
            ) { visibleDestination ->
                RuuviTheme(darkTheme = visibleDestination.forcesDarkTheme || systemDarkTheme) {
                    key(sensor.id, visibleDestination) {
                        saveableStateHolder.SaveableStateProvider("${sensor.id}:$visibleDestination") {
                            SensorDestinationContent(
                                sensor = sensor,
                                selected = currentSensorId == sensor.id,
                                destination = visibleDestination,
                                scaffoldState = scaffoldState,
                                showChartStats = showChartStats,
                                chartSizeLevel = chartSizeLevel,
                                viewPeriod = viewPeriod,
                                unitsConverter = unitsConverter,
                                useNewSensorCard = useNewSensorCard,
                                viewModel = viewModel,
                                viewModelProvider = viewModelProvider,
                                onDestinationSelected = onDestinationSelected,
                                onSettingsRouteSelected = onSettingsRouteSelected,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SensorDestinationContent(
    sensor: RuuviTag,
    selected: Boolean,
    destination: SensorDetailDestination,
    scaffoldState: ScaffoldState,
    showChartStats: Boolean,
    chartSizeLevel: Int,
    viewPeriod: Period,
    unitsConverter: UnitsConverter,
    useNewSensorCard: Boolean,
    viewModel: SensorCardViewModel,
    viewModelProvider: SensorDetailViewModelProvider,
    onDestinationSelected: (SensorDetailDestination) -> Unit,
    onSettingsRouteSelected: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        when (destination) {
            SensorDetailDestination.CARD -> {
                if (useNewSensorCard) {
                    SensorCard(
                        sensor = sensor,
                        modifier = Modifier.weight(1f),
                        getChartData = viewModel::getChartData,
                        scrollToChart = { unitType ->
                            onDestinationSelected(SensorDetailDestination.HISTORY)
                            viewModel.scrollToChart(unitType)
                        },
                    )
                } else {
                    SensorCardLegacy(
                        sensor = sensor,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            SensorDetailDestination.HISTORY -> SensorHistoryContent(
                sensor = sensor,
                selected = selected,
                showChartStats = showChartStats,
                chartSizeLevel = chartSizeLevel,
                viewPeriod = viewPeriod,
                unitsConverter = unitsConverter,
                viewModel = viewModel,
            )
            SensorDetailDestination.ALERTS -> Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(RuuviStationTheme.colors.background),
            ) {
                SensorAlertsScreen(
                    scaffoldState = scaffoldState,
                    viewModel = viewModelProvider.alerts(sensor.id),
                )
            }
            SensorDetailDestination.SETTINGS -> Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(RuuviStationTheme.colors.background),
            ) {
                SensorSettingsRootScreen(
                    scaffoldState = scaffoldState,
                    viewModel = viewModelProvider.settings(sensor.id),
                    onNavigate = onSettingsRouteSelected,
                    onAlertsClick = {
                        onDestinationSelected(SensorDetailDestination.ALERTS)
                    },
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.SensorHistoryContent(
    sensor: RuuviTag,
    selected: Boolean,
    showChartStats: Boolean,
    chartSizeLevel: Int,
    viewPeriod: Period,
    unitsConverter: UnitsConverter,
    viewModel: SensorCardViewModel,
) {
    var chartCount by remember(sensor.id) { mutableIntStateOf(0) }
    val hideIncreaseChartSize = chartCount < MINIMUM_CHARTS_BEFORE_SIZE_INCREASE

    ChartControlElement2(
        sensorId = sensor.id,
        showChartStats = showChartStats,
        viewPeriod = viewPeriod,
        syncStatus = viewModel.getGattEvents(sensor.id),
        disconnectGattAction = viewModel::disconnectGatt,
        shouldSkipGattSyncDialog = viewModel::shouldSkipGattSyncDialog,
        syncGatt = viewModel::syncGatt,
        setViewPeriod = viewModel::setViewPeriod,
        exportToCsv = viewModel::exportToCsv,
        exportToXlsx = viewModel::exportToXlsx,
        removeTagData = viewModel::removeTagData,
        refreshStatus = viewModel::refreshStatus,
        dontShowGattSyncDescription = viewModel::dontShowGattSyncDescription,
        changeShowStats = viewModel::changeShowChartStats,
        chartSizeLevel = chartSizeLevel,
        hideIncreaseChartSize = hideIncreaseChartSize,
        increaseChartSize = viewModel::increaseChartSize,
        decreaseChartSize = viewModel::decreaseChartSize,
    )

    var chartSize by remember { mutableStateOf(Size.Zero) }
    ChartsView(
        modifier = Modifier
            .weight(1f)
            .onGloballyPositioned { coordinates ->
                chartSize = coordinates.size.toSize()
            },
        sensor = sensor,
        unitsConverter = unitsConverter,
        graphDrawDots = viewModel.graphDrawDots,
        selected = selected,
        viewPeriod = viewPeriod,
        chartCleared = viewModel.getChartCleared(sensor.id),
        showChartStats = showChartStats,
        historyUpdater = viewModel::historyUpdater,
        chartSizeLevel = chartSizeLevel,
        scrollToChartEvent = viewModel.scrollToChartEvent,
        size = chartSize,
        onChartCountChanged = { count -> chartCount = count },
    )
}

@Composable
private fun SensorSettingsNestedScreen(
    sensorId: String,
    route: String,
    scaffoldState: ScaffoldState,
    viewModelProvider: SensorDetailViewModelProvider,
    onSettingsRouteSelected: (String) -> Unit,
    onFinish: () -> Unit,
) {
    when (route) {
        SensorSettingsRoutes.SENSOR_REMOVE -> RemoveSensor(
            scaffoldState = scaffoldState,
            viewModel = viewModelProvider.removeSensor(sensorId),
            onRemoved = onFinish,
        )
        SensorSettingsRoutes.VISIBLE_MEASUREMENTS -> {
            val viewModel = viewModelProvider.visibleMeasurements(sensorId)
            val useDefault by viewModel.useDefaultOrder.collectAsStateWithLifecycle()
            val sensorState by viewModel.sensorState.collectAsStateWithLifecycle()
            val selected by viewModel.selected.collectAsStateWithLifecycle()
            val possibleOptions by viewModel.possibleOptions.collectAsStateWithLifecycle()

            VisibleMeasurements(
                useDefault = useDefault,
                sensorState = sensorState,
                dashboardType = viewModel.dashBoardType,
                onAction = viewModel::onAction,
                effects = viewModel.effects,
                getUnitName = viewModel::getUnitName,
                selected = selected,
                allOptions = possibleOptions,
            )
        }
        SensorSettingsRoutes.LED_CONTROL -> LedControlScreen(
            viewModel = viewModelProvider.ledControl(sensorId),
        )
        SensorSettingsRoutes.NOTES -> {
            val viewModel = viewModelProvider.notes(sensorId)
            val note by viewModel.note.collectAsStateWithLifecycle()
            Notes(
                scaffoldState = scaffoldState,
                note = note,
                onAction = viewModel::onAction,
                effects = viewModel.effects,
                uiEvent = viewModel.uiEvent,
                onNavigateBack = {
                    onSettingsRouteSelected(SensorSettingsRoutes.SENSOR_SETTINGS_ROOT)
                },
            )
        }
    }
}

@Composable
private fun SensorDetailBackground(
    sensor: RuuviTag?,
    destination: SensorDetailDestination,
    enabled: Boolean,
) {
    if (!enabled) return

    val backgroundUri = sensor
        ?.userBackground
        ?.takeIf(String::isNotBlank)
        ?.let(Uri::parse)
        ?.takeIf { !it.path.isNullOrBlank() }
    if (backgroundUri != null) {
        SensorCardImage(userBackground = backgroundUri)
    } else {
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(
                if (sensor?.isAir() == true) R.drawable.new_bg_air else R.drawable.new_bg2,
            ),
            contentDescription = null,
            contentScale = ContentScale.Crop,
        )
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(R.drawable.tag_bg_layer),
            contentDescription = null,
            contentScale = ContentScale.Crop,
        )
    }

    val overlayColor by animateColorAsState(
        targetValue = when (destination) {
            SensorDetailDestination.CARD -> Color.Transparent
            SensorDetailDestination.HISTORY -> HISTORY_STATUS_BAR_COLOR
            SensorDetailDestination.ALERTS,
            SensorDetailDestination.SETTINGS -> Titan70
        },
        animationSpec = tween(durationMillis = BACKGROUND_COLOR_ANIMATION_MILLIS),
        label = "sensor detail background overlay",
    )
    if (overlayColor != Color.Transparent) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(overlayColor),
        )
    }
}

@Composable
private fun SensorDetailSystemBars(
    useTransparentStatusBar: Boolean,
    useTransparentNavigationBar: Boolean,
    useDarkTheme: Boolean,
) {
    val systemUiController = rememberSystemUiController()
    val statusBarColor = RuuviStationTheme.colors.systemBars
    val navigationBarColor = RuuviStationTheme.colors.background

    SideEffect {
        systemUiController.setStatusBarColor(
            color = if (useTransparentStatusBar) Color.Transparent else statusBarColor,
            darkIcons = false,
        )
        systemUiController.setNavigationBarColor(
            color = if (useTransparentNavigationBar) Color.Transparent else navigationBarColor,
            navigationBarContrastEnforced = !useTransparentNavigationBar,
            darkIcons = !useTransparentNavigationBar && !useDarkTheme,
        )
    }
}
