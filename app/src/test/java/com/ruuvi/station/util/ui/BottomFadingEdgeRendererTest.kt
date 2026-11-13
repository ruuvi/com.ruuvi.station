package com.ruuvi.station.util.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BottomFadingEdgeRendererTest {
    @Test
    fun `default renderer fades across at least two text lines`() {
        val bitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        BottomFadingEdgeRenderer(RuntimeEnvironment.getApplication(), null)
            .draw(canvas, viewport()) { canvas.drawColor(Color.BLACK) }

        assertEquals(Color.BLACK, bitmap.getPixel(50, 135))
        assertTrue(Color.red(bitmap.getPixel(50, 151)) in 1..254)
        assertTrue(Color.red(bitmap.getPixel(50, 199)) > Color.red(bitmap.getPixel(50, 151)))
        bitmap.recycle()
    }

    @Test
    fun `inset changes refresh the cached default fade length`() {
        val renderer = BottomFadingEdgeRenderer(RuntimeEnvironment.getApplication(), null)
        val view = viewport()
        for (navigationBarHeight in listOf(0, 48, 0)) {
            val insets = androidx.core.view.WindowInsetsCompat.Builder()
                .setInsets(
                    androidx.core.view.WindowInsetsCompat.Type.navigationBars(),
                    androidx.core.graphics.Insets.of(0, 0, 0, navigationBarHeight),
                )
                .build()
            renderer.refreshFadeLength(view, insets)

            repeat(2) {
                val bitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                renderer.draw(canvas, view) { canvas.drawColor(Color.BLACK) }

                if (navigationBarHeight == 0) {
                    assertEquals(Color.BLACK, bitmap.getPixel(50, 120))
                } else {
                    assertTrue(Color.red(bitmap.getPixel(50, 120)) in 1..254)
                }
                bitmap.recycle()
            }
        }
    }

    @Test
    fun `XML length overrides the shared default and zero disables fading`() {
        for (length in listOf(0, 24, 300)) {
            val renderer = renderer(length)
            val view = viewport()
            val bitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            renderer.draw(canvas, view) { canvas.drawColor(Color.BLACK) }
            if (length == 0) {
                assertEquals(Color.BLACK, bitmap.getPixel(50, 199))
            } else {
                val midpoint = if (length == 24) 188 else 100
                val red = Color.red(bitmap.getPixel(50, midpoint))
                assertTrue("Expected a smooth fade for $length dp", red in 1..254)
                assertTrue(Color.red(bitmap.getPixel(50, 199)) > red)
            }
            bitmap.recycle()
        }
    }

    @Test
    fun `background remains unchanged for dark and light themes`() {
        val renderer = renderer(48)
        for (background in listOf(Color.WHITE, Color.rgb(0, 29, 27))) {
            val bitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(background)
            renderer.draw(canvas, viewport()) { /* Empty transparent content. */ }
            assertEquals(background, bitmap.getPixel(50, 180))
            assertEquals(background, bitmap.getPixel(50, 199))
            bitmap.recycle()
        }
    }

    @Test
    fun `resizing the viewport moves the fade and repeated draws reuse it correctly`() {
        val renderer = renderer(48)
        val view = viewport()
        for (height in listOf(200, 200, 300, 150)) {
            view.layout(0, 0, 100, height)
            val bitmap = Bitmap.createBitmap(100, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            renderer.draw(canvas, view) { canvas.drawColor(Color.BLACK) }
            assertEquals(Color.BLACK, bitmap.getPixel(50, height - 49))
            assertTrue(Color.red(bitmap.getPixel(50, height - 24)) in 1..254)
            bitmap.recycle()
        }
    }

    @Test
    fun `viewport shorter than the fade refreshes its cached end position`() {
        val renderer = renderer(48)
        val view = viewport()
        for (height in listOf(24, 36, 36)) {
            view.layout(0, 0, 100, height)
            val bitmap = Bitmap.createBitmap(100, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            renderer.draw(canvas, view) { canvas.drawColor(Color.BLACK) }
            assertTrue(Color.red(bitmap.getPixel(50, height / 2)) in 1..254)
            assertTrue(Color.red(bitmap.getPixel(50, height - 1)) > 240)
            bitmap.recycle()
        }
    }

    @Test
    fun `empty viewport and non scrollable content draw normally`() {
        for (height in listOf(0, 200)) {
            val renderer = renderer(48)
            val view = viewport(canScroll = height == 0).apply { layout(0, 0, 100, height) }
            val bitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
            var calls = 0
            renderer.draw(Canvas(bitmap), view) { calls++ }
            assertEquals(1, calls)
            bitmap.recycle()
        }
    }

    @Test
    fun `canvas is restored even when drawing throws`() {
        val bitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val saveCount = canvas.saveCount
        val failure = IllegalStateException("drawing failed")
        val caught = assertThrows(IllegalStateException::class.java) {
            renderer(48).draw(canvas, viewport()) { throw failure }
        }
        assertSame(failure, caught)
        assertEquals(saveCount, canvas.saveCount)
        bitmap.recycle()
    }

    private fun renderer(length: Int): BottomFadingEdgeRenderer {
        val attributes = Robolectric.buildAttributeSet()
            .addAttribute(android.R.attr.fadingEdgeLength, "${length}dp")
            .build()
        return BottomFadingEdgeRenderer(RuntimeEnvironment.getApplication(), attributes)
    }

    private fun viewport(canScroll: Boolean = true): View =
        object : View(RuntimeEnvironment.getApplication()) {
            override fun canScrollVertically(direction: Int) = canScroll && direction == 1
        }.apply { layout(0, 0, 100, 200) }
}
