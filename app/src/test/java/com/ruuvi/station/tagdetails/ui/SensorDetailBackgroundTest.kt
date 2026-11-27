package com.ruuvi.station.tagdetails.ui

import android.content.Context
import android.net.Uri
import androidx.compose.ui.unit.IntSize
import androidx.test.core.app.ApplicationProvider
import coil.size.Scale
import coil.transition.Transition
import coil.size.Size
import kotlinx.coroutines.runBlocking
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
    fun `first Air measurement keeps the same custom background key`() {
        val photo = "file:///data/background_a.jpg"
        val before = sensor("air", userBackground = photo)
        val after = sensor(
            "air",
            userBackground = photo,
            latestMeasurement = mockk { every { dataFormat } returns 0xE1 },
        )

        assertEquals(sensorBackground(before), sensorBackground(after))
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

    @Test
    fun `background request decodes at the card size with crop scaling`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val request = sensorBackgroundRequest(
            context = context,
            userBackground = Uri.parse("file:///data/background_a.jpg"),
            defaultBackground = R.drawable.new_bg2,
            size = IntSize(1080, 2400),
        )

        assertEquals(Size(1080, 2400), request.sizeResolver.size())
        assertEquals(Scale.FILL, request.scale)
        assertEquals(Uri.parse("file:///data/background_a.jpg"), request.data)
        // Coil's own fade is off: the background transition is the only animation.
        assertEquals(Transition.Factory.NONE, request.transitionFactory)
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
