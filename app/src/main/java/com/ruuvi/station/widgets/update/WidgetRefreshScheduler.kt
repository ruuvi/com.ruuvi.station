package com.ruuvi.station.widgets.update

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ruuvi.station.widgets.domain.ComplexWidgetPreferencesInteractor
import com.ruuvi.station.widgets.domain.WidgetPreferencesInteractor
import com.ruuvi.station.widgets.ui.complexWidget.ComplexWidgetProvider
import com.ruuvi.station.widgets.ui.simpleWidget.SimpleWidget
import org.kodein.di.DI
import org.kodein.di.android.closestDI
import org.kodein.di.instance
import java.util.Locale
import java.util.concurrent.TimeUnit

internal enum class WidgetRefreshType(
    val inputValue: String,
    val uniqueWorkName: String,
    val workTag: String,
) {
    SIMPLE(
        inputValue = "simple",
        uniqueWorkName = "widget-refresh-all:simple",
        workTag = "widget-refresh:simple",
    ),
    COMPLEX(
        inputValue = "complex",
        uniqueWorkName = "widget-refresh-all:complex",
        workTag = "widget-refresh:complex",
    );

    companion object {
        fun fromInputValue(value: String?): WidgetRefreshType? =
            entries.firstOrNull { it.inputValue == value }
    }
}

internal enum class WidgetRefreshTrigger(val inputValue: String) {
    AUTOMATIC("automatic"),
    MANUAL("manual");

    companion object {
        fun fromInputValue(value: String?): WidgetRefreshTrigger? =
            entries.firstOrNull { it.inputValue == value }
    }
}

internal data class WidgetRefreshTarget(
    val refreshType: WidgetRefreshType,
    val appWidgetId: Int? = null,
    val refreshTrigger: WidgetRefreshTrigger = WidgetRefreshTrigger.AUTOMATIC,
) {
    init {
        require(appWidgetId == null || appWidgetId > 0) {
            "A widget-specific refresh requires a positive app widget ID"
        }
    }

    val uniqueWorkName: String
        get() = if (appWidgetId == null) {
            refreshType.uniqueWorkName
        } else {
            "widget-refresh:${refreshType.inputValue}:$appWidgetId"
        }

    val isWidgetSpecific: Boolean
        get() = appWidgetId != null

    fun toInputData(): Data = workDataOf(
        WidgetRefreshScheduler.WIDGET_REFRESH_TYPE_KEY to refreshType.inputValue,
        WidgetRefreshScheduler.WIDGET_REFRESH_SCOPE_KEY to if (isWidgetSpecific) {
            WIDGET_REFRESH_SCOPE_SINGLE
        } else {
            WIDGET_REFRESH_SCOPE_ALL
        },
        WidgetRefreshScheduler.WIDGET_REFRESH_TRIGGER_KEY to refreshTrigger.inputValue,
        WidgetRefreshScheduler.APP_WIDGET_ID_KEY to appWidgetId,
    )

    companion object {
        fun fromInputData(data: Data): WidgetRefreshTarget? {
            val refreshType = WidgetRefreshType.fromInputValue(
                data.getString(WidgetRefreshScheduler.WIDGET_REFRESH_TYPE_KEY),
            ) ?: return null
            val refreshTrigger = data.getString(
                WidgetRefreshScheduler.WIDGET_REFRESH_TRIGGER_KEY,
            )?.let { WidgetRefreshTrigger.fromInputValue(it) ?: return null }
                ?: WidgetRefreshTrigger.AUTOMATIC

            return when (data.getString(WidgetRefreshScheduler.WIDGET_REFRESH_SCOPE_KEY)) {
                WIDGET_REFRESH_SCOPE_ALL -> WidgetRefreshTarget(
                    refreshType = refreshType,
                    refreshTrigger = refreshTrigger,
                )
                WIDGET_REFRESH_SCOPE_SINGLE -> {
                    val appWidgetId = data.getInt(
                        WidgetRefreshScheduler.APP_WIDGET_ID_KEY,
                        INVALID_APP_WIDGET_ID,
                    )
                    if (appWidgetId > 0) {
                        WidgetRefreshTarget(
                            refreshType = refreshType,
                            appWidgetId = appWidgetId,
                            refreshTrigger = refreshTrigger,
                        )
                    } else {
                        null
                    }
                }
                else -> null
            }
        }

        private const val INVALID_APP_WIDGET_ID = -1
        private const val WIDGET_REFRESH_SCOPE_ALL = "all"
        private const val WIDGET_REFRESH_SCOPE_SINGLE = "single"
    }
}

object WidgetRefreshScheduler {
    internal fun enqueueSimpleRefreshAll(
        context: Context,
        refreshTrigger: WidgetRefreshTrigger = WidgetRefreshTrigger.AUTOMATIC,
    ) {
        enqueue(
            WorkManager.getInstance(context.applicationContext),
            WidgetRefreshTarget(
                refreshType = WidgetRefreshType.SIMPLE,
                refreshTrigger = refreshTrigger,
            ),
        )
    }

    internal fun enqueueComplexRefreshAll(
        context: Context,
        refreshTrigger: WidgetRefreshTrigger = WidgetRefreshTrigger.AUTOMATIC,
    ) {
        enqueue(
            WorkManager.getInstance(context.applicationContext),
            WidgetRefreshTarget(
                refreshType = WidgetRefreshType.COMPLEX,
                refreshTrigger = refreshTrigger,
            ),
        )
    }

    internal fun enqueueSimpleRefreshForSensor(
        context: Context,
        sensorId: String,
        refreshTrigger: WidgetRefreshTrigger = WidgetRefreshTrigger.AUTOMATIC,
    ) {
        val normalizedSensorId = normalizeSensorId(sensorId) ?: return
        val applicationContext = context.applicationContext
        val appWidgetIds = installedWidgetIds(applicationContext, SimpleWidget::class.java)
        if (appWidgetIds.isEmpty()) return

        val di: DI by closestDI(applicationContext)
        val simplePreferences: WidgetPreferencesInteractor by di.instance()
        val matchingWidgetIds = matchingSimpleWidgetIdsBySensor(
            appWidgetIds = appWidgetIds,
            sensorId = normalizedSensorId,
            sensorIdForWidget = simplePreferences::getSimpleWidgetSensor,
        )
        if (matchingWidgetIds.isEmpty()) return

        val workManager = WorkManager.getInstance(applicationContext)
        matchingWidgetIds.forEach { appWidgetId ->
            enqueue(
                workManager,
                WidgetRefreshTarget(
                    refreshType = WidgetRefreshType.SIMPLE,
                    appWidgetId = appWidgetId,
                    refreshTrigger = refreshTrigger,
                ),
            )
        }
    }

    internal fun enqueueComplexRefreshForSensor(
        context: Context,
        sensorId: String,
        refreshTrigger: WidgetRefreshTrigger = WidgetRefreshTrigger.AUTOMATIC,
    ) {
        val normalizedSensorId = normalizeSensorId(sensorId) ?: return
        val applicationContext = context.applicationContext
        val appWidgetIds = installedWidgetIds(applicationContext, ComplexWidgetProvider::class.java)
        if (appWidgetIds.isEmpty()) return

        val di: DI by closestDI(applicationContext)
        val complexPreferences: ComplexWidgetPreferencesInteractor by di.instance()
        val matchingWidgetIds = matchingComplexWidgetIdsBySensor(
            appWidgetIds = appWidgetIds,
            sensorId = normalizedSensorId,
            sensorIdsForWidget = { appWidgetId ->
                complexPreferences.getComplexWidgetSettings(appWidgetId).map { it.sensorId }
            },
        )
        if (matchingWidgetIds.isEmpty()) return

        val workManager = WorkManager.getInstance(applicationContext)
        matchingWidgetIds.forEach { appWidgetId ->
            enqueue(
                workManager,
                WidgetRefreshTarget(
                    refreshType = WidgetRefreshType.COMPLEX,
                    appWidgetId = appWidgetId,
                    refreshTrigger = refreshTrigger,
                ),
            )
        }
    }

    fun enqueueSimpleRefresh(context: Context, appWidgetId: Int) {
        enqueue(
            WorkManager.getInstance(context.applicationContext),
            WidgetRefreshTarget(
                refreshType = WidgetRefreshType.SIMPLE,
                appWidgetId = appWidgetId,
                refreshTrigger = WidgetRefreshTrigger.MANUAL,
            ),
        )
    }

    fun enqueueComplexRefresh(context: Context, appWidgetId: Int) {
        enqueue(
            WorkManager.getInstance(context.applicationContext),
            WidgetRefreshTarget(
                refreshType = WidgetRefreshType.COMPLEX,
                appWidgetId = appWidgetId,
                refreshTrigger = WidgetRefreshTrigger.MANUAL,
            ),
        )
    }

    internal fun enqueue(workManager: WorkManager, target: WidgetRefreshTarget) {
        workManager.enqueueUniqueWork(
            target.uniqueWorkName,
            existingWorkPolicy(target),
            createRequest(target),
        )
    }

    private fun existingWorkPolicy(target: WidgetRefreshTarget): ExistingWorkPolicy =
        if (target.refreshTrigger == WidgetRefreshTrigger.MANUAL) {
            ExistingWorkPolicy.REPLACE
        } else {
            ExistingWorkPolicy.KEEP
        }

    internal fun createRequest(target: WidgetRefreshTarget): OneTimeWorkRequest {
        val builder = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
            .setInputData(target.toInputData())
            .addTag(WIDGET_REFRESH_WORK_TAG)
            .addTag(target.refreshType.workTag)
        if (target.refreshTrigger == WidgetRefreshTrigger.AUTOMATIC && target.isWidgetSpecific) {
            builder.setInitialDelay(WIDGET_SPECIFIC_COALESCE_WINDOW_SECONDS, TimeUnit.SECONDS)
        }
        return builder.build()
    }

    private fun installedWidgetIds(context: Context, widgetReceiver: Class<*>): IntArray =
        AppWidgetManager.getInstance(context).getAppWidgetIds(
            ComponentName(context, widgetReceiver),
        )

    internal fun matchingSimpleWidgetIdsBySensor(
        appWidgetIds: IntArray,
        sensorId: String,
        sensorIdForWidget: (appWidgetId: Int) -> String?,
    ): IntArray {
        val normalizedSensorId = normalizeSensorId(sensorId) ?: return intArrayOf()
        return appWidgetIds.filter { appWidgetId ->
            normalizeSensorId(sensorIdForWidget(appWidgetId)) == normalizedSensorId
        }.toIntArray()
    }

    internal fun matchingComplexWidgetIdsBySensor(
        appWidgetIds: IntArray,
        sensorId: String,
        sensorIdsForWidget: (appWidgetId: Int) -> List<String>,
    ): IntArray {
        val normalizedSensorId = normalizeSensorId(sensorId) ?: return intArrayOf()
        return appWidgetIds.filter { appWidgetId ->
            sensorIdsForWidget(appWidgetId).any { configuredSensorId ->
                normalizeSensorId(configuredSensorId) == normalizedSensorId
            }
        }.toIntArray()
    }

    private fun normalizeSensorId(sensorId: String?): String? =
        sensorId?.trim()
            ?.takeIf(String::isNotEmpty)
            ?.lowercase(Locale.ROOT)

    internal const val WIDGET_REFRESH_TYPE_KEY = "widget_refresh_type"
    internal const val WIDGET_REFRESH_SCOPE_KEY = "widget_refresh_scope"
    internal const val WIDGET_REFRESH_TRIGGER_KEY = "widget_refresh_trigger"
    internal const val APP_WIDGET_ID_KEY = "app_widget_id"
    internal const val WIDGET_REFRESH_WORK_TAG = "widget-refresh"
    private const val WIDGET_SPECIFIC_COALESCE_WINDOW_SECONDS = 2L
}
