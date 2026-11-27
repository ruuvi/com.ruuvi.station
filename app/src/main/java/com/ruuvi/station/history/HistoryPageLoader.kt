package com.ruuvi.station.history

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import java.io.IOException
import javax.net.ssl.SSLException
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** Serial requests with cancellable, bounded retries; preserve spacing after throttling. */
class HistoryPageLoader(
    private val load: suspend (HistoryQuery) -> HistoryPage,
    private val pause: suspend (Long) -> Unit = { delay(it) },
    private val jitter: () -> Double = { Random.nextDouble(1.0, 1.25) },
) {
    private var spacing = 0L
    private var successes = 0
    suspend fun next(query: HistoryQuery): HistoryPage {
        currentCoroutineContext().ensureActive()
        if (spacing > 0) pause(spacing)
        for (attempt in 0..5) {
            currentCoroutineContext().ensureActive()
            try {
                val page = load(query)
                currentCoroutineContext().ensureActive()
                query.validate(page)
                if (++successes >= 3) { spacing = (spacing * .75).toLong(); successes = 0 }
                return page
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                val wait = retryDelay(error, attempt, spacing, jitter())
                if (attempt == 5 || wait == null) throw error
                successes = 0
                // Only explicit throttling retains pacing after the failed page recovers.
                if (error is HistoryFailure.Http && error.status == 429)
                    spacing = max(spacing, min(30000, wait))
                pause(wait)
            }
        }
        error("Unreachable")
    }
    /** One request at a time, at most one page ahead of decoding/storage. */
    suspend fun pages(initial: HistoryQuery,
                      consume: suspend (HistoryPage, HistoryQuery, HistoryWindow) -> Unit) = coroutineScope {
        fun fetch(query: HistoryQuery) = async {
            try { Result.success(next(query)) }
            catch (error: Exception) { currentCoroutineContext().ensureActive(); Result.failure(error) }
        }
        var query = initial
        var pending = fetch(query)
        while (true) {
            val page = pending.await().getOrThrow()
            currentCoroutineContext().ensureActive()
            val lower = page.readings.lastOrNull()?.timestamp ?: query.window.since
            val covered = HistoryWindow(lower, query.window.until)
            val following = if (lower > query.window.since)
                query.copy(window = HistoryWindow(query.window.since, lower - 1)) else null
            // Start the next HTTP request before decoding, merging or persisting this page.
            val prefetched = following?.let(::fetch)
            consume(page, query, covered)
            currentCoroutineContext().ensureActive()
            if (following == null || prefetched == null) break
            query = following; pending = prefetched
        }
    }
    companion object {
        fun retryDelay(error: Exception, attempt: Int, spacing: Long = 0, jitter: Double = 1.0): Long? {
            val header = when (error) {
                is HistoryFailure.Http -> {
                    if (error.status !in listOf(408, 429, 500, 502, 503, 504)) return null
                    (error.retryAfterMillis ?: 0).coerceAtLeast(0)
                }
                is SSLException -> return null
                is IOException -> 0L
                else -> return null
            }
            if (header > 120000) return null
            val exponential = min(30000L, max(spacing, 2000L shl attempt.coerceIn(0, 5)))
            return max(header, (exponential * jitter.coerceIn(1.0, 1.25)).toLong())
        }
    }
}
