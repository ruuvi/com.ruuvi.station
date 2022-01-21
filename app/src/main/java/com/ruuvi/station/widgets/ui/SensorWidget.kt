package com.ruuvi.station.widgets.ui

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.ruuvi.station.R
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.widgets.domain.WidgetPreferencesInteractor
import org.kodein.di.Kodein
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

/**
 * Implementation of App Widget functionality.
* App Widget Configuration implemented in [SensorWidgetConfigureActivity]
 */
class SensorWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Timber.d("onUpdate ${appWidgetIds.size}")
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        Timber.d("onDeleted ${appWidgetIds.size}")
        val preferences = WidgetPreferencesInteractor(context)
        for (appWidgetId in appWidgetIds) {
            preferences.removeWidgetSensor(appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
        Timber.d("onEnabled")
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
        Timber.d("onDisabled")
    }
}

internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    Timber.d("updateAppWidget $appWidgetId")
    val kodein: Kodein by closestKodein(context)
    val tagRepository: TagRepository by kodein.instance()

    val preferences = WidgetPreferencesInteractor(context)
    val sensorId = preferences.getWidgetSensor(appWidgetId)

    if (sensorId != null) {
        val sensor = tagRepository.getFavoriteSensorById(sensorId!!)

        // Construct the RemoteViews object
        val views = RemoteViews(context.packageName, R.layout.sensor_widget)
        if (sensor != null) {
            val time = if (sensor.updatedAt != null) {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(sensor.updatedAt)
            } else {
                "none"
            }

            views.setTextViewText(R.id.widgetHeaderTextView, sensor.displayName)
            views.setTextViewText(R.id.temperatureTextView, sensor.temperatureString)
            views.setTextViewText(R.id.humidityTextView, sensor.humidityString)
            views.setTextViewText(R.id.pressureTextView, sensor.pressureString)
            views.setTextViewText(R.id.motionTextView, sensor.movementCounter.toString())
            views.setTextViewText(R.id.lastUpdateTextView, time)
        }
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}