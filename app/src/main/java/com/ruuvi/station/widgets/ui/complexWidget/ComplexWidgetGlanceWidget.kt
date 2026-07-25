package com.ruuvi.station.widgets.ui.complexWidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.theme.ruuviStationFontsSizes
import com.ruuvi.station.dashboard.ui.DashboardActivity
import com.ruuvi.station.tagdetails.ui.SensorCardActivity
import com.ruuvi.station.widgets.data.ComplexWidgetData
import com.ruuvi.station.widgets.data.SensorValue
import com.ruuvi.station.widgets.ui.glance.GlanceColors
import com.ruuvi.station.widgets.ui.glance.GlanceFontUtils
import com.ruuvi.station.widgets.ui.glance.RefreshButton
import com.ruuvi.station.widgets.ui.glance.WidgetContentPaddingDefaults
import com.ruuvi.station.widgets.ui.glance.WidgetRefreshButtonDefaults
import com.ruuvi.station.widgets.ui.glance.getEffectiveFontScale
import com.ruuvi.station.widgets.ui.glance.getZoomFactor
import com.ruuvi.station.widgets.ui.glance.toWidgetSp
import com.ruuvi.station.widgets.ui.simpleWidget.SimpleWidget

object ComplexWidgetGlanceWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val dataJson = currentState<Preferences>()[ComplexWidgetPrefKeys.data]
            val sensors = dataJson?.takeIf { it.isNotEmpty() }?.let {
                runCatching {
                    val type = object : TypeToken<List<ComplexWidgetData>>() {}.type
                    Gson().fromJson<List<ComplexWidgetData>>(it, type)
                }.getOrDefault(emptyList())
            }.orEmpty()

            ComplexWidgetContent(sensors)
        }
    }
}

@Composable
private fun ComplexWidgetContent(sensors: List<ComplexWidgetData>) {
    val context = LocalContext.current
    val zoomFactor = getZoomFactor(context)
    val measurementDescriptionFontScale = getEffectiveFontScale(
        context = context,
        referenceFontSizeSp = ruuviStationFontsSizes.petite.toWidgetSp(context).value
    )
    val layout = complexWidgetLayoutConfig(
        width = LocalSize.current.width,
        fontScale = measurementDescriptionFontScale,
        maximumColumns = sensors.maxOfOrNull { it.sensorValues.size } ?: 1
    )

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceColors.background)
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(all = layout.outerPadding / zoomFactor)
        ) {
            if (sensors.isEmpty()) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(actionStartActivity<DashboardActivity>()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = context.getString(R.string.widgets_loading),
                        modifier = GlanceModifier.fillMaxWidth(),
                        style = TextStyle(
                            color = GlanceColors.widgetSensorName,
                            fontSize = ruuviStationFontsSizes.normal.toWidgetSp(context),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 1
                    )
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    sensors.forEach { sensor ->
                        val openAction = actionRunCallback<OpenComplexWidgetSensorAction>(
                            SimpleWidget.openSensorActionParameters(sensor.sensorId)
                        )

                        item {
                            SensorHeader(sensor, openAction, layout, zoomFactor)
                            Spacer(
                                modifier = GlanceModifier.height(
                                    layout.headerBottomSpacing / zoomFactor
                                )
                            )
                        }

                        items(
                            sensor.sensorValues.chunked(layout.measurementColumns)
                        ) { rowValues ->
                            MeasurementRow(
                                rowValues = rowValues,
                                columnCount = layout.measurementColumns,
                                action = openAction,
                                layout = layout,
                                zoomFactor = zoomFactor
                            )
                        }

                        item {
                            SensorFooter(sensor, openAction, layout, zoomFactor)
                        }
                    }
                }
            }
        }

        RefreshButton(
            backingColor = GlanceColors.background,
            action = actionRunCallback<RefreshComplexWidgetAction>()
        )
    }
}

@Composable
private fun SensorHeader(
    sensor: ComplexWidgetData,
    action: Action,
    layout: ComplexWidgetLayoutConfig,
    zoomFactor: Float
) {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(horizontal = layout.horizontalPadding / zoomFactor)
            .padding(top = layout.headerTopPadding / zoomFactor)
            .clickable(action),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = sensor.displayName,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(
                color = GlanceColors.widgetSensorName,
                fontSize = ruuviStationFontsSizes.normal.toWidgetSp(context),
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun MeasurementRow(
    rowValues: List<SensorValue>,
    columnCount: Int,
    action: Action,
    layout: ComplexWidgetLayoutConfig,
    zoomFactor: Float
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(
                horizontal = layout.horizontalPadding / zoomFactor,
                vertical = layout.rowVerticalPadding / zoomFactor
            )
            .clickable(action)
    ) {
        repeat(columnCount) { columnIndex ->
            if (columnIndex > 0) {
                Spacer(
                    modifier = GlanceModifier.width(
                        layout.measurementColumnSpacing / zoomFactor
                    )
                )
            }

            val value = rowValues.getOrNull(columnIndex)
            if (value == null) {
                Spacer(modifier = GlanceModifier.defaultWeight())
            } else {
                MeasurementItem(
                    value = value,
                    modifier = GlanceModifier.defaultWeight(),
                    layout = layout,
                    zoomFactor = zoomFactor
                )
            }
        }
    }
}

@Composable
private fun SensorFooter(
    sensor: ComplexWidgetData,
    action: Action,
    layout: ComplexWidgetLayoutConfig,
    zoomFactor: Float
) {
    val context = LocalContext.current
    val contentEndInset = (layout.outerPadding + layout.horizontalPadding) / zoomFactor
    val timestampEndReservation = calculateComplexTimestampEndReservation(
        contentEndInset = contentEndInset,
        refreshBackingEndInset = WidgetRefreshButtonDefaults.backingEndInset
    )
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(horizontal = layout.horizontalPadding / zoomFactor)
            .padding(bottom = layout.footerBottomPadding / zoomFactor)
            .clickable(action)
    ) {
        Spacer(modifier = GlanceModifier.height(layout.footerTopSpacing / zoomFactor))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = sensor.updated.orEmpty(),
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(
                    color = GlanceColors.widgetSensorName,
                    fontSize = ruuviStationFontsSizes.tiny2.toWidgetSp(context)
                ),
                maxLines = 1
            )
            Spacer(
                modifier = GlanceModifier.width(timestampEndReservation)
            )
        }
    }
}

@Composable
private fun MeasurementItem(
    value: SensorValue,
    modifier: GlanceModifier,
    layout: ComplexWidgetLayoutConfig,
    zoomFactor: Float
) {
    val context = LocalContext.current
    val valueFontSize = ruuviStationFontsSizes.compact.toWidgetSp(context)
    val secondaryFontSize = ruuviStationFontsSizes.petite.toWidgetSp(context)
    val valueBaselineOffset = GlanceFontUtils.measureSystemFontBaselineOffset(
        context = context,
        fontSize = valueFontSize,
        bold = true
    )
    val unitTopPadding = calculateBaselineTopPadding(
        referenceBaselineOffset = valueBaselineOffset,
        textBaselineOffset = GlanceFontUtils.measureSystemFontBaselineOffset(
            context = context,
            fontSize = secondaryFontSize,
            bold = true
        )
    )
    val descriptionTopPadding = calculateBaselineTopPadding(
        referenceBaselineOffset = valueBaselineOffset,
        textBaselineOffset = GlanceFontUtils.measureSystemFontBaselineOffset(
            context = context,
            fontSize = secondaryFontSize,
            bold = false
        )
    )
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = value.sensorValue,
            style = TextStyle(
                color = GlanceColors.valueColor,
                fontSize = valueFontSize,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )

        if (value.unit.isNotBlank()) {
            Spacer(
                modifier = GlanceModifier.width(
                    layout.measurementUnitSpacing / zoomFactor
                )
            )
            Text(
                text = value.unit,
                modifier = GlanceModifier.padding(top = unitTopPadding),
                style = TextStyle(
                    color = GlanceColors.widgetSensorName,
                    fontSize = secondaryFontSize,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
        }

        Spacer(
            modifier = GlanceModifier.width(
                layout.measurementDescriptionSpacing / zoomFactor
            )
        )
        Text(
            text = context.getString(value.type.unitType.measurementName),
            modifier = GlanceModifier
                .defaultWeight()
                .padding(top = descriptionTopPadding),
            style = TextStyle(
                color = GlanceColors.widgetSensorName,
                fontSize = secondaryFontSize
            ),
            maxLines = 1
        )
    }
}

internal fun calculateBaselineTopPadding(
    referenceBaselineOffset: Dp,
    textBaselineOffset: Dp
): Dp = (
    referenceBaselineOffset.value - textBaselineOffset.value
).coerceAtLeast(0f).dp

private val COMPLEX_WIDGET_OUTER_PADDING = WidgetContentPaddingDefaults.outerPadding
private val COMPLEX_WIDGET_HORIZONTAL_PADDING = WidgetContentPaddingDefaults.innerPadding
private val COMPLEX_WIDGET_COLUMN_SPACING = 12.dp
private val COMPLEX_WIDGET_MEASUREMENT_UNIT_SPACING = 2.dp
private val COMPLEX_WIDGET_MEASUREMENT_DESCRIPTION_SPACING = 4.dp
private val COMPLEX_WIDGET_TWO_COLUMN_BASE_WIDTH = 180.dp
private val COMPLEX_WIDGET_THREE_COLUMN_BASE_WIDTH = 316.dp
private const val COMPLEX_WIDGET_MAX_COLUMNS = 6

internal fun complexWidgetMeasurementColumns(
    width: Dp,
    fontScale: Float = 1f,
    maximumColumns: Int = COMPLEX_WIDGET_MAX_COLUMNS
): Int {
    if (!width.value.isFinite()) return 1

    val safeMaximumColumns = maximumColumns.coerceIn(1, COMPLEX_WIDGET_MAX_COLUMNS)
    for (columnCount in safeMaximumColumns downTo 2) {
        if (width >= complexWidgetRequiredWidth(columnCount, fontScale)) {
            return columnCount
        }
    }
    return 1
}

internal fun complexWidgetRequiredWidth(
    columnCount: Int,
    fontScale: Float
): Dp {
    require(columnCount in 2..COMPLEX_WIDGET_MAX_COLUMNS)

    val normalizedFontScale =
        fontScale.takeIf { it.isFinite() && it > 0f } ?: 1f
    // Padding and gaps are fixed geometry. Only the text budget grows with the
    // system font setting, so larger text reduces columns without inflating gaps.
    val fixedWidth = complexWidgetFixedWidth(columnCount)
    val fontSensitiveWidth = if (columnCount == 2) {
        COMPLEX_WIDGET_TWO_COLUMN_BASE_WIDTH.value - fixedWidth
    } else {
        val threeColumnTextWidth =
            COMPLEX_WIDGET_THREE_COLUMN_BASE_WIDTH.value - complexWidgetFixedWidth(3)
        (threeColumnTextWidth / 3f) * columnCount
    }

    return (fixedWidth + (fontSensitiveWidth * normalizedFontScale)).dp
}

private fun complexWidgetFixedWidth(columnCount: Int): Float =
    (COMPLEX_WIDGET_OUTER_PADDING.value * 2f) +
        (COMPLEX_WIDGET_HORIZONTAL_PADDING.value * 2f) +
        (COMPLEX_WIDGET_COLUMN_SPACING.value * (columnCount - 1)) +
        (
            COMPLEX_WIDGET_MEASUREMENT_UNIT_SPACING.value +
                COMPLEX_WIDGET_MEASUREMENT_DESCRIPTION_SPACING.value
            ) * columnCount

internal fun calculateComplexTimestampEndReservation(
    contentEndInset: Dp,
    refreshBackingEndInset: Dp
): Dp = (
    refreshBackingEndInset.value - contentEndInset.value
).coerceAtLeast(0f).dp

internal data class ComplexWidgetLayoutConfig(
    val measurementColumns: Int,
    val outerPadding: Dp,
    val horizontalPadding: Dp,
    val headerTopPadding: Dp,
    val headerBottomSpacing: Dp,
    val rowVerticalPadding: Dp,
    val footerTopSpacing: Dp,
    val footerBottomPadding: Dp,
    val measurementColumnSpacing: Dp,
    val measurementUnitSpacing: Dp,
    val measurementDescriptionSpacing: Dp
)

internal fun complexWidgetLayoutConfig(
    width: Dp,
    fontScale: Float = 1f,
    maximumColumns: Int = COMPLEX_WIDGET_MAX_COLUMNS
) = ComplexWidgetLayoutConfig(
    measurementColumns = complexWidgetMeasurementColumns(width, fontScale, maximumColumns),
    outerPadding = COMPLEX_WIDGET_OUTER_PADDING,
    horizontalPadding = COMPLEX_WIDGET_HORIZONTAL_PADDING,
    headerTopPadding = 12.dp,
    headerBottomSpacing = 12.dp,
    rowVerticalPadding = 3.dp,
    footerTopSpacing = 8.dp,
    footerBottomPadding = 12.dp,
    measurementColumnSpacing = COMPLEX_WIDGET_COLUMN_SPACING,
    measurementUnitSpacing = COMPLEX_WIDGET_MEASUREMENT_UNIT_SPACING,
    measurementDescriptionSpacing = COMPLEX_WIDGET_MEASUREMENT_DESCRIPTION_SPACING
)

class RefreshComplexWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val appWidgetId = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        }.getOrNull() ?: return
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
        val appWidgetId = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        }.getOrNull() ?: return
        SensorCardActivity.createWidgetPendingIntent(context, sensorId, appWidgetId)?.send()
    }
}
