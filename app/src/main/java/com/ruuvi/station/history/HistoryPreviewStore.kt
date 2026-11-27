package com.ruuvi.station.history

import com.google.gson.Gson
import java.io.File

/** Disposable, bounded drawing previews. Raw history remains the source of truth. */
class HistoryPreviewStore(private val root: File) {
    private data class Snapshot(val key: String, val scope: String, val sensor: String, val plot: HistoryPlot)
    private val memoryLock = Any()
    private val diskLock = Any()
    private val memory = linkedMapOf<String, Snapshot>()
    private val gson = Gson()
    private fun id(scope: String, sensor: String, key: String) = HistoryPageCache.hash("$scope|$sensor|$key")
    private fun remember(snapshot: Snapshot) = synchronized(memoryLock) {
        memory.remove(snapshot.key); memory[snapshot.key] = snapshot
        while (memory.size > 16) memory.remove(memory.keys.first())
    }
    private fun valid(plot: HistoryPlot): Boolean = try {
        HistoryWindow(plot.window.since, plot.window.until)
        plot.complete && plot.samples.size <= 32 && plot.statistics.size <= 32 &&
            plot.coverage.size <= 4096 && plot.readings >= 0 && plot.samples.values.all { rows ->
                rows.size <= HistorySampler.MAX_POINTS && rows.zipWithNext().all { it.first.timestamp < it.second.timestamp } &&
                    rows.all { it.timestamp in plot.window && it.value.isFinite() && it.segment >= 0 }
            } && plot.statistics.values.all { it.minimum.isFinite() && it.maximum.isFinite() && it.average.isFinite() } &&
            plot.coverage.all { it.intersect(plot.window) == it }
    } catch (_: Exception) { false }
    private fun preview(snapshot: Snapshot?, window: HistoryWindow): HistoryPlot? {
        val plot = snapshot?.plot ?: return null
        if (plot.window.until > window.until || plot.window.intersect(window) == null) return null
        return plot.copy(window = window, samples = plot.samples.mapValues { (_, rows) -> rows.filter { it.timestamp in window } },
            coverage = plot.coverage.mapNotNull { it.intersect(window) }, complete = false, statisticsPartial = true)
    }
    /** No file I/O: safe during the controller's first synchronous state update. */
    fun peek(scope: String, sensor: String, key: String, window: HistoryWindow): HistoryPlot? = synchronized(memoryLock) {
        preview(memory[id(scope, sensor, key)], window)
    }
    /** Called only from the background history pipeline. */
    fun read(scope: String, sensor: String, key: String, window: HistoryWindow): HistoryPlot? {
        peek(scope, sensor, key, window)?.let { return it }
        return synchronized(diskLock) {
            val id = id(scope, sensor, key); val file = File(root, "${HistoryPageCache.hash(sensor)}.$id.json")
            try {
                if (!file.isFile || file.length() > 2 * 1024 * 1024) return@synchronized null
                val item = gson.fromJson(file.readText(), Snapshot::class.java)
                require(item.key == id && item.scope == HistoryPageCache.hash(scope) && item.sensor == HistoryPageCache.hash(sensor) && valid(item.plot))
                remember(item); preview(item, window)
            } catch (_: Exception) { file.delete(); null }
        }
    }
    fun save(scope: String, sensor: String, key: String, plot: HistoryPlot) = synchronized(diskLock) {
        if (!valid(plot) || plot.statisticsPartial) return@synchronized
        val id = id(scope, sensor, key)
        val item = Snapshot(id, HistoryPageCache.hash(scope), HistoryPageCache.hash(sensor), plot)
        // A full disk must never turn a successful history load into an error.
        try {
            val bytes = gson.toJson(item).toByteArray(Charsets.UTF_8)
            if (bytes.size > 2 * 1024 * 1024) return@synchronized
            check(root.isDirectory || root.mkdirs())
            val file = File(root, "${HistoryPageCache.hash(sensor)}.$id.json"); val temporary = File(root, "${HistoryPageCache.hash(sensor)}.$id.tmp")
            temporary.writeBytes(bytes)
            check(temporary.renameTo(file))
            var size = 0L
            root.listFiles().orEmpty().filter { it.extension == "json" }.sortedByDescending { it.lastModified() }
                .forEachIndexed { index, path -> size += path.length(); if (index >= 64 || size > 32L * 1024 * 1024) path.delete() }
        } catch (_: Exception) { /* The in-memory preview is still useful. */ }
        remember(item)
    }
    fun remove(scope: String, sensor: String) = discard(HistoryPageCache.hash(sensor), HistoryPageCache.hash(scope), null)
    fun prune(sensor: String, cutoff: Long) = discard(HistoryPageCache.hash(sensor), null, cutoff)
    private fun discard(sensor: String, scope: String?, cutoff: Long?) = synchronized(diskLock) {
        fun matches(item: Snapshot) = item.sensor == sensor && (scope == null || item.scope == scope) &&
            (cutoff == null || item.plot.window.since < cutoff)
        fun retained(item: Snapshot): Snapshot? {
            if (cutoff == null || item.plot.window.until < cutoff) return null
            val range = HistoryWindow(cutoff, item.plot.window.until)
            return item.copy(plot = item.plot.copy(window = range,
                samples = item.plot.samples.mapValues { (_, rows) -> rows.filter { it.timestamp >= cutoff } },
                statistics = emptyMap(), coverage = item.plot.coverage.mapNotNull { it.intersect(range) },
                readings = 0, statisticsPartial = true))
        }
        synchronized(memoryLock) {
            for ((key, item) in memory.toMap()) if (matches(item)) {
                val clipped = retained(item)
                if (clipped == null) memory.remove(key) else memory[key] = clipped
            }
        }
        root.listFiles().orEmpty().filter { it.extension == "json" && it.name.startsWith("$sensor.") }.forEach { file ->
            try {
                if (file.length() > 2 * 1024 * 1024) { file.delete(); return@forEach }
                val item = gson.fromJson(file.readText(), Snapshot::class.java)
                if (!matches(item)) return@forEach
                val clipped = retained(item)
                if (clipped == null) file.delete() else {
                    val temporary = File(file.parentFile, file.name + ".tmp")
                    temporary.writeText(gson.toJson(clipped)); check(temporary.renameTo(file))
                }
            } catch (_: Exception) { file.delete() }
        }
    }
}
