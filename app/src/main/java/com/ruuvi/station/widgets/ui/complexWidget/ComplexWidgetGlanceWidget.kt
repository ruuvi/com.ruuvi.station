package com.ruuvi.station.widgets.ui.complexWidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.*
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
import com.ruuvi.station.widgets.ui.glance.CustomFontText
import com.ruuvi.station.widgets.ui.glance.GlanceColors
import com.ruuvi.station.widgets.ui.glance.RefreshButton
import com.ruuvi.station.widgets.ui.simpleWidget.SimpleWidget

object ComplexWidgetGlanceWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

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

@Composable
private fun ComplexWidgetContent(sensors: List<ComplexWidgetData>) {
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
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(sensors) { sensor ->
                    SensorCard(sensor)
                }
            }
        }
        
        RefreshButton(action = actionRunCallback<RefreshComplexWidgetAction>())
    }
}

@Composable
private fun SensorCard(sensor: ComplexWidgetData) {
    val openAction = actionRunCallback<OpenComplexWidgetSensorAction>(
        SimpleWidget.openSensorActionParameters(sensor.sensorId, 0)
    )

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .clickable(openAction)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
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

        Spacer(modifier = GlanceModifier.height(12.dp))

        val rows = sensor.sensorValues.chunked(2)
        rows.forEach { rowValues ->
            Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp)) {
                rowValues.forEach { value ->
                    MeasurementItem(value, modifier = GlanceModifier.defaultWeight())
                }
                if (rowValues.size == 1) {
                    Spacer(modifier = GlanceModifier.defaultWeight())
                }
            }
        }

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
private fun MeasurementItem(value: SensorValue, modifier: GlanceModifier) {
    val context = LocalContext.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CustomFontText(
            text = value.sensorValue,
            fontSize = ruuviStationFontsSizes.compact,
            colorProvider = GlanceColors.valueColor,
            fontResId = R.font.oswald_bold
        )
        
        if (value.unit.isNotEmpty()) {
            Spacer(modifier = GlanceModifier.width(2.dp))
            CustomFontText(
                text = value.unit,
                fontSize = ruuviStationFontsSizes.tiny,
                colorProvider = GlanceColors.widgetSensorName,
                fontResId = R.font.oswald_light
            )
        }

        Spacer(modifier = GlanceModifier.width(4.dp))
        CustomFontText(
            text = context.getString(value.type.unitType.measurementName),
            fontSize = ruuviStationFontsSizes.tiny,
            colorProvider = GlanceColors.widgetSensorName,
            fontResId = R.font.mulish_regular
        )
    }
}

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
