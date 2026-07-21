package com.ruuvi.station.tagdetails.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ruuvi.station.alarm.domain.AlarmSensorStatus
import com.ruuvi.station.alarm.ui.AlarmItemsViewModel
import com.ruuvi.station.app.ui.RuuviTopAppBar
import com.ruuvi.station.app.ui.theme.DefaultSensorBackgroundDark
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.graph.ChartControlElement2
import com.ruuvi.station.graph.ChartsView
import com.ruuvi.station.nfc.ui.NfcInteractor
import com.ruuvi.station.tag.domain.RuuviTag
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
    val increasedChartSize by viewModel.increasedChartSize.collectAsStateWithLifecycle()

    var destination by rememberSaveable { mutableStateOf(startDestination.root) }
    var settingsRoute by rememberSaveable { mutableStateOf(startDestination.settingsRoute) }
    val scaffoldState = rememberScaffoldState()

    fun selectDestination(newDestination: SensorDetailDestination) {
        if (newDestination == destination) return
        settingsRoute = SensorSettingsRoutes.SENSOR_SETTINGS_ROOT
        destination = newDestination
    }

    if (sensors.isNotEmpty()) {
        SensorDetailScreen(
            sensors = sensors,
            selectedSensorId = selectedSensorId,
            destination = destination,
            settingsRoute = settingsRoute,
            scaffoldState = scaffoldState,
            syncInProgress = syncInProgress,
            showChartStats = showChartStats,
            increasedChartSize = increasedChartSize,
            viewPeriod = viewPeriod,
            unitsConverter = unitsConverter,
            useNewSensorCard = useNewSensorCard,
            viewModel = viewModel,
            viewModelProvider = viewModelProvider,
            onDestinationSelected = ::selectDestination,
            onSettingsRouteSelected = { settingsRoute = it },
            onFinish = onFinish,
        )
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
    increasedChartSize: Boolean,
    viewPeriod: Period,
    unitsConverter: UnitsConverter,
    useNewSensorCard: Boolean,
    viewModel: SensorCardViewModel,
    viewModelProvider: SensorDetailViewModelProvider,
    onDestinationSelected: (SensorDetailDestination) -> Unit,
    onSettingsRouteSelected: (String) -> Unit,
    onFinish: () -> Unit,
) {
    val sensorIds = remember(sensors) { sensors.map(RuuviTag::id) }
    val initialPage = sensorIds.indexOf(selectedSensorId).takeIf { it >= 0 } ?: 0
    val pagerState = rememberPagerState(initialPage = initialPage) { sensors.size }
    val currentSensor = sensors.getOrNull(pagerState.currentPage)
    val nestedSettings = destination == SensorDetailDestination.SETTINGS &&
        settingsRoute != SensorSettingsRoutes.SENSOR_SETTINGS_ROOT
    val saveableStateHolder = rememberSaveableStateHolder()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(selectedSensorId, sensorIds) {
        val selectedPage = sensorIds.indexOf(selectedSensorId)
        if (selectedPage >= 0 && selectedPage != pagerState.currentPage) {
            pagerState.scrollToPage(selectedPage)
        }
    }

    LaunchedEffect(pagerState, sensorIds) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                sensorIds.getOrNull(page)?.let(viewModel::saveSelected)
            }
    }

    androidx.activity.compose.BackHandler(enabled = nestedSettings) {
        onSettingsRouteSelected(SensorSettingsRoutes.SENSOR_SETTINGS_ROOT)
    }

    SensorDetailSystemBars(useSensorBackground = destination.usesSensorBackground)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (destination.usesSensorBackground) {
                    DefaultSensorBackgroundDark
                } else {
                    RuuviStationTheme.colors.background
                },
            ),
    ) {
        SensorDetailBackground(
            sensor = currentSensor,
            destination = destination,
        )

        NfcInteractor(
            addSensor = viewModel::addSensor,
            getNfcScanResponse = viewModel::getNfcScanResponse,
        )

        Column(modifier = Modifier.systemBarsPadding()) {
            if (nestedSettings) {
                RuuviTopAppBar(
                    title = SensorSettingsRoutes.getTitleByRoute(context, settingsRoute),
                    navigationAction = {
                        onSettingsRouteSelected(SensorSettingsRoutes.SENSOR_SETTINGS_ROOT)
                    },
                )
            } else {
                SensorDetailTopAppBar(
                    destination = destination,
                    syncInProgress = syncInProgress,
                    alarmStatus = currentSensor?.alarmSensorStatus ?: AlarmSensorStatus.NoAlarms,
                    themed = !destination.usesSensorBackground,
                    onBack = onFinish,
                    onDestinationSelected = onDestinationSelected,
                )
            }

            if (!nestedSettings && currentSensor != null) {
                SensorDetailTitle(
                    sensor = currentSensor,
                    subtitle = destination.subtitleRes?.let { stringResource(id = it) },
                    canSelectPrevious = pagerState.canScrollBackward,
                    canSelectNext = pagerState.canScrollForward,
                    onSelectPrevious = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    onSelectNext = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = if (destination.usesSensorBackground) {
                        Modifier
                    } else {
                        Modifier.background(RuuviStationTheme.colors.topBar)
                    },
                )
            }

            if (nestedSettings && currentSensor != null) {
                key(currentSensor.id, settingsRoute) {
                    saveableStateHolder.SaveableStateProvider("${currentSensor.id}:$settingsRoute") {
                        SensorSettingsNestedScreen(
                            sensorId = currentSensor.id,
                            route = settingsRoute,
                            scaffoldState = scaffoldState,
                            viewModelProvider = viewModelProvider,
                            onSettingsRouteSelected = onSettingsRouteSelected,
                            onFinish = onFinish,
                        )
                    }
                }
            } else {
                SensorRootPager(
                    sensors = sensors,
                    currentSensorId = currentSensor?.id,
                    destination = destination,
                    pagerState = pagerState,
                    scaffoldState = scaffoldState,
                    showChartStats = showChartStats,
                    increasedChartSize = increasedChartSize,
                    viewPeriod = viewPeriod,
                    unitsConverter = unitsConverter,
                    useNewSensorCard = useNewSensorCard,
                    viewModel = viewModel,
                    viewModelProvider = viewModelProvider,
                    saveableStateHolder = saveableStateHolder,
                    onDestinationSelected = onDestinationSelected,
                    onSettingsRouteSelected = onSettingsRouteSelected,
                )
            }
        }
    }
}

@Composable
private fun SensorRootPager(
    sensors: List<RuuviTag>,
    currentSensorId: String?,
    destination: SensorDetailDestination,
    pagerState: PagerState,
    scaffoldState: ScaffoldState,
    showChartStats: Boolean,
    increasedChartSize: Boolean,
    viewPeriod: Period,
    unitsConverter: UnitsConverter,
    useNewSensorCard: Boolean,
    viewModel: SensorCardViewModel,
    viewModelProvider: SensorDetailViewModelProvider,
    saveableStateHolder: androidx.compose.runtime.saveable.SaveableStateHolder,
    onDestinationSelected: (SensorDetailDestination) -> Unit,
    onSettingsRouteSelected: (String) -> Unit,
) {
    HorizontalPager(
        modifier = Modifier.fillMaxSize(),
        state = pagerState,
        userScrollEnabled = destination.allowsSensorSwipe,
    ) { page ->
        sensors.getOrNull(page)?.let { sensor ->
            key(sensor.id, destination) {
                saveableStateHolder.SaveableStateProvider("${sensor.id}:$destination") {
                    SensorDestinationContent(
                        sensor = sensor,
                        selected = currentSensorId == sensor.id,
                        destination = destination,
                        scaffoldState = scaffoldState,
                        showChartStats = showChartStats,
                        increasedChartSize = increasedChartSize,
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

@Composable
private fun SensorDestinationContent(
    sensor: RuuviTag,
    selected: Boolean,
    destination: SensorDetailDestination,
    scaffoldState: ScaffoldState,
    showChartStats: Boolean,
    increasedChartSize: Boolean,
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
                increasedChartSize = increasedChartSize,
                viewPeriod = viewPeriod,
                unitsConverter = unitsConverter,
                viewModel = viewModel,
            )
            SensorDetailDestination.ALERTS -> Box(modifier = Modifier.weight(1f)) {
                SensorAlertsScreen(
                    scaffoldState = scaffoldState,
                    viewModel = viewModelProvider.alerts(sensor.id),
                )
            }
            SensorDetailDestination.SETTINGS -> Box(modifier = Modifier.weight(1f)) {
                SensorSettingsRootScreen(
                    scaffoldState = scaffoldState,
                    viewModel = viewModelProvider.settings(sensor.id),
                    onNavigate = onSettingsRouteSelected,
                )
            }
        }

        if (destination.showsSensorFooter) {
            SensorCardBottom(
                sensor = sensor,
                modifier = Modifier.height(intrinsicSize = IntrinsicSize.Min),
            )
        }
    }
}

@Composable
private fun ColumnScope.SensorHistoryContent(
    sensor: RuuviTag,
    selected: Boolean,
    showChartStats: Boolean,
    increasedChartSize: Boolean,
    viewPeriod: Period,
    unitsConverter: UnitsConverter,
    viewModel: SensorCardViewModel,
) {
    val hideIncreaseChartSize = sensor.latestMeasurement?.humidity == null ||
        sensor.latestMeasurement.pressure == null

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
        increasedChartSize = increasedChartSize,
        hideIncreaseChartSize = hideIncreaseChartSize,
        changeIncreasedChartSize = viewModel::changeIncreaseChartSize,
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
        increasedChartSize = increasedChartSize,
        scrollToChartEvent = viewModel.scrollToChartEvent,
        size = chartSize,
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
) {
    if (!destination.usesSensorBackground) return
    val backgroundUri = sensor?.userBackground?.let(Uri::parse) ?: return
    if (backgroundUri.path != null) {
        SensorCardImage(
            userBackground = backgroundUri,
            chartsEnabled = destination == SensorDetailDestination.HISTORY,
        )
    }
}

@Composable
private fun SensorDetailSystemBars(useSensorBackground: Boolean) {
    val systemUiController = rememberSystemUiController()
    val isDarkTheme = isSystemInDarkTheme()
    val statusBarColor = RuuviStationTheme.colors.systemBars
    val navigationBarColor = RuuviStationTheme.colors.background

    SideEffect {
        systemUiController.setStatusBarColor(
            color = if (useSensorBackground) Color.Transparent else statusBarColor,
            darkIcons = false,
        )
        systemUiController.setNavigationBarColor(
            color = if (useSensorBackground) Color.Transparent else navigationBarColor,
            navigationBarContrastEnforced = !useSensorBackground,
            darkIcons = !useSensorBackground && !isDarkTheme,
        )
    }
}
