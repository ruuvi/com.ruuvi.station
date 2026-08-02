package com.ruuvi.station.widgets.update

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

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

internal data class WidgetRefreshTarget(
    val refreshType: WidgetRefreshType,
    val appWidgetId: Int? = null,
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
        WidgetRefreshScheduler.APP_WIDGET_ID_KEY to appWidgetId,
    )

    companion object {
        fun fromInputData(data: Data): WidgetRefreshTarget? {
            val refreshType = WidgetRefreshType.fromInputValue(
                data.getString(WidgetRefreshScheduler.WIDGET_REFRESH_TYPE_KEY),
            ) ?: return null

            return when (data.getString(WidgetRefreshScheduler.WIDGET_REFRESH_SCOPE_KEY)) {
                WIDGET_REFRESH_SCOPE_ALL -> WidgetRefreshTarget(refreshType)
                WIDGET_REFRESH_SCOPE_SINGLE -> {
                    val appWidgetId = data.getInt(
                        WidgetRefreshScheduler.APP_WIDGET_ID_KEY,
                        INVALID_APP_WIDGET_ID,
                    )
                    if (appWidgetId > 0) {
                        WidgetRefreshTarget(refreshType, appWidgetId)
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
    fun enqueueSimpleRefreshAll(context: Context) {
        enqueue(
            WorkManager.getInstance(context.applicationContext),
            WidgetRefreshTarget(WidgetRefreshType.SIMPLE),
        )
    }

    fun enqueueComplexRefreshAll(context: Context) {
        enqueue(
            WorkManager.getInstance(context.applicationContext),
            WidgetRefreshTarget(WidgetRefreshType.COMPLEX),
        )
    }

    fun enqueueSimpleRefresh(context: Context, appWidgetId: Int) {
        enqueue(
            WorkManager.getInstance(context.applicationContext),
            WidgetRefreshTarget(WidgetRefreshType.SIMPLE, appWidgetId),
        )
    }

    fun enqueueComplexRefresh(context: Context, appWidgetId: Int) {
        enqueue(
            WorkManager.getInstance(context.applicationContext),
            WidgetRefreshTarget(WidgetRefreshType.COMPLEX, appWidgetId),
        )
    }

    internal fun enqueue(workManager: WorkManager, target: WidgetRefreshTarget) {
        workManager.enqueueUniqueWork(
            target.uniqueWorkName,
            ExistingWorkPolicy.KEEP,
            createRequest(target),
        )
    }

    internal fun createRequest(target: WidgetRefreshTarget): OneTimeWorkRequest {
        return OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
            .setInputData(target.toInputData())
            .addTag(WIDGET_REFRESH_WORK_TAG)
            .addTag(target.refreshType.workTag)
            .build()
    }

    internal const val WIDGET_REFRESH_TYPE_KEY = "widget_refresh_type"
    internal const val WIDGET_REFRESH_SCOPE_KEY = "widget_refresh_scope"
    internal const val APP_WIDGET_ID_KEY = "app_widget_id"
    internal const val WIDGET_REFRESH_WORK_TAG = "widget-refresh"
}
