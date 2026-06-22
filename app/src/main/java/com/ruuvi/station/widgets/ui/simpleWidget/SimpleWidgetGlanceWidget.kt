package com.ruuvi.station.widgets.ui.simpleWidget

import android.content.Context
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.stringPreferencesKey
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
import androidx.glance.LocalSize
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
import com.ruuvi.station.app.ui.theme.ruuviStationFonts
import com.ruuvi.station.app.ui.theme.ruuviStationFontsSizes

object SimpleWidgetGlanceWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Exact

    private val SensorIdKey = stringPreferencesKey("sensor_id")
    private val DisplayNameKey = stringPreferencesKey("display_name")
    private val SensorValueKey = stringPreferencesKey("sensor_value")
    private val UnitKey = stringPreferencesKey("unit")
    private val MeasurementNameKey = stringPreferencesKey("measurement_name")
    private val UpdatedKey = stringPreferencesKey("updated")
    private val MeasurementTypeKey = stringPreferencesKey("measurement_type")

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)

        provideContent {
            val prefs = currentState<Preferences>()

            val sensorId = prefs[SensorIdKey]
            val displayName = prefs[DisplayNameKey]
            val sensorValue = prefs[SensorValueKey]
            val unit = prefs[UnitKey]
            val measurementName = prefs[MeasurementNameKey]
            val updated = prefs[UpdatedKey]
            val measurementTypeCode = prefs[MeasurementTypeKey]

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

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceColors.background)
            .padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
            .clickable(openAction)
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            CustomFontText(
                text = displayName,
                fontSize = ruuviStationFontsSizes.normal,
                colorProvider = GlanceColors.widgetSensorName,
                fontFamily = ruuviStationFonts.mulishBold
            )

            if (measurementType == WidgetType.AIR_QUALITY) {
                GlanceAQIDisplay(
                    sensorValue = sensorValue,
                    measurementName = measurementName
                )
            } else {
                GlanceMeasurementDisplay(
                    sensorValue = sensorValue,
                    unit = unit,
                    measurementName = measurementName
                )
            }

            CustomFontText(
                text = updated,
                fontSize = ruuviStationFontsSizes.tiny,
                colorProvider = GlanceColors.widgetSensorName,
                fontFamily = ruuviStationFonts.mulishRegular
            )
        }

        RefreshButton()
    }
}

@Composable
private fun GlanceMeasurementDisplay(
    sensorValue: String,
    unit: String,
    measurementName: String
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        CustomFontText(
            text = sensorValue,
            fontSize = ruuviStationFontsSizes.bigger,
            colorProvider = GlanceColors.valueColor,
            fontFamily = ruuviStationFonts.oswaldBold
        )

        Spacer(modifier = GlanceModifier.width(2.dp))

        CustomFontText(
            text = unit,
            fontSize = ruuviStationFontsSizes.tiny,
            colorProvider = GlanceColors.widgetSensorName,
            fontFamily = ruuviStationFonts.oswaldLight,
            modifier = GlanceModifier.padding(top = 4.dp)
        )
    }

    CustomFontText(
        text = measurementName,
        fontSize = ruuviStationFontsSizes.tiny,
        colorProvider = GlanceColors.widgetSensorName,
        fontFamily = ruuviStationFonts.mulishRegular
    )
}

@Composable
private fun GlanceAQIDisplay(
    sensorValue: String,
    measurementName: String
) {
    val aqiText = sensorValue.substringBefore("/", missingDelimiterValue = "-")
    val aqiValue = aqiText.toDoubleOrNull()
    val aqiColor = aqiValue?.let { AQI.CalculatedAQI(it).color } ?: Color.Gray
    val aqiColorProvider = ColorProvider(day = aqiColor, night = aqiColor)

    val widgetWidth = LocalSize.current.width
    val availableWidth = widgetWidth - 52.dp // Total width - (start padding 12 + button width 40)
    val progressBarWidth = if (availableWidth > 100.dp) 100.dp else availableWidth - 10.dp

    Column {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            CustomFontText(
                text = aqiText,
                fontSize = ruuviStationFontsSizes.bigger,
                colorProvider = GlanceColors.valueColor,
                fontFamily = ruuviStationFonts.oswaldBold
            )

            Spacer(modifier = GlanceModifier.width(2.dp))

            Box(modifier = GlanceModifier.height(34.dp)) {
                CustomFontText(
                    text = "/100",
                    fontSize = ruuviStationFontsSizes.miniature,
                    colorProvider = GlanceColors.valueColor,
                    fontFamily = ruuviStationFonts.oswaldLight,
                    modifier = GlanceModifier.padding(top = 4.dp)
                )

                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomStart
                ) {
                    CustomFontText(
                        text = measurementName,
                        fontSize = ruuviStationFontsSizes.tiny,
                        colorProvider = GlanceColors.widgetSensorName,
                        fontFamily = ruuviStationFonts.mulishRegular,
                        modifier = GlanceModifier.padding(bottom = 2.dp)
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
            modifier = GlanceModifier
                .padding(bottom = 2.dp),
            totalWidth = progressBarWidth
        )
    }
}

@Composable
private fun RefreshButton() {
    Box(
        modifier = GlanceModifier
            .fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        Box(
            modifier = GlanceModifier
                .width(40.dp)
                .height(40.dp)
                .clickable(actionRunCallback<RefreshSimpleWidgetAction>()),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_d_update),
                contentDescription = null,
                modifier = GlanceModifier
                    .width(16.dp)
                    .height(16.dp),
                colorFilter = ColorFilter.tint(GlanceColors.refreshButtonColor)
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
    totalWidth: androidx.compose.ui.unit.Dp = 100.dp
) {
    val safeProgress = progress.coerceIn(0f, 1f)
    val dotSize = 6.dp
    val glowSize = 14.dp

    val progressPosition = totalWidth * safeProgress

    Box(
        modifier = modifier
            .width(totalWidth)
            .height(glowSize),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().height(glowSize),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .width(progressPosition)
                    .height(3.dp)
                    .background(activeColor)
            ) {}
            Box(
                modifier = GlanceModifier
                    .width(totalWidth - progressPosition)
                    .height(3.dp)
                    .background(backgroundColor)
            ) {}
        }

        Row(
            modifier = GlanceModifier.fillMaxWidth().height(glowSize),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val spacerWidth = (progressPosition - (glowSize / 2)).coerceIn(0.dp, totalWidth - glowSize)
            Spacer(modifier = GlanceModifier.width(spacerWidth))

            Box(
                modifier = GlanceModifier.width(glowSize).height(glowSize),
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
                    modifier = GlanceModifier.width(dotSize).height(dotSize),
                    colorFilter = ColorFilter.tint(activeColor)
                )
            }
        }
    }
}
