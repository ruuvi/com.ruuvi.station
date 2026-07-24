package com.ruuvi.station.widgets.ui.simpleWidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.unit.ColorProvider as GlanceColorProvider
import com.ruuvi.station.dashboard.ui.DashboardActivity
import com.ruuvi.station.tagdetails.ui.SensorCardActivity
import com.ruuvi.station.R
import com.ruuvi.station.units.domain.aqi.AQI
import com.ruuvi.station.widgets.data.WidgetType
import com.ruuvi.station.widgets.ui.glance.CustomFontText
import com.ruuvi.station.widgets.ui.glance.GlanceColors
import com.ruuvi.station.widgets.ui.glance.RefreshButton
import com.ruuvi.station.widgets.ui.glance.WidgetRefreshButtonDefaults
import com.ruuvi.station.widgets.ui.glance.getZoomFactor
import com.ruuvi.station.widgets.ui.glance.toWidgetSp

object SimpleWidgetGlanceWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
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
                sensorId = sensorId,
                displayName = displayName?.takeIf { it.isNotBlank() } ?: "-",
                sensorValue = sensorValue?.takeIf { it.isNotBlank() } ?: "-",
                unit = unit.orEmpty(),
                measurementName = measurementName?.takeIf { it.isNotBlank() } ?: "-",
                updated = updated?.takeIf { it.isNotBlank() } ?: "-",
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
        val appWidgetId = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        }.getOrNull() ?: return
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
        val appWidgetId = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        }.getOrNull() ?: return
        SensorCardActivity.createWidgetPendingIntent(context, sensorId, appWidgetId)?.send()
    }
}

@Composable
private fun SimpleWidgetContent(
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
            SimpleWidget.openSensorActionParameters(sensorId)
        )
    } else {
        actionStartActivity<DashboardActivity>()
    }

    val context = LocalContext.current
    val zoomFactor = getZoomFactor(context)
    val size = LocalSize.current
    val widgetWidth = validDimensionOrFallback(size.width, 110.dp)
    val widgetHeight = validDimensionOrFallback(size.height, 45.dp)
    val config = SimpleWidgetLayoutConfig.fromHeight(
        height = widgetHeight,
        zoomFactor = zoomFactor
    )

    val availableWidth = positiveDp(widgetWidth - (config.horizontalPadding * 2))
    val timestampMaxWidth = calculateTimestampMaxWidth(
        widgetWidth = widgetWidth,
        contentStartPadding = config.horizontalPadding,
        refreshVisualEndInset = WidgetRefreshButtonDefaults.visualEndInset,
        inlineGap = config.inlineSpacing
    )

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceColors.background)
            .clickable(openAction)
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(
                    horizontal = config.horizontalPadding,
                    vertical = config.verticalPadding
                )
        ) {
            CustomFontText(
                text = displayName,
                fontSize = config.displayNameFontSize.toWidgetSp(context),
                colorProvider = GlanceColors.widgetSensorName,
                fontResId = R.font.mulish_bold,
                maxWidth = availableWidth
            )

            if (measurementType == WidgetType.AIR_QUALITY) {
                GlanceAQIDisplay(
                    sensorValue = sensorValue,
                    measurementName = measurementName,
                    config = config,
                    availableWidth = availableWidth
                )
            } else {
                GlanceMeasurementDisplay(
                    sensorValue = sensorValue,
                    unit = unit,
                    measurementName = measurementName,
                    config = config,
                    availableWidth = availableWidth
                )
            }

            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.BottomStart
            ) {
                CustomFontText(
                    text = updated,
                    fontSize = config.secondaryFontSize.toWidgetSp(context),
                    colorProvider = GlanceColors.widgetSensorName,
                    fontResId = R.font.mulish_regular,
                    maxWidth = timestampMaxWidth
                )
            }
        }

        RefreshButton(
            action = actionRunCallback<RefreshSimpleWidgetAction>()
        )
    }
}

@Composable
private fun GlanceMeasurementDisplay(
    sensorValue: String,
    unit: String,
    measurementName: String,
    config: SimpleWidgetLayoutConfig,
    availableWidth: Dp
) {
    val context = LocalContext.current
    val rowWidth = positiveDp(availableWidth - config.inlineSpacing)
    val valueWidthFraction = if (unit.isBlank()) 1f else 0.7f
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        CustomFontText(
            text = sensorValue,
            fontSize = config.valueFontSize.toWidgetSp(context),
            colorProvider = GlanceColors.valueColor,
            fontResId = R.font.oswald_bold,
            maxWidth = positiveDp(rowWidth * valueWidthFraction)
        )

        if (unit.isNotBlank()) {
            Spacer(modifier = GlanceModifier.width(config.inlineSpacing))
            CustomFontText(
                text = unit,
                fontSize = config.secondaryFontSize.toWidgetSp(context),
                colorProvider = GlanceColors.widgetSensorName,
                fontResId = R.font.oswald_light,
                modifier = GlanceModifier.padding(top = config.unitPadding),
                maxWidth = positiveDp(rowWidth * 0.3f)
            )
        }
    }

    CustomFontText(
        text = measurementName,
        fontSize = config.secondaryFontSize.toWidgetSp(context),
        colorProvider = GlanceColors.widgetSensorName,
        fontResId = R.font.mulish_regular,
        maxWidth = availableWidth
    )
}

@Composable
private fun GlanceAQIDisplay(
    sensorValue: String,
    measurementName: String,
    config: SimpleWidgetLayoutConfig,
    availableWidth: Dp
) {
    val context = LocalContext.current
    val aqiText = sensorValue.substringBefore("/")
    val aqiValue = aqiText.toDoubleOrNull()?.takeIf { it.isFinite() }
    val aqiColor = aqiValue?.let { AQI.CalculatedAQI(it).color } ?: Color.Gray
    val aqiColorProvider = ColorProvider(day = aqiColor, night = aqiColor)

    val progressBarWidth = positiveDp(
        availableWidth -
            WidgetRefreshButtonDefaults.touchTargetSize -
            (config.inlineSpacing * 2)
    )

    Column {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CustomFontText(
                text = aqiText,
                fontSize = config.valueFontSize.toWidgetSp(context),
                colorProvider = GlanceColors.valueColor,
                fontResId = R.font.oswald_bold,
                maxWidth = positiveDp(availableWidth * 0.55f)
            )

            Spacer(modifier = GlanceModifier.width(config.inlineSpacing))

            Box(modifier = GlanceModifier.height(config.aqiBoxHeight)) {
                CustomFontText(
                    text = "/100",
                    fontSize = config.secondaryFontSize.toWidgetSp(context),
                    colorProvider = GlanceColors.valueColor,
                    fontResId = R.font.oswald_light,
                    modifier = GlanceModifier.padding(top = config.unitPadding),
                    maxWidth = positiveDp(availableWidth * 0.25f)
                )

                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomStart
                ) {
                    CustomFontText(
                        text = measurementName,
                        fontSize = config.secondaryFontSize.toWidgetSp(context),
                        colorProvider = GlanceColors.widgetSensorName,
                        fontResId = R.font.mulish_regular,
                        modifier = GlanceModifier.padding(bottom = config.aqiMeasurementPadding),
                        maxWidth = positiveDp(availableWidth * 0.4f)
                    )
                }
            }
        }

        GlanceProgressBarWithDot(
            progress = (aqiValue?.toFloat() ?: 0f) / 100f,
            activeColor = aqiColorProvider,
            backgroundColor = ColorProvider(
                day = aqiColor.copy(alpha = 0.2f),
                night = aqiColor.copy(alpha = 0.2f)
            ),
            modifier = GlanceModifier.padding(start = 1.dp, bottom = 2.dp),
            totalWidth = progressBarWidth,
            config = config
        )
    }
}

@Composable
private fun GlanceProgressBarWithDot(
    progress: Float,
    activeColor: GlanceColorProvider,
    backgroundColor: GlanceColorProvider,
    modifier: GlanceModifier = GlanceModifier,
    totalWidth: androidx.compose.ui.unit.Dp = 100.dp,
    config: SimpleWidgetLayoutConfig
) {
    val geometry = calculateAqiProgressGeometry(
        requestedWidth = totalWidth,
        glowSize = config.glowSize,
        progress = progress
    )

    Box(
        modifier = modifier
            .width(geometry.totalWidth)
            .height(config.glowSize),
        contentAlignment = Alignment.CenterStart
    ) {
        // Progress track
        Row(
            modifier = GlanceModifier.fillMaxWidth().height(config.barHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .width(geometry.activeWidth)
                    .fillMaxHeight()
                    .background(activeColor)
            ) {}
            Box(
                modifier = GlanceModifier
                    .width(geometry.inactiveWidth)
                    .fillMaxHeight()
                    .background(backgroundColor)
            ) {}
        }

        // Indicator Dot and Glow
        Row(
            modifier = GlanceModifier.fillMaxWidth().height(config.glowSize),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = GlanceModifier.width(geometry.dotOffset))

            Box(
                modifier = GlanceModifier.size(config.glowSize),
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
                    modifier = GlanceModifier.size(config.dotSize),
                    colorFilter = ColorFilter.tint(activeColor)
                )
            }
        }
    }
}

internal data class AqiProgressGeometry(
    val totalWidth: Dp,
    val activeWidth: Dp,
    val inactiveWidth: Dp,
    val dotOffset: Dp
)

internal fun calculateAqiProgressGeometry(
    requestedWidth: Dp,
    glowSize: Dp,
    progress: Float
): AqiProgressGeometry {
    val safeGlow = glowSize.value.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
    val requested = requestedWidth.value.takeIf { it.isFinite() } ?: safeGlow
    val total = requested.coerceIn(safeGlow, maxOf(100f, safeGlow))
    val normalizedProgress = progress.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
    val active = total * normalizedProgress
    val maxDotOffset = (total - safeGlow).coerceAtLeast(0f)
    val dotOffset = (active - (safeGlow / 2f)).coerceIn(0f, maxDotOffset)

    return AqiProgressGeometry(
        totalWidth = total.dp,
        activeWidth = active.dp,
        inactiveWidth = (total - active).dp,
        dotOffset = dotOffset.dp
    )
}

private fun positiveDp(value: Dp): Dp = value.value.coerceAtLeast(1f).dp

internal fun calculateTimestampMaxWidth(
    widgetWidth: Dp,
    contentStartPadding: Dp,
    refreshVisualEndInset: Dp,
    inlineGap: Dp
): Dp = positiveDp(
    widgetWidth - contentStartPadding - refreshVisualEndInset - inlineGap
)

private fun validDimensionOrFallback(value: Dp, fallback: Dp): Dp =
    value.value.takeIf { it.isFinite() && it > 0f }?.dp ?: fallback
