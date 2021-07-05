package com.ruuvi.station.database.domain

import com.raizlabs.android.dbflow.config.FlowManager
import com.raizlabs.android.dbflow.kotlinextensions.from
import com.raizlabs.android.dbflow.sql.language.Method
import com.raizlabs.android.dbflow.sql.language.SQLite
import com.raizlabs.android.dbflow.sql.queriable.StringQuery
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.database.tables.TagSensorReading_Table
import timber.log.Timber
import java.util.*

class SensorHistoryRepository {
    fun getCompositeHistory(sensorId: String, daysPeriod: Int, interval: Int): List<TagSensorReading> {
        val fromDate = Calendar.getInstance()
        fromDate.time = Date()
        fromDate.add(Calendar.DATE, -daysPeriod)
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

    fun getHistory(sensorId: String, daysPeriod: Int): List<TagSensorReading> {
        val fromDate = Calendar.getInstance()
        fromDate.time = Date()
        fromDate.add(Calendar.DATE, -daysPeriod)
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

    fun removeOlderThan(historyLengthHours: Int) {
        val cal = Calendar.getInstance()
        cal.time = Date()
        cal.add(Calendar.HOUR, -historyLengthHours)
        SQLite
            .delete()
            .from(TagSensorReading::class.java)
            .where(TagSensorReading_Table.createdAt.lessThan(cal.time))
            .async()
            .execute()
    }

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

    fun bulkInsert(readings: List<TagSensorReading>) {
        fun executeSQL(sql: StringBuilder) {
            sql.replace(sql.length - 1, sql.length, ";")
            Timber.d("bulkInsert $sql")
            FlowManager.getWritableDatabase(LocalDatabase.NAME).execSQL(sql.toString())
        }

        val insertQuery = "insert into TagSensorReading (`ruuviTagId`, `createdAt`, `temperature`, `humidity`, `pressure`, `rssi`, `accelX`, `accelY`, `accelZ`, `voltage`, `dataFormat`, `txPower`, `movementCounter`, `measurementSequenceNumber`, `humidityOffset`, 'temperatureOffset', 'pressureOffset') values "
        val queries: MutableList<String> = ArrayList()
        for (reading in readings) {
            with(reading) {
                queries.add("(\"${ruuviTagId}\", ${createdAt.time}, $temperature, $humidity, $pressure, $rssi, $accelX, $accelY, $accelZ, $voltage, $dataFormat, $txPower, $movementCounter, $measurementSequenceNumber, $humidityOffset, $temperatureOffset, $pressureOffset),")
            }
        }

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

    companion object {
        const val POINT_THRESHOLD = 1000
        const val HIGH_DENSITY_INTERVAL_MINUTES = 15
        const val BULK_INSERT_BATCH_SIZE = 100
        const val MAXIMUM_POINTS_COUNT = 3000
    }
}