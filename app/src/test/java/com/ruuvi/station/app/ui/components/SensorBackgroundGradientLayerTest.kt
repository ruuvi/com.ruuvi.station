package com.ruuvi.station.app.ui.components

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.ruuvi.station.dashboard.ui.DashboardImage
import com.ruuvi.station.tagdetails.ui.SensorCardImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SensorBackgroundGradientLayerTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun `dashboard gradient covers the photo and keeps the right-side light spot`() {
        verifyGradient(160, 90) { photo ->
            DashboardImage(userBackground = photo, modifier = Modifier.fillMaxSize())
        }
    }

    @Test
    fun `sensor details gradient covers the photo and scales to a portrait layout`() {
        verifyGradient(100, 180) { photo ->
            SensorCardImage(userBackground = photo, defaultBackground = android.R.color.black)
        }
    }

    private fun verifyGradient(width: Int, height: Int, content: @Composable (Uri) -> Unit) {
        val photo = temporaryFolder.newFile("sensor.png")
        val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.rgb(240, 160, 80))
        photo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        compose.setContent {
            Box(Modifier.size(width.dp, height.dp).background(Color.Black).testTag("sensor_photo")) {
                content(Uri.fromFile(photo))
            }
        }

        val centerY = (height * 0.343f).toInt()
        // Coil loads the local image asynchronously before the gradient can multiply its colors.
        compose.waitUntil(timeoutMillis = 5_000) {
            capture()[width - 1, centerY].red > 0.7f
        }
        val pixels = capture()
        val lightSpot = pixels[width - 1, centerY]
        val leftEdge = pixels[0, centerY]
        val lowerRight = pixels[width - 1, height - 1]

        assertMultiplier(lightSpot, 240f / 255)
        assertMultiplier(leftEdge, 105f / 255)
        assertMultiplier(lowerRight, 105f / 255)
        assertTrue(pixels[width / 2, centerY].red in leftEdge.red..lightSpot.red)
    }

    private fun capture() = compose.onNodeWithTag("sensor_photo").captureToImage().toPixelMap()

    private fun assertMultiplier(pixel: Color, multiplier: Float) {
        assertEquals(240f / 255 * multiplier, pixel.red, 0.025f)
        assertEquals(160f / 255 * multiplier, pixel.green, 0.025f)
        assertEquals(80f / 255 * multiplier, pixel.blue, 0.025f)
    }
}
