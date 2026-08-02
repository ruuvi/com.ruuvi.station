package com.ruuvi.station.widgets.ui.simpleWidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.ruuvi.station.widgets.domain.WidgetPreferencesInteractor
import com.ruuvi.station.widgets.update.WidgetRefreshScheduler
import org.kodein.di.Kodein
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import timber.log.Timber

class SimpleWidget : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = SimpleWidgetGlanceWidget

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetRefreshScheduler.enqueueSimpleRefreshAll(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        try {
            val kodein: Kodein by kodein(context.applicationContext)
            val preferences: WidgetPreferencesInteractor by kodein.instance()
            for (appWidgetId in appWidgetIds) {
                Timber.d("onDeleted Id $appWidgetId")
                preferences.removeSimpleWidgetSettings(appWidgetId)
            }
        } finally {
            super.onDeleted(context, appWidgetIds)
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
        super.onReceive(context, intent)
        if (intent.action == REFRESH_ALL_WIDGETS) {
            WidgetRefreshScheduler.enqueueSimpleRefreshAll(context)
        }
    }

    companion object {
        private const val REFRESH_ALL_WIDGETS =
            "com.ruuvi.station.widgets.ui.simpleWidget.REFRESH_ALL_WIDGETS"
        private val SENSOR_ID_KEY = ActionParameters.Key<String>("sensor_id")

        fun getRefreshAllPendingIntent(context: Context): PendingIntent {
            val updateIntent = Intent(context, SimpleWidget::class.java).apply {
                action = REFRESH_ALL_WIDGETS
            }
            return PendingIntent.getBroadcast(
                context,
                REFRESH_ALL_REQUEST_CODE,
                updateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun getSimpleWidgetsIds(context: Context): IntArray {
            val appWidgetManager =
                AppWidgetManager.getInstance(context)

            return appWidgetManager.getAppWidgetIds(
                ComponentName(context, SimpleWidget::class.java.name)
            )
        }

        fun openSensorActionParameters(sensorId: String): ActionParameters =
            actionParametersOf(SENSOR_ID_KEY to sensorId)

        fun sensorIdFromParameters(parameters: ActionParameters): String? = parameters[SENSOR_ID_KEY]

        private const val REFRESH_ALL_REQUEST_CODE = 1
    }
}
