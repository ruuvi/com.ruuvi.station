package com.ruuvi.station.widgets.domain

import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.widget.RemoteViews
import com.ruuvi.station.R
import com.ruuvi.station.tagdetails.ui.TagDetailsActivity
import com.ruuvi.station.util.Vibration
import com.ruuvi.station.widgets.data.WidgetType
import com.ruuvi.station.widgets.ui.firstWidget.updateAppWidget
import com.ruuvi.station.widgets.ui.simpleWidget.SimpleWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import timber.log.Timber

class WidgetsService(): Service(), KodeinAware {

    override val kodein: Kodein by kodein()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val updateAll = intent?.getBooleanExtra(UPDATE_ALL, false) ?: false
        Timber.d("Update all $updateAll")

        if (updateAll) {
            Vibration.buzz(this, Vibration.TAP)
            updateAllWidgets(this)
            return super.onStartCommand(intent, flags, startId)
        }

        val appWidgetId = intent?.getIntExtra(APP_WIDGET_ID, -1)
        Timber.d("WidgetsService onStartCommand $appWidgetId")

        if (appWidgetId == null || appWidgetId == -1) return super.onStartCommand(
            intent,
            flags,
            startId
        )

        val context = this@WidgetsService

        val preferences = WidgetPreferencesInteractor(context)


        val sensorId = preferences.getSimpleWidgetSensor(appWidgetId)
        val widgetType = preferences.getSimpleWidgetType(appWidgetId)

        if (sensorId != null) {
            updateSimpleWidget(appWidgetId, sensorId, widgetType)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun updateSimpleWidget(appWidgetId: Int, sensorId: String, widgetType: WidgetType) {
        val context = this@WidgetsService
        val widgetInteractor: WidgetInteractor by kodein.instance()
        val appWidgetManager = AppWidgetManager.getInstance(this)

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
                TagDetailsActivity.createPendingIntent(context, sensorId, appWidgetId)
            )
            views.setOnClickPendingIntent(
                R.id.refreshButton,
                getPendingIntentToUpdateAll(context, appWidgetId)
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onDestroy() {
        Timber.d("WidgetsService onDestroy")
        super.onDestroy()
    }

    companion object {
        fun getPendingIntent(context: Context, appWidgetId: Int): PendingIntent {
            val widgetsServiceIntent = Intent(context, WidgetsService::class.java)
            widgetsServiceIntent.putExtra(APP_WIDGET_ID, appWidgetId)
            return PendingIntent.getService(context, appWidgetId, widgetsServiceIntent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        fun getPendingIntentToUpdateAll(context: Context, appWidgetId: Int): PendingIntent {
            val widgetsServiceIntent = Intent(context, WidgetsService::class.java)
            widgetsServiceIntent.putExtra(UPDATE_ALL, true)
            return PendingIntent.getService(context, appWidgetId, widgetsServiceIntent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager =
                AppWidgetManager.getInstance(context)

            val widgetIds = getSimpleWidgetsIds(context)
            Timber.d("widgetIds count ${widgetIds.size}")

            for (id in widgetIds) {
                Timber.d("widgetIds $id")
                updateAppWidget(context, appWidgetManager, id)
            }
        }

        fun getSimpleWidgetsIds(context: Context): IntArray {
            val appWidgetManager =
                AppWidgetManager.getInstance(context)

            return appWidgetManager.getAppWidgetIds(ComponentName(context, SimpleWidget::class.java.name ))
        }

        const val APP_WIDGET_ID = "appWidgetId"
        const val UPDATE_ALL = "UPDATE_ALL"
    }
}