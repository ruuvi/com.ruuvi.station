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
import androidx.glance.LocalSize
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
import com.ruuvi.station.R
import androidx.datastore.preferences.core.Preferences
import com.ruuvi.station.app.ui.theme.Red
import com.ruuvi.station.app.ui.theme.White
import com.ruuvi.station.dashboard.ui.DashboardActivity
import com.ruuvi.station.tagdetails.ui.SensorCardActivity
import com.ruuvi.station.widgets.ui.glance.GlanceColors
import com.ruuvi.station.widgets.ui.glance.CustomFontText
import com.ruuvi.station.app.ui.theme.ruuviStationFonts
import com.ruuvi.station.app.ui.theme.ruuviStationFontsSizes
import java.time.Year

object SimpleWidgetGlanceWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    private val SensorIdKey = stringPreferencesKey("sensor_id")
    private val DisplayNameKey = stringPreferencesKey("display_name")
    private val SensorValueKey = stringPreferencesKey("sensor_value")
    private val UnitKey = stringPreferencesKey("unit")
    private val MeasurementNameKey = stringPreferencesKey("measurement_name")
    private val UpdatedKey = stringPreferencesKey("updated")

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

            SimpleWidgetContent(
                appWidgetId = appWidgetId,
                sensorId = sensorId,
                displayName = displayName?.takeIf { it.isNotBlank() }
                    ?: context.getString(R.string.widgets_loading),
                sensorValue = sensorValue.orEmpty(),
                unit = unit.orEmpty(),
                measurementName = measurementName.orEmpty(),
                updated = updated.orEmpty()
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
    updated: String
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
            .padding(start = 8.dp, top = 2.dp, end = 8.dp, bottom = 2.dp)
            .clickable(openAction)
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize()
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.logo_2021),
                    contentDescription = null,
                    modifier = GlanceModifier.width(36.dp),
                    colorFilter = ColorFilter.tint(GlanceColors.logoColor)
                )

                Spacer(modifier = GlanceModifier.defaultWeight())

                CustomFontText(
                    text = updated,
                    fontSize = ruuviStationFontsSizes.tiny,
                    colorProvider = GlanceColors.widgetSensorName,
                    fontFamily = ruuviStationFonts.mulishRegular
                )
            }

            CustomFontText(
                text = displayName,
                fontSize = ruuviStationFontsSizes.normal,
                colorProvider = GlanceColors.widgetSensorName,
                fontFamily = ruuviStationFonts.mulishBold
            )

            CustomFontText(
                text = measurementName,
                fontSize = ruuviStationFontsSizes.tiny,
                colorProvider = GlanceColors.widgetSensorName,
                fontFamily = ruuviStationFonts.mulishRegular
            )

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
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(2.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Box(
                modifier = GlanceModifier
                    .width(40.dp)
                    .height(40.dp)
                    .padding(bottom = 4.dp, end = 4.dp)
                    .clickable(actionRunCallback<RefreshSimpleWidgetAction>()),
                contentAlignment = Alignment.BottomEnd
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
}
