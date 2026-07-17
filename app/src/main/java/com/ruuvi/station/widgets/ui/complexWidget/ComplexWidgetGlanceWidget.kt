package com.ruuvi.station.widgets.ui.complexWidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.datastore.preferences.core.Preferences
import androidx.glance.*
import androidx.glance.appwidget.PreviewSizeMode
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.*
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.theme.ruuviStationFontsSizes
import com.ruuvi.station.dashboard.ui.DashboardActivity
import com.ruuvi.station.tagdetails.ui.SensorCardActivity
import com.ruuvi.station.widgets.data.ComplexWidgetData
import com.ruuvi.station.widgets.data.SensorValue
import com.ruuvi.station.widgets.data.WidgetType
import com.ruuvi.station.widgets.ui.glance.CustomFontText
import com.ruuvi.station.widgets.ui.glance.FontBitmapSize
import com.ruuvi.station.widgets.ui.glance.GlanceColors
import com.ruuvi.station.widgets.ui.glance.GlanceFontUtils
import com.ruuvi.station.widgets.ui.glance.RefreshButton
import com.ruuvi.station.widgets.ui.simpleWidget.SimpleWidget
import com.ruuvi.station.widgets.ui.simpleWidget.SimpleWidgetLayoutConfig
import java.util.Date
import kotlin.math.roundToInt

// Width cap for one sensor block; wider widgets add sensor columns instead of stretching
// a single value grid.
private val MAX_CONTENT_WIDTH = 420.dp
private val SENSOR_COLUMN_GAP = 16.dp
private val TWO_SENSOR_WIDTH = MAX_CONTENT_WIDTH * 2 + SENSOR_COLUMN_GAP
private val SIMPLE_WIDGET_REFRESH_BUTTON_CONFIG = SimpleWidgetLayoutConfig.fromHeight(100.dp)

// Switch to two sensor blocks per row once the launcher grants enough width.
private val TWO_COLUMN_THRESHOLD = MAX_CONTENT_WIDTH + MAX_CONTENT_WIDTH / 2
private const val DASHBOARD_SMALL_VALUE_MAX_FONT_SCALE = 1.5f

object ComplexWidgetGlanceWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    // Use the launcher's real granted size so column thresholds see the actual widget width.
    override val sizeMode = SizeMode.Exact

    // There's no real host size to read for a generated preview, so pick one representative
    // single-column width for providePreview/LocalSize to compose against.
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(setOf(DpSize(350.dp, 90.dp)))

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent {
            ComplexWidgetContent(mockPreviewSensors(context))
        }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val dataJson = prefs[ComplexWidgetPrefKeys.data]
            val sensors = if (!dataJson.isNullOrEmpty()) {
                try {
                    val type = object : TypeToken<List<ComplexWidgetData>>() {}.type
                    Gson().fromJson<List<ComplexWidgetData>>(dataJson, type)
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }

            ComplexWidgetContent(sensors)
        }
    }
}

// Mirrors widget_complex_picker_preview_v2.xml's mock values, so the generated preview
// (Android 15+) and the static picker preview XML (pre-15 fallback) show the same content.
private fun mockPreviewSensors(context: Context): List<ComplexWidgetData> = listOf(
    ComplexWidgetData(
        sensorId = "preview",
        timestamp = Date(),
        displayName = "Bedroom",
        sensorValues = listOf(
            SensorValue(WidgetType.TEMPERATURE, "22.54", context.getString(R.string.temperature_celsius_unit)),
            SensorValue(WidgetType.DEW_POINT_F, "59.19", context.getString(R.string.temperature_fahrenheit_unit)),
            SensorValue(WidgetType.HUMIDITY, "39.27", context.getString(R.string.humidity_relative_unit)),
            SensorValue(WidgetType.VOLTAGE, "2.97", context.getString(R.string.voltage_unit))
        ),
        updated = context.getString(R.string.widgets_preview_time)
    )
)

@Composable
private fun ComplexWidgetContent(sensors: List<ComplexWidgetData>) {
    val rawWidth = LocalSize.current.width
    val sensorColumns = if (rawWidth >= TWO_COLUMN_THRESHOLD) 2 else 1

    // Cap only the 2-column layout; single-column widgets should keep filling their cell.
    val perSensorWidth = if (sensorColumns == 2) {
        (minOf(rawWidth, TWO_SENSOR_WIDTH) - SENSOR_COLUMN_GAP) / 2
    } else {
        rawWidth
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceColors.background)
            .padding(all = 4.dp)
    ) {
        if (sensors.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity<DashboardActivity>()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = LocalContext.current.getString(R.string.widgets_loading),
                    style = TextStyle(color = GlanceColors.widgetSensorName, fontSize = ruuviStationFontsSizes.normal)
                )
            }
        } else if (sensorColumns == 1) {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(sensors) { sensor ->
                    SensorBlock(
                        sensor = sensor,
                        sensorWidth = perSensorWidth,
                        modifier = GlanceModifier.fillMaxWidth()
                    )
                }
            }
        } else {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = GlanceModifier
                        .fillMaxHeight()
                        .width(minOf(rawWidth, TWO_SENSOR_WIDTH))
                ) {
                    items(sensors.chunked(sensorColumns)) { rowSensors ->
                        Row(modifier = GlanceModifier.fillMaxWidth()) {
                            rowSensors.forEach { sensor ->
                                SensorBlock(
                                    sensor = sensor,
                                    sensorWidth = perSensorWidth,
                                    modifier = GlanceModifier.defaultWeight()
                                )
                            }
                            // Keep a lone final sensor at the same width as paired rows.
                            if (rowSensors.size == 1) {
                                Spacer(modifier = GlanceModifier.defaultWeight())
                            }
                        }
                    }
                }
            }
        }

        RefreshButton(
            action = actionRunCallback<RefreshComplexWidgetAction>(),
            size = SIMPLE_WIDGET_REFRESH_BUTTON_CONFIG.refreshButtonSize,
            iconSize = SIMPLE_WIDGET_REFRESH_BUTTON_CONFIG.refreshIconSize
        )
    }
}

@Composable
private fun SensorBlock(sensor: ComplexWidgetData, sensorWidth: Dp, modifier: GlanceModifier) {
    val openAction = actionRunCallback<OpenComplexWidgetSensorAction>(
        SimpleWidget.openSensorActionParameters(sensor.sensorId, 0)
    )
    Column(modifier = modifier) {
        SensorHeader(sensor, openAction)
        Spacer(modifier = GlanceModifier.height(12.dp))
        MeasurementGrid(sensor.sensorValues, sensorWidth, openAction)
        SensorFooter(sensor, openAction)
    }
}

@Composable
private fun SensorHeader(sensor: ComplexWidgetData, action: Action) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(top = 12.dp)
            .clickable(action),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CustomFontText(
            text = sensor.displayName,
            fontSize = ruuviStationFontsSizes.normal,
            colorProvider = GlanceColors.widgetSensorName,
            fontResId = R.font.mulish_bold,
            modifier = GlanceModifier.defaultWeight()
        )
    }
}

// Match the dashboard card: split values by index parity into two fixed columns.
@Composable
private fun MeasurementGrid(values: List<SensorValue>, sensorWidth: Dp, action: Action) {
    val evenValues = values.filterIndexed { index, _ -> index % 2 == 0 }
    val oddValues = values.filterIndexed { index, _ -> index % 2 != 0 }

    // Bound measurement labels so bitmap text ellipsizes instead of spilling into the next column.
    val columnWidth = (sensorWidth - 24.dp) / 2

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .clickable(action)
    ) {
        Column(
            modifier = GlanceModifier.defaultWeight(),
            verticalAlignment = Alignment.Bottom
        ) {
            evenValues.forEach { value ->
                MeasurementItem(value, columnWidth, modifier = GlanceModifier.padding(top = 2.dp))
            }
        }
        Column(
            modifier = GlanceModifier.defaultWeight(),
            verticalAlignment = Alignment.Bottom
        ) {
            oddValues.forEach { value ->
                MeasurementItem(value, columnWidth, modifier = GlanceModifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun SensorFooter(sensor: ComplexWidgetData, action: Action) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp)
            .clickable(action)
    ) {
        Spacer(modifier = GlanceModifier.height(8.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            CustomFontText(
                text = sensor.updated ?: "",
                fontSize = ruuviStationFontsSizes.tiny2,
                colorProvider = GlanceColors.widgetSensorName,
                fontResId = R.font.mulish_regular
            )
        }
    }
}

@Composable
private fun MeasurementItem(value: SensorValue, columnWidth: Dp, modifier: GlanceModifier) {
    val context = LocalContext.current
    val measurementName = context.getString(value.type.unitType.measurementName)
    val textLayout = calculateMeasurementTextLayout(value, measurementName, columnWidth)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top
    ) {
        CustomFontText(
            text = value.sensorValue,
            fontSize = ruuviStationFontsSizes.compact,
            colorProvider = GlanceColors.valueColor,
            fontResId = R.font.oswald_bold,
            modifier = GlanceModifier.padding(top = textLayout.valueTopPadding),
            maxFontScale = DASHBOARD_SMALL_VALUE_MAX_FONT_SCALE
        )

        if (value.unit.isNotEmpty()) {
            Spacer(modifier = GlanceModifier.width(4.dp))
            CustomFontText(
                text = value.unit,
                fontSize = ruuviStationFontsSizes.petite,
                colorProvider = GlanceColors.widgetSensorName,
                fontResId = R.font.oswald_light,
                modifier = GlanceModifier.padding(top = textLayout.unitTopPadding),
                maxFontScale = DASHBOARD_SMALL_VALUE_MAX_FONT_SCALE
            )
        }

        Spacer(modifier = GlanceModifier.width(4.dp))
        CustomFontText(
            text = measurementName,
            fontSize = ruuviStationFontsSizes.petite,
            maxWidth = columnWidth,
            colorProvider = GlanceColors.widgetSensorName,
            fontResId = R.font.mulish_regular,
            modifier = GlanceModifier.padding(top = textLayout.measurementTopPadding),
            maxFontScale = DASHBOARD_SMALL_VALUE_MAX_FONT_SCALE
        )
    }
}

private data class MeasurementTextLayout(
    val valueTopPadding: Dp,
    val unitTopPadding: Dp,
    val measurementTopPadding: Dp
)

@Composable
private fun calculateMeasurementTextLayout(
    value: SensorValue,
    measurementName: String,
    columnWidth: Dp
): MeasurementTextLayout {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val valueMetrics = measureWidgetText(
        text = value.sensorValue,
        fontSize = ruuviStationFontsSizes.compact,
        fontResId = R.font.oswald_bold
    )
    val unitMetrics = if (value.unit.isNotEmpty()) {
        measureWidgetText(
            text = value.unit,
            fontSize = ruuviStationFontsSizes.petite,
            fontResId = R.font.oswald_light
        )
    } else {
        FontBitmapSize(0, 0, 0)
    }
    val measurementMetrics = measureWidgetText(
        text = measurementName,
        fontSize = ruuviStationFontsSizes.petite,
        fontResId = R.font.mulish_regular,
        maxWidth = columnWidth
    )
    val targetBaseline = maxOf(
        valueMetrics.baseline,
        unitMetrics.baseline,
        measurementMetrics.baseline
    )

    return MeasurementTextLayout(
        valueTopPadding = (targetBaseline - valueMetrics.baseline).toDp(density),
        unitTopPadding = (targetBaseline - unitMetrics.baseline).toDp(density),
        measurementTopPadding = (targetBaseline - measurementMetrics.baseline).toDp(density)
    )
}

@Composable
private fun measureWidgetText(
    text: String,
    fontSize: TextUnit,
    fontResId: Int,
    maxWidth: Dp? = null
): FontBitmapSize {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    return GlanceFontUtils.measureFontBitmap(
        context = context,
        text = text,
        fontSize = fontSize,
        fontResId = fontResId,
        maxWidth = maxWidth?.toPx(density),
        maxFontScale = DASHBOARD_SMALL_VALUE_MAX_FONT_SCALE
    )
}

private fun Int.toDp(density: Float): Dp = (this.coerceAtLeast(0) / density).dp

private fun Dp.toPx(density: Float): Int = (value * density).roundToInt()

class RefreshComplexWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        ComplexWidgetProvider.updateComplexWidget(context, appWidgetId)
    }
}

class OpenComplexWidgetSensorAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val sensorId = SimpleWidget.sensorIdFromParameters(parameters) ?: return
        val appWidgetId = SimpleWidget.appWidgetIdFromParameters(parameters)
        SensorCardActivity.createPendingIntent(context, sensorId, appWidgetId)?.send()
    }
}
