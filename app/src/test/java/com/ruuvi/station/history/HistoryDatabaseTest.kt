package com.ruuvi.station.history

import android.app.Application
import com.raizlabs.android.dbflow.config.FlowManager
import com.raizlabs.android.dbflow.sql.language.SQLite
import com.ruuvi.station.database.domain.LocalDatabase
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.tables.TagSensorReading
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class HistoryDatabaseTest {
    @Before fun setup() { FlowManager.init(RuntimeEnvironment.getApplication()) }
    @After fun cleanup() { FlowManager.destroy() }
    @Test fun `keyset pages preserve equal timestamp records without pulling another sensor`() {
        val database = FlowManager.getDatabase(LocalDatabase::class.java).writableDatabase
        database.beginTransaction()
        try {
            repeat(3100) { index -> TagSensorReading(ruuviTagId = "synthetic",createdAt = Date(100000L - index / 10),temperature = index.toDouble()).save() }
            TagSensorReading(ruuviTagId = "other", createdAt = Date(100000),temperature = -1.0).save()
            database.setTransactionSuccessful()
        } finally { database.endTransaction() }
        val repository = SensorHistoryRepository()
        var cursor: String? = null
        val ids = mutableSetOf<Int>(); var count = 0
        do {
            val page = repository.readHistoryPage("synthetic",HistoryWindow(0,100),cursor)
            assertTrue(page.rows.size <= 1500); assertTrue(page.rows.all { it.ruuviTagId == "synthetic" })
            page.rows.forEach { assertTrue(ids.add(it.id)); count++ }
            cursor = page.next
        } while (cursor != null)
        assertEquals(3100,count)
        val plan = database.rawQuery("EXPLAIN QUERY PLAN SELECT * FROM TagSensorReading WHERE ruuviTagId = 'synthetic' AND createdAt >= 0 AND createdAt < 101000 ORDER BY createdAt DESC, id DESC LIMIT 1500",null)
        plan.use { cursor ->
            val details = mutableListOf<String>(); while (cursor.moveToNext()) details += cursor.getString(cursor.getColumnIndex("detail"))
            assertTrue(details.any { it.contains("INDEX") }); assertFalse(details.any { it.contains("SCAN TagSensorReading") })
        }
    }
    @Test fun `retention prunes local and free history but follows each cloud owner plan in bounded batches`() = kotlinx.coroutines.runBlocking {
        val now = 1_800_000_000_000L
        val day = 86_400_000L
        val database = FlowManager.getDatabase(LocalDatabase::class.java).writableDatabase
        val plans = listOf("local" to null, "free" to 0, "business" to 1096, "short" to 92, "unknown" to null)
        database.beginTransaction()
        try {
            for ((id, days) in plans) com.ruuvi.station.database.tables.SensorSettings(id = id, networkSensor = id != "local", maxHistoryDays = days).save()
            repeat(6003) { TagSensorReading(ruuviTagId = "local", createdAt = Date(now - 11 * day)).save() }
            for ((id, _) in plans) {
                TagSensorReading(ruuviTagId = id, createdAt = Date(now - 9 * day)).save()
                TagSensorReading(ruuviTagId = id, createdAt = Date(now - 400 * day)).save()
            }
            TagSensorReading(ruuviTagId = "local", createdAt = Date(now - 10 * day)).save()
            database.setTransactionSuccessful()
        } finally { database.endTransaction() }
        val repository = SensorHistoryRepository()
        repository.pruneExpired(now)
        fun remaining(id: String) = repository.getHistory(id, Date(0)).size
        assertEquals(2, remaining("local")) // Exact boundary retained.
        assertEquals(1, remaining("free"))
        assertEquals(2, remaining("business"))
        assertEquals(1, remaining("short"))
        assertEquals(2, remaining("unknown")) // Unknown allowance never authorizes deletion.
        database.execSQL("UPDATE SensorSettings SET maxHistoryDays = 92 WHERE id = 'business'")
        repository.pruneExpired(now)
        assertEquals(1, remaining("business"))
        assertEquals(2, remaining("unknown"))
    }
    @Test fun `allowance migration is additive and works with earlier experimental schema`() {
        val database = FlowManager.getDatabase(LocalDatabase::class.java).writableDatabase
        database.execSQL("DROP TABLE SensorSettings")
        database.execSQL("CREATE TABLE SensorSettings (id TEXT PRIMARY KEY, name TEXT)")
        database.execSQL("INSERT INTO SensorSettings VALUES ('synthetic','Preserved')")
        LocalDatabase.Migration46HistoryAllowance().migrate(database)
        LocalDatabase.Migration46HistoryAllowance().migrate(database)
        database.rawQuery("SELECT name, maxHistoryDays, imageUrl FROM SensorSettings", null).use { cursor ->
            assertTrue(cursor.moveToFirst()); assertEquals("Preserved",cursor.getString(0)); assertTrue(cursor.isNull(1)); assertTrue(cursor.isNull(2))
        }
    }
}
