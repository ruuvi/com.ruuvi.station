package com.ruuvi.station.widgets.ui.simpleWidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ruuvi.station.widgets.domain.WidgetPreferencesInteractor
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

// Legacy receiver for old SimpleWidget instances. Migrates them to SimpleLargeWidgetReceiver on app update.
class SimpleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SimpleWidgetGlanceWidget

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        // Migrate existing old widgets when app is updated
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            migrateOldWidgetsToGlanceWidgets(context)
        }
    }

    private fun migrateOldWidgetsToGlanceWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val oldWidgetIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, SimpleWidgetReceiver::class.java)
        )

        if (oldWidgetIds.isNotEmpty()) {
            val prefs = WidgetPreferencesInteractor(context)

            MainScope().launch {
                try {
                    val glanceManager = GlanceAppWidgetManager(context)
                    val glanceIds = glanceManager.getGlanceIds(SimpleWidgetGlanceWidget::class.java)

                    if (glanceIds.isEmpty()) {
                        return@launch
                    }

                    for (oldWidgetId in oldWidgetIds) {
                        val sensorId = prefs.getSimpleWidgetSensor(oldWidgetId) ?: continue
                        val widgetType = prefs.getSimpleWidgetType(oldWidgetId)

                        val glanceId = glanceIds[0]

                        updateAppWidgetState(context, glanceId) { prefState ->
                            prefState[stringPreferencesKey("sensor_id")] = sensorId
                            prefState[stringPreferencesKey("measurement_type")] = widgetType.code.toString()
                        }

                        SimpleWidgetGlanceWidget.update(context, glanceId)
                    }
                } catch (e: Exception) {
                    // Migration failed
                }
            }
        }
    }
}
