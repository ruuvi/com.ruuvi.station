package com.ruuvi.station.widgets.ui.complexWidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.*
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
import com.ruuvi.station.widgets.ui.glance.getZoomFactor
import com.ruuvi.station.widgets.ui.glance.toWidgetSp
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
    val context = LocalContext.current
    val zoomFactor = getZoomFactor(context)
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceColors.background)
            .padding(all = 4.dp / zoomFactor)
    ) {
        if (sensors.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity<DashboardActivity>()),
                contentAlignment = Alignment.Center
            ) {
                CustomFontText(
                    text = context.getString(R.string.widgets_loading),
                    fontSize = ruuviStationFontsSizes.normal.toWidgetSp(context),
                    colorProvider = GlanceColors.widgetSensorName,
                    fontResId = R.font.mulish_bold
                )
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                sensors.forEach { sensor ->
                    val openAction = actionRunCallback<OpenComplexWidgetSensorAction>(
                        SimpleWidget.openSensorActionParameters(sensor.sensorId, 0)
                    )
                    
                    item {
                        SensorHeader(sensor, openAction, zoomFactor)
                        Spacer(modifier = GlanceModifier.height(12.dp / zoomFactor))
                    }

                    items(sensor.sensorValues.chunked(2)) { rowValues ->
                        MeasurementRow(rowValues, openAction, zoomFactor)
                    }

                    item {
                        SensorFooter(sensor, openAction, zoomFactor)
                    }
                }
            }
        }
        
        RefreshButton(
            size = 44.dp / zoomFactor,
            iconSize = 18.dp / zoomFactor,
            paddingBottom = 12.dp / zoomFactor,
            paddingEnd = 12.dp / zoomFactor,
            action = actionRunCallback<RefreshComplexWidgetAction>()
        )
    }
}

@Composable
private fun SensorHeader(sensor: ComplexWidgetData, action: Action, zoomFactor: Float) {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp / zoomFactor)
            .padding(top = 12.dp / zoomFactor)
            .clickable(action),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CustomFontText(
            text = sensor.displayName,
            fontSize = ruuviStationFontsSizes.normal.toWidgetSp(context),
            colorProvider = GlanceColors.widgetSensorName,
            fontResId = R.font.mulish_bold,
            modifier = GlanceModifier.defaultWeight()
        )
    }
}

@Composable
private fun MeasurementRow(rowValues: List<SensorValue>, action: Action, zoomFactor: Float) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp / zoomFactor, vertical = 3.dp / zoomFactor)
            .clickable(action)
    ) {
        rowValues.forEach { value ->
            MeasurementItem(value, modifier = GlanceModifier.defaultWeight(), zoomFactor = zoomFactor)
        }
        if (rowValues.size == 1) {
            Spacer(modifier = GlanceModifier.defaultWeight())
        }
    }
}

@Composable
private fun SensorFooter(sensor: ComplexWidgetData, action: Action, zoomFactor: Float) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp / zoomFactor)
            .padding(bottom = 12.dp / zoomFactor)
            .clickable(action)
    ) {
        Spacer(modifier = GlanceModifier.height(8.dp / zoomFactor))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            CustomFontText(
                text = sensor.updated ?: "",
                fontSize = ruuviStationFontsSizes.tiny2.toWidgetSp(context),
                colorProvider = GlanceColors.widgetSensorName,
                fontResId = R.font.mulish_regular
            )
        }
    }
}

@Composable
private fun MeasurementItem(value: SensorValue, modifier: GlanceModifier, zoomFactor: Float) {
    val context = LocalContext.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CustomFontText(
            text = value.sensorValue,
            fontSize = ruuviStationFontsSizes.compact.toWidgetSp(context),
            colorProvider = GlanceColors.valueColor,
            fontResId = R.font.mulish_extrabold
        )
        
        if (value.unit.isNotEmpty()) {
            Spacer(modifier = GlanceModifier.width(2.dp / zoomFactor))
            CustomFontText(
                text = value.unit,
                fontSize = ruuviStationFontsSizes.petite.toWidgetSp(context),
                colorProvider = GlanceColors.widgetSensorName,
                fontResId = R.font.mulish_bold
            )
        }

        Spacer(modifier = GlanceModifier.width(4.dp / zoomFactor))
        CustomFontText(
            text = context.getString(value.type.unitType.measurementName),
            fontSize = ruuviStationFontsSizes.petite.toWidgetSp(context),
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
