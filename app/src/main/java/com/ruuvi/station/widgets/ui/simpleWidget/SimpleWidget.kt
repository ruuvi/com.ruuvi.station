package com.ruuvi.station.widgets.ui.simpleWidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.ruuvi.station.R
import com.ruuvi.station.tagdetails.ui.SensorCardActivity
import com.ruuvi.station.widgets.domain.WidgetInteractor
import com.ruuvi.station.widgets.domain.WidgetPreferencesInteractor
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
            updateSimpleWidget(context, appWidgetManager, appWidgetId)
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
            Timber.d("MANUAL_REFRESH $appWidgetId")
            onUpdate(context, appWidgetManager, getSimpleWidgetsIds(context))
        }
        super.onReceive(context, intent)
    }

    companion object {
        private const val MANUAL_REFRESH = "com.ruuvi.station.widgets.ui.simpleWidget.MANUAL_REFRESH"

        fun updateSimpleWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val kodein: Kodein by kodein(context)

            val preferences: WidgetPreferencesInteractor by kodein.instance()
            val widgetInteractor: WidgetInteractor by kodein.instance()

            val sensorId = preferences.getSimpleWidgetSensor(appWidgetId)
            val widgetType = preferences.getSimpleWidgetType(appWidgetId)

            if (!sensorId.isNullOrEmpty()) {
                val views = RemoteViews(context.packageName, R.layout.widget_simple)
                CoroutineScope(Dispatchers.Main).launch {
                    val widgetData = widgetInteractor.getSimpleWidgetData(
                        sensorId = sensorId,
                        widgetType = widgetType
                    )
                    if (widgetData != null) {
                        views.setTextViewText(R.id.sensorNameTextView, widgetData.displayName)
                        views.setTextViewText(R.id.unitTextView, widgetData.unit)
                        views.setTextViewText(R.id.sensorValueTextView, widgetData.sensorValue)
                        views.setTextViewText(R.id.updateTextView, widgetData.updated)
                    }

                    views.setOnClickPendingIntent(
                        R.id.simpleWidgetLayout,
                        SensorCardActivity.createPendingIntent(context, sensorId, appWidgetId)
                    )
                    views.setOnClickPendingIntent(
                        R.id.refreshButton,
                        getUpdatePendingIntent(context, appWidgetId)
                    )

                    appWidgetManager.updateAppWidget(appWidgetId, views)
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

        fun updateAll(context: Context) {
            val ids = getSimpleWidgetsIds(context)
            val appWidgetManager =
                AppWidgetManager.getInstance(context)

            for (appWidgetId in ids) {
                updateSimpleWidget(context, appWidgetManager, appWidgetId)
            }
        }

        fun getSimpleWidgetsIds(context: Context): IntArray {
            val appWidgetManager =
                AppWidgetManager.getInstance(context)

            return appWidgetManager.getAppWidgetIds(ComponentName(context, SimpleWidget::class.java.name ))
        }
    }
}