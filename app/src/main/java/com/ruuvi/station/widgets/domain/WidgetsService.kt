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
import com.ruuvi.station.widgets.ui.firstWidget.SensorWidget
import com.ruuvi.station.widgets.ui.firstWidget.updateAppWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class WidgetsService(): Service(), KodeinAware {

    override val kodein: Kodein by kodein()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val appWidgetId = intent?.getIntExtra("appWidgetId",-1)
        Timber.d("WidgetsService onStartCommand $appWidgetId")

        if (appWidgetId == null || appWidgetId == -1) return super.onStartCommand(intent, flags, startId)

        val context = this@WidgetsService
        val appWidgetManager = AppWidgetManager.getInstance(this)

        val widgetInteractor: WidgetInteractor by kodein.instance()

        val preferences = WidgetPreferencesInteractor(context)
        val sensorId = preferences.getWidgetSensor(appWidgetId)
        Timber.d("WidgetsService sensorId=$sensorId")


        CoroutineScope(Dispatchers.Main).launch {
            val views = RemoteViews(context.packageName, R.layout.sensor_widget)

            if (sensorId != null) {
                val widgetData = widgetInteractor.getSensorData(sensorId)
                // Construct the RemoteViews object
                val time = if (widgetData.updatedAt != null) {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(widgetData.updatedAt)
                } else {
                    "none"
                }

                val sensorName = if (widgetData.displayName.isEmpty()) widgetData.sensorId else widgetData.displayName

                views.setTextViewText(R.id.widgetHeaderTextView, sensorName)
                views.setTextViewText(R.id.temperatureTextView, widgetData.temperature)
                views.setTextViewText(R.id.humidityTextView, widgetData.humidity)
                views.setTextViewText(R.id.pressureTextView, widgetData.pressure)
                views.setTextViewText(R.id.motionTextView, widgetData.movement)
                views.setTextViewText(R.id.lastUpdateTextView, getString(R.string.updated,time))

                views.setOnClickPendingIntent(R.id.widgetLayout, TagDetailsActivity.createPendingIntent(context, sensorId, appWidgetId))
            }

            val tagDetailsPendingIntent =
                TagDetailsActivity.createPendingIntent(
                    context,
                    sensorId ?: "0",
                    0
                )

            views.setOnClickPendingIntent(R.id.refreshButton, getPendingIntent(context, appWidgetId))

            appWidgetManager.updateAppWidget(appWidgetId, views)
            Timber.d("WidgetsService FINISHED")

        }

        return super.onStartCommand(intent, flags, startId)
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

        fun updateAllWidgets(context: Context) {
            val appWidgetManager =
                AppWidgetManager.getInstance(context)

            val widgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, SensorWidget::class.java.name ))
            Timber.d("widgetIds count ${widgetIds.size}")

            for (id in widgetIds) {
                Timber.d("widgetIds $id")
                updateAppWidget(context, appWidgetManager, id)
            }
        }

        const val APP_WIDGET_ID = "appWidgetId"
    }
}