package com.ruuvi.station.history

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.math.ceil
import kotlin.math.floor

/** UI-thread state owner; page production, decoding and storage remain on IO. */
class HistoryController(private val scope: CoroutineScope, private val previews: HistoryPreviewStore? = null, private val clock: () -> Long = System::currentTimeMillis) {
    data class State(val sensor: String? = null, val overview: HistoryPlot? = null, val detail: HistoryPlot? = null,
                     val visible: HistoryWindow? = null, val progress: Float? = null,
                     val error: Long = 0, val lostAccess: Boolean = false,
                     val moving: Boolean = false,
                     val retainedStatistics: Map<String, HistoryStatistics> = emptyMap()) {
        val timeline get() = overview?.window
        val zoomed get() = visible != null && visible != timeline
        fun statistics(metric: String): HistoryStatistics? {
            val selected = if (zoomed) detail?.takeIf { it.window == visible } else overview
            return selected?.takeIf { it.complete && !it.statisticsPartial }?.statistics?.get(metric)
        }
        fun statisticsDisplay(metric: String): HistoryStatisticsDisplay {
            val selected = if (zoomed) detail?.takeIf { it.window == visible } else overview
            val current = selected?.statistics?.get(metric)
            val covered = selected?.let { plot -> HistoryRanges().also { ranges ->
                plot.coverage.forEach(ranges::insert)
            }.missing(plot.window).isEmpty() } == true
            val phase = when {
                moving -> HistoryStatisticsPhase.UPDATING
                error > 0 && progress == null -> HistoryStatisticsPhase.FAILED
                selected?.statisticsPartial == true -> HistoryStatisticsPhase.PARTIAL
                selected?.complete == true || covered -> HistoryStatisticsPhase.READY
                current != null -> HistoryStatisticsPhase.PARTIAL
                else -> HistoryStatisticsPhase.UPDATING
            }
            val values = if (phase == HistoryStatisticsPhase.READY) current
                else if (moving) retainedStatistics[metric] else current ?: retainedStatistics[metric]
            return HistoryStatisticsDisplay(values, phase)
        }
        /** Separate unknown coverage and overview/detail seams; never bridge an unloaded interval. */
        fun groups(metric: String): List<List<HistorySample>> {
            val overview = overview ?: return emptyList()
            val detail = detail
            val sources = if (detail == null) listOf(overview) else listOf(
                overview.copy(samples = overview.samples.mapValues { (_, values) -> values.filter { it.timestamp !in detail.window } },
                    coverage = overview.coverage.flatMap { range -> HistoryRanges().also { it.insert(detail.window) }.missing(range) }), detail)
            return sources.flatMap { plot -> plot.coverage.map { range -> plot.samples[metric].orEmpty().filter { it.timestamp in range } } }
                .filter { it.isNotEmpty() }.sortedBy { it.first().timestamp }
        }
    }
    private val mutable = MutableStateFlow(State())
    val state: StateFlow<State> = mutable
    private var generation = 0L
    private var runId = 0L
    private var task: Job? = null
    private var timer: Job? = null
    private var activeGesture = false
    private var identity: String? = null
    private var source: ((HistoryWindow, HistoryRefresh) -> Flow<HistoryProgress>)? = null
    private var refresh = HistoryRefresh.opening(clock())
    private var retryNotBefore = 0L
    private var rolling = true

    fun start(sensor: String, key: String, window: HistoryWindow, rolling: Boolean = true, previewScope: String? = null,
              source: (HistoryWindow, HistoryRefresh) -> Flow<HistoryProgress>) {
        if (identity == key && mutable.value.sensor == sensor && mutable.value.timeline != null) return
        stop(clear = true)
        identity = key; this.source = source; this.rolling = rolling
        refresh = HistoryRefresh.opening(clock())
        val saved = previewScope?.let { previews?.peek(it, sensor, key, window) }
        mutable.value = State(sensor, saved ?: HistoryPlot(window), visible = window, progress = null,
            retainedStatistics = saved?.statistics.orEmpty())
        run(window, detail = false)
    }
    fun stop(clear: Boolean = false) {
        generation++; runId++; task?.cancel(); task = null; timer?.cancel(); timer = null; activeGesture = false
        if (clear) { identity = null; source = null; mutable.value = State() }
        else { identity = null; mutable.value = mutable.value.copy(progress = null) }
    }
    fun interaction(active: Boolean) {
        activeGesture = active
        mutable.value = mutable.value.copy(moving = active)
        if (active) { runId++; task?.cancel(); task = null; timer?.cancel(); timer = null }
        else {
            val state = mutable.value
            if (state.zoomed) state.visible?.let { if (state.detail?.window != it || state.detail.complete != true) run(it, true) else schedule() }
            else if (state.overview?.complete != true) state.timeline?.let { run(it, false) }
            else { mutable.value = state.copy(progress = null); schedule() }
        }
    }
    fun move(sinceSeconds: Double, untilSeconds: Double) {
        val state = mutable.value; val timeline = state.timeline ?: return
        if (!sinceSeconds.isFinite() || !untilSeconds.isFinite()) return
        // MPAndroidChart uses Float coordinates. Treat sub-pixel edge rounding as the same clamped view.
        val lower = floor(sinceSeconds).toLong().coerceIn(timeline.since, timeline.until)
        val upper = ceil(untilSeconds).toLong().coerceIn(timeline.since, timeline.until)
        if (lower >= upper) return
        val tolerance = maxOf(1L, (timeline.seconds / 8_000_000))
        val window = if (lower - timeline.since <= tolerance && timeline.until - upper <= tolerance) timeline else HistoryWindow(lower, upper)
        if (state.visible == window && (task != null || (if (window == timeline) state.overview else state.detail)?.complete == true)) return
        runId++; task?.cancel(); task = null
        mutable.value = state.copy(visible = window, detail = if (window == timeline) null else state.detail)
        if (activeGesture) return
        if (window == timeline && state.overview?.complete == true) { mutable.value = mutable.value.copy(progress = null); schedule() }
        else run(window, window != timeline)
    }
    fun refresh(silent: Boolean = false) {
        if (activeGesture) { schedule(); return }
        val state = mutable.value; var window = state.visible ?: return
        if (!state.zoomed && rolling && state.overview?.complete == true && clock() / 1000 - window.until >= 60) {
            val until = clock() / 1000
            window = HistoryWindow(maxOf(0, until - (window.until - window.since)), until)
            if (!silent) mutable.value = state.copy(overview = state.overview.copy(window = window, complete = false), visible = window)
        }
        run(window, state.zoomed, silent)
    }
    private fun run(window: HistoryWindow, detail: Boolean, silent: Boolean = false) {
        if (activeGesture) return
        val source = source ?: return
        val previousPlot = if (detail) mutable.value.detail else mutable.value.overview
        var holdingPreview = !detail && previousPlot?.samples?.values?.any { it.isNotEmpty() } == true
        val incremental = silent && previousPlot?.complete == true
        val policy = if (incremental) HistoryRefresh(previousPlot!!.window.until + 1, clock(),
            networkSince = previousPlot.window.until + 1) else refresh
        task?.cancel(); timer?.cancel()
        val generation = generation; val run = ++runId
        task = scope.launch {
            try {
                if (detail) delay(200)
                delay((retryNotBefore - clock()).coerceAtLeast(0))
                currentCoroutineContext().ensureActive()
                if (!silent) mutable.value = mutable.value.copy(progress = null, error = 0)
                source(window, policy).collect { update ->
                    currentCoroutineContext().ensureActive()
                    if (generation != this@HistoryController.generation || run != runId) return@collect
                    // A completed graph stays visible while a timer checks recent readings.
                    // Cache replay is not a new user-initiated download/progress cycle.
                    if (silent && !update.plot.complete) return@collect
                    if (update.preview) {
                        if (holdingPreview) {
                            mutable.value = mutable.value.copy(progress = if (update.downloading) update.fraction else null)
                            return@collect
                        }
                        holdingPreview = true
                    } else if (holdingPreview && !update.plot.complete) {
                        mutable.value = mutable.value.copy(progress = if (update.downloading) update.fraction else null)
                        return@collect
                    }
                    val previous = mutable.value
                    val plot = if (incremental) update.plot.preservingLoadedHistory(previousPlot!!) else update.plot
                    val retained = if (plot.complete && !plot.statisticsPartial) plot.statistics
                        else previous.retainedStatistics + plot.statistics
                    val old = previous.copy(retainedStatistics = retained, error = 0,
                        visible = if (silent && !detail) window else previous.visible)
                    mutable.value = if (detail) old.copy(detail = plot, progress = if (plot.complete || !update.downloading) null else update.fraction)
                        else old.copy(overview = plot, progress = if (plot.complete || !update.downloading) null else update.fraction)
                }
                currentCoroutineContext().ensureActive()
                if (generation != this@HistoryController.generation || run != runId) return@launch
                task = null; schedule()
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                if (generation != this@HistoryController.generation || run != runId) return@launch
                task = null
                val lost = error is HistoryFailure.Access || error is HistoryFailure.Http && error.status in listOf(401, 403)
                val number = mutable.value.error + 1
                mutable.value = if (lost) State(error = number, lostAccess = true) else mutable.value.copy(progress = null, error = number)
                val retry = when (error) {
                    is HistoryFailure.Http -> if (error.status in listOf(408, 429, 500, 502, 503, 504)) maxOf(60000, error.retryAfterMillis ?: 60000) else null
                    is IOException -> 60000L
                    else -> null
                }
                if (!lost && retry != null && retry <= 86400000L) { retryNotBefore = clock() + retry; schedule(retry) }
            }
        }
    }
    private fun schedule(delayMillis: Long = 60000) {
        timer?.cancel()
        val visible = mutable.value.visible ?: return
        if (visible.until < clock() / 1000 - 300) return
        timer = scope.launch {
            delay(delayMillis)
            if (mutable.value.overview?.complete == true) refresh = HistoryRefresh.live(clock())
            val current = mutable.value
            refresh(silent = (if (current.zoomed) current.detail else current.overview)?.complete == true)
        }
    }
}
