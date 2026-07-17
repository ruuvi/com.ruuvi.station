package com.ruuvi.station.widgets.ui.simpleWidget

import android.content.Context
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.PreviewSizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.SizeMode
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
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.size
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.ruuvi.station.R
import androidx.datastore.preferences.core.Preferences
import androidx.glance.color.ColorProvider as GlanceColorProvider
import androidx.glance.unit.ColorProvider
import com.ruuvi.station.units.domain.aqi.AQI
import com.ruuvi.station.widgets.data.WidgetType
import com.ruuvi.station.dashboard.ui.DashboardActivity
import com.ruuvi.station.tagdetails.ui.SensorCardActivity
import com.ruuvi.station.widgets.ui.glance.GlanceColors
import com.ruuvi.station.widgets.ui.glance.RefreshButton
import com.ruuvi.station.app.ui.theme.ruuviStationFontsSizes

// System-font experiment: Oswald's condensed geometric look is approximated with the
// "sans-serif-condensed" system family (a real Typeface, not an app font resource), so it
// renders natively through Glance/RemoteViews instead of GlanceFontUtils bitmaps.
private val CONDENSED_FONT_FAMILY = FontFamily("sans-serif-condensed")

private val SIMPLE_WIDGET_HORIZONTAL_CONTENT_PADDING = 12.dp
private val SIMPLE_WIDGET_DEFAULT_VERTICAL_CONTENT_PADDING = 8.dp
private val SIMPLE_WIDGET_COMPACT_VERTICAL_CONTENT_PADDING = 4.dp
private val SIMPLE_WIDGET_COMPACT_HEIGHT = 50.dp
private val SIMPLE_WIDGET_MIN_INTER_ITEM_GAP = 2.dp

// Native Text can't be measured from Kotlin the way the old bitmap text could, so a tall
// name/value block at a large system font size has nowhere left to shrink on its own. Rather than
// let that silently clip whatever comes last in the Column, the timestamp is rendered as a
// separate overlay anchored to the widget's bottom edge (see SimpleWidgetContent) so it can never
// be clipped away by Column overflow - it's the one thing here users actually rely on to know the
// data isn't stale.
private val TIMESTAMP_RESERVED_HEIGHT = 16.dp

// Glance applies TextStyle.fontSize via RemoteViews' COMPLEX_UNIT_SP, so the host re-multiplies
// our value by its own current fontScale at apply-time - we can't intercept or measure that the
// way GlanceFontUtils measured a Paint. What we CAN do is pre-divide: if the system fontScale
// exceeds maxFontScale, shrink the value we hand to Text so the host's multiplication lands back
// at baseFontSize * maxFontScale instead of baseFontSize * fontScale. Below the cap this is a
// no-op, so normal accessibility scaling still works exactly as intended.
private const val BIG_VALUE_MAX_FONT_SCALE = 1.5f
private const val SUPPORTING_TEXT_MAX_FONT_SCALE = 1.5f
private const val SIDE_TEXT_MAX_FONT_SCALE = 1.2f

@Composable
private fun cappedFontSize(baseFontSize: TextUnit, maxFontScale: Float): TextUnit {
    val fontScale = LocalContext.current.resources.configuration.fontScale
    return if (fontScale <= maxFontScale) {
        baseFontSize
    } else {
        (baseFontSize.value * maxFontScale / fontScale).sp
    }
}

// Rough sp-to-dp line-height estimate (no Paint/bitmap measurement available for native Text
// anymore). Only needs to be good enough to decide whether a trailing element fits, not pixel
// precise - actual font sizes are already bounded by cappedFontSize above.
private const val LINE_HEIGHT_FACTOR = 1.3f
private fun lineHeight(fontSize: TextUnit): Dp = (fontSize.value * LINE_HEIGHT_FACTOR).dp

object SimpleWidgetGlanceWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    // Use the launcher's real granted size so layout config can scale smoothly.
    override val sizeMode = SizeMode.Exact

    // There's no real host size to read for a generated preview, so pick one representative
    // size (a typical 2x1 placement) for providePreview/LocalSize to compose against.
    override val previewSizeMode: PreviewSizeMode = SizeMode.Responsive(setOf(DpSize(160.dp, 90.dp)))

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent {
            SimpleWidgetContent(
                appWidgetId = 0,
                sensorId = null,
                displayName = context.getString(R.string.widgets_preview_sensor_name),
                sensorValue = context.getString(R.string.widgets_preview_sensor_value),
                unit = context.getString(R.string.temperature_celsius_unit),
                measurementName = context.getString(R.string.temperature),
                updated = context.getString(R.string.widgets_preview_time),
                measurementType = WidgetType.TEMPERATURE
            )
        }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)

        provideContent {
            val prefs = currentState<Preferences>()

            val sensorId = prefs[SimpleWidgetPrefKeys.sensorId]
            val displayName = prefs[SimpleWidgetPrefKeys.displayName]
            val sensorValue = prefs[SimpleWidgetPrefKeys.sensorValue]
            val unit = prefs[SimpleWidgetPrefKeys.unit]
            val measurementName = prefs[SimpleWidgetPrefKeys.measurementName]
            val updated = prefs[SimpleWidgetPrefKeys.updated]
            val measurementTypeCode = prefs[SimpleWidgetPrefKeys.measurementType]

            SimpleWidgetContent(
                appWidgetId = appWidgetId,
                sensorId = sensorId,
                displayName = displayName?.takeIf { it.isNotBlank() }
                    ?: context.getString(R.string.widgets_loading),
                sensorValue = sensorValue.orEmpty(),
                unit = unit.orEmpty(),
                measurementName = measurementName.orEmpty(),
                updated = updated.orEmpty(),
                measurementType = measurementTypeCode?.toIntOrNull()?.let { WidgetType.getByCode(it) }
            )
        }
    }
}

class RefreshSimpleWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        SimpleWidget.updateSimpleWidget(context, appWidgetId)
    }
}

class OpenSimpleWidgetSensorAction : ActionCallback {
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

@Composable
private fun SimpleWidgetContent(
    appWidgetId: Int,
    sensorId: String?,
    displayName: String,
    sensorValue: String,
    unit: String,
    measurementName: String,
    updated: String,
    measurementType: WidgetType?
) {
    val openAction = if (!sensorId.isNullOrEmpty()) {
        actionRunCallback<OpenSimpleWidgetSensorAction>(
            SimpleWidget.openSensorActionParameters(sensorId, appWidgetId)
        )
    } else {
        actionStartActivity<DashboardActivity>()
    }

    val size = LocalSize.current
    val config = SimpleWidgetLayoutConfig.fromSize(size.width, size.height)
    val contentPadding = SimpleWidgetContentPadding.fromHeight(size.height)
    val timestampFontSize = if (config.secondaryFontSize.value < ruuviStationFontsSizes.tiny2.value) {
        ruuviStationFontsSizes.tiny2
    } else {
        config.secondaryFontSize
    }

    // How much room is actually left for the value/measurement-name (or AQI/progress-bar) block
    // once the sensor name, its gap, and the reserved timestamp row are accounted for. Used to
    // decide whether the trailing, less-critical line of that block can be shown at all, rather
    // than letting it silently clip when the widget is small or the font scale is high.
    val availableContentHeight = (
        size.height - contentPadding.vertical * 2 - TIMESTAMP_RESERVED_HEIGHT -
            lineHeight(config.displayNameFontSize) - SIMPLE_WIDGET_MIN_INTER_ITEM_GAP
        ).coerceAtLeast(0.dp)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceColors.background)
            .padding(
                start = contentPadding.horizontal,
                top = contentPadding.vertical,
                bottom = contentPadding.vertical,
                end = contentPadding.horizontal
            )
            .clickable(openAction)
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(bottom = TIMESTAMP_RESERVED_HEIGHT)
        ) {
            Text(
                text = displayName,
                style = TextStyle(
                    color = GlanceColors.widgetSensorName,
                    fontSize = cappedFontSize(config.displayNameFontSize, SUPPORTING_TEXT_MAX_FONT_SCALE),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                ),
                maxLines = 1
            )

            Spacer(modifier = GlanceModifier.height(SIMPLE_WIDGET_MIN_INTER_ITEM_GAP))
            Spacer(modifier = GlanceModifier.defaultWeight())

            if (measurementType == WidgetType.AIR_QUALITY) {
                GlanceAQIDisplay(
                    sensorValue = sensorValue,
                    measurementName = measurementName,
                    config = config,
                    availableHeight = availableContentHeight
                )
            } else {
                GlanceMeasurementDisplay(
                    sensorValue = sensorValue,
                    unit = unit,
                    measurementName = measurementName,
                    config = config,
                    availableHeight = availableContentHeight
                )
            }

            Spacer(modifier = GlanceModifier.defaultWeight())
        }

        // Rendered as a sibling of the Column above, not a child of it, so it's never subject
        // to the Column's own overflow clipping - see TIMESTAMP_RESERVED_HEIGHT.
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                text = updated,
                style = TextStyle(
                    color = GlanceColors.widgetSensorName,
                    fontSize = cappedFontSize(timestampFontSize, SUPPORTING_TEXT_MAX_FONT_SCALE),
                    fontFamily = FontFamily.SansSerif
                ),
                maxLines = 1
            )
        }
    }
    RefreshButton(
        size = config.refreshButtonSize,
        iconSize = config.refreshIconSize,
        contentAlignment = Alignment.BottomEnd,
        action = actionRunCallback<RefreshSimpleWidgetAction>()
    )
}

private data class SimpleWidgetContentPadding(
    val horizontal: Dp,
    val vertical: Dp
) {
    companion object {
        fun fromHeight(height: Dp): SimpleWidgetContentPadding {
            val verticalPadding = if (height <= SIMPLE_WIDGET_COMPACT_HEIGHT) {
                SIMPLE_WIDGET_COMPACT_VERTICAL_CONTENT_PADDING
            } else {
                SIMPLE_WIDGET_DEFAULT_VERTICAL_CONTENT_PADDING
            }
            return SimpleWidgetContentPadding(
                horizontal = SIMPLE_WIDGET_HORIZONTAL_CONTENT_PADDING,
                vertical = verticalPadding
            )
        }
    }
}

@Composable
private fun GlanceMeasurementDisplay(
    sensorValue: String,
    unit: String,
    measurementName: String,
    config: SimpleWidgetLayoutConfig,
    availableHeight: Dp
) {
    val valueRowHeight = maxOf(
        lineHeight(config.valueFontSize),
        lineHeight(config.unitFontSize)
    )
    val showMeasurementName = availableHeight >= valueRowHeight + lineHeight(config.secondaryFontSize)

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = sensorValue,
            style = TextStyle(
                color = GlanceColors.valueColor,
                fontSize = cappedFontSize(config.valueFontSize, BIG_VALUE_MAX_FONT_SCALE),
                fontWeight = FontWeight.Bold,
                fontFamily = CONDENSED_FONT_FAMILY
            ),
            maxLines = 1
        )

        Spacer(modifier = GlanceModifier.width(4.dp))

        Text(
            text = unit,
            style = TextStyle(
                color = GlanceColors.widgetSensorName,
                fontSize = cappedFontSize(config.unitFontSize, SIDE_TEXT_MAX_FONT_SCALE),
                fontFamily = CONDENSED_FONT_FAMILY
            ),
            modifier = GlanceModifier.padding(bottom = 2.dp),
            maxLines = 1
        )
    }

    // Dropped cleanly instead of being silently clipped when the value row alone already
    // uses up the space available for this block (small widget, or a large font scale).
    if (showMeasurementName) {
        Text(
            text = measurementName,
            style = TextStyle(
                color = GlanceColors.widgetSensorName,
                fontSize = cappedFontSize(config.secondaryFontSize, SIDE_TEXT_MAX_FONT_SCALE),
                fontFamily = FontFamily.SansSerif
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun GlanceAQIDisplay(
    sensorValue: String,
    measurementName: String,
    config: SimpleWidgetLayoutConfig,
    availableHeight: Dp
) {
    val aqiText = sensorValue.substringBefore("/")
    val aqiValue = aqiText.toDoubleOrNull()
    val aqiColor = aqiValue?.let { AQI.CalculatedAQI(it).color } ?: Color.Gray
    val aqiColorProvider = GlanceColorProvider(day = aqiColor, night = aqiColor)

    val widgetWidth = LocalSize.current.width
    val availableWidth = widgetWidth - (config.refreshButtonSize + 12.dp)
    val progressBarWidth = if (availableWidth > 100.dp) 100.dp else availableWidth - 10.dp

    val aqiRowHeight = maxOf(
        lineHeight(config.valueFontSize),
        lineHeight(config.unitFontSize) + lineHeight(config.secondaryFontSize)
    )
    // Same reasoning as GlanceMeasurementDisplay's measurementName: drop the progress bar
    // cleanly rather than let it be silently clipped when the AQI number/label row alone
    // already fills the available space.
    val showProgressBar = availableHeight >= aqiRowHeight + config.glowSize

    Column {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = aqiText,
                style = TextStyle(
                    color = GlanceColors.valueColor,
                    fontSize = cappedFontSize(config.valueFontSize, BIG_VALUE_MAX_FONT_SCALE),
                    fontWeight = FontWeight.Bold,
                    fontFamily = CONDENSED_FONT_FAMILY
                ),
                maxLines = 1
            )

            Spacer(modifier = GlanceModifier.width(2.dp))

            Column(modifier = GlanceModifier.padding(bottom = 2.dp)) {
                Text(
                    text = "/100",
                    style = TextStyle(
                        color = GlanceColors.valueColor,
                        fontSize = cappedFontSize(config.unitFontSize, SIDE_TEXT_MAX_FONT_SCALE),
                        fontFamily = CONDENSED_FONT_FAMILY
                    ),
                    maxLines = 1
                )

                Text(
                    text = measurementName,
                    style = TextStyle(
                        color = GlanceColors.widgetSensorName,
                        fontSize = cappedFontSize(config.secondaryFontSize, SIDE_TEXT_MAX_FONT_SCALE),
                        fontFamily = FontFamily.SansSerif
                    ),
                    maxLines = 1
                )
            }
        }

        if (showProgressBar) {
            GlanceProgressBarWithDot(
                progress = (aqiValue?.toFloat() ?: 0f) / 100f,
                activeColor = aqiColorProvider,
                // Match the dashboard progress track rather than fading the active color.
                backgroundColor = GlanceColorProvider(
                    day = Color.Black.copy(alpha = 0.8f),
                    night = Color.Black.copy(alpha = 0.8f)
                ),
                modifier = GlanceModifier.padding(start = 1.dp, top = 4.dp, bottom = 2.dp),
                totalWidth = progressBarWidth,
                progressHeight = config.glowSize,
                config = config
            )
        }
    }
}

@Composable
private fun GlanceProgressBarWithDot(
    progress: Float,
    activeColor: ColorProvider,
    backgroundColor: ColorProvider,
    modifier: GlanceModifier = GlanceModifier,
    totalWidth: Dp = 100.dp,
    progressHeight: Dp = config.glowSize,
    config: SimpleWidgetLayoutConfig
) {
    val safeProgress = progress.coerceIn(0f, 1f)
    val progressPosition = totalWidth * safeProgress
    val indicatorSize = minOf(config.glowSize, progressHeight)
    val dotSize = minOf(config.dotSize, indicatorSize)
    val trackHeight = minOf(config.barHeight, progressHeight)

    Box(
        modifier = modifier
            .width(totalWidth)
            .height(progressHeight),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().height(trackHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .width(progressPosition)
                    .fillMaxHeight()
                    .background(activeColor)
            ) {}
            Box(
                modifier = GlanceModifier
                    .width(totalWidth - progressPosition)
                    .fillMaxHeight()
                    .background(backgroundColor)
            ) {}
        }

        val maxDotOffset = maxOf(0.dp, totalWidth - indicatorSize)
        val dotOffset = (progressPosition - (indicatorSize / 2)).coerceIn(0.dp, maxDotOffset)
        Row(
            modifier = GlanceModifier.fillMaxWidth().height(progressHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = GlanceModifier.width(dotOffset))

            Box(
                modifier = GlanceModifier.size(indicatorSize),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_glow_circle),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxSize(),
                    colorFilter = ColorFilter.tint(activeColor)
                )
                Image(
                    provider = ImageProvider(R.drawable.ic_white_circle),
                    contentDescription = null,
                    modifier = GlanceModifier.size(dotSize),
                    colorFilter = ColorFilter.tint(activeColor)
                )
            }
        }
    }
}
