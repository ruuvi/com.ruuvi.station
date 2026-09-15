package com.ruuvi.station.database.domain

import com.raizlabs.android.dbflow.config.FlowManager
import com.raizlabs.android.dbflow.sql.language.Method
import com.raizlabs.android.dbflow.sql.language.SQLite
import com.ruuvi.station.app.preferences.GlobalSettings
import com.ruuvi.station.database.tables.*
import com.ruuvi.station.history.HistoryRange
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Date
import timber.log.Timber

class SensorHistoryRepository(private val now: () -> Long = System::currentTimeMillis) {
    /** Read a bounded range without materializing its potentially large raw history list. */
    fun forEachReading(sensorId: String, range: HistoryRange, consume: (TagSensorReading) -> Unit) {
        val adapter = FlowManager.getModelAdapter(TagSensorReading::class.java)
        SQLite.select().from(TagSensorReading::class.java)
            .where(TagSensorReading_Table.ruuviTagId.eq(sensorId))
            .and(TagSensorReading_Table.createdAt.greaterThanOrEq(Date(range.startMillis)))
            .and(TagSensorReading_Table.createdAt.lessThan(Date(range.endExclusiveMillis)))
            .orderBy(TagSensorReading_Table.createdAt, true).query()?.use { cursor ->
                while (cursor.moveToNext()) consume(adapter.loadFromCursor(cursor))
            }
    }

    fun getHistory(sensorId: String, hoursPeriod: Int) = getHistory(sensorId, recentRange(hoursPeriod))

    fun getHistory(sensorId: String, fromDate: Date) =
        getHistory(sensorId, HistoryRange(fromDate.time, maxOf(fromDate.time, now())))

    fun getHistory(sensorId: String, range: HistoryRange): List<TagSensorReading> = SQLite.select()
        .from(TagSensorReading::class.java)
        .where(TagSensorReading_Table.ruuviTagId.eq(sensorId))
        .and(TagSensorReading_Table.createdAt.greaterThanOrEq(Date(range.startMillis)))
        .and(TagSensorReading_Table.createdAt.lessThan(Date(range.endExclusiveMillis)))
        .orderBy(TagSensorReading_Table.createdAt, true)
        .queryList()

    fun getLatestForSensor(sensorId: String, limit: Int): List<TagSensorReading> = SQLite.select()
        .from(TagSensorReading::class.java)
        .where(TagSensorReading_Table.ruuviTagId.eq(sensorId))
        .orderBy(TagSensorReading_Table.createdAt, false).limit(limit).queryList()

    fun getCount(sensorId: String, fromDate: Date) =
        getCount(sensorId, HistoryRange(fromDate.time, maxOf(fromDate.time, now())))

    fun getCount(sensorId: String, range: HistoryRange): Long = SQLite.select(Method.count())
        .from(TagSensorReading::class.java)
        .where(TagSensorReading_Table.ruuviTagId.eq(sensorId))
        .and(TagSensorReading_Table.createdAt.greaterThanOrEq(Date(range.startMillis)))
        .and(TagSensorReading_Table.createdAt.lessThan(Date(range.endExclusiveMillis))).longValue()

    fun countAll() = SQLite.selectCountOf().from(TagSensorReading::class.java).longValue()

    fun revision(sensorId: String): Long = revisions.value[sensorId] ?: 0

    fun cleanup(force: Boolean = false) = synchronized(historyLock) {
        val current = now()
        if (!force && current - lastCleanup < CLEANUP_INTERVAL_MILLIS) return@synchronized
        val cutoff = current - GlobalSettings.historyLengthMillis
        FlowManager.getDatabase(LocalDatabase::class.java).executeTransaction { db ->
            db.execSQL("DELETE FROM TagSensorReading WHERE createdAt < $cutoff")
            db.execSQL("DELETE FROM HistoryCoverage WHERE endExclusiveMillis <= $cutoff")
            db.execSQL("UPDATE HistoryCoverage SET startMillis = $cutoff WHERE startMillis < $cutoff")
        }
        lastCleanup = current
        revisions.value = revisions.value.mapValues { it.value + 1 }
    }

    fun removeForSensor(sensorId: String) = synchronized(historyLock) {
        // A clear also invalidates pages already in flight, even if deletion precedes their response.
        generations[sensorId] = generation(sensorId) + 1
        FlowManager.getDatabase(LocalDatabase::class.java).executeTransaction { db ->
            SQLite.delete(TagSensorReading::class.java)
                .where(TagSensorReading_Table.ruuviTagId.eq(sensorId)).execute(db)
            SQLite.delete(HistoryCoverage::class.java)
                .where(HistoryCoverage_Table.sensorId.eq(sensorId)).execute(db)
        }
        changed(sensorId)
    }

    fun generation(sensorId: String): Long = synchronized(historyLock) { generations[sensorId] ?: 0 }

    fun clearCloudCoverage() = synchronized(historyLock) {
        SQLite.delete(HistoryCoverage::class.java).execute()
    }

    fun coverage(sensorId: String, account: String, backend: String, freshAfter: Long): List<HistoryRange> =
        coverageRows(sensorId, account, backend, freshAfter).map { HistoryRange(it.startMillis, it.endExclusiveMillis) }

    private fun coverageRows(sensorId: String, account: String, backend: String, freshAfter: Long) =
        SQLite.select().from(HistoryCoverage::class.java)
            .where(HistoryCoverage_Table.sensorId.eq(sensorId))
            .and(HistoryCoverage_Table.account.eq(account))
            .and(HistoryCoverage_Table.backend.eq(backend))
            .and(HistoryCoverage_Table.fetchedAt.greaterThanOrEq(freshAfter))
            .orderBy(HistoryCoverage_Table.startMillis, true).queryList()

    fun bulkInsert(sensorId: String, readings: List<TagSensorReading>) = synchronized(historyLock) {
        cleanup()
        for (page in readings.sortedBy { it.createdAt }.chunked(INSERT_PAGE_SIZE)) {
            FlowManager.getDatabase(LocalDatabase::class.java).executeTransaction { db ->
                insertReadings(sensorId, page, db)
            }
            changed(sensorId)
        }
    }

    /** Returns false when the sensor/history was removed while this request was in flight. */
    fun saveCloudPage(
        sensorId: String, readings: List<TagSensorReading>, range: HistoryRange,
        account: String, backend: String, expectedGeneration: Long
    ): Boolean = synchronized(historyLock) {
        if (generation(sensorId) != expectedGeneration) return@synchronized false
        if (SQLite.selectCountOf().from(SensorSettings::class.java)
                .where(SensorSettings_Table.id.eq(sensorId)).longValue() == 0L) return@synchronized false
        cleanup()
        val current = now()
        val retainedRange = range.intersect(HistoryRange.retained(current))
        if (retainedRange.isEmpty) return@synchronized true
        FlowManager.getDatabase(LocalDatabase::class.java).executeTransaction { db ->
            insertReadings(sensorId, readings, db)
            var start = retainedRange.startMillis
            var end = retainedRange.endExclusiveMillis
            var fetched = current
            for (row in coverageRows(sensorId, account, backend, current - CACHE_FRESHNESS_MILLIS)) {
                if (row.endExclusiveMillis < start || row.startMillis > end) continue
                start = minOf(start, row.startMillis)
                end = maxOf(end, row.endExclusiveMillis)
                // Conservative expiry: merging must never make old coverage appear newly fetched.
                fetched = minOf(fetched, row.fetchedAt)
                row.delete(db)
            }
            HistoryCoverage(sensorId = sensorId, account = account, backend = backend,
                startMillis = maxOf(start, current - GlobalSettings.historyLengthMillis),
                endExclusiveMillis = end, fetchedAt = fetched).insert(db)
        }
        if (readings.isNotEmpty()) changed(sensorId)
        true
    }

    private fun insertReadings(sensorId: String, readings: List<TagSensorReading>, db: com.raizlabs.android.dbflow.structure.database.DatabaseWrapper) {
        val cutoff = now() - GlobalSettings.historyLengthMillis
        val incoming = readings.filter { it.ruuviTagId == sensorId && it.createdAt.time >= cutoff }.sortedBy { it.createdAt }
        if (incoming.isEmpty()) return
        val existing = SQLite.select(TagSensorReading_Table.createdAt).from(TagSensorReading::class.java)
            .where(TagSensorReading_Table.ruuviTagId.eq(sensorId))
            .and(TagSensorReading_Table.createdAt.greaterThanOrEq(Date(incoming.first().createdAt.time - TIMELINE_DISTANCE)))
            .and(TagSensorReading_Table.createdAt.lessThanOrEq(Date(incoming.last().createdAt.time + TIMELINE_DISTANCE)))
            .orderBy(TagSensorReading_Table.createdAt, true).queryList(db).map { it.createdAt.time }
        var index = 0
        var lastInserted: Long? = null
        for (reading in incoming) {
            val time = reading.createdAt.time
            while (index < existing.size && existing[index] <= time - TIMELINE_DISTANCE) index++
            val duplicate = index < existing.size && existing[index] < time + TIMELINE_DISTANCE
            if (!duplicate && (lastInserted == null || time - lastInserted >= TIMELINE_DISTANCE)) {
                reading.copy(id = 0).insert(db)
                lastInserted = time
            }
        }
    }

    fun insertPoint(historyPoint: TagSensorReading) = synchronized(historyLock) {
        cleanup()
        if (historyPoint.createdAt.time >= now() - GlobalSettings.historyLengthMillis) {
            historyPoint.insert()
            historyPoint.ruuviTagId?.let(::changed)
        }
    }

    private fun recentRange(hours: Int): HistoryRange {
        val current = now()
        return HistoryRange(current - hours.coerceIn(0, GlobalSettings.historyLengthHours) * 3_600_000L, current)
    }

    private fun changed(sensorId: String) {
        revisions.value = revisions.value + (sensorId to (revision(sensorId) + 1))
    }

    fun recalibrate(sensorSettings: SensorSettings) {
        fun executeSQL(sql: String) {
            Timber.d("executeSQL $sql")
            FlowManager.getWritableDatabase(LocalDatabase.NAME).execSQL(sql)
        }
        val updateQuery = """
            update TagSensorReading 
                set 
                    temperature = temperature - ifnull(temperatureOffset, 0) + ${sensorSettings.temperatureOffset ?: 0.0},
                    humidity = humidity - ifnull(humidityOffset, 0) + ${sensorSettings.humidityOffset ?: 0.0},
                    pressure = pressure - ifnull(pressureOffset, 0) + ${sensorSettings.pressureOffset ?: 0.0},
                    temperatureOffset = ${sensorSettings.temperatureOffset ?: 0.0},
                    humidityOffset = ${sensorSettings.humidityOffset ?: 0.0},
                    pressureOffset = ${sensorSettings.pressureOffset ?: 0.0}
            where ruuviTagId = "${sensorSettings.id}"
        """
        synchronized(historyLock) { executeSQL(updateQuery) }
        changed(sensorSettings.id)
    }

    companion object {
        internal val historyLock = Any()
        private val generations = mutableMapOf<String, Long>()
        private val revisions = MutableStateFlow<Map<String, Long>>(emptyMap())
        const val TIMELINE_DISTANCE = 1500L
        private const val INSERT_PAGE_SIZE = 5000
        const val CACHE_FRESHNESS_MILLIS = 24 * 60 * 60 * 1000L
        private const val CLEANUP_INTERVAL_MILLIS = 10 * 60 * 1000L
        private var lastCleanup = 0L
    }
}
