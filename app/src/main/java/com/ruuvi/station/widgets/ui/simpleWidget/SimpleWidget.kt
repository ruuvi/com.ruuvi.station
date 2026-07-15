package com.ruuvi.station.widgets.ui.simpleWidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.ruuvi.station.units.domain.aqi.AQI
import com.ruuvi.station.widgets.domain.WidgetInteractor
import com.ruuvi.station.widgets.domain.WidgetPreferencesInteractor
import com.ruuvi.station.widgets.ui.glance.GlanceColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import timber.log.Timber

class SimpleWidget: AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        for (appWidgetId in appWidgetIds) {
            updateSimpleWidget(context, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val preferences = WidgetPreferencesInteractor(context)
        for (appWidgetId in appWidgetIds) {
            Timber.d("onDeleted Id $appWidgetId")
            preferences.removeSimpleWidgetSettings(appWidgetId)
        }
    }

    override fun onEnabled(context: Context?) {
        super.onEnabled(context)
        Timber.d("onEnabled")
    }

    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
        Timber.d("onDisabled")
    }

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("onReceive $intent")
        if (MANUAL_REFRESH == intent.action) {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            val appWidgetManager = AppWidgetManager.getInstance(context)
            onUpdate(context, appWidgetManager, getSimpleWidgetsIds(context))
        }
        super.onReceive(context, intent)
    }

    companion object {
        private const val MANUAL_REFRESH = "com.ruuvi.station.widgets.ui.simpleWidget.MANUAL_REFRESH"
        private val SENSOR_ID_KEY = ActionParameters.Key<String>("sensor_id")
        private val APP_WIDGET_ID_KEY = ActionParameters.Key<Int>("app_widget_id")

        fun updateSimpleWidget(context: Context, appWidgetId: Int) {
            val kodein: Kodein by kodein(context)

            val preferences: WidgetPreferencesInteractor by kodein.instance()
            val widgetInteractor: WidgetInteractor by kodein.instance()

            val sensorId = preferences.getSimpleWidgetSensor(appWidgetId)
            val widgetType = preferences.getSimpleWidgetType(appWidgetId)

            if (!sensorId.isNullOrEmpty()) {
                CoroutineScope(Dispatchers.Main).launch {
                    val widgetData = widgetInteractor.getSimpleWidgetData(
                        sensorId = sensorId,
                        widgetType = widgetType
                    )

                    val glanceId = GlanceAppWidgetManager(context)
                        .getGlanceIdBy(appWidgetId)

                    updateAppWidgetState(context, glanceId) { prefs ->
                        prefs[SimpleWidgetPrefKeys.sensorId] = sensorId
                        prefs[SimpleWidgetPrefKeys.displayName] =
                            widgetData?.displayName.orEmpty()
                        prefs[SimpleWidgetPrefKeys.sensorValue] =
                            widgetData?.sensorValue.orEmpty()
                        prefs[SimpleWidgetPrefKeys.unit] =
                            widgetData?.unit.orEmpty()
                        prefs[SimpleWidgetPrefKeys.measurementName] =
                            widgetData?.measurementName.orEmpty()
                        prefs[SimpleWidgetPrefKeys.updated] =
                            widgetData?.updated.orEmpty()
                        prefs[SimpleWidgetPrefKeys.measurementType] =
                            widgetType.code.toString()
                    }

                    SimpleWidgetGlanceWidget.update(context, glanceId)
                }
            }
        }

        fun getUpdatePendingIntent(context: Context, appWidgetId: Int): PendingIntent {
            val updateIntent = Intent(context, SimpleWidget::class.java).apply {
                action = MANUAL_REFRESH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            return PendingIntent.getBroadcast(context, appWidgetId, updateIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun getSimpleWidgetsIds(context: Context): IntArray {
            val appWidgetManager =
                AppWidgetManager.getInstance(context)

            return appWidgetManager.getAppWidgetIds(ComponentName(context, SimpleWidget::class.java.name ))
        }

        fun openSensorActionParameters(sensorId: String, appWidgetId: Int): ActionParameters {
            return actionParametersOf(
                SENSOR_ID_KEY to sensorId,
                APP_WIDGET_ID_KEY to appWidgetId
            )
        }

        fun sensorIdFromParameters(parameters: ActionParameters): String? = parameters[SENSOR_ID_KEY]

        fun appWidgetIdFromParameters(parameters: ActionParameters): Int =
            parameters[APP_WIDGET_ID_KEY] ?: AppWidgetManager.INVALID_APPWIDGET_ID
    }
}


//@Composable
//private fun GlanceMeasurementDisplay(
//    sensorValue: String,
//    unit: String,
//    measurementName: String,
//    valueFontSize: TextUnit,
//    secondaryFontSize: TextUnit
//) {
//    Column(modifier = GlanceModifier.fillMaxSize()) {
//        Row(
//            modifier = GlanceModifier.defaultWeight(),
//            verticalAlignment = Alignment.Top,
//        ) {
//            Row {
//                Text(
//                    text = sensorValue,
//                    style = TextStyle(
//                        fontSize = valueFontSize,
//                        color = GlanceColors.valueColor,
//                        fontFamily = FontFamily.SansSerif,
//                        fontWeight = FontWeight.Bold
//                    ),
//                    maxLines = 1
//                )
//            }
//
//            Spacer(modifier = GlanceModifier.width(2.dp.fixed()))
//
//            Row(modifier = GlanceModifier.padding(top = 2.dp.fixed())) {
//                Text(
//                    text = unit,
//                    style = TextStyle(
//                        fontSize = secondaryFontSize,
//                        color = GlanceColors.widgetSensorName,
//                        fontFamily = FontFamily.SansSerif,
//                        fontWeight = FontWeight.Normal
//                    ),
//                    modifier = GlanceModifier.padding(top = 2.dp.fixed()),
//                    maxLines = 1
//                )
//            }
//        }
//
//        Text(
//            text = measurementName,
//            style = TextStyle(
//                fontSize = secondaryFontSize,
//                color = GlanceColors.widgetSensorName,
//                fontFamily = FontFamily.SansSerif,
//                fontWeight = FontWeight.Normal
//            ),
//            maxLines = 1
//        )
//    }
//}

//@Composable
//private fun GlanceAQIDisplay(
//    sensorValue: String,
//    measurementName: String,
//    valueFontSize: TextUnit,
//    secondaryFontSize: TextUnit,
//    refreshButtonSize: Dp
//) {
//    val aqiText = sensorValue.substringBefore("/")
//    val aqiValue = aqiText.toDoubleOrNull()
//    val aqiColor = aqiValue?.let { AQI.CalculatedAQI(it).color } ?: Color.Gray
//    val aqiColorProvider = ColorProviderDayNight(day = aqiColor, night = aqiColor)
//
//    val widgetWidth = LocalSize.current.width
//    val availableWidth = widgetWidth - (refreshButtonSize + 12.dp.fixed())
//    val progressBarWidth = if (availableWidth > 100.dp.fixed()) 100.dp.fixed() else availableWidth - 10.dp.fixed()
//
//    Column(modifier = GlanceModifier.fillMaxSize()) {
//        Row(
//            modifier = GlanceModifier.fillMaxWidth(),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Text(
//                text = aqiText,
//                style = TextStyle(
//                    fontSize = valueFontSize,
//                    color = GlanceColors.valueColor,
//                    fontFamily = FontFamily.SansSerif,
//                    fontWeight = FontWeight.Bold
//                ),
//                maxLines = 1
//            )
//
//            Spacer(modifier = GlanceModifier.width(2.dp.fixed()))
//
//            Column(
//                modifier = GlanceModifier.height(valueFontSize.value.dp),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Text(
//                    text = "/100",
//                    style = TextStyle(
//                        fontSize = secondaryFontSize,
//                        color = GlanceColors.valueColor,
//                        fontFamily = FontFamily.SansSerif,
//                        fontWeight = FontWeight.Normal
//                    ),
//                    modifier = GlanceModifier.defaultWeight(),
//                    maxLines = 1
//                )
//                Text(
//                    text = measurementName,
//                    style = TextStyle(
//                        fontSize = secondaryFontSize,
//                        color = GlanceColors.widgetSensorName,
//                        fontFamily = FontFamily.SansSerif,
//                        fontWeight = FontWeight.Normal
//                    ),
//                    modifier = GlanceModifier.defaultWeight(),
//                    maxLines = 1
//                )
//            }
//        }
//
//        Spacer(modifier = GlanceModifier.height(2.dp.fixed()))
//
//        GlanceProgressBarWithDot(
//            progress = (aqiValue?.toFloat() ?: 0f) / 100f,
//            activeColor = aqiColorProvider,
//            backgroundColor = ColorProviderDayNight(
//                day = aqiColor.copy(alpha = 0.2f),
//                night = aqiColor.copy(alpha = 0.2f)
//            ),
//            modifier = GlanceModifier.padding(start = 1.dp.fixed(), bottom = 2.dp.fixed()),
//            totalWidth = progressBarWidth
//        )
//    }
//}
