package com.ruuvi.station.util.ui

import android.app.Activity
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ApplicationProvider
import com.ruuvi.station.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import java.util.concurrent.TimeUnit

/** Android 7–9 suppresses an app's toasts while its notifications are disabled. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class RuuviToastInAppFallbackTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        RuuviToast.cancel()
        ShadowToast.reset()
    }

    @After
    fun tearDown() {
        RuuviToast.cancel()
        setNotificationsEnabled(true)
        RuuviToast.recommendedTimeout = { _, millis -> millis }
    }

    @Test
    fun `with notifications disabled the toast is drawn inside the activity`() {
        setNotificationsEnabled(false)
        val activity = createActivity()

        activity.showToast("Saved")

        assertNull(latestToast())
        val overlay = requireNotNull(activity.inAppToast())
        assertEquals("Saved", activity.inAppToastText())
        val params = overlay.layoutParams as FrameLayout.LayoutParams
        assertEquals(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, params.gravity)
        assertEquals(View.ACCESSIBILITY_LIVE_REGION_POLITE, overlay.accessibilityLiveRegion)
        assertFalse(overlay.isClickable)
        assertTrue(RuuviToast.isShowing("Saved"))
    }

    @Test
    fun `in-app toast disappears after its duration`() {
        setNotificationsEnabled(false)
        val activity = createActivity()

        activity.showToast(R.string.ok, ToastDuration.Long)
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(ToastDuration.Long.visibleMillis - 1, TimeUnit.MILLISECONDS)
        assertNotNull(activity.inAppToast())

        Shadows.shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.MILLISECONDS)
        assertNull(activity.inAppToast())
        assertFalse(RuuviToast.isShowing())
    }

    @Test
    fun `in-app toast stays up for the accessibility-extended time`() {
        setNotificationsEnabled(false)
        RuuviToast.recommendedTimeout = { _, millis -> millis * 2 }
        val activity = createActivity()

        activity.showToast("Saved")
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(2 * ToastDuration.Short.visibleMillis - 1, TimeUnit.MILLISECONDS)
        assertNotNull(activity.inAppToast())

        Shadows.shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.MILLISECONDS)
        assertNull(activity.inAppToast())
    }

    @Test
    fun `a new in-app toast replaces the visible one and cancel removes it`() {
        setNotificationsEnabled(false)
        val activity = createActivity()

        activity.showToast("Saving")
        activity.showToast("Saved")

        assertEquals(1, activity.inAppToasts().size)
        assertEquals("Saved", activity.inAppToastText())

        RuuviToast.cancel("Saving")
        assertNotNull(activity.inAppToast())
        RuuviToast.cancel("Saved")
        assertNull(activity.inAppToast())
    }

    @Test
    fun `with notifications enabled the system toast is used`() {
        setNotificationsEnabled(true)
        val activity = createActivity()

        activity.showToast("Saved")

        assertEquals("Saved", latestToastText())
        assertNull(activity.inAppToast())
    }

    @Test
    fun `without an activity the system toast is used`() {
        setNotificationsEnabled(false)

        context.showToast("Saved")

        assertEquals("Saved", latestToastText())
    }

    @Test
    @Config(sdk = [29])
    fun `Android 10 and later keep the system toast even with notifications disabled`() {
        setNotificationsEnabled(false)
        val activity = createActivity()

        activity.showToast("Saved")

        assertEquals("Saved", latestToastText())
        assertNull(activity.inAppToast())
    }

    private fun setNotificationsEnabled(enabled: Boolean) {
        val manager = context.getSystemService(NotificationManager::class.java)
        Shadows.shadowOf(manager).setNotificationsEnabled(enabled)
    }

    private fun createActivity(): Activity =
        Robolectric.buildActivity(AppCompatActivity::class.java)
            .setup()
            .get()
            .apply {
                setTheme(R.style.AppTheme)
                setContentView(FrameLayout(this))
            }

    private fun Activity.inAppToasts(): List<View> {
        val decor = window.decorView as ViewGroup
        return (0 until decor.childCount).map(decor::getChildAt).filter { it.tag == IN_APP_TOAST_TAG }
    }

    private fun Activity.inAppToast(): View? = inAppToasts().singleOrNull()

    private fun Activity.inAppToastText(): String? = inAppToast()?.findTextView()?.text?.toString()
}
