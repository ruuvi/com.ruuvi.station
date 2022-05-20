package com.ruuvi.station.widgets.ui.complexWidget

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.ruuvi.station.R
import timber.log.Timber

class ComplexWidgetProvider: AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Timber.d("onUpdate $appWidgetIds")
        appWidgetIds.forEach { appWidgetId ->
            updateComplexWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("onReceive $intent")
        if (MANUAL_REFRESH == intent.action) {
            val appWidgetId= intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            val appWidgetManager = AppWidgetManager.getInstance(context)
            Timber.d("MANUAL_REFRESH $appWidgetId")
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.sensorsListView)
        }
        super.onReceive(context, intent)
    }

    companion object {
        const val MANUAL_REFRESH = "com.ruuvi.station.widgets.complexWidget.MANUAL_REFRESH"

        private fun updateComplexWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val adapterIntent = Intent(context, CollectionWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }

            val views = RemoteViews(context.packageName, R.layout.widget_complex).apply{
                setRemoteAdapter(R.id.sensorsListView, adapterIntent)
                setEmptyView(R.id.sensorsListView, R.id.emptyView)
            }

            views.setOnClickPendingIntent(R.id.refreshButton, getUpdatePendingIntent(context, appWidgetId))

            views.setOnClickPendingIntent(R.id.addButton, ComplexWidgetConfigureActivity.createPendingIntent(context, appWidgetId))

            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.sensorsListView)
        }

        private fun getUpdatePendingIntent(context: Context, appWidgetId: Int): PendingIntent {
            val updateIntent = Intent(context, ComplexWidgetProvider::class.java).apply {
                action = MANUAL_REFRESH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            return PendingIntent.getBroadcast(context, appWidgetId, updateIntent, FLAG_IMMUTABLE)
        }
    }
}