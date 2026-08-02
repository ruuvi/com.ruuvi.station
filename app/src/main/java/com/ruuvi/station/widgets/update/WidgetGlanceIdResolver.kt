package com.ruuvi.station.widgets.update

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CancellationException
import timber.log.Timber

internal suspend fun resolveAppWidgetId(
    context: Context,
    glanceId: GlanceId,
    widgetName: String,
): Int? = try {
    GlanceAppWidgetManager(context.applicationContext).getAppWidgetId(glanceId)
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (error: IllegalArgumentException) {
    Timber.d("$widgetName widget was deleted before its refresh action")
    null
}
