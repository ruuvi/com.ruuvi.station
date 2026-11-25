package com.ruuvi.station.network.domain

import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import kotlin.math.abs

/**
 * Offset between Ruuvi Cloud time and the device clock.
 *
 * Cloud measurement timestamps are real time, while everything else in the app (Bluetooth
 * readings, "updated ago", stale checks, alarms) uses the device clock. When the user sets the
 * device time manually, Cloud timestamps are converted to device time so the app respects it.
 *
 * The offset is measured from the HTTP `Date` header of Cloud responses. It only has second
 * precision, so offsets below [MIN_OFFSET_MILLIS] are treated as a correctly set clock, and the
 * stored offset only changes when it moves by more than [UPDATE_TOLERANCE_MILLIS]. Keeping it
 * stable means the same Cloud point always converts to the same device time.
 */
object CloudClock {
    internal const val MIN_OFFSET_MILLIS = 60_000L
    internal const val UPDATE_TOLERANCE_MILLIS = 30_000L

    /** Cloud time minus device time. */
    @Volatile
    var offsetMillis: Long = 0L
        private set

    fun update(serverTimeMillis: Long, deviceTimeMillis: Long) {
        val measured = (serverTimeMillis - deviceTimeMillis).takeIf { abs(it) >= MIN_OFFSET_MILLIS } ?: 0L
        if (abs(measured - offsetMillis) > UPDATE_TOLERANCE_MILLIS || (measured == 0L && offsetMillis != 0L)) {
            Timber.d("Cloud clock offset changed from $offsetMillis ms to $measured ms")
            offsetMillis = measured
        }
    }

    fun toDeviceTime(cloudTimeMillis: Long): Long = cloudTimeMillis - offsetMillis

    fun toCloudTime(deviceTimeMillis: Long): Long = deviceTimeMillis + offsetMillis

    internal fun reset() {
        offsetMillis = 0L
    }

    /** Network interceptor that measures the offset from each Cloud response. */
    val interceptor = Interceptor { chain ->
        chain.proceed(chain.request()).also { response -> measure(response) }
    }

    internal fun measure(response: Response) {
        val serverDate = response.headers.getDate("Date") ?: return
        // The server stamps the Date header while handling the request, so compare it with the
        // middle of the round trip on the device clock.
        val deviceTime = (response.sentRequestAtMillis + response.receivedResponseAtMillis) / 2
        if (deviceTime <= 0L) return
        update(serverDate.time, deviceTime)
    }
}
