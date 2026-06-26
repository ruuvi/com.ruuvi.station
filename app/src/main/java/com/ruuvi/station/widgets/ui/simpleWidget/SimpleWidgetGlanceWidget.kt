package com.ruuvi.station.widgets.ui.simpleWidget

import android.content.Context
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
import androidx.glance.LocalSize
import androidx.glance.LocalContext
import com.ruuvi.station.R
import androidx.datastore.preferences.core.Preferences
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider
import com.ruuvi.station.units.domain.aqi.AQI
import com.ruuvi.station.widgets.data.WidgetType
import com.ruuvi.station.dashboard.ui.DashboardActivity
import com.ruuvi.station.tagdetails.ui.SensorCardActivity
import com.ruuvi.station.widgets.ui.glance.GlanceColors
import com.ruuvi.station.widgets.ui.glance.CustomFontText
import com.ruuvi.station.widgets.ui.glance.RefreshButton
import com.ruuvi.station.widgets.ui.WidgetScreenSizeCategory
import com.ruuvi.station.widgets.ui.resolveWidgetScreenSizeCategory

object SimpleWidgetGlanceWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Exact

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

    val context = LocalContext.current
    val screenSizeCategory = resolveWidgetScreenSizeCategory(context)
    val size = LocalSize.current
    val height = size.height
    val fallbackHeight = when (screenSizeCategory) {
        WidgetScreenSizeCategory.SMALL -> 65.dp
        WidgetScreenSizeCategory.MEDIUM -> 80.dp
        WidgetScreenSizeCategory.BIG -> 90.dp
    }
    val config = SimpleWidgetLayoutConfig.fromHeight(
        if (height < 65.dp) fallbackHeight else height
    )
    
    val availableWidth = size.width - 12.dp

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceColors.background)
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 12.dp)
            .clickable(openAction)
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            CustomFontText(
                text = displayName,
                fontSize = config.displayNameFontSize,
                colorProvider = GlanceColors.widgetSensorName,
                fontResId = R.font.mulish_bold,
                maxWidth = availableWidth
            )

            if (measurementType == WidgetType.AIR_QUALITY) {
                GlanceAQIDisplay(
                    sensorValue = sensorValue,
                    measurementName = measurementName,
                    config = config
                )
            } else {
                GlanceMeasurementDisplay(
                    sensorValue = sensorValue,
                    unit = unit,
                    measurementName = measurementName,
                    config = config
                )
            }

            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.BottomStart
            ) {
                CustomFontText(
                    text = updated,
                    fontSize = config.secondaryFontSize,
                    colorProvider = GlanceColors.widgetSensorName,
                    fontResId = R.font.mulish_regular
                )
            }
        }
    }
    RefreshButton(
        size = config.refreshButtonSize,
        iconSize = config.refreshIconSize,
        contentAlignment = Alignment.BottomEnd,
        action = actionRunCallback<RefreshSimpleWidgetAction>()
    )
}

@Composable
private fun GlanceMeasurementDisplay(
    sensorValue: String,
    unit: String,
    measurementName: String,
    config: SimpleWidgetLayoutConfig
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        CustomFontText(
            text = sensorValue,
            fontSize = config.valueFontSize,
            colorProvider = GlanceColors.valueColor,
            fontResId = R.font.oswald_bold
        )

        Spacer(modifier = GlanceModifier.width(2.dp))

        CustomFontText(
            text = unit,
            fontSize = config.secondaryFontSize,
            colorProvider = GlanceColors.widgetSensorName,
            fontResId = R.font.oswald_light,
            modifier = GlanceModifier.padding(top = config.unitPadding)
        )
    }

    CustomFontText(
        text = measurementName,
        fontSize = config.secondaryFontSize,
        colorProvider = GlanceColors.widgetSensorName,
        fontResId = R.font.mulish_regular
    )
}

@Composable
private fun GlanceAQIDisplay(
    sensorValue: String,
    measurementName: String,
    config: SimpleWidgetLayoutConfig
) {
    val aqiText = sensorValue.substringBefore("/")
    val aqiValue = aqiText.toDoubleOrNull()
    val aqiColor = aqiValue?.let { AQI.CalculatedAQI(it).color } ?: Color.Gray
    val aqiColorProvider = ColorProvider(day = aqiColor, night = aqiColor)

    val widgetWidth = LocalSize.current.width
    val availableWidth = widgetWidth - (config.refreshButtonSize + 12.dp) // Total width - (start padding 12 + button width 40)
    val progressBarWidth = if (availableWidth > 100.dp) 100.dp else availableWidth - 10.dp

    Column {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CustomFontText(
                text = aqiText,
                fontSize = config.valueFontSize,
                colorProvider = GlanceColors.valueColor,
                fontResId = R.font.oswald_bold
            )

            Spacer(modifier = GlanceModifier.width(2.dp))

            Box(modifier = GlanceModifier.height(config.aqiBoxHeight)) {
                CustomFontText(
                    text = "/100",
                    fontSize = config.secondaryFontSize,
                    colorProvider = GlanceColors.valueColor,
                    fontResId = R.font.oswald_light,
                    modifier = GlanceModifier.padding(top = config.unitPadding)
                )

                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomStart
                ) {
                    CustomFontText(
                        text = measurementName,
                        fontSize = config.secondaryFontSize,
                        colorProvider = GlanceColors.widgetSensorName,
                        fontResId = R.font.mulish_regular,
                        modifier = GlanceModifier.padding(bottom = config.aqiMeasurementPadding)
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
    activeColor: ColorProvider,
    backgroundColor: ColorProvider,
    modifier: GlanceModifier = GlanceModifier,
    totalWidth: androidx.compose.ui.unit.Dp = 100.dp,
    config: SimpleWidgetLayoutConfig
) {
    val safeProgress = progress.coerceIn(0f, 1f)
    val progressPosition = totalWidth * safeProgress

    Box(
        modifier = modifier
            .width(totalWidth)
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

        // Indicator Dot and Glow
        val dotOffset = (progressPosition - (config.glowSize / 2)).coerceIn(0.dp, totalWidth - config.glowSize)
        Row(
            modifier = GlanceModifier.fillMaxWidth().height(config.glowSize),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = GlanceModifier.width(dotOffset))

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
