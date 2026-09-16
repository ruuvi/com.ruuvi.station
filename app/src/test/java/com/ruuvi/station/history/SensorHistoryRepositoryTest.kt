package com.ruuvi.station.history

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.raizlabs.android.dbflow.config.DatabaseConfig
import com.raizlabs.android.dbflow.config.FlowConfig
import com.raizlabs.android.dbflow.config.FlowManager
import com.raizlabs.android.dbflow.sql.language.SQLite
import com.raizlabs.android.dbflow.structure.database.AndroidDatabase
import com.ruuvi.station.app.preferences.GlobalSettings
import com.ruuvi.station.database.domain.LocalDatabase
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.tables.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class SensorHistoryRepositoryTest {
    private val now = 1_800_000_000_000L
    private val sensorId = "AA:BB:CC:DD:EE:FF"
    private val retained = HistoryRange.retained(now)
    private val repository = SensorHistoryRepository { now }

    @Before fun setup() {
        FlowManager.init(FlowConfig.builder(ApplicationProvider.getApplicationContext<Application>())
            .addDatabaseConfig(DatabaseConfig.builder(LocalDatabase::class.java).inMemory().build()).build())
        SensorSettings(id = sensorId, networkSensor = true, cloudHistoryDays = 100).insert()
        repository.cleanup(force = true)
    }

    @After fun tearDown() { FlowManager.destroy() }

    @Test fun `BLE point ingestion rejects expired readings and keeps cutoff`() {
        repository.insertPoint(point(retained.startMillis - 1))
        repository.insertPoint(point(retained.startMillis))
        repository.insertPoint(point(retained.startMillis + 1))
        assertEquals(2L, repository.countAll())
        assertEquals(listOf(retained.startMillis, retained.startMillis + 1), repository.getHistory(sensorId, retained).map { it.createdAt.time })
    }

    @Test fun `GATT and cloud batch paths enforce the same retention`() {
        repository.bulkInsert(sensorId, listOf(point(retained.startMillis - 5000), point(retained.startMillis)))
        assertTrue(repository.saveCloudPage(sensorId,
            listOf(point(retained.startMillis - 2000), point(retained.startMillis + 5000)), retained,
            "account", "production", repository.generation(sensorId)))
        assertEquals(2L, repository.countAll())
    }

    @Test fun `cleanup works without any BLE callbacks and clips coverage`() {
        point(retained.startMillis - 1).insert()
        point(retained.startMillis).insert()
        HistoryCoverage(sensorId = sensorId, startMillis = retained.startMillis - 10, endExclusiveMillis = now, fetchedAt = now).insert()
        HistoryCoverage(sensorId = sensorId, startMillis = retained.startMillis - 10, endExclusiveMillis = retained.startMillis, fetchedAt = now).insert()
        repository.cleanup(force = true)
        assertEquals(1L, repository.countAll())
        val coverage = SQLite.select().from(HistoryCoverage::class.java).queryList()
        assertEquals(1, coverage.size)
        assertEquals(retained.startMillis, coverage.single().startMillis)
    }

    @Test fun `raw and count queries include start but exclude end`() {
        val range = HistoryRange(now - 10_000, now - 5000)
        listOf(range.startMillis - 1, range.startMillis, range.endExclusiveMillis - 1, range.endExclusiveMillis).forEach { point(it).insert() }
        assertEquals(2L, repository.getCount(sensorId, range))
        assertEquals(listOf(range.startMillis, range.endExclusiveMillis - 1), repository.getHistory(sensorId, range).map { it.createdAt.time })
    }

    @Test fun `sampling an old window never includes recent measurements`() {
        val range = HistoryRange(now - 80 * 86_400_000L, now - 79 * 86_400_000L)
        FlowManager.getDatabase(LocalDatabase::class.java).executeTransaction { db ->
            for (i in 0..2000) point(range.startMillis + i * 1000).insert(db)
            point(now - 1000).insert(db)
            point(range.endExclusiveMillis).insert(db)
        }
        val readings = mutableListOf<TagSensorReading>()
        repository.forEachReading(sensorId, range) { readings.add(it) }
        assertTrue(readings.isNotEmpty())
        assertTrue(readings.all { it.createdAt.time >= range.startMillis && it.createdAt.time < range.endExclusiveMillis })
    }

    @Test fun `batch deduplication includes earliest timestamp and duplicates inside a page`() {
        val start = now - 100_000
        repository.bulkInsert(sensorId, listOf(point(start), point(start + 5000)))
        repository.bulkInsert(sensorId, listOf(point(start), point(start + 1499), point(start + 1500), point(start + 1500), point(start + 4999)))
        assertEquals(listOf(start, start + 1500, start + 5000), repository.getHistory(sensorId, retained).map { it.createdAt.time })
    }

    @Test fun `page readings and coverage roll back together on a write failure`() {
        FlowManager.getWritableDatabase(LocalDatabase.NAME).execSQL(
            "CREATE TRIGGER reject_coverage BEFORE INSERT ON HistoryCoverage BEGIN SELECT RAISE(ABORT, 'test rollback'); END"
        )
        assertThrows(Exception::class.java) {
            repository.saveCloudPage(sensorId, listOf(point(now - 1000)), retained, "account", "production", repository.generation(sensorId))
        }
        assertEquals(0L, repository.countAll())
        assertTrue(repository.coverage(sensorId, "account", "production", 0).isEmpty())
    }

    @Test fun `large GATT imports deduplicate across page boundaries`() {
        val start = now - 20_000_000
        val readings = (0 until 5000).map { point(start + it * 2000L) } +
            listOf(point(start + 4999 * 2000L + 1499), point(start + 5000 * 2000L))
        repository.bulkInsert(sensorId, readings.reversed())
        assertEquals(5001L, repository.countAll())
    }

    @Test fun `large GATT imports retain completed pages and roll back the failing page`() {
        val start = now - 20_000_000
        val failureTime = start + 5001 * 2000L
        FlowManager.getWritableDatabase(LocalDatabase.NAME).execSQL(
            "CREATE TRIGGER reject_reading BEFORE INSERT ON TagSensorReading " +
                "WHEN NEW.createdAt = $failureTime BEGIN SELECT RAISE(ABORT, 'test rollback'); END"
        )
        assertThrows(Exception::class.java) {
            repository.bulkInsert(sensorId, (0..5001).map { point(start + it * 2000L) })
        }
        assertEquals(5000L, repository.countAll())
        assertEquals(start + 4999 * 2000L, repository.getLatestForSensor(sensorId, 1).single().createdAt.time)
    }

    @Test fun `clearing history also clears coverage and rejects stale in flight page`() {
        val generation = repository.generation(sensorId)
        repository.saveCloudPage(sensorId, listOf(point(now - 1000)), retained, "account", "production", generation)
        repository.removeForSensor(sensorId)
        assertFalse(repository.saveCloudPage(sensorId, listOf(point(now - 1000)), retained, "account", "production", generation))
        assertEquals(0L, repository.countAll())
        assertTrue(repository.coverage(sensorId, "account", "production", 0).isEmpty())
    }

    @Test fun `coverage is account backend and freshness specific`() {
        val generation = repository.generation(sensorId)
        repository.saveCloudPage(sensorId, emptyList(), retained, "account", "production", generation)
        assertEquals(listOf(retained), repository.coverage(sensorId, "account", "production", now))
        assertTrue(repository.coverage(sensorId, "another", "production", 0).isEmpty())
        assertTrue(repository.coverage(sensorId, "account", "testnet", 0).isEmpty())
        assertTrue(repository.coverage(sensorId, "account", "production", now + 1).isEmpty())
    }

    @Test fun `signout cache cleanup keeps local readings but clears every account and backend`() {
        val generation = repository.generation(sensorId)
        repository.saveCloudPage(sensorId, listOf(point(now - 1000)), retained, "account", "production", generation)
        repository.saveCloudPage(sensorId, emptyList(), retained, "another", "testnet", generation)
        repository.clearCloudCoverage()
        assertEquals(1L, repository.countAll())
        assertTrue(repository.coverage(sensorId, "account", "production", 0).isEmpty())
        assertTrue(repository.coverage(sensorId, "another", "testnet", 0).isEmpty())
    }

    @Test fun `page which expired during download cannot create inverted coverage`() {
        assertTrue(repository.saveCloudPage(sensorId, emptyList(),
            HistoryRange(retained.startMillis - 2000, retained.startMillis - 1000),
            "account", "production", repository.generation(sensorId)))
        assertTrue(repository.coverage(sensorId, "account", "production", 0).isEmpty())
    }

    @Test fun `revalidating history replaces obsolete coverage instead of accumulating it`() {
        repeat(20) {
            HistoryCoverage(sensorId = sensorId, account = "account", backend = "production",
                startMillis = retained.startMillis, endExclusiveMillis = now,
                fetchedAt = now - SensorHistoryRepository.CACHE_FRESHNESS_MILLIS - 1).insert()
        }
        repository.saveCloudPage(sensorId, emptyList(), retained, "account", "production", repository.generation(sensorId))
        assertEquals(listOf(retained), repository.coverage(sensorId, "account", "production", Long.MIN_VALUE))
    }

    @Test fun `revalidating part of stale coverage preserves only its unchecked remainders`() {
        val stale = now - SensorHistoryRepository.CACHE_FRESHNESS_MILLIS - 1
        HistoryCoverage(sensorId = sensorId, account = "account", backend = "production",
            startMillis = retained.startMillis, endExclusiveMillis = now, fetchedAt = stale).insert()
        val refreshed = HistoryRange(now - 7 * 86_400_000L, now - 6 * 86_400_000L)
        repository.saveCloudPage(sensorId, emptyList(), refreshed, "account", "production", repository.generation(sensorId))
        assertEquals(listOf(refreshed), repository.coverage(sensorId, "account", "production", stale + 1))
        assertEquals(listOf(HistoryRange(retained.startMillis, refreshed.startMillis), refreshed,
            HistoryRange(refreshed.endExclusiveMillis, now)), repository.coverage(sensorId, "account", "production", Long.MIN_VALUE))
    }

    @Test fun `duplicate cloud pages do not grow readings or invalidate cached graphs`() {
        val readings = listOf(point(now - 10_000), point(now - 5000))
        val generation = repository.generation(sensorId)
        repository.saveCloudPage(sensorId, readings, retained, "account", "production", generation)
        val revision = repository.revision(sensorId)
        repeat(3) { repository.saveCloudPage(sensorId, readings, retained, "account", "production", generation) }
        assertEquals(2L, repository.countAll())
        assertEquals(revision, repository.revision(sensorId))
    }

    @Test fun `cloud persistence accepts only readings inside the fetched page interval`() {
        val range = HistoryRange(now - 10_000, now - 5000)
        repository.saveCloudPage(sensorId, listOf(point(range.startMillis - 5000), point(range.startMillis),
            point(range.endExclusiveMillis)), range, "account", "production", repository.generation(sensorId))
        assertEquals(listOf(range.startMillis), repository.getHistory(sensorId, retained).map { it.createdAt.time })
    }

    @Test fun `migration adds range cache without erasing existing readings or guessing coverage`() {
        SQLiteDatabase.create(null).use { sqlite ->
            sqlite.execSQL("CREATE TABLE SensorSettings(id TEXT PRIMARY KEY, networkHistoryLastSync INTEGER)")
            sqlite.execSQL("CREATE TABLE TagSensorReading(id INTEGER PRIMARY KEY, ruuviTagId TEXT, createdAt INTEGER)")
            sqlite.execSQL("INSERT INTO SensorSettings VALUES ('sensor', $now)")
            sqlite.execSQL("INSERT INTO TagSensorReading VALUES (1, 'sensor', $now)")
            LocalDatabase.Migration43().migrate(AndroidDatabase.from(sqlite))
            sqlite.rawQuery("SELECT COUNT(*) FROM TagSensorReading", null).use { cursor ->
                assertTrue(cursor.moveToFirst()); assertEquals(1, cursor.getInt(0))
            }
            sqlite.rawQuery("SELECT cloudHistoryDays FROM SensorSettings", null).use { cursor ->
                assertTrue(cursor.moveToFirst()); assertTrue(cursor.isNull(0))
            }
            sqlite.rawQuery("SELECT COUNT(*) FROM HistoryCoverage", null).use { cursor ->
                assertTrue(cursor.moveToFirst()); assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test fun `100 day minute resolution history is sampled to a bounded chart`() {
        val db = FlowManager.getWritableDatabase(LocalDatabase.NAME)
        db.execSQL("""WITH RECURSIVE ticks(n) AS (SELECT 0 UNION ALL SELECT n+1 FROM ticks WHERE n < 143999)
            INSERT INTO TagSensorReading(ruuviTagId, createdAt, temperature)
            SELECT '$sensorId', ${retained.startMillis} + n * 60000, 20 FROM ticks""")
        assertEquals(144000L, repository.getCount(sensorId, retained))
        val sampler = HistorySampler(retained)
        repository.forEachReading(sensorId, retained) { sampler.add(it.createdAt.time, it.temperature) }
        val sampled = sampler.finish()
        assertEquals(144000L, sampled.statistics!!.count)
        assertTrue(sampled.points.size <= HistorySampler.MAX_POINTS)
        assertTrue(sampled.points.zipWithNext().all { (a, b) -> a.timestamp <= b.timestamp })
        assertTrue(sampled.points.all { it.timestamp in retained.startMillis until retained.endExclusiveMillis })
        // Sparse cloud pages can overlap a much denser local BLE interval.
        repository.saveCloudPage(sensorId, listOf(point(retained.startMillis), point(retained.startMillis + 72_000 * 60_000L),
            point(now - 60_000), point(now - 55_000)), retained, "account", "production", repository.generation(sensorId))
        assertEquals(144001L, repository.getCount(sensorId, retained))
    }

    private fun point(time: Long) = TagSensorReading(ruuviTagId = sensorId, createdAt = Date(time), temperature = 20.0, dataFormat = 5)
}
