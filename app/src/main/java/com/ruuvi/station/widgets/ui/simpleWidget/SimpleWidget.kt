package com.ruuvi.station.widgets.ui.simpleWidget

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.ruuvi.station.widgets.domain.WidgetInteractor
import com.ruuvi.station.widgets.domain.WidgetPreferencesInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kodein.di.Kodein
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import timber.log.Timber

class SimpleWidget : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = SimpleWidgetGlanceWidget

    @SuppressLint("MissingSuperCall")
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        launchBroadcastWork {
            updateWidgets(context, appWidgetIds)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        try {
            val preferences = WidgetPreferencesInteractor(context.applicationContext)
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
                Timber.e(error, "Simple widget broadcast update failed")
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val REFRESH_WIDGET =
            "com.ruuvi.station.widgets.ui.simpleWidget.REFRESH_WIDGET"
        private const val REFRESH_ALL_WIDGETS =
            "com.ruuvi.station.widgets.ui.simpleWidget.REFRESH_ALL_WIDGETS"
        private val SENSOR_ID_KEY = ActionParameters.Key<String>("sensor_id")

        suspend fun updateSimpleWidget(context: Context, appWidgetId: Int) =
            withContext(Dispatchers.IO) {
                val applicationContext = context.applicationContext
                val kodein: Kodein by kodein(applicationContext)

                val preferences: WidgetPreferencesInteractor by kodein.instance()
                val widgetInteractor: WidgetInteractor by kodein.instance()

                val sensorId = preferences.getSimpleWidgetSensor(appWidgetId)
                val widgetType = preferences.getSimpleWidgetType(appWidgetId)

                if (!sensorId.isNullOrEmpty()) {
                    val widgetData = widgetInteractor.getSimpleWidgetData(
                        sensorId = sensorId,
                        widgetType = widgetType
                    )

                    val glanceId = try {
                        GlanceAppWidgetManager(applicationContext).getGlanceIdBy(appWidgetId)
                    } catch (error: IllegalArgumentException) {
                        Timber.d("Simple widget $appWidgetId was deleted before its update")
                        return@withContext
                    }

                    updateAppWidgetState(applicationContext, glanceId) { prefs ->
                        prefs[SimpleWidgetPrefKeys.sensorId] = sensorId
                        prefs[SimpleWidgetPrefKeys.displayName] =
                            widgetData?.displayName.orEmpty()
                        prefs[SimpleWidgetPrefKeys.sensorValue] =
                            widgetData?.sensorValue.orEmpty()
                        prefs[SimpleWidgetPrefKeys.unit] =
                            widgetData?.unit.orEmpty()
                        prefs[SimpleWidgetPrefKeys.measurementName] =
                            widgetData?.measurementName.orEmpty()
                        prefs[SimpleWidgetPrefKeys.updated] =
                            widgetData?.updated.orEmpty()
                        prefs[SimpleWidgetPrefKeys.measurementType] =
                            widgetType.code.toString()
                    }

                    SimpleWidgetGlanceWidget.update(applicationContext, glanceId)
                }
            }

        suspend fun updateAll(context: Context) = withContext(Dispatchers.IO) {
            updateWidgets(context, getSimpleWidgetsIds(context))
        }

        private suspend fun updateWidgets(context: Context, appWidgetIds: IntArray) {
            for (appWidgetId in appWidgetIds) {
                updateWidgetSafely(context, appWidgetId)
            }
        }

        private suspend fun updateWidgetSafely(context: Context, appWidgetId: Int) {
            try {
                updateSimpleWidget(context, appWidgetId)
            } catch (error: Exception) {
                Timber.e(error, "Simple widget update failed for Id $appWidgetId")
            }
        }

        fun getUpdatePendingIntent(context: Context, appWidgetId: Int): PendingIntent {
            val updateIntent = Intent(context, SimpleWidget::class.java).apply {
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
