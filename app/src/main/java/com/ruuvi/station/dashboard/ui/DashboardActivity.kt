package com.ruuvi.station.dashboard.ui

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterEnd
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment.Companion.Top
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.whenStarted
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ruuvi.station.R
import com.ruuvi.station.addtag.ui.AddTagActivity
import com.ruuvi.station.alarm.domain.AlarmState
import com.ruuvi.station.alarm.domain.AlarmType
import com.ruuvi.station.alarm.ui.AlarmStateIndicator
import com.ruuvi.station.app.permissions.BluetoothPermissions
import com.ruuvi.station.app.permissions.NotificationPermission
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.app.ui.DashboardMainMenu
import com.ruuvi.station.app.ui.DashboardTopAppBar
import com.ruuvi.station.app.ui.components.DashboardMenuItemText
import com.ruuvi.station.app.ui.components.RuuviButton
import com.ruuvi.station.app.ui.components.limitedScale
import com.ruuvi.station.app.ui.components.rememberResourceUri
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviStationTheme.colors
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.dashboard.DashboardTapAction
import com.ruuvi.station.dashboard.DashboardType
import com.ruuvi.station.network.data.NetworkSyncEvent
import com.ruuvi.station.network.ui.ShareSensorActivity
import com.ruuvi.station.network.ui.SignInActivity
import com.ruuvi.station.network.ui.claim.ClaimSensorActivity
import com.ruuvi.station.nfc.ui.NfcInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.UpdateSource
import com.ruuvi.station.tag.domain.isAir
import com.ruuvi.station.tag.domain.isLowBattery
import com.ruuvi.station.tagdetails.ui.SensorCardActivity
import com.ruuvi.station.tagdetails.ui.SensorCardOpenType
import com.ruuvi.station.tagsettings.ui.BackgroundActivity
import com.ruuvi.station.tagsettings.ui.SetSensorName
import com.ruuvi.station.tagsettings.ui.TagSettingsActivity
import com.ruuvi.station.units.domain.aqi.AQI
import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.util.base.NfcActivity
import com.ruuvi.station.util.extensions.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import timber.log.Timber

class DashboardActivity : NfcActivity(), KodeinAware {

    override val kodein by closestKodein()

    private val dashboardViewModel: DashboardActivityViewModel by viewModel()
    private val preferencesRepository: PreferencesRepository by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        observeSyncStatus()

        setContent {
            var bluetoothCheckReady by remember {
                mutableStateOf(false)
            }

            RuuviTheme {
                val lifecycleOwner = LocalLifecycleOwner.current
                val scaffoldState = rememberScaffoldState()
                val systemUiController = rememberSystemUiController()
                val systemBarsColor = colors.dashboardBackground
                val configuration = LocalConfiguration.current
                val context = LocalContext.current
                val isDarkTheme = isSystemInDarkTheme()
                val scope = rememberCoroutineScope()
                val userEmail by dashboardViewModel.userEmail.observeAsState()
                val signedIn = !userEmail.isNullOrEmpty()
                val signedInOnce by dashboardViewModel.signedInOnce.collectAsState(false)
                val bannerDisabled by dashboardViewModel.bannerDisabled.collectAsState(false)
                val sensors by dashboardViewModel.sensorsList.collectAsState()
                val refreshing by dashboardViewModel.dataRefreshing.collectAsState(false)
                val syncInProgress by dashboardViewModel.syncInProgress.collectAsState(false)
                val dashboardType by dashboardViewModel.dashboardType.collectAsState()
                val dashboardTapAction by dashboardViewModel.dashboardTapAction.collectAsState()
                val shouldAskNotificationPermission by dashboardViewModel.shouldAskNotificationPermission.collectAsState()
                val dragDropListState = rememberDragDropListState(
                    onMove = dashboardViewModel::moveItem,
                    onDoneDragging = dashboardViewModel::onDoneDragging
                )
                val coroutineScope = rememberCoroutineScope()
                val navigationColor = if (gestureNavigationEnabled()) {
                    Color.Transparent
                } else {
                    colors.navigationTransparent
                }

                if (shouldAskNotificationPermission) {
                    NotificationPermission(
                        scaffoldState = scaffoldState,
                        shouldAskNotificationPermission = shouldAskNotificationPermission
                    ) {
                        bluetoothCheckReady = true
                    }
                } else {
                    bluetoothCheckReady = true
                }

                if (bluetoothCheckReady) {
                    BluetoothPermissions(
                        scaffoldState = scaffoldState,
                        askToEnableBluetooth = dashboardViewModel.shouldAskToEnableBluetooth,
                        askForBackgroundLocation = dashboardViewModel.shouldAskForBackgroundLocationPermission,
                        preferencesRepository = preferencesRepository
                    )
                }

                Box (
                    modifier =
                        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            Modifier
                                .statusBarsPadding()
                                .windowInsetsPadding(WindowInsets.tappableElement)
                        } else {
                            Modifier.statusBarsPadding()
                        }
                ) {
                    Scaffold(
                        scaffoldState = scaffoldState,
                        modifier = Modifier.fillMaxSize(),
                        backgroundColor = RuuviStationTheme.colors.dashboardBackground,
                        topBar = {
                            DashboardTopAppBar(
                                dashboardType = dashboardType,
                                dashboardTapAction = dashboardTapAction,
                                syncInProgress = syncInProgress,
                                changeDashboardType = dashboardViewModel::changeDashboardType,
                                changeDashboardTapAction = dashboardViewModel::changeDashboardTapAction,
                                navigationCallback = {
                                    scope.launch {
                                        scaffoldState.drawerState.open()
                                    }
                                },
                                clearSensorOrder = dashboardViewModel::clearSensorOrder,
                                isCustomOrderEnabled = dashboardViewModel::isCustomOrderEnabled
                            )
                        },
                        drawerContent = {
                            DashboardMainMenu(
                                scaffoldState = scaffoldState,
                                signedIn = signedIn
                            )
                        },
                        drawerBackgroundColor = RuuviStationTheme.colors.background
                    ) { paddingValues ->
                        Surface(
                            color = RuuviStationTheme.colors.dashboardBackground,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            sensors?.let {
                                if (it.isEmpty()) {
                                    EmptyDashboard(
                                        signedIn,
                                        signedInOnce
                                    ) {
                                        openUrl(getString(R.string.buy_sensors_link))
                                    }
                                } else {
                                    Column {
                                        if (!signedIn && signedInOnce && !bannerDisabled) {
                                            SignInBanner(
                                                disableBanner = dashboardViewModel::disableBanner
                                            )
                                            Spacer(modifier =  Modifier.height(RuuviStationTheme.dimensions.medium))
                                        }
                                        DashboardItems(
                                            items = it,
                                            userEmail = userEmail,
                                            dashboardType = dashboardType,
                                            dashboardTapAction = dashboardTapAction,
                                            syncCloud = dashboardViewModel::syncCloud,
                                            setName = dashboardViewModel::setName,
                                            onMove = dashboardViewModel::moveItem,
                                            refreshing = refreshing,
                                            dragDropListState = dragDropListState
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                NfcInteractor(
                    getNfcScanResponse = dashboardViewModel::getNfcScanResponse,
                    addSensor = dashboardViewModel::addSensor
                )

                SideEffect {
                    systemUiController.setStatusBarColor(
                        color = systemBarsColor,
                        darkIcons = !isDarkTheme
                    )
                    systemUiController.setNavigationBarColor(
                        color = navigationColor,
                        navigationBarContrastEnforced = false,
                        darkIcons = !isDarkTheme
                    )
                }

                LaunchedEffect(key1 = null) {
                    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        while (true) {
                            if (!dragDropListState.isDragInProgress) {
                                Timber.d("Refreshing dashboard")
                                dashboardViewModel.refreshSensors()
                            }
                            delay(1000)
                        }
                    }
                }
            }
        }
    }

    private fun observeSyncStatus() {
        lifecycleScope.launchWhenStarted {
            dashboardViewModel.syncEvents.collect {
                if (it is NetworkSyncEvent.Unauthorised) {
                    dashboardViewModel.signOut()
                    SignInActivity.start(this@DashboardActivity)
                }

                if (it is NetworkSyncEvent.Success) {
                    dashboardViewModel.refreshDashboardType()
                    dashboardViewModel.refreshDashboardTapAction()
                }

                if (it is NetworkSyncEvent.SensorsSynced) {
                    dashboardViewModel.refreshNotificationStatus()
                }
            }
        }
    }

    companion object {
        fun start(context: Context) {
            val dashboardIntent = Intent(context, DashboardActivity::class.java)
            dashboardIntent
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(dashboardIntent)
        }
    }
}

@Composable
fun EmptyDashboard(
    signedIn: Boolean,
    signedInOnce: Boolean,
    buySensors: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = RuuviStationTheme.dimensions.medium,
                end = RuuviStationTheme.dimensions.medium,
                bottom = RuuviStationTheme.dimensions.medium,
            )
            .systemBarsPadding(),
        shape = RoundedCornerShape(10.dp),
        elevation = 0.dp,
        backgroundColor = RuuviStationTheme.colors.dashboardCardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!signedIn && signedInOnce) {
                Text(
                    modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.extended),
                    style = RuuviStationTheme.typography.onboardingSubtitle,
                    color = colors.primary,
                    text = stringResource(id = R.string.dashboard_no_sensors_message_signed_out),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extraBig))
                RuuviButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = RuuviStationTheme.dimensions.extended),
                    text = stringResource(id = R.string.sign_in)
                ) {
                    SignInActivity.start(context)
                }
                Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))
            } else {
                Text(
                    modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.extended),
                    style = RuuviStationTheme.typography.onboardingSubtitle,
                    color = colors.primary,
                    text = stringResource(id = R.string.dashboard_no_sensors_message),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extraBig))
            }
            RuuviButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RuuviStationTheme.dimensions.extended),
                text = stringResource(id = R.string.add_a_sensor)
            ) {
                AddTagActivity.start(context)
            }
            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extraBig))
            Text(
                modifier = Modifier
                    .padding(horizontal = RuuviStationTheme.dimensions.extended)
                    .clickable { buySensors.invoke() },
                style = RuuviStationTheme.typography.title,
                textAlign = TextAlign.Center,
                text = stringResource(id = R.string.menu_buy_sensors),
                textDecoration = TextDecoration.Underline
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DashboardItems(
    items: List<RuuviTag>,
    userEmail: String?,
    dashboardType: DashboardType,
    dashboardTapAction: DashboardTapAction,
    syncCloud: ()-> Unit,
    setName: (String, String?) -> Unit,
    onMove: (Int, Int, Boolean) -> Unit,
    dragDropListState: ItemListDragAndDropState,
    refreshing: Boolean
) {
    val itemHeight = 156.dp.limitedScale()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = {
            syncCloud.invoke()
        }
    )

    val coroutineScope = rememberCoroutineScope()
    val overscrollJob = remember { mutableStateOf<Job?>(null) }

    val pullToRefreshModifier = if (userEmail.isNullOrEmpty()) {
        Modifier
    } else {
        Modifier.pullRefresh(pullRefreshState)
    }

    Box(modifier = pullToRefreshModifier) {
        LazyVerticalGrid(
            modifier = Modifier
                .dragGestureHandler(coroutineScope, dragDropListState, overscrollJob),
            columns = GridCells.Adaptive(300.dp),
            contentPadding = PaddingValues(
                start = RuuviStationTheme.dimensions.medium,
                end = RuuviStationTheme.dimensions.medium,
                bottom = RuuviStationTheme.dimensions.medium,
                top = 0.dp
            ),
            verticalArrangement = Arrangement.spacedBy(RuuviStationTheme.dimensions.medium),
            horizontalArrangement = Arrangement.spacedBy(RuuviStationTheme.dimensions.medium),
            state = dragDropListState.getLazyListState()
        ) {


            itemsIndexed(items) { index, sensor ->
                val displacementOffset = if (index == dragDropListState.getCurrentIndexOfDraggedListItem()) {
                    Timber.d("dragGestureHandler - elementDisplacement ${dragDropListState.elementDisplacement}")
                    dragDropListState.elementDisplacement.takeIf { it != IntOffset.Zero }
                } else {
                    null
                }

                val itemIsDragged = dragDropListState.getCurrentIndexOfDraggedListItem() == index
                when (dashboardType) {
                    DashboardType.SIMPLE_VIEW ->
                        DashboardItemSimple(
                            lazyGridState = dragDropListState.getLazyListState(),
                            itemIndex = index,
                            sensor = sensor,
                            userEmail = userEmail,
                            setName = setName,
                            displacementOffset = displacementOffset,
                            itemIsDragged = itemIsDragged,
                            moveItem = onMove
                            )
                    DashboardType.IMAGE_VIEW ->
                        DashboardItem(
                            lazyGridState = dragDropListState.getLazyListState(),
                            itemIndex = index,
                            itemHeight = itemHeight,
                            sensor = sensor,
                            userEmail = userEmail,
                            displacementOffset = displacementOffset,
                            itemIsDragged = itemIsDragged,
                            setName = setName,
                            moveItem = onMove
                        )
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) { Box(modifier = Modifier.navigationBarsPadding()) }
        }
        PullRefreshIndicator(
            refreshing = refreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = RuuviStationTheme.colors.dashboardBackground,
            contentColor = RuuviStationTheme.colors.indicatorColor
        )
    }
}

@Composable
fun SignInBanner(
    modifier: Modifier = Modifier,
    disableBanner: () -> Unit
) {
    val context = LocalContext.current

    Surface (
        modifier = modifier
            .fillMaxWidth(),
        color = RuuviStationTheme.colors.bannerBackground
    ) {
        Box (
            contentAlignment = CenterEnd,
            modifier = Modifier
                .padding(RuuviStationTheme.dimensions.small)
        ){
            IconButton(
                onClick = {
                    disableBanner.invoke()
                }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_clear_24),
                    contentDescription = null,
                    tint = RuuviStationTheme.colors.primary
                )
            }
        }
        Column (
            modifier = modifier.padding(vertical = RuuviStationTheme.dimensions.extended),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row (verticalAlignment = Alignment.CenterVertically){
                Text(
                    modifier = Modifier
                        .weight(1f),
                    style = RuuviStationTheme.typography.subtitle,
                    fontSize = 18.limitedSp,
                    text = stringResource(id = R.string.dashboard_banner_signed_out),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.mediumPlus))
            RuuviButton(
                modifier = Modifier
                    .padding(horizontal = RuuviStationTheme.dimensions.extended),
                text = stringResource(id = R.string.sign_in)
            ) {
                SignInActivity.start(context)
            }
        }
    }
}

@Composable
fun DashboardItem(
    lazyGridState: LazyGridState,
    itemIndex: Int,
    itemHeight: Dp,
    sensor: RuuviTag,
    userEmail: String?,
    displacementOffset: IntOffset?,
    itemIsDragged: Boolean,
    setName: (String, String?) -> Unit,
    moveItem: (Int, Int, Boolean) -> Unit
) {
    val itemHeight = (itemHeight.value * if (sensor.isAir()) 1.4 else 1.0).dp
    val context = LocalContext.current
    val modifier = if (itemIsDragged) {
        Modifier
            .zIndex(2f)
    } else {
        Modifier
            .zIndex(1f)
    }

    Card(
        modifier = modifier
            .height(itemHeight)
            .fillMaxWidth()
            .graphicsLayer {
                Timber.d("dragGestureHandler - graphicsLayer $displacementOffset")
                translationY = displacementOffset?.y?.toFloat() ?: 0f
                translationX = displacementOffset?.x?.toFloat() ?: 0f
                scaleX = if (itemIsDragged) 1.04f else 1f
                scaleY = if (itemIsDragged) 1.04f else 1f
                alpha = if (itemIsDragged) 0.7f else 1f
            }
            .clickableSingle {
                SensorCardActivity.start(
                    context,
                    sensor.id,
                    SensorCardOpenType.DEFAULT
                )
            },
        shape = RoundedCornerShape(10.dp),
        backgroundColor = colors.dashboardCardBackground
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .fillMaxWidth(fraction = 0.25f)
                    .fillMaxHeight()
                    .background(color = RuuviStationTheme.colors.defaultSensorBackground)
            ) {
                Timber.d("Image path ${sensor.userBackground} ")

                if (sensor.userBackground != null) {
                    val uri = Uri.parse(sensor.userBackground)

                    if (uri.path != null) {
                        DashboardImage(uri)
                    }
                }
            }

            ConstraintLayout(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = RuuviStationTheme.dimensions.mediumPlus,
                        start = RuuviStationTheme.dimensions.mediumPlus,
                        bottom = RuuviStationTheme.dimensions.medium,
                        end = RuuviStationTheme.dimensions.medium,
                    )
            ) {
                val (title, bigTemperature, buttons, values, updated) = createRefs()

                ItemName(
                    sensor = sensor,
                    modifier = Modifier.constrainAs(title) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(buttons.start)
                        width = Dimension.fillToConstraints
                    }
                )

                ItemButtons(
                    lazyGridState = lazyGridState,
                    itemIndex = itemIndex,
                    sensor = sensor,
                    userEmail = userEmail,
                    modifier = Modifier
                        .constrainAs(buttons) {
                            top.linkTo(parent.top)
                            end.linkTo(parent.end)
                        },
                    setName = setName,
                    moveItem = moveItem
                )

                if (sensor.latestMeasurement != null) {
                    if (sensor.isAir()) {
                        AQIDisplay(
                            value = AQI.getAQI(sensor.latestMeasurement),
                            alertTriggered = false,
                            modifier = Modifier
                                .constrainAs(bigTemperature) {
                                    top.linkTo(title.bottom)
                                    start.linkTo(parent.start)
                                }
                                .padding(top = RuuviStationTheme.dimensions.small)
                        )
                        ItemValues(
                            sensor = sensor,
                            modifier = Modifier
                                .padding(vertical = RuuviStationTheme.dimensions.small)
                                .constrainAs(values) {
                                    bottom.linkTo(updated.top)
                                    start.linkTo(parent.start)
                                    end.linkTo(parent.end)
                                    width = Dimension.fillToConstraints
                                }
                        )
                    } else {
                        if (sensor.latestMeasurement.temperature != null) {
                            BigValueDisplay(
                                value = sensor.latestMeasurement.temperature,
                                alarmState = sensor.alarmSensorStatus.getState(AlarmType.TEMPERATURE),
                                modifier = Modifier
                                    .constrainAs(bigTemperature) {
                                        top.linkTo(title.bottom)
                                        start.linkTo(parent.start)
                                    }
                                    .padding(top = RuuviStationTheme.dimensions.small)
                            )
                        }
                        ItemValuesWithoutTemperature(
                            sensor = sensor,
                            modifier = Modifier
                                .padding(vertical = RuuviStationTheme.dimensions.small)
                                .constrainAs(values) {
                                    bottom.linkTo(updated.top)
                                    start.linkTo(parent.start)
                                    end.linkTo(parent.end)
                                    width = Dimension.fillToConstraints
                                }
                        )
                    }

                    ItemBottom(
                        sensor = sensor,
                        modifier = Modifier
                            .constrainAs(updated) {
                                start.linkTo(parent.start)
                                end.linkTo(parent.end)
                                bottom.linkTo(parent.bottom)
                                width = Dimension.fillToConstraints
                            }
                    )
                } else {
                    ItemBottomNoData(
                        modifier = Modifier
                            .constrainAs(updated) {
                                start.linkTo(parent.start)
                                end.linkTo(parent.end)
                                top.linkTo(title.bottom)
                                bottom.linkTo(parent.bottom)
                                width = Dimension.fillToConstraints
                                height = Dimension.fillToConstraints
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardItemSimple(
    lazyGridState: LazyGridState,
    itemIndex: Int,
    sensor: RuuviTag,
    userEmail: String?,
    setName: (String, String?) -> Unit,
    moveItem: (Int, Int, Boolean) -> Unit,
    displacementOffset: IntOffset?,
    itemIsDragged: Boolean,
) {
    val context = LocalContext.current
    val modifier = if (itemIsDragged) {
        Modifier.zIndex(2f)
    } else {
        Modifier.zIndex(1f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                Timber.d("dragGestureHandler - graphicsLayer $displacementOffset")
                translationY = displacementOffset?.y?.toFloat() ?: 0f
                translationX = displacementOffset?.x?.toFloat() ?: 0f
                scaleX = if (itemIsDragged) 1.04f else 1f
                scaleY = if (itemIsDragged) 1.04f else 1f
                alpha = if (itemIsDragged) 0.7f else 1f
            }
            .clickableSingle {
                SensorCardActivity.start(
                    context,
                    sensor.id,
                    SensorCardOpenType.DEFAULT
                )
            },
        shape = RoundedCornerShape(10.dp),
        elevation = 0.dp,
        backgroundColor = colors.dashboardCardBackground
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = RuuviStationTheme.dimensions.mediumPlus,
                    start = RuuviStationTheme.dimensions.mediumPlus,
                    bottom = RuuviStationTheme.dimensions.medium,
                    end = RuuviStationTheme.dimensions.medium,
                )
        ) {
            val (title, buttons, updated, values) = createRefs()

            ItemName(
                sensor = sensor,
                modifier = Modifier.constrainAs(title) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(buttons.start)
                    width = Dimension.fillToConstraints
                }
            )

            ItemButtons(
                lazyGridState = lazyGridState,
                itemIndex = itemIndex,
                sensor = sensor,
                userEmail = userEmail,
                modifier = Modifier
                    .constrainAs(buttons) {
                        top.linkTo(parent.top)
                        end.linkTo(parent.end)
                    },
                setName = setName,
                moveItem = moveItem
            )

            ItemValues(
                sensor = sensor,
                showAqi = true,
                modifier = Modifier
                    .padding(vertical = RuuviStationTheme.dimensions.small)
                    .constrainAs(values) {
                        top.linkTo(title.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        width = Dimension.fillToConstraints
                    }
            )

            ItemBottom(
                sensor = sensor,
                modifier = Modifier.constrainAs(updated) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(values.bottom)
                    width = Dimension.fillToConstraints
                }
            )
        }
    }
}

@Composable
fun ItemName(
    sensor: RuuviTag,
    modifier: Modifier = Modifier
) {
    Text(
        style = RuuviStationTheme.typography.title,
        text = sensor.displayName,
        lineHeight = RuuviStationTheme.fontSizes.extended,
        fontSize = 16.limitedSp,
        modifier = modifier,
        maxLines = 2
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ItemButtons(
    lazyGridState: LazyGridState,
    itemIndex: Int,
    sensor: RuuviTag,
    userEmail: String?,
    setName: (String, String?) -> Unit,
    moveItem: (Int, Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Row(
        modifier = modifier
            .width(RuuviStationTheme.dimensions.dashboardIconSize * 2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
            DashboardItemDropdownMenu(lazyGridState, itemIndex, sensor, userEmail, setName, moveItem)
        }
    }
}

@Composable
fun ItemValues(
    sensor: RuuviTag,
    showAqi:Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Top,
        modifier = modifier
    ) {
        if (sensor.latestMeasurement != null) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Bottom,
            ) {
                sensor.latestMeasurement.aqi?.let {
                    if (sensor.isAir() && showAqi) {
                        ValueDisplay(
                            value = it,
                            AlarmState.NO_ALARM
                        )
                    }
                }

                sensor.latestMeasurement.temperature?.let {
                    ValueDisplay(
                        value = it,
                        sensor.alarmSensorStatus.getState(AlarmType.TEMPERATURE)
                    )
                }
                sensor.latestMeasurement.humidity?.let {
                    ValueDisplay(
                        value = it,
                        sensor.alarmSensorStatus.getState(AlarmType.HUMIDITY)
                    )
                }
                sensor.latestMeasurement.luminosity?.let {
                    ValueDisplay(
                        value = it,
                        sensor.alarmSensorStatus.getState(AlarmType.LUMINOSITY)
                    )
                }
                sensor.latestMeasurement.dBaAvg?.let {
                    ValueDisplay(
                        value = it,
                        sensor.alarmSensorStatus.getState(AlarmType.SOUND)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Bottom,
            ) {
                sensor.latestMeasurement.pressure?.let {
                    ValueDisplay(
                        value = it,
                        sensor.alarmSensorStatus.getState(AlarmType.PRESSURE)
                    )
                }

                sensor.latestMeasurement.movement?.let {
                    ValueDisplay(
                        value = it,
                        sensor.alarmSensorStatus.getState(AlarmType.MOVEMENT)
                    )
                }
                sensor.latestMeasurement.co2?.let {
                    ValueDisplay(
                        value = it,
                        sensor.alarmSensorStatus.getState(AlarmType.CO2)
                    )
                }
                sensor.latestMeasurement.voc?.let {
                    ValueDisplay(
                        value = it,
                        sensor.alarmSensorStatus.getState(AlarmType.VOC)
                    )
                }
                sensor.latestMeasurement.nox?.let {
                    ValueDisplay(
                        value = it,
                        sensor.alarmSensorStatus.getState(AlarmType.NOX)
                    )
                }
                sensor.latestMeasurement.pm25?.let {
                    ValueDisplay(
                        value = it,
                        sensor.alarmSensorStatus.getState(AlarmType.PM25)
                    )
                }
            }
        }
    }
}

@Composable
fun ItemValuesWithoutTemperature(
    sensor: RuuviTag,
    modifier: Modifier = Modifier
) {
    if (sensor.latestMeasurement != null) {
        Row(
            verticalAlignment = Top,
            modifier = modifier
        ) {

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top,
            ) {
                sensor.latestMeasurement.humidity?.let {
                    ValueDisplay(
                        value = it,
                        sensor.alarmSensorStatus.getState(AlarmType.HUMIDITY)
                    )
                }
                sensor.latestMeasurement.movement?.let {
                    ValueDisplay(
                        value = it,
                        sensor.alarmSensorStatus.getState(AlarmType.MOVEMENT)
                    )
                }
                sensor.latestMeasurement.luminosity?.let {
                    ValueDisplay(
                        value = it,
                        sensor.alarmSensorStatus.getState(AlarmType.LUMINOSITY)
                    )
                }
                sensor.latestMeasurement.dBaAvg?.let {
                    ValueDisplay(
                        value = it,
                        sensor.alarmSensorStatus.getState(AlarmType.SOUND)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top,
            ) {
                sensor.latestMeasurement.pressure?.let {
                    ValueDisplay(
                        value = it,
                        sensor.alarmSensorStatus.getState(AlarmType.PRESSURE)
                    )
                }
                sensor.latestMeasurement.co2?.let {
                    ValueDisplay(
                        value = it,
                        sensor.alarmSensorStatus.getState(AlarmType.CO2)
                    )
                }
                sensor.latestMeasurement.voc?.let {
                    ValueDisplay(
                        value = it,
                        sensor.alarmSensorStatus.getState(AlarmType.VOC)
                    )
                }
                sensor.latestMeasurement.nox?.let {
                    ValueDisplay(
                        value = it,
                        sensor.alarmSensorStatus.getState(AlarmType.NOX)
                    )
                }
                sensor.latestMeasurement.pm25?.let {
                    ValueDisplay(
                        value = it,
                        sensor.alarmSensorStatus.getState(AlarmType.PM25)
                    )
                }
            }
        }
    }
}

@Composable
fun ItemBottom(
    sensor: RuuviTag,
    modifier: Modifier = Modifier
) {
    if (sensor.latestMeasurement != null) {
        ItemBottomUpdatedInfo(sensor = sensor, modifier = modifier)
    } else {
        ItemBottomNoData(modifier = modifier)
    }
}

@Composable
fun ItemBottomNoData(
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier.padding(top = 2.dp)
    ) {
        Text(
            modifier = Modifier.weight(1f),
            style = RuuviStationTheme.typography.dashboardSecondary,
            textAlign = TextAlign.Center,
            fontSize = 11.limitedSp,
            text = stringResource(id = R.string.no_data_10_days),
        )
    }
}

@Composable
fun ItemBottomUpdatedInfo(
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
            modifier = modifier.padding(top = 2.dp)
        ) {
            val fontScale = LocalContext.current.resources.configuration.fontScale

            // Do not simplify this - glitches are possible due to gateway and bluetooth icon differences
            val icon = sensor.getSource().getIconResource()
            if (sensor.getSource() == UpdateSource.Cloud) {
                Icon(
                    modifier = Modifier.height(RuuviStationTheme.dimensions.mediumPlus * fontScale),
                    painter = painterResource(id = icon),
                    tint = RuuviStationTheme.colors.primary.copy(alpha = 0.5f),
                    contentDescription = null,
                )
            } else {
                Icon(
                    modifier = Modifier.height(RuuviStationTheme.dimensions.mediumPlus * fontScale),
                    painter = painterResource(id = icon),
                    tint = RuuviStationTheme.colors.primary.copy(alpha = 0.5f),
                    contentDescription = null,
                )
            }

            Spacer(modifier = Modifier.width(RuuviStationTheme.dimensions.small))

            Text(
                modifier = Modifier.weight(1f),
                style = RuuviStationTheme.typography.dashboardSecondary,
                fontSize = 11.limitedSp,
                text = updatedText,
            )

            if (sensor.isLowBattery()) {
                LowBattery(modifier = Modifier.weight(1f))
            }
        }

        LaunchedEffect(key1 = lifecycle, key2 = sensor.latestMeasurement.updatedAt) {
            lifecycle.whenStarted {
                while (true) {
                    updatedText = sensor.latestMeasurement.updatedAt?.describingTimeSince(context) ?: ""
                    delay(500)
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun DashboardImage(userBackground: Uri) {
    Timber.d("Image path $userBackground")
    GlideImage(
        modifier = Modifier.fillMaxSize(),
        model = userBackground,
        contentDescription = null,
        contentScale = ContentScale.Crop
    )
    GlideImage(
        modifier = Modifier.fillMaxSize(),
        model = rememberResourceUri(R.drawable.tag_bg_layer),
        contentDescription = null,
        alpha = RuuviStationTheme.colors.backgroundAlpha,
        contentScale = ContentScale.Crop
    )
}

@Composable
fun ValueDisplay(
    value: EnvironmentValue,
    alarmState: AlarmState
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            modifier = Modifier
                .alignByBaseline(),
            text = value.valueWithoutUnit,
            style = RuuviStationTheme.typography.dashboardValue,
            fontSize = 14.limitedSp,
            color = colors.primary,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(width = 4.dp))
        Text(
            modifier = Modifier
                .weight(1f, false)
                .alignByBaseline(),
            text = value.unitString,
            style = RuuviStationTheme.typography.dashboardUnit,
            fontSize = 11.limitedSp,
            color = colors.primary,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(width = RuuviStationTheme.dimensions.small))

        if (alarmState != AlarmState.NO_ALARM) {
            AlarmStateIndicator(
                alarmState = alarmState,
                useThemes = true
            )
            Spacer(modifier = Modifier
                .width(RuuviStationTheme.dimensions.small)
            )
        }
    }
}

@Composable
fun BigValueDisplay(
    value: EnvironmentValue,
    alarmState: AlarmState,
    modifier: Modifier = Modifier
) {

    Row(
        modifier = modifier
            .offset(y = (-8).dp.limitedScale()),
        verticalAlignment = Top
    ) {
        Text(
            style = RuuviStationTheme.typography.dashboardBigValue,
            text = value.valueWithoutUnit,
            fontSize = 32.limitedSp,
            lineHeight = 10.sp,
            color = colors.settingsTitleText
        )
        Text(
            modifier = Modifier
                .padding(
                    top = 6.dp,
                    start = 2.dp
                ),
            style = RuuviStationTheme.typography.dashboardBigValueUnit,
            lineHeight = 5.sp,
            fontSize = 16.limitedSp,
            text = value.unitString,
            color = colors.settingsTitleText,
        )

        Spacer(modifier = Modifier.width(width = 4.dp))

        AlarmStateIndicator(
            modifier = Modifier.offset(y = 11.dp.limitedScale()),
            alarmState = alarmState,
            useThemes = true
        )
    }
}
@Composable
fun AQIDisplay(
    value: AQI,
    alertTriggered: Boolean,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier.offset(y = (-8).dp.limitedScale()),
            verticalAlignment = Top
        ) {
            Text(
                style = RuuviStationTheme.typography.dashboardBigValue,
                text = value.score.toString(),
                lineHeight = 10.sp,
                fontSize = 32.limitedSp,
                color = colors.settingsTitleText
            )
            Column (
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ){
                Text(
                    modifier = Modifier
                        .padding(
                            top = 6.dp,
                            start = 4.dp
                        ),
                    style = RuuviStationTheme.typography.dashboardBigValueUnit,
                    lineHeight = 10.sp,
                    fontSize = 16.limitedSp,
                    text = "/100",
                    color = colors.settingsTitleText
                )
                Text(
                    modifier = Modifier
                        .padding(start = 4.dp),
                    style = RuuviStationTheme.typography.dashboardSecondary,
                    fontSize = 11.limitedSp,
                    lineHeight = 6.sp,
                    text = stringResource(id = R.string.air_quality),
                )
            }
        }
        LinearProgressIndicator(
            modifier = Modifier.width(110.dp),
            progress = (value.score ?: 0) / 100F,
            color = value.color
        )
    }
}

@Composable
fun DashboardItemDropdownMenu(
    lazyGridState: LazyGridState,
    itemIndex: Int,
    sensor: RuuviTag,
    userEmail: String?,
    setName: (String, String?) -> Unit,
    moveItem: (Int, Int, Boolean) -> Unit
) {
    val context = LocalContext.current
    var threeDotsMenuExpanded by remember {
        mutableStateOf(false)
    }
    val coroutineScope = rememberCoroutineScope()

    var setNameDialog by remember { mutableStateOf(false) }

    val canBeShared = sensor.owner.equals(userEmail, true)
    val canBeClaimed = sensor.owner.isNullOrEmpty() && userEmail?.isNotEmpty() == true

    Box() {
        IconButton(
            modifier = Modifier.size(RuuviStationTheme.dimensions.dashboardIconSize),
            onClick = { threeDotsMenuExpanded = !threeDotsMenuExpanded }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_3dots),
                tint = RuuviStationTheme.colors.dashboardBurger,
                contentDescription = ""
            )
        }

        DropdownMenu(
            modifier = Modifier.background(color = RuuviStationTheme.colors.background),
            expanded = threeDotsMenuExpanded,
            onDismissRequest = { threeDotsMenuExpanded = false }
        ) {
            DropdownMenuItem(onClick = {
                SensorCardActivity.start(context, sensor.id, SensorCardOpenType.CARD)
                threeDotsMenuExpanded = false
            }) {
                DashboardMenuItemText(text = stringResource(
                    id = R.string.full_image_view
                ))
            }

            DropdownMenuItem(onClick = {
                SensorCardActivity.start(context, sensor.id, SensorCardOpenType.HISTORY)
                threeDotsMenuExpanded = false
            }) {
                DashboardMenuItemText(text = stringResource(
                    id = R.string.history_view
                ))
            }
            DropdownMenuItem(onClick = {
                TagSettingsActivity.start(context, sensor.id)
                threeDotsMenuExpanded = false
            }) {
                DashboardMenuItemText(text = stringResource(
                    id = R.string.settings_and_alerts
                ))
            }
            DropdownMenuItem(onClick = {
                BackgroundActivity.start(context, sensor.id)
                threeDotsMenuExpanded = false
            }) {
                DashboardMenuItemText(text = stringResource(
                    id = R.string.change_background
                ))
            }

            DropdownMenuItem(onClick = {
                setNameDialog = true
                threeDotsMenuExpanded = false
            }) {
                DashboardMenuItemText(text = stringResource(
                    id = R.string.rename
                ))
            }

            if (itemIndex != 0) {
                DropdownMenuItem(onClick = {
                    var newIndex = itemIndex - 1
                    moveItem(itemIndex, newIndex, true)
                    if (newIndex < 0) newIndex = 0
                    threeDotsMenuExpanded = false
                    coroutineScope.launch {
                        lazyGridState.centerViewportOnItem(newIndex)
                    }
                }) {
                    DashboardMenuItemText(text = stringResource(id = R.string.move_up))
                }
            }

            if (itemIndex != lazyGridState.layoutInfo.totalItemsCount - 2) {
                DropdownMenuItem(onClick = {
                    var newIndex = itemIndex + 1
                    moveItem(itemIndex, newIndex, true)
                    if (newIndex >= lazyGridState.layoutInfo.totalItemsCount) {
                        newIndex = lazyGridState.layoutInfo.totalItemsCount - 2
                    }
                    threeDotsMenuExpanded = false
                    coroutineScope.launch {
                        lazyGridState.centerViewportOnItem(newIndex)
                    }
                }) {
                    DashboardMenuItemText(text = stringResource(id = R.string.move_down))
                }
            }

            if (canBeClaimed) {
                DropdownMenuItem(onClick = {
                    ClaimSensorActivity.start(context, sensor.id)
                    threeDotsMenuExpanded = false
                }) {
                    DashboardMenuItemText(text = stringResource(
                        id = R.string.claim_sensor
                    ))
                }
            } else if (canBeShared) {
                DropdownMenuItem(onClick = {
                    ShareSensorActivity.start(context, sensor.id)
                    threeDotsMenuExpanded = false
                }) {
                    DashboardMenuItemText(text = stringResource(
                        id = R.string.share
                    ))
                }
            }

            DropdownMenuItem(onClick = {
                TagSettingsActivity.startToRemove(context, sensor.id)
                threeDotsMenuExpanded = false
            }) {
                DashboardMenuItemText(text = stringResource(
                    id = R.string.remove
                ))
            }
        }
    }

    if (setNameDialog) {
        SetSensorName(
            value = sensor.name,
            defaultName = sensor.getDefaultName(),
            setName = {newName ->
                setName.invoke(sensor.id, newName)
            }
        ) {
            setNameDialog = false
        }
    }
}

@Composable
fun LowBattery(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = modifier
    ) {
        Text(
            style = RuuviStationTheme.typography.dashboardSecondary,
            fontSize = 11.limitedSp,
            text = stringResource(id = R.string.low_battery),
        )
        Spacer(modifier = Modifier.width(RuuviStationTheme.dimensions.medium))
        Image(
            modifier = Modifier
                .height(12.dp)
                .align(CenterVertically),
            painter = painterResource(id = R.drawable.icon_battery_low),
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(RuuviStationTheme.dimensions.mediumPlus))
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
}