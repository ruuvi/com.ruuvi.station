package com.ruuvi.station.widgets.update

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.google.gson.Gson
import com.ruuvi.station.widgets.domain.ComplexWidgetPreferencesInteractor
import com.ruuvi.station.widgets.domain.WidgetInteractor
import com.ruuvi.station.widgets.domain.WidgetPreferencesInteractor
import com.ruuvi.station.widgets.ui.complexWidget.ComplexWidgetGlanceWidget
import com.ruuvi.station.widgets.ui.complexWidget.ComplexWidgetPrefKeys
import com.ruuvi.station.widgets.ui.simpleWidget.SimpleWidgetGlanceWidget
import com.ruuvi.station.widgets.ui.simpleWidget.SimpleWidgetPrefKeys
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

class WidgetUpdater(
    private val simplePreferences: WidgetPreferencesInteractor,
    private val complexPreferences: ComplexWidgetPreferencesInteractor,
    private val widgetInteractor: WidgetInteractor,
) {
    private val gson = Gson()
    private val simpleWidgetLocks = ConcurrentHashMap<Int, Mutex>()
    private val complexWidgetLocks = ConcurrentHashMap<Int, Mutex>()

    suspend fun updateSimpleWidget(context: Context, appWidgetId: Int) =
        withContext(Dispatchers.IO) {
            simpleWidgetLocks.lockFor(appWidgetId).withLock {
                val applicationContext = context.applicationContext
                val sensorId = simplePreferences.getSimpleWidgetSensor(appWidgetId)
                val widgetType = simplePreferences.getSimpleWidgetType(appWidgetId)

                if (!sensorId.isNullOrEmpty()) {
                    val widgetData = widgetInteractor.getSimpleWidgetData(
                        sensorId = sensorId,
                        widgetType = widgetType,
                    )
                    val glanceId = getGlanceIdOrNull(
                        context = applicationContext,
                        appWidgetId = appWidgetId,
                        widgetName = "Simple",
                    ) ?: return@withLock

                    updateAppWidgetState(applicationContext, glanceId) { preferences ->
                        preferences[SimpleWidgetPrefKeys.sensorId] = sensorId
                        preferences[SimpleWidgetPrefKeys.displayName] =
                            widgetData?.displayName.orEmpty()
                        preferences[SimpleWidgetPrefKeys.sensorValue] =
                            widgetData?.sensorValue.orEmpty()
                        preferences[SimpleWidgetPrefKeys.unit] =
                            widgetData?.unit.orEmpty()
                        preferences[SimpleWidgetPrefKeys.measurementName] =
                            widgetData?.measurementName.orEmpty()
                        preferences[SimpleWidgetPrefKeys.updated] =
                            widgetData?.updated.orEmpty()
                        preferences[SimpleWidgetPrefKeys.measurementType] =
                            widgetType.code.toString()
                    }

                    SimpleWidgetGlanceWidget.update(applicationContext, glanceId)
                }
            }
        }

    suspend fun updateComplexWidget(context: Context, appWidgetId: Int) =
        withContext(Dispatchers.IO) {
            complexWidgetLocks.lockFor(appWidgetId).withLock {
                val applicationContext = context.applicationContext
                val widgetSettings = complexPreferences.getComplexWidgetSettings(appWidgetId)
                val cloudSensors = widgetInteractor.getCloudSensorsList()
                    .filter { cloudSensor ->
                        widgetSettings.any { setting -> setting.sensorId == cloudSensor.id }
                    }
                val sensorsData = cloudSensors.map { sensor ->
                    val settings = widgetSettings.firstOrNull { it.sensorId == sensor.id }
                    widgetInteractor.getComplexWidgetData(sensor.id, settings)
                }
                val glanceId = getGlanceIdOrNull(
                    context = applicationContext,
                    appWidgetId = appWidgetId,
                    widgetName = "Complex",
                ) ?: return@withLock

                updateAppWidgetState(applicationContext, glanceId) { preferences ->
                    preferences[ComplexWidgetPrefKeys.data] = gson.toJson(sensorsData)
                }
                ComplexWidgetGlanceWidget.update(applicationContext, glanceId)
            }
        }

    suspend fun updateSimpleWidgets(context: Context, appWidgetIds: IntArray) {
        updateWidgetsSequentially(
            appWidgetIds = appWidgetIds,
            update = { appWidgetId -> updateSimpleWidget(context, appWidgetId) },
            onFailure = { appWidgetId, error ->
                Timber.e(error, "Simple widget update failed for Id $appWidgetId")
            },
        )
    }

    suspend fun updateComplexWidgets(context: Context, appWidgetIds: IntArray) {
        updateWidgetsSequentially(
            appWidgetIds = appWidgetIds,
            update = { appWidgetId -> updateComplexWidget(context, appWidgetId) },
            onFailure = { appWidgetId, error ->
                Timber.e(error, "Complex widget update failed for Id $appWidgetId")
            },
        )
    }

    private suspend fun getGlanceIdOrNull(
        context: Context,
        appWidgetId: Int,
        widgetName: String,
    ) = try {
        GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
    } catch (error: IllegalArgumentException) {
        Timber.d("$widgetName widget $appWidgetId was deleted before its update")
        null
    }

    private fun ConcurrentHashMap<Int, Mutex>.lockFor(appWidgetId: Int): Mutex =
        getOrPut(appWidgetId) { Mutex() }
}

internal suspend fun updateWidgetsSequentially(
    appWidgetIds: IntArray,
    update: suspend (appWidgetId: Int) -> Unit,
    onFailure: (appWidgetId: Int, error: Exception) -> Unit,
) {
    for (appWidgetId in appWidgetIds) {
        try {
            update(appWidgetId)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            onFailure(appWidgetId, error)
        }
    }
}
