package com.ruuvi.station.app.ui.components

import android.app.Application
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Looper
import androidx.compose.ui.unit.IntSize
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import coil.imageLoader
import coil.memory.MemoryCache
import coil.request.SuccessResult
import coil.size.Precision
import coil.size.Scale
import coil.size.Size
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SensorBackgroundImageRequestTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    private val context: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun `background request fills the view with an inexact software bitmap`() {
        val request = backgroundImageRequest(context, "model", IntSize(1440, 3120))

        assertEquals(Precision.INEXACT, request.precision)
        assertEquals(Scale.FILL, request.scale)
        assertFalse(request.allowHardware)
        assertEquals(Size(1440, 3120), runBlocking { request.sizeResolver.size() })
    }

    @Test
    fun `a photo smaller than the view is decoded at its own size, not enlarged`() {
        val photo = temporaryFolder.newFile("small.png")
        Bitmap.createBitmap(80, 60, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.rgb(240, 160, 80))
            photo.outputStream().use { compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        val loader = ImageLoader.Builder(context).memoryCache(null).diskCache(null).build()

        // Coil starts requests on the main thread, so run it elsewhere and keep the looper going.
        val load = CoroutineScope(Dispatchers.IO).async {
            loader.execute(backgroundImageRequest(context, Uri.fromFile(photo), IntSize(1440, 3120)))
        }
        val deadline = System.currentTimeMillis() + 10_000
        while (!load.isCompleted && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertTrue("load did not finish", load.isCompleted)
        val result = runBlocking { load.await() }

        assertTrue("load failed: $result", result is SuccessResult)
        val bitmap = ((result as SuccessResult).drawable as BitmapDrawable).bitmap
        assertEquals(80, bitmap.width)
        assertEquals(60, bitmap.height)
    }

    @Test
    fun `a background loaded before is available at once from the memory cache`() {
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val key = MemoryCache.Key("cached-background-test")
        context.imageLoader.memoryCache!![key] = MemoryCache.Value(bitmap)

        assertNull(cachedBackground(context, "cached model"))
        rememberBackgroundKey("cached model", key)
        assertSame(bitmap, cachedBackground(context, "cached model"))
    }

    @Test
    fun `no cached background without a model, a key, or a bitmap still in memory`() {
        assertNull(cachedBackground(context, null))

        rememberBackgroundKey("model without key", null)
        assertNull(cachedBackground(context, "model without key"))

        rememberBackgroundKey("evicted model", MemoryCache.Key("evicted-background-test"))
        assertNull(cachedBackground(context, "evicted model"))
    }
}
