package com.ruuvi.station.app.ui.components

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Before API 28 hardware rendering cannot draw a ComposeShader nested in another or one that
 * combines two shaders of the same type, so those versions must get one plain shader per paint.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class SensorBackgroundPaintsTest {
    private val photo = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)

    @Test
    fun `before API 28 the photo and each layer are drawn with separate plain shaders`() {
        val paints = SensorBackgroundOverlay.SensorDetails.design
            .createPaints(150f, 280f, photo, sdkInt = Build.VERSION_CODES.O_MR1)

        assertEquals(3, paints.size)
        assertTrue(paints[0].shader is BitmapShader)
        assertTrue(paints[1].shader is RadialGradient)
        assertTrue(paints[2].shader is RadialGradient)
        paints.forEach { assertDithered(it) }
    }

    @Test
    fun `before API 28 without a photo only the layers are drawn`() {
        val paints = SensorBackgroundOverlay.Dashboard.design
            .createPaints(120f, 180f, background = null, sdkInt = Build.VERSION_CODES.N)

        assertEquals(2, paints.size)
        assertTrue(paints[0].shader is RadialGradient)
        assertTrue(paints[1].shader is LinearGradient)
    }

    @Test
    fun `from API 28 the photo and overlay are composed into one paint`() {
        val withPhoto = SensorBackgroundOverlay.SensorDetails.design
            .createPaints(150f, 280f, photo, sdkInt = Build.VERSION_CODES.P)
        val withoutPhoto = SensorBackgroundOverlay.Dashboard.design
            .createPaints(120f, 180f, background = null, sdkInt = Build.VERSION_CODES.P)

        assertEquals(1, withPhoto.size)
        assertTrue(withPhoto.single().shader is ComposeShader)
        assertDithered(withPhoto.single())
        assertEquals(1, withoutPhoto.size)
        assertTrue(withoutPhoto.single().shader is ComposeShader)
    }

    private fun assertDithered(paint: Paint) {
        assertTrue(paint.isDither)
        assertTrue(paint.isFilterBitmap)
    }
}
