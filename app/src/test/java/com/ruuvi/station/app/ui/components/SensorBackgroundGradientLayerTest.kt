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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Expected alphas are sampled from the original design bitmaps (`tag_bg_layer_dashboard.png`
 * and `tag_bg_layer.png`) at the given fractions of the design's width and height.
 */
@RunWith(RobolectricTestRunner::class)
// API 26 draws the image and each layer separately (before API 28), API 35 in one shader.
@Config(application = Application::class, sdk = [26, 35], qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SensorBackgroundGradientLayerTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @get:Rule val temporaryFolder = TemporaryFolder()

    private data class Sample(val x: Float, val y: Float, val designAlpha: Int)

    @Test
    fun `dashboard overlay matches the design`() {
        verifyOverlay(
            width = 120,
            height = 180,
            samples = listOf(
                Sample(0.97f, 0.34f, 3),
                Sample(0.05f, 0.50f, 138),
                Sample(0.50f, 0.95f, 149),
                Sample(0.50f, 0.25f, 57),
            ),
        ) { photo -> DashboardImage(userBackground = photo, modifier = Modifier.fillMaxSize()) }
    }

    @Test
    fun `dashboard overlay is centre-cropped into a landscape card`() {
        // 960x1440 design cropped into 160x90: the visible band is the design's middle,
        // so the card's vertical centre is the design's vertical centre.
        verifyOverlay(
            width = 160,
            height = 90,
            samples = listOf(
                Sample(0.05f, 0.50f, 138),
                Sample(0.97f, 0.20f, 3), // design y = (18 + 75) / 240 ≈ 0.39, near the light spot
            ),
        ) { photo -> DashboardImage(userBackground = photo, modifier = Modifier.fillMaxSize()) }
    }

    @Test
    fun `sensor details overlay matches the design`() {
        verifyOverlay(
            width = 150,
            height = 280,
            samples = listOf(
                Sample(0.97f, 0.32f, 30),
                Sample(0.05f, 0.32f, 176),
                Sample(0.50f, 0.95f, 204),
                Sample(0.60f, 0.30f, 89),
            ),
        ) { photo ->
            SensorCardImage(userBackground = photo, defaultBackground = android.R.color.black)
        }
    }

    private fun verifyOverlay(
        width: Int,
        height: Int,
        samples: List<Sample>,
        content: @Composable (Uri) -> Unit,
    ) {
        val photo = temporaryFolder.newFile("sensor.png")
        val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.rgb(240, 160, 80))
        photo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        compose.setContent {
            Box(Modifier.size(width.dp, height.dp).background(Color.Black).testTag("sensor_photo")) {
                content(Uri.fromFile(photo))
            }
        }

        fun pixelAt(pixels: androidx.compose.ui.graphics.PixelMap, sample: Sample) =
            pixels[(sample.x * width).toInt(), (sample.y * height).toInt()]

        val brightest = samples.minBy { it.designAlpha }
        // Coil loads the local image asynchronously before the overlay darkens it.
        compose.waitUntil(timeoutMillis = 5_000) { pixelAt(capture(), brightest).red > 0.7f }

        val pixels = capture()
        samples.forEach { sample ->
            val keep = 1f - sample.designAlpha / 255f
            val pixel = pixelAt(pixels, sample)
            val message = "at (${sample.x}, ${sample.y})"
            assertEquals(message, 240f / 255 * keep, pixel.red, TOLERANCE)
            assertEquals(message, 160f / 255 * keep, pixel.green, TOLERANCE)
            assertEquals(message, 80f / 255 * keep, pixel.blue, TOLERANCE)
        }
    }

    private fun capture() = compose.onNodeWithTag("sensor_photo").captureToImage().toPixelMap()

    private companion object {
        // About 8/255 of overlay alpha on the brightest channel.
        const val TOLERANCE = 0.035f
    }
}
