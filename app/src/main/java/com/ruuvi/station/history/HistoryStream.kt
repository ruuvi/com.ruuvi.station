package com.ruuvi.station.history

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.math.max
import kotlin.math.min

typealias HistoryDecode = (HistoryReading) -> Map<String, Double>
typealias HistoryLocalLoad = suspend (HistoryWindow, String?) -> HistoryLocalPage<HistoryValues>

class HistoryStream(private val cache: HistoryPageCache, private val load: suspend (HistoryQuery) -> HistoryPage,
                    private val clock: () -> Long = System::currentTimeMillis) {
    fun snapshots(sensor: String, window: HistoryWindow, metrics: List<String>, scope: String, decoderKey: String,
                  refresh: HistoryRefresh?, local: HistoryLocalLoad, decode: HistoryDecode): Flow<HistoryProgress> =
        snapshots(sensor, window, metrics, scope, decoderKey, refresh, local, decode, null)

    fun snapshots(sensor: String, window: HistoryWindow, metrics: List<String>, scope: String, decoderKey: String,
                  refresh: HistoryRefresh?, local: HistoryLocalLoad, decode: HistoryDecode, previewKey: String?): Flow<HistoryProgress> = flow {
        val savedPreview = previewKey?.let { cache.previews.read(scope, sensor, it, window) }
        savedPreview?.let { emit(HistoryProgress(it, 0f, 0, 0, preview = true)) }
        val now = clock(); val split = now / 1000 - 86400
        val run = Run(sensor, window, metrics, split, local, decode)
        val revalidate = refresh != null && refresh.networkSince == null && refresh.since != Long.MAX_VALUE
        val preview = if (revalidate) Run(sensor, window, metrics, split, local, decode) else run
        var staleRecent = false
        var lastPublish = 0L
        suspend fun publish(state: Run, force: Boolean = false, complete: Boolean = false) {
            if (!force && clock() - lastPublish < 250) return
            lastPublish = clock()
            val update = state.progress(complete)
            // Conflation must never replace the ready preview with a half-rebuilt graph.
            emit(if (savedPreview != null && !complete) update.copy(plot = savedPreview, preview = true) else update)
        }
        for (entry in cache.candidates(scope, sensor, now)) {
            currentCoroutineContext().ensureActive()
            // A downloaded second remains available when it ages past the 24-hour split.
            val range = entry.window.intersect(window) ?: continue
            val loaded = preview.combinedCoverage()
            val missing = loaded.missing(range)
            if (missing.isEmpty()) continue
            val cached = cache.read(entry, sensor) ?: continue
            val prepared = cached.values.takeIf { cached.decoderKey == decoderKey }
            val values = preview.include(cached.page, missing, entry.mode, prepared)
            preview.cachedPages++
            val reusable = if (revalidate && refresh != null && entry.created < refresh.startedMillis) {
                if (missing.any { it.until >= refresh.since }) staleRecent = true
                missing.mapNotNull { if (it.since < refresh.since) HistoryWindow(it.since, min(it.until, refresh.since - 1)) else null }
            } else missing
            if (preview !== run) { run.include(cached.page, reusable, entry.mode, values); run.cachedPages++ }
            publish(preview)
        }
        publish(preview, force = true)
        val loader = HistoryPageLoader(load)
        val stored = run.combinedCoverage()
        val requests = HistoryMode.entries.flatMap { mode ->
            val lane = run.lane(mode)
            val lower = lane?.let { max(it.since, refresh?.networkSince ?: it.since) }
            if (lane == null || lower == null || lower > lane.until) emptyList()
            else stored.missing(HistoryWindow(lower, lane.until)).asReversed().map { HistoryQuery(sensor, it, mode) }
        }
        run.networkTotal = requests.sumOf { it.window.seconds }
        for (request in requests) {
            run.downloading = true
            publish(run, force = true)
            loader.pages(request) { page, query, checked ->
                val values = run.include(page, listOf(checked), query.mode, null)
                run.networkPages++; run.networkCovered += checked.seconds
                cache.save(page, query, checked, scope, clock(), decoderKey, values)
                if (!(staleRecent && refresh != null && checked.since >= refresh.since)) publish(run, force = run.networkPages == 1)
            }
        }
        if (previewKey != null) cache.previews.save(scope, sensor, previewKey, run.progress(true).plot)
        publish(run, force = true, complete = true)
    }.flowOn(Dispatchers.IO).buffer(Channel.CONFLATED)

    private class Run(val sensor: String, val window: HistoryWindow, metrics: List<String>, val split: Long,
                      val local: HistoryLocalLoad, val decode: HistoryDecode) {
        val coverage = HistoryMode.entries.associateWith { HistoryRanges() }
        private val summary = HistorySampler(window, metrics)
        var cachedPages = 0; var networkPages = 0
        var downloading = false
        var networkTotal = 0L; var networkCovered = 0L
        fun combinedCoverage() = HistoryRanges().also { all -> coverage.values.flatMap { it.ranges }.forEach(all::insert) }
        fun lane(mode: HistoryMode): HistoryWindow? {
            val lower = if (mode == HistoryMode.DENSE) max(window.since, split + 1) else window.since
            val upper = if (mode == HistoryMode.SPARSE) min(window.until, split) else window.until
            return if (lower <= upper) HistoryWindow(lower, upper) else null
        }
        suspend fun include(page: HistoryPage, ranges: List<HistoryWindow>, mode: HistoryMode,
                            prepared: List<Map<String, Double>>?): List<Map<String, Double>> {
            val values = prepared ?: page.readings.map { currentCoroutineContext().ensureActive(); decode(it) }
            for (range in ranges) {
                val rows = page.readings.mapIndexedNotNull { index, row ->
                    if (row.timestamp in range) HistoryValues(row.timestamp, values[index]) else null
                }
                HistoryLocalMerger.merge(rows, range, { it.timestamp }, local) { summary.addPage(it) }
                coverage.getValue(mode).insert(range)
            }
            return values
        }
        fun progress(complete: Boolean): HistoryProgress {
            val combined = combinedCoverage()
            val fraction = if (complete) 1f else if (networkTotal > 0)
                (networkCovered.toDouble() / networkTotal).coerceIn(0.0, 1.0).toFloat() else 0f
            return HistoryProgress(summary.plot(combined.ranges, complete).copy(
                statisticsPartial = complete && combined.missing(window).isNotEmpty()), fraction, networkPages, cachedPages, downloading && !complete)
        }
    }

    /** Backpressured traversal for export. No producer queue and at most one raw page in memory. */
    suspend fun read(sensor: String, window: HistoryWindow, scope: String, refresh: HistoryRefresh?,
                     consume: suspend (HistoryPage, HistoryWindow) -> Unit) {
        val now = clock(); val split = now / 1000 - 86400
        suspend fun candidates() = cache.candidates(scope, sensor, now).mapNotNull { entry ->
            val upper = if (refresh != null && entry.created < refresh.startedMillis) min(entry.upper, refresh.since - 1) else entry.upper
            if (upper >= entry.lower) entry to upper else null
        }.toMutableList()
        var reusable = candidates()
        val loader = HistoryPageLoader(load)
        for (mode in HistoryMode.entries) {
            val lower = if (mode == HistoryMode.DENSE) max(window.since, split + 1) else window.since
            var cursor = if (mode == HistoryMode.SPARSE) min(window.until, split) else window.until
            while (cursor >= lower) {
                currentCoroutineContext().ensureActive()
                val matching = reusable.filter { it.second >= lower && it.first.lower <= cursor }
                val index = matching.indexOfFirst { cursor in it.first.lower..it.second }
                if (index >= 0) {
                    val entry = matching[index].first
                    val cached = cache.read(entry, sensor)
                    if (cached == null) { reusable = candidates(); continue }
                    val newerBoundary = matching.take(index).filter { it.second < cursor }.maxOfOrNull { it.second + 1 } ?: lower
                    val start = max(max(lower, entry.lower), newerBoundary)
                    val checked = HistoryWindow(start, cursor)
                    consume(HistoryPage(sensor, cached.page.readings.filter { it.timestamp in checked }), checked)
                    cursor = start - 1
                } else {
                    val start = max(lower, (matching.maxOfOrNull { it.second } ?: (lower - 1)) + 1)
                    val query = HistoryQuery(sensor, HistoryWindow(start, cursor), mode)
                    val page = loader.next(query)
                    val checked = HistoryWindow(page.readings.lastOrNull()?.timestamp ?: start, cursor)
                    cache.save(page, query, checked, scope, clock())
                    reusable = candidates()
                    consume(page, checked); cursor = checked.since - 1
                }
            }
        }
    }
}
