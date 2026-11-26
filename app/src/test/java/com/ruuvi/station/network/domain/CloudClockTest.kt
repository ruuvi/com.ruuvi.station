package com.ruuvi.station.network.domain

import io.mockk.every
import io.mockk.mockk
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.time.Instant

class CloudClockTest {
    @After
    fun tearDown() = CloudClock.reset()

    @Test
    fun `network interceptor measures the server date against the request midpoint`() {
        val serverTime = Instant.parse("2026-01-01T00:00:00Z").toEpochMilli()
        val deviceTime = serverTime + 3_600_000L
        val wall = ArrayDeque(listOf(deviceTime - 200, deviceTime + 200))
        val monotonic = ArrayDeque(listOf(1_000L, 1_400L))
        CloudClock.wallClock = { wall.removeFirst() }
        CloudClock.monotonicClock = { monotonic.removeFirst() }
        val response = responseBuilder().header("Date", "Thu, 01 Jan 2026 00:00:00 GMT").build()
        val chain = mockk<Interceptor.Chain> {
            every { request() } returns response.request
            every { proceed(response.request) } returns response
        }

        assertSame(response, CloudClock.interceptor.intercept(chain))
        assertEquals(-3_600_000L, CloudClock.offsetMillis)
    }

    @Test
    fun `device time changed during the request leaves the offset unchanged`() {
        val hour = 3_600_000L
        CloudClock.update(serverTimeMillis = 2 * hour, deviceTimeMillis = hour)
        val response = responseBuilder().header("Date", "Thu, 01 Jan 2026 00:00:00 GMT").build()
        val serverTime = Instant.parse("2026-01-01T00:00:00Z").toEpochMilli()

        // Sent with the clock an hour ahead, received after it was set 9 hours back.
        CloudClock.measure(response, wallStart = serverTime + hour, wallEnd = serverTime - 9 * hour, monotonicDuration = 300L)

        assertEquals(hour, CloudClock.offsetMillis)
    }

    @Test
    fun `missing date header leaves the measured offset unchanged`() {
        CloudClock.update(serverTimeMillis = 3_600_000L, deviceTimeMillis = 0L)

        CloudClock.measure(responseBuilder().build(), wallStart = 0L, wallEnd = 100L, monotonicDuration = 100L)

        assertEquals(3_600_000L, CloudClock.offsetMillis)
    }

    private fun responseBuilder() = Response.Builder()
        .request(Request.Builder().url("https://example.test/measurements").build())
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")

    @Test
    fun `small differences are treated as a correct clock`() {
        CloudClock.update(serverTimeMillis = 1_000_000L, deviceTimeMillis = 1_000_000L + 59_000L)
        assertEquals(0L, CloudClock.offsetMillis)
        assertEquals(5_000L, CloudClock.toDeviceTime(5_000L))
    }

    @Test
    fun `device clock ahead converts cloud time forward`() {
        val hours5 = 5 * 3_600_000L
        CloudClock.update(serverTimeMillis = 10_000L, deviceTimeMillis = 10_000L + hours5)
        assertEquals(-hours5, CloudClock.offsetMillis)
        assertEquals(20_000L + hours5, CloudClock.toDeviceTime(20_000L))
        assertEquals(20_000L, CloudClock.toCloudTime(20_000L + hours5))
    }

    @Test
    fun `device clock behind converts cloud time backward`() {
        val hour = 3_600_000L
        CloudClock.update(serverTimeMillis = 10_000L + hour, deviceTimeMillis = 10_000L)
        assertEquals(20_000L, CloudClock.toDeviceTime(20_000L + hour))
    }

    @Test
    fun `offset stays stable within tolerance so the same point converts the same way`() {
        val hour = 3_600_000L
        CloudClock.update(serverTimeMillis = hour, deviceTimeMillis = 0L)
        CloudClock.update(serverTimeMillis = hour + 1_000L, deviceTimeMillis = 0L)
        assertEquals(hour, CloudClock.offsetMillis)
    }

    @Test
    fun `setting timestamps use cloud time when the device clock is off`() {
        val hour = 3_600_000L
        // Device clock is an hour behind the cloud.
        CloudClock.update(serverTimeMillis = 10 * hour, deviceTimeMillis = 9 * hour)
        assertEquals(10 * hour / 1000, CloudClock.nowSeconds(deviceTimeMillis = 9 * hour))
    }

    @Test
    fun `offset resets when the clock is corrected`() {
        CloudClock.update(serverTimeMillis = 3_600_000L, deviceTimeMillis = 0L)
        CloudClock.update(serverTimeMillis = 5_000L, deviceTimeMillis = 4_000L)
        assertEquals(0L, CloudClock.offsetMillis)
    }
}
