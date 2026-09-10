package com.ruuvi.station.tagdetails.ui

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.core.app.TaskStackBuilder
import androidx.core.content.IntentCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ruuvi.station.R
import com.ruuvi.station.alarm.ui.AlarmItemsViewModel
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.app.ui.components.limitScaleTo
import com.ruuvi.station.app.ui.components.modifier.fadingEdge
import com.ruuvi.station.app.ui.components.scaleUpTo
import com.ruuvi.station.app.ui.theme.*
import com.ruuvi.station.dashboard.DashboardTapAction
import com.ruuvi.station.dashboard.ui.DashboardActivity
import com.ruuvi.station.feature.data.FeatureFlag
import com.ruuvi.station.feature.domain.RuntimeBehavior
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.UpdateSource
import com.ruuvi.station.tag.domain.isLowBattery
import com.ruuvi.station.tagdetails.ui.elements.BigValueDisplay
import com.ruuvi.station.tagdetails.ui.elements.CircularAQIDisplay
import com.ruuvi.station.tagdetails.ui.elements.SensorValueItem
import com.ruuvi.station.tagdetails.ui.popup.ValueBottomSheet
import com.ruuvi.station.tagsettings.di.RemoveSensorViewModelArgs
import com.ruuvi.station.tagsettings.di.TagSettingsViewModelArgs
import com.ruuvi.station.tagsettings.ui.RemoveSensorViewModel
import com.ruuvi.station.tagsettings.ui.TagSettingsViewModel
import com.ruuvi.station.tagsettings.ui.led_control.LedControlViewModel
import com.ruuvi.station.tagsettings.ui.notes.NotesViewModel
import com.ruuvi.station.tagsettings.ui.visible_measurements.VisibleMeasurementsViewModel
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.units.model.UnitType
import com.ruuvi.station.util.base.NfcActivity
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.android.closestDI
import com.ruuvi.station.util.extensions.*
import com.ruuvi.station.util.ui.pxToDp
import com.ruuvi.station.vico.model.ChartData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import org.kodein.di.instance
import timber.log.Timber
import kotlin.math.ceil
import kotlin.math.floor

private const val MAXIMUM_FONT_SCALE = 1.5f
private const val TABLET_MINIMUM_WIDTH_DP = 600
private const val TABLET_LANDSCAPE_COLUMNS = 4
private const val TABLET_COLUMNS = 3
private const val LANDSCAPE_COLUMNS = 3
private const val PHONE_COLUMNS = 2
private val SENSOR_VALUE_ITEM_SPACING = 6.dp
private val PAGE_INDICATOR_WIDTH = 4.dp
private const val SENSOR_STATUS_REFRESH_DELAY_MILLIS = 500L

class SensorCardActivity : NfcActivity(), DIAware {


    override val di: DI by closestDI()

    private val unitsConverter: UnitsConverter by instance()
    private val runtimeBehavior: RuntimeBehavior by instance()
    private val preferences: PreferencesRepository by instance()

    private val requestedOpenType: SensorCardOpenType by lazy(LazyThreadSafetyMode.NONE) {
        IntentCompat.getSerializableExtra(
            intent,
            ARGUMENT_OPEN_TYPE,
            SensorCardOpenType::class.java,
        ) ?: SensorCardOpenType.DEFAULT
    }

    private val startDestination: SensorDetailStartDestination by lazy(LazyThreadSafetyMode.NONE) {
        requestedOpenType.resolveStartDestination(
            defaultShowsHistory = preferences.getDashboardTapAction() == DashboardTapAction.SHOW_CHART,
        )
    }

    private inline fun <reified TViewModel : ViewModel, reified TArgument> keyedViewModel(
        key: String,
        argument: TArgument,
    ): TViewModel = ViewModelProvider(
        this,
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                kodein.direct.instance<TArgument, TViewModel>(arg = argument) as T
        }
    )[key, TViewModel::class.java]

    private fun tagSettingsViewModel(sensorId: String): TagSettingsViewModel = keyedViewModel(
        key = "sensor-settings:$sensorId",
        argument = TagSettingsViewModelArgs(
            tagId = sensorId,
            newSensor = intent.getBooleanExtra(ARGUMENT_NEW_SENSOR, false) &&
                sensorId == intent.getStringExtra(ARGUMENT_SENSOR_ID),
            openRemove = false,
        )
    )

    private fun alarmsViewModel(sensorId: String): AlarmItemsViewModel = keyedViewModel(
        key = "sensor-alerts:$sensorId",
        argument = sensorId,
    )

    private fun removeSensorViewModel(sensorId: String): RemoveSensorViewModel = keyedViewModel(
        key = "sensor-remove:$sensorId",
        argument = RemoveSensorViewModelArgs(sensorId),
    )

    private fun visibleMeasurementsViewModel(sensorId: String): VisibleMeasurementsViewModel = keyedViewModel(
        key = "visible-measurements:$sensorId",
        argument = sensorId,
    )

    private fun ledControlViewModel(sensorId: String): LedControlViewModel = keyedViewModel(
        key = "led-control:$sensorId",
        argument = sensorId,
    )

    private fun notesViewModel(sensorId: String): NotesViewModel = keyedViewModel(
        key = "notes:$sensorId",
        argument = sensorId,
    )

    private val detailViewModelProvider by lazy(LazyThreadSafetyMode.NONE) {
        SensorDetailViewModelProvider(
            settings = ::tagSettingsViewModel,
            alerts = ::alarmsViewModel,
            removeSensor = ::removeSensorViewModel,
            visibleMeasurements = ::visibleMeasurementsViewModel,
            ledControl = ::ledControlViewModel,
            notes = ::notesViewModel,
        )
    }

    private val viewModel: SensorCardViewModel by viewModel {
        SensorCardViewModelArguments(
            sensorId = intent.getStringExtra(ARGUMENT_SENSOR_ID),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            RuuviTheme {
                SensorDetailRoute(
                    viewModel = viewModel,
                    startDestination = startDestination,
                    viewModelProvider = detailViewModelProvider,
                    unitsConverter = unitsConverter,
                    useNewSensorCard = runtimeBehavior.isFeatureEnabled(FeatureFlag.NEW_SENSOR_CARD),
                    onFinish = ::finish,
                )
            }
        }
    }

    companion object {
        const val ARGUMENT_SENSOR_ID = "ARGUMENT_SENSOR_ID"
        const val ARGUMENT_OPEN_TYPE = "ARGUMENT_OPEN_TYPE"
        const val ARGUMENT_NEW_SENSOR = "ARGUMENT_NEW_SENSOR"

        fun start(
            context: Context,
            sensorId: String,
            openType: SensorCardOpenType = SensorCardOpenType.DEFAULT
        ) {
            context.startActivity(createIntent(context, sensorId, openType))
        }

        fun startAfterAddingNewSensor(context: Context, sensorId: String?) {
            createDashboardStack(
                context = context,
                detailIntent = createIntent(
                    context = context,
                    sensorId = sensorId,
                    openType = SensorCardOpenType.SETTINGS,
                    newSensor = true,
                ),
            ).startActivities()
        }

        fun startToRemove(context: Context, sensorId: String?) {
            start(context, requireNotNull(sensorId), SensorCardOpenType.REMOVE)
        }

        fun startWithDashboard(
            context: Context,
            sensorId: String,
            openType: SensorCardOpenType = SensorCardOpenType.DEFAULT
        ) {
            createDashboardStack(
                context = context,
                detailIntent = createIntent(context, sensorId, openType),
            ).startActivities()
        }

        fun createPendingIntent(
            context: Context,
            sensorId: String,
            requestCode: Int,
            openType: SensorCardOpenType = SensorCardOpenType.DEFAULT
        ): PendingIntent? = createPendingIntent(
            context = context,
            sensorId = sensorId,
            requestCode = requestCode,
            openType = openType,
            identity = null
        )

        fun createWidgetPendingIntent(
            context: Context,
            sensorId: String,
            appWidgetId: Int
        ): PendingIntent? = createPendingIntent(
            context = context,
            sensorId = sensorId,
            requestCode = appWidgetId,
            openType = SensorCardOpenType.DEFAULT,
            identity = Uri.Builder()
                .scheme("ruuvi-station")
                .authority("widget")
                .appendPath(appWidgetId.toString())
                .appendPath(sensorId)
                .build()
        )

        private fun createPendingIntent(
            context: Context,
            sensorId: String,
            requestCode: Int,
            openType: SensorCardOpenType,
            identity: Uri?
        ): PendingIntent? {
            return createDashboardStack(
                context = context,
                detailIntent = createIntent(context, sensorId, openType).apply {
                    data = identity
                },
            )
                .getPendingIntent(requestCode, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        private fun createIntent(
            context: Context,
            sensorId: String?,
            openType: SensorCardOpenType,
            newSensor: Boolean = false,
        ) = Intent(context, SensorCardActivity::class.java).apply {
            putExtra(ARGUMENT_SENSOR_ID, sensorId)
            putExtra(ARGUMENT_OPEN_TYPE, openType)
            putExtra(ARGUMENT_NEW_SENSOR, newSensor)
        }

        private fun createDashboardStack(
            context: Context,
            detailIntent: Intent,
        ): TaskStackBuilder = TaskStackBuilder.create(context)
            .addNextIntent(Intent(context, DashboardActivity::class.java))
            .addNextIntent(detailIntent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorCard(
    modifier: Modifier = Modifier,
    sensor: RuuviTag,
    getChartData: (String, UnitType, Int) -> Flow<ChartData>,
    scrollToChart: (UnitType) -> Unit
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    var sheetUnitType by remember(sensor.id) { mutableStateOf<UnitType?>(null) }
    val itemHeight = 48.dp.scaleUpTo(MAXIMUM_FONT_SCALE)
    var size by remember { mutableStateOf(IntSize.Zero) }
    var topSize by remember { mutableStateOf(IntSize.Zero) }
    val halfSize = (size.height / 2).pxToDp()
    val scrollState = rememberScrollState()
    val valuesWithoutFirst = if (sensor.valuesToDisplay.isNotEmpty()) {
        sensor.valuesToDisplay.subList(1, sensor.valuesToDisplay.size)
    } else {
        listOf()
    }
    val padding = if (halfSize < 200.dp) 8.dp else 32.dp
    val itemSeparator = SENSOR_VALUE_ITEM_SPACING
    val bottomSize = floor(((size.height - topSize.height).pxToDp() - padding - itemSeparator).value).dp

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
                    .padding(top = padding, bottom = itemSeparator)
                    .onGloballyPositioned { layoutCoordinates ->
                        topSize = layoutCoordinates.size
                    }
            ) {
                val firstValue = sensor.valuesToDisplay.firstOrNull()
                if (firstValue != null) {
                    TopMeasurement(
                        sensor = sensor,
                        value = firstValue,
                    ) {
                        showBottomSheet = true
                        sheetUnitType = firstValue.unitType
                    }
                }
            }

            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val isTablet = configuration.smallestScreenWidthDp >= TABLET_MINIMUM_WIDTH_DP
            val columnCount = when {
                isTablet && isLandscape -> TABLET_LANDSCAPE_COLUMNS
                isTablet -> TABLET_COLUMNS
                isLandscape -> LANDSCAPE_COLUMNS
                else -> PHONE_COLUMNS
            }
            val horizontalPadding = when {
                isTablet && isLandscape -> 80.dp
                isTablet -> 60.dp
                else -> 20.dp
            }
            val rowCount = ceil(valuesWithoutFirst.size / columnCount.toFloat())

            if (rowCount * (itemHeight + itemSeparator) <= bottomSize) {
                Column(
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .height(bottomSize)
                        .fillMaxWidth()
                ) {
                    SensorValues(
                        modifier = Modifier,
                        sensor = sensor,
                        itemHeight = itemHeight,
                        columnCount = columnCount,
                        horizontalPadding = horizontalPadding,
                    ) {
                        showBottomSheet = true
                        sheetUnitType = it.unitType
                    }
                }
            } else {
                SensorValues(
                    modifier = Modifier,
                    sensor = sensor,
                    itemHeight = itemHeight,
                    columnCount = columnCount,
                    horizontalPadding = horizontalPadding,
                ) {
                    showBottomSheet = true
                    sheetUnitType = it.unitType
                }
            }
        }
    }

    if (showBottomSheet) {
        val value = resolveCurrentSheetValue(sensor, sheetUnitType)
        value?.let {

            val chartHistory by produceState<ChartData?>(
                initialValue = null,
                key1 = sensor.id,
                key2 = value.unitType
            ) {
                getChartData(sensor.id, value.unitType, 48).collectLatest { data ->
                    this.value = data
                }
            }

            val extraValues = if (value.unitType is UnitType.AirQuality.AqiIndex) {
                listOfNotNull(sensor.latestMeasurement?.pm25, sensor.latestMeasurement?.co2)
            } else {
                listOf()
            }

            ValueBottomSheet(
                sheetValue = value,
                extraValues = extraValues,
                alarmStatus = sensor.alarmSensorStatus,
                chartHistory = chartHistory,
                maxHeight = size.height,
                lastUpdate = sensor.latestMeasurement?.updatedAt,
                modifier = Modifier,
                scrollToChart = scrollToChart,
                onChangeValue = { newValue -> sheetUnitType = newValue.unitType }
            ) {
                showBottomSheet = false
            }
        }
    }
}

internal fun resolveCurrentSheetValue(
    sensor: RuuviTag,
    selectedUnitType: UnitType?,
): EnvironmentValue? {
    if (selectedUnitType == null) return null

    return sensor.valuesToDisplay.firstOrNull { it.unitType == selectedUnitType }
        ?: listOfNotNull(
            sensor.latestMeasurement?.pm25,
            sensor.latestMeasurement?.co2,
        ).firstOrNull { it.unitType == selectedUnitType }
}

@Composable
fun TopMeasurement(
    sensor: RuuviTag,
    value: EnvironmentValue,
    modifier: Modifier = Modifier,
    clickAction: () -> Unit = {}
) {
    if (value.unitType is UnitType.AirQuality) {
        if (sensor.latestMeasurement != null) {
            CircularAQIDisplay(
                value = value,
                aqi = sensor.latestMeasurement.aqiScore,
                alertActive = value.unitType.alarmType?.let {
                    sensor.alarmSensorStatus.triggered(it)
                } ?: false
            ) { clickAction.invoke() }
        }
    } else {
        BigValueDisplay(
            value = value,
            showName = true,
            alertActive = value.unitType.alarmType?.let {
                sensor.alarmSensorStatus.triggered(it)
            } ?: false
        ) { clickAction.invoke() }
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
    columnCount: Int,
    itemHeight: Dp,
    horizontalPadding: Dp,
    onValueClick: (EnvironmentValue) -> Unit
) {
    if (sensor.valuesToDisplay.size <= 1) return
    val valuesWithoutFirst = sensor.valuesToDisplay.subList(1, sensor.valuesToDisplay.size)

    val valuesDistributed = distributeRoundRobin(valuesWithoutFirst, columnCount)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(SENSOR_VALUE_ITEM_SPACING, Alignment.CenterHorizontally)
    ) {
        for (columnValues in valuesDistributed) {
            Column(
                modifier = Modifier
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(SENSOR_VALUE_ITEM_SPACING, Alignment.Top),
                horizontalAlignment = Alignment.Start
            ) {
                if (columnValues.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth()) {}
                }
                for (value in columnValues) {
                    val unit = if (value.unitType == UnitType.MovementUnit.MovementsCount) {
                        ""
                    } else {
                        value.unitString
                    }
                    SensorValueItem(
                        icon = value.unitType.iconRes,
                        value = value.valueWithoutUnit,
                        unit = unit,
                        itemHeight = itemHeight,
                        modifier = Modifier.fillMaxWidth(),
                        alertActive = value.unitType.alarmType?.let {
                            sensor.alarmSensorStatus.triggered(it)
                        } ?: false,
                        name = value.unitType.measurementName.let { stringResource(it) }
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
            .width(PAGE_INDICATOR_WIDTH)
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
                .width(PAGE_INDICATOR_WIDTH)
                .height(scrollBarHeight)
                .background(
                    Color.White.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(2.dp),
                )
        )
    }
}

@Composable
fun SensorCardLowBattery(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = modifier
    ) {
        Text(
            color = White80,
            style = RuuviStationTheme.typography.dashboardSecondary,
            textAlign = TextAlign.Right,
            text = stringResource(id = R.string.low_battery),
            fontSize = ruuviStationFontsSizes.petite.limitScaleTo(1.5f),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Image(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = R.drawable.icon_battery_low),
            contentDescription = null,
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
fun SensorCardImage(
    userBackground: Uri,
) {
    Timber.d("Image path $userBackground")

    AsyncImage(
        modifier = Modifier.fillMaxSize(),
        model = ImageRequest.Builder(LocalContext.current)
            .data(userBackground)
            .crossfade(true)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop
    )
    Image(
        modifier = Modifier.fillMaxSize(),
        painter = painterResource(R.drawable.tag_bg_layer),
        contentDescription = null,
        contentScale = ContentScale.Crop
    )
}

@Composable
fun SensorCardBottom(
    sensor: RuuviTag,
    modifier: Modifier = Modifier
) {
    if (sensor.latestMeasurement != null) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current

        var updatedText by remember {
            mutableStateOf(sensor.latestMeasurement.updatedAt.describingTimeSince(context))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
        ) {
            val icon = sensor.getSource().getIconResource()
            Row (
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier
                        .size(
                            width = if (sensor.getSource() == UpdateSource.Cloud) {
                                22.dp
                            } else {
                                16.dp
                            },
                            height = if (sensor.getSource() == UpdateSource.Cloud) {
                                16.dp
                            } else {
                                24.dp
                            },
                        ),
                    painter = painterResource(id = icon),
                    tint = White80,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(SENSOR_VALUE_ITEM_SPACING))
                Text(
                    modifier = Modifier,
                    style = RuuviStationTheme.typography.dashboardSecondary,
                    color = White80,
                    fontSize = ruuviStationFontsSizes.petite.limitScaleTo(1.5f),
                    textAlign = TextAlign.Right,
                    text = updatedText,
                )
            }

            if (sensor.isLowBattery()) {
                SensorCardLowBattery()
            }
        }

        LaunchedEffect(lifecycleOwner, sensor.latestMeasurement.updatedAt) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    updatedText =
                        sensor.latestMeasurement.updatedAt.describingTimeSince(context)
                    delay(SENSOR_STATUS_REFRESH_DELAY_MILLIS)
                }
            }
        }

    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
        ) {
            Text(
                modifier = Modifier.weight(1f),
                style = RuuviStationTheme.typography.dashboardSecondary,
                color = White80,
                fontSize = ruuviStationFontsSizes.petite.limitScaleTo(1.5f),
                textAlign = TextAlign.Center,
                text = stringResource(id = R.string.no_data_10_days),
            )
        }
    }
}
