package com.ruuvi.station.widgets.update

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ruuvi.station.widgets.ui.complexWidget.ComplexWidgetProvider
import com.ruuvi.station.widgets.ui.simpleWidget.SimpleWidget
import com.ruuvi.station.bluetooth.BluetoothInteractor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.kodein.di.Kodein
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import timber.log.Timber

class WidgetRefreshWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result {
        val target = WidgetRefreshTarget.fromInputData(inputData) ?: return Result.failure()

        return try {
            val kodein: Kodein by kodein(applicationContext)
            val widgetUpdater: WidgetUpdater by kodein.instance()
            val bluetoothInteractor: BluetoothInteractor by kodein.instance()

            val appWidgetIds = target.appWidgetId?.let { intArrayOf(it) }
                ?: installedWidgetIds(target.refreshType)

            if (appWidgetIds.isEmpty()) return Result.success()

            // Initial update with existing database data
            updateWidgets(widgetUpdater, target.refreshType, appWidgetIds)

            // Bluetooth scan for fresh data
            try {
                bluetoothInteractor.startScan(true)
                delay(SCAN_DURATION_MS)
            } finally {
                bluetoothInteractor.stopScanningFromBackground()
            }

            // Final update with potentially new data
            updateWidgets(widgetUpdater, target.refreshType, appWidgetIds)

            Result.success()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Timber.e(
                error,
                "Unable to run ${target.refreshType.inputValue} widget refresh work",
            )
            Result.failure()
        }
    }

    private suspend fun updateWidgets(
        widgetUpdater: WidgetUpdater,
        refreshType: WidgetRefreshType,
        appWidgetIds: IntArray
    ) {
        when (refreshType) {
            WidgetRefreshType.SIMPLE ->
                widgetUpdater.updateSimpleWidgets(applicationContext, appWidgetIds)
            WidgetRefreshType.COMPLEX ->
                widgetUpdater.updateComplexWidgets(applicationContext, appWidgetIds)
        }
    }

    private fun installedWidgetIds(refreshType: WidgetRefreshType): IntArray {
        val receiverClass = when (refreshType) {
            WidgetRefreshType.SIMPLE -> SimpleWidget::class.java
            WidgetRefreshType.COMPLEX -> ComplexWidgetProvider::class.java
        }
        return AppWidgetManager.getInstance(applicationContext).getAppWidgetIds(
            ComponentName(applicationContext, receiverClass),
        )
    }

    companion object {
        private const val SCAN_DURATION_MS = 5000L
    }
}
