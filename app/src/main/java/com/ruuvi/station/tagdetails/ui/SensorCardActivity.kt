package com.ruuvi.station.tagdetails.ui

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.platform.LocalLifecycleOwner
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.systemuicontroller.rememberSystemUiController
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
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import com.ruuvi.station.util.extensions.*
import com.ruuvi.station.util.ui.pxToDp
import com.ruuvi.station.vico.model.ChartData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import org.kodein.di.generic.instance
import org.kodein.di.direct
import timber.log.Timber
import kotlin.math.ceil
import kotlin.math.floor

class SensorCardActivity : NfcActivity(), KodeinAware {

    override val kodein by closestKodein()

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
        ): PendingIntent? {
            return createDashboardStack(
                context = context,
                detailIntent = createIntent(context, sensorId, openType),
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
    val padding = if (halfSize < 200.dp) 8.dp else 32.dp
    val itemSeparator = 8.dp
    val bottomSize = floor(((size.height - topSize.height).pxToDp() - padding - itemSeparator).value).dp
    val columnMaxWidth = 200.dp

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
                        sheetValue = firstValue
                    }
                }
            }

            val configuration = LocalConfiguration.current
            val columnCount = if (configuration.screenWidthDp > 650) {
                floor(configuration.screenWidthDp / columnMaxWidth.value).toInt()
            } else {
                2
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
                        columnMaxWidth = columnMaxWidth
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
                    columnCount = columnCount,
                    columnMaxWidth = columnMaxWidth
                ) {
                    showBottomSheet = true
                    sheetValue = it
                }
            }
        }
    }

    if (showBottomSheet) {
        sheetValue?.let { value ->

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
                chartHistory = chartHistory,
                maxHeight = size.height,
                lastUpdate = sensor.latestMeasurement?.updatedAt,
                modifier = Modifier,
                scrollToChart = scrollToChart,
                onChangeValue = { newValue -> sheetValue = newValue}
            ) {
                showBottomSheet = false
            }
        }
    }
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
    columnMaxWidth: Dp,
    onValueClick: (EnvironmentValue) -> Unit
) {
    if (sensor.valuesToDisplay.size <= 1) return
    val valuesWithoutFirst = sensor.valuesToDisplay.subList(1, sensor.valuesToDisplay.size)

    val valuesDistributed = distributeRoundRobin(valuesWithoutFirst, columnCount)

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

@Composable
fun SensorCardImage(
    userBackground: Uri,
    chartsEnabled: Boolean
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
