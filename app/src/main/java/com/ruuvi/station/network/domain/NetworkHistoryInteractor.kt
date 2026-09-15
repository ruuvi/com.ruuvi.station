package com.ruuvi.station.network.domain

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.app.preferences.GlobalSettings
import com.ruuvi.station.bluetooth.BluetoothLibrary
import com.ruuvi.station.bluetooth.DefaultOnTagFoundListener.Companion.legacyAirDataformats
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.history.HistoryRange
import com.ruuvi.station.history.uncoveredHistory
import com.ruuvi.station.network.data.request.GetSensorDataRequest
import com.ruuvi.station.network.data.request.SensorDataMode
import com.ruuvi.station.network.data.request.SensorDenseRequest
import com.ruuvi.station.network.data.request.SortMode
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.Date
import java.util.Locale

data class HistorySyncState(
    val sensorId: String? = null,
    val loading: Boolean = false,
    val failed: Boolean = false,
    val restricted: Boolean = false,
    val cloudHistoryDays: Int? = null
)

/** Called exclusively by the resumed, selected full history view. */
class NetworkHistoryInteractor(
    private val network: RuuviNetworkInteractor,
    private val settings: SensorSettingsRepository,
    private val history: SensorHistoryRepository,
    private val preferences: PreferencesRepository,
    private val now: () -> Long = System::currentTimeMillis
) {
    private val _state = MutableStateFlow(HistorySyncState())
    val state = _state.asStateFlow()
    private val jobs = mutableSetOf<Job>()
    private val syncMutex = Mutex()

    suspend fun syncHistory(sensorId: String, range: HistoryRange, force: Boolean = false, revalidateHistorical: Boolean = true) {
        val job = currentCoroutineContext().job
        synchronized(jobs) { jobs.add(job) }
        try {
            syncMutex.withLock {
                withContext(Dispatchers.IO) { fetch(sensorId, range, force, revalidateHistorical) }
            }
        } finally {
            synchronized(jobs) { jobs.remove(job) }
        }
    }

    private suspend fun fetch(sensorId: String, selected: HistoryRange, force: Boolean, revalidateHistorical: Boolean) {
        _state.value = HistorySyncState(sensorId)
        if (!network.signedIn || selected.isEmpty) return
        val sensor = settings.getSensorSettings(sensorId) ?: return
        if (!sensor.networkSensor) return
        val account = network.getEmail()?.lowercase(Locale.ROOT) ?: return
        val backend = if (preferences.isDevServerEnabled()) "testnet" else "production"
        val generation = history.generation(sensorId)
        _state.value = HistorySyncState(sensorId, loading = true)
        try {
            // Older installations don't yet have per-sensor subscription history limits.
            val days = sensor.cloudHistoryDays ?: run {
                val response = network.getSensorDenseLastData(SensorDenseRequest(sensor = sensorId, sharedToMe = true, measurements = true))
                check(response?.isSuccess() == true) { "Cannot read sensor subscription" }
                val info = response.data?.sensors?.firstOrNull { it.sensor == sensorId }
                    ?: error("Sensor is no longer accessible")
                currentCoroutineContext().ensureActive()
                check(history.generation(sensorId) == generation) { "History was cleared" }
                settings.updateCloudHistoryDays(sensorId, info.subscription.maxHistoryDays)
                info.subscription.maxHistoryDays
            }
            val current = now()
            val allowed = HistoryRange.retained(current).intersect(
                HistoryRange(current - days.coerceIn(0, GlobalSettings.historyLengthDays) * 86_400_000L, current)
            )
            val range = selected.intersect(allowed)
            val restricted = days <= 0 || (days < GlobalSettings.historyLengthDays &&
                selected.startMillis < allowed.startMillis - LIVE_REFRESH_MILLIS)
            _state.value = HistorySyncState(sensorId, loading = !range.isEmpty, restricted = restricted, cloudHistoryDays = days)
            if (range.isEmpty) return

            val coverage = if (force) emptyList() else history.coverage(
                sensorId, account, backend, if (revalidateHistorical) current - SensorHistoryRepository.CACHE_FRESHNESS_MILLIS else Long.MIN_VALUE
            )
            // Recheck a minute at the live edge to pick up late arrivals, always within the selection.
            val stableEnd = if (range.endExclusiveMillis >= current - LIVE_REFRESH_MILLIS)
                maxOf(range.startMillis, range.endExclusiveMillis - LIVE_REFRESH_MILLIS)
            else range.endExclusiveMillis
            val stableCoverage = coverage.map { it.intersect(HistoryRange(range.startMillis, stableEnd)) }
            for (missing in uncoveredHistory(range, stableCoverage)) {
                fetchRange(sensorId, missing, account, backend, generation)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.w(error, "History sync failed for %s", sensorId)
            _state.value = _state.value.copy(failed = true)
        } finally {
            _state.value = _state.value.copy(loading = false)
        }
    }

    private suspend fun fetchRange(sensorId: String, range: HistoryRange, account: String, backend: String, generation: Long) {
        var cursor = range.startMillis
        val untilSeconds = (range.endExclusiveMillis - 1) / 1000
        while (cursor < range.endExclusiveMillis) {
            currentCoroutineContext().ensureActive()
            val sinceSeconds = (cursor + 999) / 1000
            // There are no whole-second cloud measurements in this sub-second remainder.
            if (sinceSeconds > untilSeconds) {
                check(history.saveCloudPage(sensorId, emptyList(), HistoryRange(cursor, range.endExclusiveMillis), account, backend, generation))
                break
            }
            val response = network.getSensorData(GetSensorDataRequest(
                sensor = sensorId, since = Date(sinceSeconds * 1000), until = Date(untilSeconds * 1000),
                sort = SortMode.ASCENDING, limit = PAGE_SIZE, mode = SensorDataMode.MIXED
            ))
            currentCoroutineContext().ensureActive()
            check(network.getEmail()?.lowercase(Locale.ROOT) == account &&
                (if (preferences.isDevServerEnabled()) "testnet" else "production") == backend) { "Cloud account changed" }
            check(response?.isSuccess() == true) { "Cloud history request failed" }
            val body = response.data ?: error("Missing history response")
            check(body.sensor == sensorId) { "Unexpected sensor in history response" }
            val measurements = body.measurements.filter { it.timestamp in sinceSeconds..untilSeconds }
            check(body.measurements.isEmpty() || measurements.isNotEmpty()) { "History cursor did not advance" }
            val next = measurements.maxOfOrNull { (it.timestamp + 1) * 1000 }
                ?.coerceAtMost(range.endExclusiveMillis) ?: range.endExclusiveMillis
            check(next > cursor) { "History cursor did not advance" }
            val readings = measurements.mapNotNull { measurement ->
                try {
                    if (measurement.data.isEmpty()) null else TagSensorReading(
                        BluetoothLibrary.decode(sensorId, measurement.data, measurement.rssi), Date(measurement.timestamp * 1000)
                    ).takeUnless { it.dataFormat in legacyAirDataformats }
                } catch (error: Exception) {
                    Timber.w(error, "Cannot decode history measurement")
                    null
                }
            }
            currentCoroutineContext().ensureActive()
            check(history.saveCloudPage(sensorId, readings, HistoryRange(cursor, next), account, backend, generation)) { "History was cleared" }
            cursor = next
        }
    }

    fun cancel() {
        synchronized(jobs) { jobs.toList() }.forEach { it.cancel() }
    }

    suspend fun cancelAndJoin() {
        val running = synchronized(jobs) { jobs.toList() }
        running.forEach { it.cancel() }
        running.joinAll()
        _state.value = HistorySyncState()
    }

    companion object {
        const val PAGE_SIZE = 5000
        const val LIVE_REFRESH_MILLIS = 60_000L
    }
}
