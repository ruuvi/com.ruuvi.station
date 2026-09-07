package com.ruuvi.station.widgets.ui

import android.appwidget.AppWidgetManager
import android.content.Intent
import kotlinx.coroutines.CancellationException

internal fun widgetConfigurationResultIntent(appWidgetId: Int): Intent =
    Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

internal suspend fun runInitialWidgetUpdate(
    update: suspend () -> Unit,
    onFailure: (Exception) -> Unit,
): Boolean = try {
    update()
    true
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (error: Exception) {
    onFailure(error)
    false
}
