package com.ruuvi.station.widgets.ui.firstWidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.ruuvi.station.widgets.domain.WidgetPreferencesInteractor
import com.ruuvi.station.widgets.domain.WidgetsService
import timber.log.Timber

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
        Timber.d("onDeleted Count = ${appWidgetIds.size}")
        val preferences = WidgetPreferencesInteractor(context)
        for (appWidgetId in appWidgetIds) {
            Timber.d("onDeleted Id $appWidgetId")
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

    val pendingIntent = WidgetsService.getPendingIntent(context, appWidgetId)
    pendingIntent.send()
}