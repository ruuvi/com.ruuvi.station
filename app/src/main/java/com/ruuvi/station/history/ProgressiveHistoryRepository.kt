package com.ruuvi.station.history

import com.raizlabs.android.dbflow.sql.language.SQLite
import com.ruuvi.station.database.tables.SensorSettings_Table
import android.content.Context
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.bluetooth.BluetoothLibrary
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.feature.data.FeatureFlag
import com.ruuvi.station.feature.domain.RuntimeBehavior
import com.ruuvi.station.network.domain.NetworkTokenRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date

class ProgressiveHistoryRepository(
    context: Context,
    private val tokens: NetworkTokenRepository,
    private val preferences: PreferencesRepository,
    private val settings: SensorSettingsRepository,
    private val local: SensorHistoryRepository,
    private val flags: RuntimeBehavior,
    private val transport: HistoryTransport = HistoryTransport(),
) {
    data class Access(val sensor: String, val scope: String, val endpoint: String, val days: Int,
                      val temperature: Double, val humidity: Double, val pressure: Double) {
        val revision get() = "$temperature/$humidity/$pressure"
        fun calibrate(reading: TagSensorReading) = reading.copy(
            temperature = reading.temperature?.plus(temperature), humidity = reading.humidity?.plus(humidity),
            pressure = reading.pressure?.plus(pressure))
    }
    private val cache = HistoryPageCache(File(context.noBackupFilesDir, "RuuviGraphHistory-v4"),
        legacyRoot = File(context.cacheDir, "RuuviGraphHistory-v3"))
    val previews get() = cache.previews
    private val lastAllowanceCheck = mutableMapOf<String, Long>()
    private val pruningMutex = kotlinx.coroutines.sync.Mutex()
    private var lastCachePrune = 0L
    private var lastRetention: Map<String, Int?> = emptyMap()
    suspend fun pruneCache() = withContext(Dispatchers.IO) {
        pruningMutex.lock()
        try {
            val policies = SQLite.select().from(SensorSettings::class.java).queryList()
                .associate { it.id to HistoryRetention.days(it.networkSensor, it.maxHistoryDays) }
            val now = System.currentTimeMillis()
            if (policies != lastRetention || now - lastCachePrune >= 3_600_000) {
                for ((sensor, days) in policies) if (days != null)
                    cache.prune(sensor, now / 1000 - days.toLong() * 86400)
                lastRetention = policies; lastCachePrune = now
            }
        } catch (_: Exception) {
            currentCoroutineContext().ensureActive()
            timber.log.Timber.w("History cache cleanup failed; will retry")
        } finally { pruningMutex.unlock() }
    }
    fun enabled() = flags.isFeatureEnabled(FeatureFlag.LONGER_HISTORY)
    fun presets() = flags.isFeatureEnabled(FeatureFlag.HISTORY_DURATION_PRESETS)
    private fun endpoint() = if (preferences.isDevServerEnabled()) "https://testnet.ruuvi.com/" else "https://network.ruuvi.com/"
    private fun scope(): String? = tokens.getTokenInfo()?.let { HistoryPageCache.hash("${it.email.lowercase()}|${it.token}|${endpoint()}") }
    private fun authorize(access: Access) {
        if (!enabled() || scope() != access.scope || settings.getSensorSettings(access.sensor)?.networkSensor != true) throw HistoryFailure.Access()
    }
    suspend fun access(sensor: String, cachedFirst: Boolean = false): Access = withContext(Dispatchers.IO) {
        if (!enabled()) throw HistoryFailure.Access()
        val token = tokens.getTokenInfo() ?: throw HistoryFailure.Access()
        val scope = scope() ?: throw HistoryFailure.Access()
        var configuration = settings.getSensorSettings(sensor)?.takeIf { it.networkSensor } ?: throw HistoryFailure.Access()
        val key = HistoryPageCache.hash("$scope|$sensor")
        var days = configuration.maxHistoryDays
        val now = System.currentTimeMillis()
        if (days == null || (!cachedFirst && now - (lastAllowanceCheck[key] ?: 0) >= 60000)) {
            try {
                val latest = transport.allowance(sensor, endpoint(), token.token)
                if (scope() != scope) throw HistoryFailure.Access()
                days = latest
                SQLite.update(SensorSettings::class.java).set(SensorSettings_Table.maxHistoryDays.eq(latest))
                    .where(SensorSettings_Table.id.eq(sensor)).execute()
                configuration = settings.getSensorSettings(sensor)?.takeIf { it.networkSensor } ?: throw HistoryFailure.Access()
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                if (days == null || error is HistoryFailure.Access || error is HistoryFailure.Http && error.status in listOf(401, 403)) throw error
                // Existing owner allowance permits an offline cache preview; request authorization still applies to every page.
            }
            if (lastAllowanceCheck.size >= 500) lastAllowanceCheck.clear()
            lastAllowanceCheck[key] = now
        }
        if (!cachedFirst) { pruneCache(); local.schedulePruning() }
        Access(sensor, scope, endpoint(), (days ?: 0).coerceIn(0, HistoryWindow.MAX_DAYS),
            configuration.temperatureOffset ?: 0.0, configuration.humidityOffset ?: 0.0, configuration.pressureOffset ?: 0.0)
    }
    private fun stream(access: Access): HistoryStream = HistoryStream(cache, { query ->
        authorize(access)
        val token = tokens.getTokenInfo()?.token ?: throw HistoryFailure.Access()
        val page = transport.load(query, access.endpoint, token)
        authorize(access); page
    })
    fun decode(sensor: String, row: HistoryReading): TagSensorReading = try {
        TagSensorReading(BluetoothLibrary.decode(sensor, row.data, row.rssi ?: 0), Date(row.timestamp * 1000))
            .copy(rssi = row.rssi)
    } catch (_: Exception) { throw HistoryFailure.Invalid() }
    fun snapshots(access: Access, window: HistoryWindow, metrics: List<String>, refresh: HistoryRefresh, previewKey: String? = null, valueRevision: String = "",
                  values: (TagSensorReading) -> Map<String, Double>) = stream(access).snapshots(
        access.sensor, window, metrics, access.scope, access.revision + (if (valueRevision.isEmpty()) "" else "|$valueRevision") + "|" + metrics.sorted().joinToString(), refresh,
        local = { range, cursor ->
            authorize(access)
            val page = local.readHistoryPage(access.sensor, range, cursor)
            HistoryLocalPage(page.rows.map { HistoryValues(it.createdAt.time / 1000, values(access.calibrate(it))) }, page.next)
        }, decode = { values(access.calibrate(decode(access.sensor, it))) }, previewKey = previewKey)
        .onEach { authorize(access) }
        .flowOn(Dispatchers.IO)

    suspend fun records(access: Access, window: HistoryWindow, consume: suspend (List<TagSensorReading>) -> Unit) = withContext(Dispatchers.IO) {
        authorize(access)
        stream(access).read(access.sensor, window, access.scope, HistoryRefresh.opening(System.currentTimeMillis())) { page, range ->
            authorize(access); currentCoroutineContext().ensureActive()
            val cloud = page.readings.map { decode(access.sensor, it) }
            HistoryLocalMerger.merge(cloud, range, { it.createdAt.time / 1000 }, local = { bounds, cursor ->
                authorize(access); local.readHistoryPage(access.sensor, bounds, cursor)
            }) { readings -> authorize(access); consume(readings) }
        }
    }
    suspend fun remove(access: Access) = withContext(Dispatchers.IO) { cache.remove(access.scope, access.sensor) }
}
