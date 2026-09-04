package com.ruuvi.station.widgets.update

import android.os.SystemClock
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ruuvi.station.widgets.ui.complexWidget.ComplexWidgetProvider
import com.ruuvi.station.widgets.ui.simpleWidget.SimpleWidget
import com.ruuvi.station.bluetooth.BluetoothInteractor
import com.ruuvi.station.widgets.domain.ComplexWidgetPreferencesInteractor
import com.ruuvi.station.widgets.domain.WidgetPreferencesInteractor
import com.ruuvi.station.widgets.domain.WidgetSensorSnapshotProvider
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
            val simplePreferences: WidgetPreferencesInteractor by kodein.instance()
            val complexPreferences: ComplexWidgetPreferencesInteractor by kodein.instance()
            val snapshotProvider: WidgetSensorSnapshotProvider by kodein.instance()

            val appWidgetIds = target.appWidgetId?.let { intArrayOf(it) }
                ?: installedWidgetIds(target.refreshType)

            if (appWidgetIds.isEmpty()) return Result.success()

            // Initial update with existing database data
            updateWidgets(widgetUpdater, target.refreshType, appWidgetIds)

            if (target.refreshTrigger == WidgetRefreshTrigger.MANUAL) {
                scanUntilRelevantReadingsStoredOrTimeout(
                    bluetoothInteractor = bluetoothInteractor,
                    snapshotProvider = snapshotProvider,
                    simplePreferences = simplePreferences,
                    complexPreferences = complexPreferences,
                    refreshType = target.refreshType,
                    appWidgetIds = appWidgetIds,
                )
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

    private suspend fun scanUntilRelevantReadingsStoredOrTimeout(
        bluetoothInteractor: BluetoothInteractor,
        snapshotProvider: WidgetSensorSnapshotProvider,
        simplePreferences: WidgetPreferencesInteractor,
        complexPreferences: ComplexWidgetPreferencesInteractor,
        refreshType: WidgetRefreshType,
        appWidgetIds: IntArray,
    ) {
        val trackedSensors = resolveTrackedLocalSensorIds(
            snapshotProvider = snapshotProvider,
            simplePreferences = simplePreferences,
            complexPreferences = complexPreferences,
            refreshType = refreshType,
            appWidgetIds = appWidgetIds,
        ).associateWith { sensorId ->
            currentLocalReadingTimestampMillis(snapshotProvider, sensorId)
        }

        try {
            bluetoothInteractor.startScan(true)
            val deadlineMillis = SystemClock.elapsedRealtime() + SCAN_DURATION_MS
            while (SystemClock.elapsedRealtime() < deadlineMillis) {
                if (
                    trackedSensors.isNotEmpty() &&
                    trackedSensors.all { (sensorId, initialTimestamp) ->
                        hasNewLocalReading(
                            snapshotProvider = snapshotProvider,
                            sensorId = sensorId,
                            initialTimestamp = initialTimestamp,
                        )
                    }
                ) {
                    break
                }
                val remainingMillis = (deadlineMillis - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                delay(minOf(SCAN_POLL_INTERVAL_MS, remainingMillis))
            }
        } finally {
            bluetoothInteractor.stopScanningFromBackground()
        }
    }

    private fun resolveTrackedLocalSensorIds(
        snapshotProvider: WidgetSensorSnapshotProvider,
        simplePreferences: WidgetPreferencesInteractor,
        complexPreferences: ComplexWidgetPreferencesInteractor,
        refreshType: WidgetRefreshType,
        appWidgetIds: IntArray,
    ): List<String> =
        when (refreshType) {
            WidgetRefreshType.SIMPLE ->
                appWidgetIds.asSequence()
                    .mapNotNull { appWidgetId -> simplePreferences.getSimpleWidgetSensor(appWidgetId) }
                    .toList()
            WidgetRefreshType.COMPLEX ->
                appWidgetIds.flatMap { appWidgetId ->
                    complexPreferences.getComplexWidgetSettings(appWidgetId).map { it.sensorId }
                }
        }
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
            .filter { sensorId ->
                val sensor = snapshotProvider.getFavoriteSensor(sensorId)
                sensor != null && sensor.networkLastSync == null
            }

    private fun hasNewLocalReading(
        snapshotProvider: WidgetSensorSnapshotProvider,
        sensorId: String,
        initialTimestamp: Long?,
    ): Boolean {
        val latestTimestamp = currentLocalReadingTimestampMillis(snapshotProvider, sensorId)
            ?: return false
        return initialTimestamp == null || latestTimestamp > initialTimestamp
    }

    private fun currentLocalReadingTimestampMillis(
        snapshotProvider: WidgetSensorSnapshotProvider,
        sensorId: String,
    ): Long? {
        val sensor = snapshotProvider.getFavoriteSensor(sensorId) ?: return null
        return snapshotProvider.mapLocalSnapshot(sensor)?.timestampEpochMillis
    }

    companion object {
        private const val SCAN_DURATION_MS = 5000L
        private const val SCAN_POLL_INTERVAL_MS = 200L
    }
}
