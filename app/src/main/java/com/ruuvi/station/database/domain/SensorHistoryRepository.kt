package com.ruuvi.station.database.domain

import com.raizlabs.android.dbflow.config.FlowManager
import com.raizlabs.android.dbflow.kotlinextensions.from
import com.raizlabs.android.dbflow.sql.language.Method
import com.raizlabs.android.dbflow.sql.language.SQLite
import com.raizlabs.android.dbflow.sql.queriable.StringQuery
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.database.tables.TagSensorReading_Table
import timber.log.Timber
import java.util.*
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

class SensorHistoryRepository {
    private val pruningScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pruning = AtomicBoolean(false)
    @Volatile private var lastPrunedAt = 0L

    fun schedulePruning(nowMillis: Long = System.currentTimeMillis()) {
        if (nowMillis - lastPrunedAt < 3_600_000 || !pruning.compareAndSet(false, true)) return
        pruningScope.launch {
            try { pruneExpired(nowMillis); lastPrunedAt = nowMillis }
            catch (_: Exception) { currentCoroutineContext().ensureActive(); Timber.w("History cleanup failed; will retry") }
            finally { pruning.set(false) }
        }
    }

    /** One bounded write transaction per batch; resolve the current owner allowance inside every delete. */
    internal suspend fun pruneExpired(nowMillis: Long) {
        val database = FlowManager.getDatabase(LocalDatabase::class.java).writableDatabase
        while (true) {
            currentCoroutineContext().ensureActive()
            var deleted = 0
            database.beginTransaction()
            try {
                database.execSQL("""DELETE FROM TagSensorReading WHERE id IN (
                    SELECT r.id FROM TagSensorReading r LEFT JOIN SensorSettings s ON s.id = r.ruuviTagId
                    WHERE r.createdAt < $nowMillis - 86400000 * CASE
                        WHEN s.networkSensor = 1 THEN CASE
                            WHEN s.maxHistoryDays > 0 THEN s.maxHistoryDays
                            WHEN s.maxHistoryDays = 0 THEN 10 ELSE NULL END
                        ELSE 10 END LIMIT 5000)""")
                database.rawQuery("SELECT changes()", null).use { cursor ->
                    if (cursor.moveToFirst()) deleted = cursor.getInt(0)
                }
                database.setTransactionSuccessful()
            } finally { database.endTransaction() }
            if (deleted < 5000) return
            yield()
        }
    }

    fun getCompositeHistory(sensorId: String, hoursPeriod: Int, interval: Int): List<TagSensorReading> {
        val fromDate = Calendar.getInstance()
        fromDate.time = Date()
        fromDate.add(Calendar.HOUR, -hoursPeriod)
        return getCompositeHistory(sensorId, fromDate.time, interval)
    }

    fun getCompositeHistory(sensorId: String, fromDate: Date, interval: Int): List<TagSensorReading> {
        val count = getCount(sensorId, fromDate)
        return if (count < POINT_THRESHOLD) {
            getHistory(sensorId, fromDate)
        } else {
            getPrunedHistory(sensorId, fromDate, interval)
        }
    }

    /** Indexed, descending keyset pages; never materialize all local history for graph/export. */
    fun readHistoryPage(sensorId: String, window: com.ruuvi.station.history.HistoryWindow, cursor: String?): com.ruuvi.station.history.HistoryLocalPage<TagSensorReading> {
        val before = cursor?.split(":")?.let {
            require(it.size == 2)
            it[0].toLong() to it[1].toInt()
        }
        var query = SQLite.select().from(TagSensorReading::class.java)
            .where(TagSensorReading_Table.ruuviTagId.eq(sensorId))
            .and(TagSensorReading_Table.createdAt.greaterThanOrEq(Date(window.since * 1000)))
            .and(TagSensorReading_Table.createdAt.lessThan(Date((window.until + 1) * 1000)))
        if (before != null) {
            query = query.and(com.raizlabs.android.dbflow.sql.language.OperatorGroup.clause()
                .and(TagSensorReading_Table.createdAt.lessThan(Date(before.first)))
                .or(com.raizlabs.android.dbflow.sql.language.OperatorGroup.clause()
                    .and(TagSensorReading_Table.createdAt.eq(Date(before.first)))
                    .and(TagSensorReading_Table.id.lessThan(before.second))))
        }
        val rows = query.orderBy(TagSensorReading_Table.createdAt, false).orderBy(TagSensorReading_Table.id, false)
            .limit(com.ruuvi.station.history.HistoryQuery.PAGE_SIZE).queryList()
        val next = rows.lastOrNull()?.takeIf { rows.size == com.ruuvi.station.history.HistoryQuery.PAGE_SIZE }
            ?.let { "${it.createdAt.time}:${it.id}" }
        return com.ruuvi.station.history.HistoryLocalPage(rows, next)
    }

    fun getHistory(sensorId: String, hoursPeriod: Int): List<TagSensorReading> {
        val fromDate = Calendar.getInstance()
        fromDate.time = Date()
        fromDate.add(Calendar.HOUR, -hoursPeriod)
        return getHistory(sensorId, fromDate.time)
    }

    fun getHistory(sensorId: String, fromDate: Date): List<TagSensorReading> {
        return SQLite
            .select()
            .from(TagSensorReading::class)
            .indexedBy(TagSensorReading_Table.index_TagId)
            .where(TagSensorReading_Table.ruuviTagId.eq(sensorId))
            .and(TagSensorReading_Table.createdAt.greaterThan(fromDate))
            .orderBy(TagSensorReading_Table.createdAt, true)
            .queryList()
    }

    fun getPrunedHistory(sensorId: String, fromDate: Date, interval: Int): List<TagSensorReading> {
        val highDensityDate = Calendar.getInstance()
        highDensityDate.time = Date()
        highDensityDate.add(Calendar.MINUTE, -HIGH_DENSITY_INTERVAL_MINUTES)

        //val pruningInterval = 1000 * 60 * interval
        val pruningInterval = (highDensityDate.time.time - fromDate.time) / MAXIMUM_POINTS_COUNT

        val sqlString = "Select tr.* from " +
            "(select min(id) as id from TagSensorReading where RuuviTagId = '$sensorId' and createdAt > ${fromDate.time} and createdAt < ${highDensityDate.time.time} group by createdAt / $pruningInterval) gr " +
            "join TagSensorReading tr on gr.id = tr.id " +
            "UNION ALL Select * from TagSensorReading where RuuviTagId = '$sensorId' and createdAt >= ${highDensityDate.time.time} order by createdAt asc"
        Timber.d("getPrunedHistory - $sqlString")
        val query = StringQuery(TagSensorReading::class.java, sqlString)
        return query.queryList()
    }

    fun getLatestForSensor(sensorId: String, limit: Int): List<TagSensorReading> =
        SQLite
            .select()
            .from(TagSensorReading::class)
            .indexedBy(TagSensorReading_Table.index_TagId)
            .where(TagSensorReading_Table.ruuviTagId.eq(sensorId))
            .orderBy(TagSensorReading_Table.createdAt, false)
            .limit(limit)
            .queryList()

    fun removeForSensor(sensorId: String) {
        SQLite.delete()
            .from(TagSensorReading::class.java)
            .where(TagSensorReading_Table.ruuviTagId.eq(sensorId))
            .async()
            .execute()
    }

    fun getCount(sensorId: String, fromDate: Date): Long {
        return SQLite
            .select(Method.count())
            .from(TagSensorReading::class)
            .indexedBy(TagSensorReading_Table.index_TagId)
            .where(TagSensorReading_Table.ruuviTagId.eq(sensorId))
            .and(TagSensorReading_Table.createdAt.greaterThan(fromDate))
            .longValue()
    }

    fun countAll() = SQLite.selectCountOf().from(TagSensorReading::class).longValue()

    fun bulkInsert(sensorId: String, readings: List<TagSensorReading>) {
        fun executeSQL(sql: StringBuilder) {
            sql.replace(sql.length - 1, sql.length, ";")
            Timber.d("bulkInsert $sql")
            FlowManager.getWritableDatabase(LocalDatabase.NAME).execSQL(sql.toString())
        }
        if (readings.size > 0) {
            val minDate = readings.minOf { it.createdAt }
            val existingHistory = getHistory(sensorId, minDate)

            //todo rewrite to automatically list all fields
            val insertQuery = "insert into TagSensorReading (`ruuviTagId`, `createdAt`, `temperature`, `humidity`, `pressure`, `rssi`, `accelX`, `accelY`, `accelZ`, `voltage`, `dataFormat`, `txPower`, `movementCounter`, `measurementSequenceNumber`, `humidityOffset`, 'temperatureOffset', 'pressureOffset', 'pm1', 'pm25', 'pm4', 'pm10', 'co2', 'voc', 'nox', 'luminosity', 'dBaAvg', 'dBaPeak') values "
            val queries: MutableList<String> = ArrayList()
            for (reading in readings) {
                if (existingHistory.none { abs(reading.createdAt.time - it.createdAt.time) < TIMELINE_DISTANCE}) {
                    with(reading) {
                        queries.add("(\"${ruuviTagId}\", ${createdAt.time}, $temperature, $humidity, $pressure, $rssi, $accelX, $accelY, $accelZ, $voltage, $dataFormat, $txPower, $movementCounter, $measurementSequenceNumber, $humidityOffset, $temperatureOffset, $pressureOffset, $pm1, $pm25, $pm4, $pm10, $co2, $voc, $nox, $luminosity, $dBaAvg, $dBaPeak),")
                    }
                }
            }

            if (queries.isNotEmpty()) {
                var query: StringBuilder? = StringBuilder(insertQuery)
                var i = 0
                for (valuesString in queries) {
                    if (query == null) query = StringBuilder(insertQuery)
                    query.append(valuesString)
                    i++
                    if (i >= BULK_INSERT_BATCH_SIZE) {
                        executeSQL(query)
                        query = null
                        i = 0
                    }
                }
                if (query != null) executeSQL(query)
            }
        }
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
        executeSQL(updateQuery)
    }

    fun insertPoint(historyPoint: TagSensorReading) {
        historyPoint.insert()
    }

    companion object {
        const val POINT_THRESHOLD = 1000
        const val HIGH_DENSITY_INTERVAL_MINUTES = 15
        const val BULK_INSERT_BATCH_SIZE = 100
        const val MAXIMUM_POINTS_COUNT = 3000
        const val TIMELINE_DISTANCE = 1500
    }
}