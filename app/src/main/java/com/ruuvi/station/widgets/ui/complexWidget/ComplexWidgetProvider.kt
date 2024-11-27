package com.ruuvi.station.widgets.ui.complexWidget

import android.app.PendingIntent
import android.app.PendingIntent.*
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.ruuvi.station.R
import com.ruuvi.station.tagdetails.ui.SensorCardActivity
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
        when (intent.action) {
            MANUAL_REFRESH -> {
                val appWidgetId= intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                val appWidgetManager = AppWidgetManager.getInstance(context)
                if (appWidgetId == 0) {
                    updateAll(context)
                } else {
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.sensorsListView)
                }
            }
            ITEM_CLICK -> {
                val sensorId = intent.getStringExtra(EXTRA_SENSOR_ID) ?: ""
                val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                SensorCardActivity.createPendingIntent(context, sensorId, appWidgetId)?.send()
            }
            else -> super.onReceive(context, intent)
        }
    }

    companion object {
        const val MANUAL_REFRESH = "com.ruuvi.station.widgets.complexWidget.MANUAL_REFRESH"
        const val ITEM_CLICK = "com.ruuvi.station.widgets.complexWidget.ITEM_CLICK"
        const val EXTRA_SENSOR_ID = "EXTRA_SENSOR_ID"

        private fun updateComplexWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val adapterIntent = Intent(context, ComplexWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }

            val views = RemoteViews(context.packageName, R.layout.widget_complex).apply{
                setRemoteAdapter(R.id.sensorsListView, adapterIntent)
                setEmptyView(R.id.sensorsListView, R.id.emptyView)
                setOnClickPendingIntent(R.id.refreshButton, getUpdatePendingIntent(context, appWidgetId))
                setOnClickPendingIntent(R.id.addButton, ComplexWidgetConfigureActivity.createPendingIntent(context, appWidgetId))
                setPendingIntentTemplate(R.id.sensorsListView, getPendingIntentTemplate(context, appWidgetId))
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.sensorsListView)
        }

        fun getUpdatePendingIntent(context: Context, appWidgetId: Int): PendingIntent {
            val updateIntent = Intent(context, ComplexWidgetProvider::class.java).apply {
                action = MANUAL_REFRESH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            return PendingIntent.getBroadcast(context, appWidgetId, updateIntent, FLAG_IMMUTABLE)
        }

        private fun getPendingIntentTemplate(context: Context, appWidgetId: Int): PendingIntent {
            val itemClickIntent = Intent(context, ComplexWidgetProvider::class.java).apply {
                action = ITEM_CLICK
            }
            return PendingIntent.getBroadcast(context, appWidgetId, itemClickIntent, FLAG_UPDATE_CURRENT or FLAG_MUTABLE)
        }

        fun updateAll(context: Context) {
            val ids = getWidgetsIds(context)
            val appWidgetManager =
                AppWidgetManager.getInstance(context)

            for (appWidgetId in ids) {
                updateComplexWidget(context, appWidgetManager, appWidgetId)
            }
        }

        private fun getWidgetsIds(context: Context): IntArray {
            val appWidgetManager =
                AppWidgetManager.getInstance(context)

            return appWidgetManager.getAppWidgetIds(ComponentName(context, ComplexWidgetProvider::class.java.name ))
        }
    }
}