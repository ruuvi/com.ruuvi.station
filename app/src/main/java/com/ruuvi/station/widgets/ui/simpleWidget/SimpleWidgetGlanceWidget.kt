package com.ruuvi.station.widgets.ui.simpleWidget

import android.content.Context
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.background
import androidx.glance.color.ColorProvider
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.ruuvi.station.R
import androidx.datastore.preferences.core.Preferences
import com.ruuvi.station.app.ui.theme.darkPalette
import com.ruuvi.station.app.ui.theme.lightPalette
import com.ruuvi.station.dashboard.ui.DashboardActivity
import com.ruuvi.station.tagdetails.ui.SensorCardActivity

object SimpleWidgetGlanceWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    private val SensorIdKey = stringPreferencesKey("sensor_id")
    private val DisplayNameKey = stringPreferencesKey("display_name")
    private val SensorValueKey = stringPreferencesKey("sensor_value")
    private val UnitKey = stringPreferencesKey("unit")
    private val UpdatedKey = stringPreferencesKey("updated")

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)

        provideContent {
            val prefs = currentState<Preferences>()

            val sensorId = prefs[SensorIdKey]
            val displayName = prefs[DisplayNameKey]
            val sensorValue = prefs[SensorValueKey]
            val unit = prefs[UnitKey]
            val updated = prefs[UpdatedKey]

            SimpleWidgetContent(
                appWidgetId = appWidgetId,
                sensorId = sensorId,
                displayName = displayName?.takeIf { it.isNotBlank() }
                    ?: context.getString(R.string.widgets_loading),
                sensorValue = sensorValue.orEmpty(),
                unit = unit.orEmpty(),
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
    updated: String
) {
    val surfaceColor = ColorProvider(day = lightPalette.background, night = darkPalette.background)
    val valueColor = ColorProvider(day = lightPalette.dashboardValue, night = darkPalette.dashboardValue)
    val secondaryColor = ColorProvider(day = lightPalette.secondaryTextColor, night = darkPalette.secondaryTextColor)
    val dashboardIconsColor = ColorProvider(day = lightPalette.dashboardIcons, night = darkPalette.dashboardIcons)

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
            .background(surfaceColor)
            .padding(8.dp)
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
                    modifier = GlanceModifier.width(44.dp),
                    colorFilter = ColorFilter.tint(dashboardIconsColor)
                )

                Spacer(modifier = GlanceModifier.defaultWeight())

                Text(
                    text = updated,
                    style = TextStyle(
                        color = secondaryColor,
                        fontSize = 12.sp
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            Text(
                text = displayName,
                maxLines = 1,
                style = TextStyle(
                    color = secondaryColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = GlanceModifier.defaultWeight())

            Row(
                verticalAlignment = Alignment.Vertical.Bottom
            ) {
                Text(
                    text = sensorValue,
                    style = TextStyle(
                        color = valueColor,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.width(4.dp))

                Text(
                    text = unit,
                    style = TextStyle(
                        color = secondaryColor,
                        fontSize = 12.sp
                    )
                )
            }
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(2.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_d_update),
                contentDescription = null,
                modifier = GlanceModifier
                    .width(16.dp)
                    .height(16.dp)
                    .clickable(actionRunCallback<RefreshSimpleWidgetAction>()),
                colorFilter = ColorFilter.tint(secondaryColor)
            )
        }
    }
}