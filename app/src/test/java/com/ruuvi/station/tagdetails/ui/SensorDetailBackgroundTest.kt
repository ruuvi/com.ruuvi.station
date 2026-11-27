package com.ruuvi.station.tagdetails.ui

import android.net.Uri
import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlarmSensorStatus
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.SensorMeasurements
import com.ruuvi.station.util.ui.UiTestApplication
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = UiTestApplication::class, sdk = [35])
class SensorDetailBackgroundTest {
    @Test
    fun `sensors sharing the default background do not cross-fade`() {
        assertEquals(sensorBackground(sensor("a")), sensorBackground(sensor("b")))
    }

    @Test
    fun `sensors with different backgrounds cross-fade`() {
        val photo = sensorBackground(sensor("a", userBackground = "file:///data/background_a.jpg"))

        assertEquals(Uri.parse("file:///data/background_a.jpg"), photo.userBackground)
        assertNotEquals(sensorBackground(sensor("b")), photo)
        assertNotEquals(
            photo,
            sensorBackground(sensor("c", userBackground = "file:///data/background_c.jpg")),
        )
    }

    @Test
    fun `air sensors use the air default background`() {
        val air = sensor("air", latestMeasurement = mockk { every { dataFormat } returns 0xE1 })

        assertEquals(R.drawable.new_bg_air, sensorBackground(air).defaultBackground)
        assertEquals(R.drawable.new_bg2, sensorBackground(sensor("tag")).defaultBackground)
    }

    @Test
    fun `blank or pathless user backgrounds fall back to the default`() {
        assertNull(sensorBackground(sensor("blank", userBackground = " ")).userBackground)
        assertNull(sensorBackground(sensor("no path", userBackground = "file://")).userBackground)
        assertEquals(R.drawable.new_bg2, sensorBackground(null).defaultBackground)
    }

    private fun sensor(
        id: String,
        userBackground: String? = null,
        latestMeasurement: SensorMeasurements? = null,
    ) = RuuviTag(
        id = id,
        name = id,
        displayName = id,
        temperatureOffset = null,
        humidityOffset = null,
        pressureOffset = null,
        defaultBackground = R.drawable.new_bg2,
        userBackground = userBackground,
        networkBackground = null,
        alarmSensorStatus = AlarmSensorStatus.NoAlarms,
        lastSync = null,
        networkLastSync = null,
        networkSensor = false,
        owner = null,
        subscriptionName = null,
        firmware = null,
        defaultDisplayOrder = true,
        displayOrder = emptyList(),
        possibleDisplayOptions = emptyList(),
        description = null,
        valuesToDisplay = emptyList(),
        latestMeasurement = latestMeasurement,
    )
}
