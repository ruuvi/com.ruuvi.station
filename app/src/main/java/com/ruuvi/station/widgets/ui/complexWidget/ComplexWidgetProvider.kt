package com.ruuvi.station.widgets.ui.complexWidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.ruuvi.station.widgets.domain.ComplexWidgetPreferencesInteractor
import com.ruuvi.station.widgets.update.WidgetRefreshScheduler
import org.kodein.di.Kodein
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import timber.log.Timber

class ComplexWidgetProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ComplexWidgetGlanceWidget

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Timber.d("onUpdate $appWidgetIds")
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetRefreshScheduler.enqueueComplexRefreshAll(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        try {
            val kodein: Kodein by kodein(context.applicationContext)
            val preferencesInteractor: ComplexWidgetPreferencesInteractor by kodein.instance()
            for (appWidgetId in appWidgetIds) {
                Timber.d("onDeleted Id $appWidgetId")
                preferencesInteractor.removeComplexWidgetSettings(appWidgetId)
            }
        } finally {
            super.onDeleted(context, appWidgetIds)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("onReceive $intent")
        super.onReceive(context, intent)
        if (intent.action == REFRESH_ALL_WIDGETS) {
            WidgetRefreshScheduler.enqueueComplexRefreshAll(context)
        }
    }

    companion object {
        private const val REFRESH_ALL_WIDGETS =
            "com.ruuvi.station.widgets.complexWidget.REFRESH_ALL_WIDGETS"

        fun getRefreshAllPendingIntent(context: Context): PendingIntent {
            val updateIntent = Intent(context, ComplexWidgetProvider::class.java).apply {
                action = REFRESH_ALL_WIDGETS
            }
            return PendingIntent.getBroadcast(
                context,
                REFRESH_ALL_REQUEST_CODE,
                updateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private const val REFRESH_ALL_REQUEST_CODE = 1
    }
}
