package com.ruuvi.station.tag.domain

import com.ruuvi.station.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class RuuviTagTest {
    @Test
    fun `measurement matching network sync uses cloud source`() {
        val timestamp = Date(1_000L)
        val sensor = ruuviTagPreview.copy(
            networkLastSync = timestamp,
            latestMeasurement = sensorMeasurementsPreview.copy(updatedAt = timestamp),
        )

        assertEquals(UpdateSource.Cloud, sensor.getSource())
    }

    @Test
    fun `measurement not matching network sync uses advertisement source`() {
        val sensor = ruuviTagPreview.copy(
            networkLastSync = Date(1_000L),
            latestMeasurement = sensorMeasurementsPreview.copy(updatedAt = Date(2_000L)),
        )

        assertEquals(UpdateSource.Advertisement, sensor.getSource())
    }

    @Test
    fun `normal source icons follow source and theme`() {
        assertEquals(R.drawable.ic_icon_gateway_light, UpdateSource.Cloud.getIconResource())
        assertEquals(
            R.drawable.ic_icon_gateway_dark,
            UpdateSource.Cloud.getIconResource(isDarkTheme = true),
        )
        assertEquals(
            R.drawable.ic_icon_bluetooth_light,
            UpdateSource.Advertisement.getIconResource(),
        )
        assertEquals(
            R.drawable.ic_icon_bluetooth_dark,
            UpdateSource.Advertisement.getIconResource(isDarkTheme = true),
        )
    }

    @Test
    fun `stale source icons follow source and theme`() {
        assertEquals(
            R.drawable.gw_icon_warning_light,
            UpdateSource.Cloud.getIconResource(isStale = true, isDarkTheme = false),
        )
        assertEquals(
            R.drawable.gw_icon_warning_dark,
            UpdateSource.Cloud.getIconResource(isStale = true, isDarkTheme = true),
        )
        assertEquals(
            R.drawable.bt_icon_warning_light,
            UpdateSource.Advertisement.getIconResource(isStale = true, isDarkTheme = false),
        )
        assertEquals(
            R.drawable.bt_icon_warning_dark,
            UpdateSource.Advertisement.getIconResource(isStale = true, isDarkTheme = true),
        )
    }

    @Test
    fun `fresh icon overload delegates to normal source icon`() {
        assertEquals(
            R.drawable.ic_icon_gateway_dark,
            UpdateSource.Cloud.getIconResource(isStale = false, isDarkTheme = true),
        )
        assertEquals(
            R.drawable.ic_icon_bluetooth_light,
            UpdateSource.Advertisement.getIconResource(isStale = false, isDarkTheme = false),
        )
    }

    @Test
    fun `source descriptions identify cloud and advertisement`() {
        assertEquals(R.string.cloud, UpdateSource.Cloud.getDescriptionResource())
        assertEquals(R.string.advertisement, UpdateSource.Advertisement.getDescriptionResource())
    }

    @Test
    fun `sensor without a measurement is not stale`() {
        val sensor = ruuviTagPreview.copy(latestMeasurement = null)

        assertFalse(sensor.isMeasurementStale(currentTimeMillis = 60_000L, timeoutSeconds = 60L))
    }

    @Test
    fun `measurement is fresh immediately before timeout`() {
        val sensor = ruuviTagPreview.copy(
            latestMeasurement = sensorMeasurementsPreview.copy(updatedAt = Date(1_000L)),
        )

        assertFalse(sensor.isMeasurementStale(currentTimeMillis = 60_999L, timeoutSeconds = 60L))
    }

    @Test
    fun `measurement becomes stale exactly at timeout`() {
        val sensor = ruuviTagPreview.copy(
            latestMeasurement = sensorMeasurementsPreview.copy(updatedAt = Date(1_000L)),
        )

        assertTrue(sensor.isMeasurementStale(currentTimeMillis = 61_000L, timeoutSeconds = 60L))
    }

    @Test
    fun `air formats include current and legacy variants`() {
        listOf(0xE0, 0xF0, 0xE1, 0x06).forEach {
            assertTrue(RuuviTag.dataFormatIsAir(it))
        }
        assertFalse(RuuviTag.dataFormatIsAir(null))
        assertFalse(RuuviTag.dataFormatIsAir(5))
    }
}
