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
        val response = responseBuilder()
            .header("Date", "Thu, 01 Jan 2026 00:00:00 GMT")
            .sentRequestAtMillis(deviceTime - 200)
            .receivedResponseAtMillis(deviceTime + 200)
            .build()
        val chain = mockk<Interceptor.Chain> {
            every { request() } returns response.request
            every { proceed(response.request) } returns response
        }

        assertSame(response, CloudClock.interceptor.intercept(chain))
        assertEquals(-3_600_000L, CloudClock.offsetMillis)
    }

    @Test
    fun `missing or unusable response timing leaves the measured offset unchanged`() {
        CloudClock.update(serverTimeMillis = 3_600_000L, deviceTimeMillis = 0L)
        val response = responseBuilder()
            .sentRequestAtMillis(0)
            .receivedResponseAtMillis(0)

        CloudClock.measure(response.build())
        assertEquals(3_600_000L, CloudClock.offsetMillis)

        CloudClock.measure(response.header("Date", "Thu, 01 Jan 2026 00:00:00 GMT").build())
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
    fun `offset resets when the clock is corrected`() {
        CloudClock.update(serverTimeMillis = 3_600_000L, deviceTimeMillis = 0L)
        CloudClock.update(serverTimeMillis = 5_000L, deviceTimeMillis = 4_000L)
        assertEquals(0L, CloudClock.offsetMillis)
    }
}
