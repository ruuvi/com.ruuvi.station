package com.ruuvi.station.util.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.widget.FrameLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BottomFadingEdgeScrollViewTest {
    @Test
    fun `content fades continuously to screen bottom including inside navigation inset`() {
        val (root, scrollView) = createScrollableContent()

        for (offset in listOf(0, 60, 180)) {
            scrollView.scrollTo(0, offset)
            assertTrue(scrollView.canScrollVertically(1))

            val bitmap = render(root)
            assertEquals("Top content must not fade at offset $offset", Color.BLACK, bitmap.getPixel(50, 20))
            assertEquals("Main content must remain opaque at offset $offset", Color.BLACK, bitmap.getPixel(50, 100))
            assertEquals("Content must stay opaque until the fade starts", Color.BLACK, bitmap.getPixel(50, 151))
            val fadeColor = Color.red(bitmap.getPixel(50, 176))
            assertTrue("Expected partial fade at offset $offset, got $fadeColor", fadeColor in 1..254)
            val navigationColor = Color.red(bitmap.getPixel(50, 190))
            val bottomColor = Color.red(bitmap.getPixel(50, 199))
            assertTrue("Fade must continue through the navigation area", navigationColor > fadeColor)
            assertTrue("Content must fade further towards the screen edge", bottomColor > navigationColor)
            bitmap.recycle()
        }
    }

    @Test
    fun `final content is fully readable when scrolled to the end`() {
        val (root, scrollView) = createScrollableContent()
        scrollView.scrollTo(0, scrollView.getChildAt(0).height)
        assertFalse(scrollView.canScrollVertically(1))

        val bitmap = render(root)
        assertEquals(Color.BLACK, bitmap.getPixel(50, 152))
        assertEquals(Color.BLACK, bitmap.getPixel(50, 170))
        assertEquals(Color.WHITE, bitmap.getPixel(50, 190))
        bitmap.recycle()
    }

    private fun createScrollableContent(): Pair<FrameLayout, BottomFadingEdgeScrollView> {
        val context = RuntimeEnvironment.getApplication()
        val scrollView = BottomFadingEdgeScrollView(context).apply {
            setBackgroundColor(Color.WHITE)
            setPadding(0, 0, 0, 24)
            clipToPadding = false
            addView(
                View(context).apply {
                    minimumHeight = 600
                    setBackgroundColor(Color.BLACK)
                },
                FrameLayout.LayoutParams(100, 600),
            )
        }
        val root = FrameLayout(context).apply {
            setBackgroundColor(Color.WHITE)
            addView(scrollView, FrameLayout.LayoutParams(100, 200))
            measure(
                View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
            )
            layout(0, 0, 100, 200)
        }
        return root to scrollView
    }

    private fun render(root: View): Bitmap =
        Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888).also {
            root.draw(Canvas(it))
        }
}
