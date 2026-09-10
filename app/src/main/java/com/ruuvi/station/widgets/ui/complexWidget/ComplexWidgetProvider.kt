package com.ruuvi.station.widgets.ui.complexWidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.ruuvi.station.widgets.domain.ComplexWidgetPreferencesInteractor
import com.ruuvi.station.widgets.update.WidgetRefreshScheduler
import com.ruuvi.station.widgets.update.WidgetRefreshTrigger
import org.kodein.di.DI
import org.kodein.di.android.closestDI
import org.kodein.di.instance
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
            val di: DI by closestDI(context.applicationContext)
            val preferencesInteractor: ComplexWidgetPreferencesInteractor by di.instance()
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
            val refreshTrigger = intent.getStringExtra(REFRESH_TRIGGER_EXTRA)
                ?.let(WidgetRefreshTrigger::fromInputValue)
                ?: WidgetRefreshTrigger.MANUAL
            WidgetRefreshScheduler.enqueueComplexRefreshAll(context, refreshTrigger)
        }
    }

    companion object {
        private const val REFRESH_ALL_WIDGETS =
            "com.ruuvi.station.widgets.complexWidget.REFRESH_ALL_WIDGETS"
        private const val REFRESH_TRIGGER_EXTRA =
            "com.ruuvi.station.widgets.complexWidget.REFRESH_TRIGGER_EXTRA"

        fun getRefreshAllPendingIntent(context: Context): PendingIntent {
            val updateIntent = Intent(context, ComplexWidgetProvider::class.java).apply {
                action = REFRESH_ALL_WIDGETS
                putExtra(REFRESH_TRIGGER_EXTRA, WidgetRefreshTrigger.AUTOMATIC.inputValue)
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
