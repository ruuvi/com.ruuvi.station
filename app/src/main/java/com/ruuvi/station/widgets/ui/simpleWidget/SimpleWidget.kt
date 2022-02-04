package com.ruuvi.station.widgets.ui.simpleWidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.ruuvi.station.widgets.domain.WidgetPreferencesInteractor
import com.ruuvi.station.widgets.domain.WidgetsService
import timber.log.Timber

class SimpleWidget: AppWidgetProvider() {

    override fun onUpdate(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray?
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
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
    }

    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
    }
}

internal fun updateSimpleWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    Timber.d("updateSimpleWidget $appWidgetId")

    val pendingIntent = WidgetsService.getPendingIntent(context, appWidgetId)
    pendingIntent.send()
}