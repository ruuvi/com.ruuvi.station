package com.ruuvi.station.history

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID

/** Persistent, backup-excluded history pages. Retention, not other sensors, removes history. */
class HistoryPageCache(private val root: File, private val maximumBytes: Long? = null,
                       private val legacyRoot: File? = null) {
    data class Entry(val id: String, val scope: String, val sensor: String, val mode: HistoryMode,
                     val lower: Long, val upper: Long, val created: Long, val bytes: Long) {
        val window get() = HistoryWindow(lower, upper)
    }
    data class Cached(val page: HistoryPage, val decoderKey: String? = null, val values: List<Map<String, Double>>? = null)
    val previews = HistoryPreviewStore(File(root, "previews"))
    private val gson = Gson()
    private val mutex = Mutex()
    private val buckets = linkedMapOf<File, List<Entry>>()
    private var initialized = false
    private fun directory(scope: String, sensorKey: String) = File(File(root, hash(scope)), sensorKey)
    private fun file(entry: Entry) = File(directory(entry.scope, entry.sensor), "${entry.id}.json")
    private fun atomic(file: File, bytes: ByteArray) {
        check(file.parentFile!!.isDirectory || file.parentFile!!.mkdirs())
        val temporary = File(file.parentFile, file.name + ".tmp")
        try {
            FileOutputStream(temporary).use { it.write(bytes) }
            if (!temporary.renameTo(file)) throw IOException("History storage replacement failed")
        } finally { temporary.delete() }
    }
    private fun decodeIndex(index: File): List<Entry> {
        if (!index.exists()) return emptyList()
        require(index.length() <= 64L * 1024 * 1024)
        val parsed: List<Entry> = gson.fromJson(index.readText(), object : TypeToken<List<Entry>>() {}.type)
        require(parsed.size <= 65536)
        parsed.forEach {
            UUID.fromString(it.id); it.window
            require(it.bytes in 0..MAX_PAGE_BYTES && it.scope.matches(Regex("[0-9a-f]{64}")) && it.sensor.matches(Regex("[0-9a-f]{64}")))
        }
        return parsed
    }
    private fun remember(dir: File, entries: List<Entry>) {
        buckets.remove(dir); buckets[dir] = entries
        while (buckets.size > 8) buckets.remove(buckets.keys.first())
    }
    private fun entries(scope: String, sensorKey: String): List<Entry> {
        val dir = directory(scope, sensorKey)
        buckets[dir]?.let { return it }
        // A damaged/unreadable index is an error, never permission to discard an archive.
        val result = decodeIndex(File(dir, "index.json"))
        require(result.all { it.scope == scope && it.sensor == sensorKey })
        remember(dir, result)
        return result
    }
    private fun commit(scope: String, sensorKey: String, updated: List<Entry>) {
        require(updated.size <= 65536)
        val dir = directory(scope, sensorKey)
        val bytes = gson.toJson(updated).toByteArray(Charsets.UTF_8)
        require(bytes.size <= 64 * 1024 * 1024)
        atomic(File(dir, "index.json"), bytes)
        remember(dir, updated)
    }
    private fun open() {
        if (initialized) return
        check(root.isDirectory || root.mkdirs())
        val marker = File(root, "imported-v3")
        if (legacyRoot != null && !marker.exists()) {
            val old = decodeIndex(File(legacyRoot, "index.json"))
            for ((key, group) in old.groupBy { it.scope to it.sensor }) {
                val current = entries(key.first, key.second).toMutableList()
                for (entry in group) {
                    if (current.any { it.id == entry.id }) continue
                    val source = File(legacyRoot, "${entry.id}.json")
                    if (!source.isFile || source.length() != entry.bytes) continue
                    atomic(file(entry), source.readBytes())
                    current += entry
                }
                commit(key.first, key.second, current)
            }
            atomic(marker, byteArrayOf(1))
            // Leave the old disposable cache intact until the OS removes it.
            // A crash during import can resume without losing the only copy.
        }
        initialized = true
    }
    suspend fun candidates(scope: String, sensor: String, nowMillis: Long): List<Entry> = mutex.withLock {
        open()
        entries(scope, hash(sensor)).filter { it.created <= nowMillis + 60000 }
            .sortedWith(compareBy<Entry> { if (it.mode == HistoryMode.DENSE) 0 else 1 }.thenByDescending { it.created })
    }
    private fun load(entry: Entry, sensor: String): Cached? {
        require(entry.sensor == hash(sensor))
        val path = file(entry)
        if (!path.exists()) return null
        if (path.length() != entry.bytes || entry.bytes > MAX_PAGE_BYTES) return null
        // File-system failures propagate; they must not erase valid coverage.
        val bytes = path.readBytes()
        return try {
            require(bytes.size.toLong() == entry.bytes && bytes.size <= MAX_PAGE_BYTES)
            val cached = gson.fromJson(bytes.toString(Charsets.UTF_8), Cached::class.java)
            HistoryQuery(sensor, entry.window, entry.mode).validate(cached.page)
            cached.values?.let { values -> require(values.size == cached.page.readings.size && values.all { it.size <= 32 && it.all { (_, v) -> v.isFinite() } }) }
            cached
        } catch (_: RuntimeException) { null } catch (_: HistoryFailure.Invalid) { null }
    }
    suspend fun read(entry: Entry, sensor: String): Cached? = mutex.withLock {
        open()
        val current = entries(entry.scope, entry.sensor)
        if (current.none { it.id == entry.id }) return@withLock null
        val cached = load(entry, sensor)
        if (cached == null) {
            commit(entry.scope, entry.sensor, current.filterNot { it.id == entry.id })
            file(entry).delete()
        }
        cached
    }
    private fun merged(older: Cached, newer: Cached, bounds: HistoryWindow): Cached? {
        val prepared = older.decoderKey == newer.decoderKey && older.values != null && newer.values != null
        val rows = linkedMapOf<Long, Pair<HistoryReading, Map<String, Double>>>()
        for (page in listOf(older, newer)) page.page.readings.forEachIndexed { index, row ->
            if (page === newer || row.timestamp !in bounds)
                rows[row.timestamp] = row to if (prepared) page.values!![index] else emptyMap()
        }
        if (rows.size > HistoryQuery.PAGE_SIZE) return null
        val ordered = rows.values.sortedByDescending { it.first.timestamp }
        return Cached(HistoryPage(newer.page.sensor, ordered.map { it.first }),
            if (prepared) newer.decoderKey else null, if (prepared) ordered.map { it.second } else null)
    }
    suspend fun save(page: HistoryPage, query: HistoryQuery, covered: HistoryWindow, scope: String,
                     nowMillis: Long, decoderKey: String? = null, values: List<Map<String, Double>>? = null) = mutex.withLock {
        query.validate(page)
        require(covered.intersect(query.window) == covered && scope.matches(Regex("[0-9a-f]{64}")))
        // Stored coverage must describe every stored reading, including a short final page.
        HistoryQuery(query.sensor, covered, query.mode).validate(page)
        open()
        val sensorKey = hash(query.sensor)
        val current = entries(scope, sensorKey)
        var combined = Cached(page, decoderKey, values)
        var bounds = covered
        var created = nowMillis
        val replaced = current.filter { it.mode == query.mode && it.lower >= covered.since && it.upper <= covered.until }.toMutableSet()
        // Join small adjacent delta pages so minute refreshes cannot create unbounded tiny files.
        for (entry in current.asReversed().filter { it.mode == query.mode && it !in replaced }.take(32)) {
            if (entry.lower > bounds.until + 1 || entry.upper + 1 < bounds.since) continue
            if (maxOf(entry.upper, bounds.until) - minOf(entry.lower, bounds.since) > HistoryWindow.MAX_SPAN) continue
            // Merging must not change which of several overlapping versions wins.
            if (current.any { it.id != entry.id && it !in replaced &&
                it.lower <= maxOf(bounds.until, entry.upper) && it.upper >= minOf(bounds.since, entry.lower) }) continue
            val older = load(entry, query.sensor) ?: continue
            val joined = merged(older, combined, bounds) ?: continue
            if (maximumBytes != null && gson.toJson(joined).toByteArray().size > maximumBytes) continue
            combined = joined; bounds = HistoryWindow(minOf(bounds.since, entry.lower), maxOf(bounds.until, entry.upper))
            created = minOf(created, entry.created); replaced += entry
        }
        val bytes = gson.toJson(combined).toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_PAGE_BYTES)
        if (maximumBytes != null && bytes.size > maximumBytes) return@withLock
        val remaining = current.filterNot { it in replaced }.toMutableList()
        // Explicit budgets are for bounded fixtures; production archives have no eviction budget.
        if (maximumBytes != null) {
            var total = remaining.sumOf { it.bytes }
            while (remaining.isNotEmpty() && total + bytes.size > maximumBytes) {
                val old = remaining.removeAt(0); total -= old.bytes; replaced += old
            }
        }
        val entry = Entry(UUID.randomUUID().toString(), scope, sensorKey, query.mode, bounds.since, bounds.until, created, bytes.size.toLong())
        atomic(file(entry), bytes)
        commit(scope, sensorKey, remaining + entry)
        replaced.forEach { file(it).delete() }
    }
    /** Trim raw rows, prepared values and coverage together, across saved session scopes. */
    suspend fun prune(sensor: String, cutoff: Long) = mutex.withLock {
        open()
        previews.prune(sensor, cutoff)
        val sensorKey = hash(sensor)
        for (scopeDirectory in root.listFiles().orEmpty().filter { it.isDirectory }) {
            val dir = File(scopeDirectory, sensorKey)
            if (!File(dir, "index.json").exists()) continue
            val current = buckets[dir] ?: decodeIndex(File(dir, "index.json"))
            val expired = current.filter { it.lower < cutoff }
            if (expired.isEmpty()) continue
            val updated = current.filterNot { it in expired }.toMutableList()
            for (entry in expired) if (entry.upper >= cutoff) {
                val old = load(entry, sensor) ?: continue
                val keep = old.page.readings.indices.filter { old.page.readings[it].timestamp >= cutoff }
                val trimmed = Cached(old.page.copy(readings = keep.map { old.page.readings[it] }),
                    old.decoderKey, old.values?.let { values -> keep.map { values[it] } })
                val bytes = gson.toJson(trimmed).toByteArray(Charsets.UTF_8)
                val next = entry.copy(id = UUID.randomUUID().toString(), lower = cutoff, bytes = bytes.size.toLong())
                atomic(file(next), bytes); updated += next
            }
            commit(expired.first().scope, sensorKey, updated)
            expired.forEach { file(it).delete() }
        }
    }
    suspend fun remove(scope: String, sensor: String) = mutex.withLock {
        open()
        previews.remove(scope, sensor)
        val key = hash(sensor); val current = entries(scope, key)
        commit(scope, key, emptyList())
        current.forEach { file(it).delete() }
    }
    companion object {
        const val MAX_PAGE_BYTES = 8L * 1024 * 1024
        fun hash(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}
