package com.ruuvi.station.widgets.ui.complexWidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.google.gson.Gson
import com.ruuvi.station.widgets.domain.ComplexWidgetPreferencesInteractor
import com.ruuvi.station.widgets.domain.WidgetInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import timber.log.Timber

class ComplexWidgetProvider: AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Timber.d("onUpdate $appWidgetIds")
        appWidgetIds.forEach { appWidgetId ->
            updateComplexWidget(context, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("onReceive $intent")
        when (intent.action) {
            MANUAL_REFRESH -> {
                val appWidgetId= intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                    updateAll(context)
                } else {
                    updateComplexWidget(context, appWidgetId)
                }
            }
            else -> super.onReceive(context, intent)
        }
    }

    companion object {
        const val MANUAL_REFRESH = "com.ruuvi.station.widgets.complexWidget.MANUAL_REFRESH"

        fun updateComplexWidget(
            context: Context,
            appWidgetId: Int
        ) {
            val kodein: Kodein by kodein(context)
            val interactor: WidgetInteractor by kodein.instance()
            val preferencesInteractor: ComplexWidgetPreferencesInteractor by kodein.instance()

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val widgetSettings = preferencesInteractor.getComplexWidgetSettings(appWidgetId)
                    val cloudSensors = interactor.getCloudSensorsList()
                        .filter { cloudSensor -> widgetSettings.any { it.sensorId == cloudSensor.id } }
                    
                    val sensorsData = cloudSensors.map { sensor ->
                        val settings = widgetSettings.firstOrNull { it.sensorId == sensor.id }
                        interactor.getComplexWidgetData(sensor.id, settings)
                    }

                    val glanceId = try {
                        GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
                    } catch (e: Exception) {
                        null
                    }

                    if (glanceId != null) {
                        updateAppWidgetState(context, glanceId) { prefs ->
                            prefs[ComplexWidgetPrefKeys.data] = Gson().toJson(sensorsData)
                        }
                        ComplexWidgetGlanceWidget.update(context, glanceId)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "updateComplexWidget failed for Id $appWidgetId")
                }
            }
        }

        fun getUpdatePendingIntent(context: Context, appWidgetId: Int): PendingIntent {
            val updateIntent = Intent(context, ComplexWidgetProvider::class.java).apply {
                action = MANUAL_REFRESH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            return PendingIntent.getBroadcast(context, appWidgetId, updateIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun updateAll(context: Context) {
            val ids = getWidgetsIds(context)
            for (appWidgetId in ids) {
                updateComplexWidget(context, appWidgetId)
            }
        }

        private fun getWidgetsIds(context: Context): IntArray {
            val appWidgetManager =
                AppWidgetManager.getInstance(context)

            return appWidgetManager.getAppWidgetIds(ComponentName(context, ComplexWidgetProvider::class.java.name ))
        }
    }
}