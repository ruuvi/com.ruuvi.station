package com.ruuvi.station.widgets.ui.complexWidget

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.state.updateAppWidgetState
import com.google.gson.Gson
import com.ruuvi.station.widgets.domain.ComplexWidgetPreferencesInteractor
import com.ruuvi.station.widgets.domain.WidgetInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kodein.di.Kodein
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import timber.log.Timber

class ComplexWidgetProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ComplexWidgetGlanceWidget

    @SuppressLint("MissingSuperCall")
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Timber.d("onUpdate $appWidgetIds")
        launchBroadcastWork {
            updateWidgets(context, appWidgetIds)
        }
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
        when (intent.action) {
            REFRESH_WIDGET -> {
                val appWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    launchBroadcastWork {
                        updateWidgetSafely(context, appWidgetId)
                    }
                }
            }
            REFRESH_ALL_WIDGETS -> {
                launchBroadcastWork {
                    updateAll(context)
                }
            }
            else -> super.onReceive(context, intent)
        }
    }

    private fun launchBroadcastWork(block: suspend () -> Unit) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                block()
            } catch (error: Exception) {
                Timber.e(error, "Complex widget broadcast update failed")
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val REFRESH_WIDGET =
            "com.ruuvi.station.widgets.complexWidget.REFRESH_WIDGET"
        private const val REFRESH_ALL_WIDGETS =
            "com.ruuvi.station.widgets.complexWidget.REFRESH_ALL_WIDGETS"

        suspend fun updateComplexWidget(
            context: Context,
            appWidgetId: Int
        ) = withContext(Dispatchers.IO) {
            val applicationContext = context.applicationContext
            val kodein: Kodein by kodein(applicationContext)
            val interactor: WidgetInteractor by kodein.instance()
            val preferencesInteractor: ComplexWidgetPreferencesInteractor by kodein.instance()

            val widgetSettings = preferencesInteractor.getComplexWidgetSettings(appWidgetId)
            val cloudSensors = interactor.getCloudSensorsList()
                .filter { cloudSensor -> widgetSettings.any { it.sensorId == cloudSensor.id } }

            val sensorsData = cloudSensors.map { sensor ->
                val settings = widgetSettings.firstOrNull { it.sensorId == sensor.id }
                interactor.getComplexWidgetData(sensor.id, settings)
            }

            val glanceId = try {
                GlanceAppWidgetManager(applicationContext).getGlanceIdBy(appWidgetId)
            } catch (error: IllegalArgumentException) {
                Timber.d("Complex widget $appWidgetId was deleted before its update")
                return@withContext
            }

            updateAppWidgetState(applicationContext, glanceId) { prefs ->
                prefs[ComplexWidgetPrefKeys.data] = Gson().toJson(sensorsData)
            }
            ComplexWidgetGlanceWidget.update(applicationContext, glanceId)
        }

        fun getUpdatePendingIntent(context: Context, appWidgetId: Int): PendingIntent {
            val updateIntent = Intent(context, ComplexWidgetProvider::class.java).apply {
                action = REFRESH_WIDGET
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            return PendingIntent.getBroadcast(
                context,
                appWidgetId,
                updateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

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

        suspend fun updateAll(context: Context) = withContext(Dispatchers.IO) {
            updateWidgets(context, getWidgetsIds(context))
        }

        private suspend fun updateWidgets(context: Context, ids: IntArray) {
            for (appWidgetId in ids) {
                updateWidgetSafely(context, appWidgetId)
            }
        }

        private suspend fun updateWidgetSafely(context: Context, appWidgetId: Int) {
            try {
                updateComplexWidget(context, appWidgetId)
            } catch (error: Exception) {
                Timber.e(error, "Complex widget update failed for Id $appWidgetId")
            }
        }

        private fun getWidgetsIds(context: Context): IntArray {
            val appWidgetManager =
                AppWidgetManager.getInstance(context)

            return appWidgetManager.getAppWidgetIds(
                ComponentName(context, ComplexWidgetProvider::class.java.name)
            )
        }

        private const val REFRESH_ALL_REQUEST_CODE = 1
    }
}
