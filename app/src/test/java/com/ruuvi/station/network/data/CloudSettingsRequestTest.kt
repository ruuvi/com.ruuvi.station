package com.ruuvi.station.network.data

import com.ruuvi.station.network.data.request.UpdateSensorRequest
import com.ruuvi.station.network.data.request.UpdateSensorSettingRequest
import com.ruuvi.station.network.data.request.UpdateUserSettingRequest
import com.ruuvi.station.network.domain.CloudClock
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudSettingsRequestTest {
    @After
    fun resetClock() = CloudClock.reset()

    @Test
    fun `new requests default to cloud time when the device clock is ahead`() {
        CloudClock.update(serverTimeMillis = 0L, deviceTimeMillis = 3_600_000L)
        val before = System.currentTimeMillis() / 1000 - 3_600L

        val timestamps = listOf(
            UpdateSensorRequest(sensor = "sensor").timestamp,
            UpdateSensorSettingRequest("sensor", listOf("name"), listOf("Living room")).timestamp,
            UpdateUserSettingRequest("chartDrawDots", "true").timestamp,
        )

        val cloudSeconds = before..(System.currentTimeMillis() / 1000 - 3_600L)
        timestamps.forEach { timestamp -> assertTrue("$timestamp must be in $cloudSeconds", timestamp in cloudSeconds) }
    }

    @Test
    fun `explicit timestamps survive a device clock change`() {
        CloudClock.update(serverTimeMillis = 3_600_000L, deviceTimeMillis = 0L)
        val timestamp = 1_700_000_000L

        assertEquals(timestamp, UpdateSensorRequest(sensor = "sensor", timestamp = timestamp).timestamp)
        assertEquals(
            timestamp,
            UpdateSensorSettingRequest("sensor", listOf("name"), listOf("Living room"), timestamp).timestamp,
        )
        assertEquals(timestamp, UpdateUserSettingRequest("chartDrawDots", "true", timestamp).timestamp)
    }
}
