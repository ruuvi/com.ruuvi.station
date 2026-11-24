package com.ruuvi.station.util.ui

import android.app.Application
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.theme.Titan
import com.ruuvi.station.app.ui.theme.ToastBackground
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class RuuviToastTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private var now = 0L

    @Before
    fun setUp() {
        resetToasts()
        RuuviToast.cancel()
        RuuviToast.clock = { now }
    }

    @After
    fun tearDown() {
        RuuviToast.cancel()
        RuuviToast.clock = android.os.SystemClock::uptimeMillis
    }

    @Test
    fun `toast uses the default toast layout with app background and font at the bottom`() {
        context.showToast("Saved")

        val toast = requireNotNull(latestToast())
        assertEquals("Saved", latestToastText())
        assertEquals(Toast.LENGTH_SHORT, toast.duration)
        assertEquals(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, toast.gravity)
        val density = context.resources.displayMetrics.density
        assertEquals((16 * density).toInt(), toast.yOffset)

        @Suppress("DEPRECATION")
        val content = (toast.view as ViewGroup).getChildAt(0) as LinearLayout
        val background = content.background as GradientDrawable
        assertEquals(ToastBackground.toArgb(), background.color?.defaultColor)
        assertEquals(28 * density, background.cornerRadius)
        assertEquals(LinearLayout.LayoutParams.WRAP_CONTENT, content.layoutParams.width)

        val icon = content.getChildAt(0) as ImageView
        assertNotNull(icon.drawable)
        assertEquals((18 * density).toInt(), icon.layoutParams.width)

        val text = content.getChildAt(1) as TextView
        assertEquals(Titan.toArgb(), text.currentTextColor)
        assertNotNull(text.typeface)
    }

    @Test
    fun `long messages wrap instead of being cut off`() {
        context.showToast(R.string.share_pending_message, ToastDuration.Long)

        @Suppress("DEPRECATION")
        val content = (requireNotNull(latestToast()).view as ViewGroup).getChildAt(0) as LinearLayout
        val text = content.getChildAt(1) as TextView
        assertEquals(Int.MAX_VALUE, text.maxLines)
        assertEquals(context.getString(R.string.share_pending_message), text.text.toString())
    }

    @Test
    fun `StringRes overload resolves the message`() {
        context.showToast(R.string.ok, ToastDuration.Long)

        assertEquals(context.getString(R.string.ok), latestToastText())
        assertEquals(Toast.LENGTH_LONG, latestToastDuration())
    }

    @Test
    fun `resource toasts default to a short duration`() {
        context.showToast(R.string.ok)

        assertEquals(context.getString(R.string.ok), latestToastText())
        assertEquals(Toast.LENGTH_SHORT, latestToastDuration())
    }

    @Test
    fun `new toast replaces the visible one instead of queueing`() {
        context.showToast("Saving")
        val first = requireNotNull(latestToast())

        context.showToast("Saved")

        assertTrue(Shadows.shadowOf(first).isCancelled)
        assertEquals("Saved", latestToastText())
        assertEquals(2, toastShownCount())
    }

    @Test
    fun `isShowing follows the toast duration and message`() {
        context.showToast("Measurements may be outdated")

        assertTrue(RuuviToast.isShowing())
        assertTrue(RuuviToast.isShowing("Measurements may be outdated"))
        assertFalse(RuuviToast.isShowing("Something else"))

        now += ToastDuration.Short.visibleMillis
        assertFalse(RuuviToast.isShowing())
    }

    @Test
    fun `cancel with a message only hides that toast`() {
        context.showToast("Permission required", ToastDuration.Long)
        val toast = requireNotNull(latestToast())

        RuuviToast.cancel("Measurements may be outdated")
        assertFalse(Shadows.shadowOf(toast).isCancelled)
        assertTrue(RuuviToast.isShowing("Permission required"))

        RuuviToast.cancel("Permission required")
        assertTrue(Shadows.shadowOf(toast).isCancelled)
        assertFalse(RuuviToast.isShowing())
    }

    @Test
    fun `toast requested off the main thread is shown on the main thread`() {
        val worker = Thread { context.showToast("From worker") }
        worker.start()
        worker.join()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertEquals("From worker", latestToastText())
    }
}
