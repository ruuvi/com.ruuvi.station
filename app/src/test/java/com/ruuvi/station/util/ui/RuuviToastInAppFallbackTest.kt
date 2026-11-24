package com.ruuvi.station.util.ui

import android.app.Activity
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
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

/**
 * Toasts are drawn inside the visible screen, so their position does not depend on how the
 * platform places toast windows, and Android 7–9 cannot suppress them when the app's
 * notifications are disabled.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class RuuviToastInAppFallbackTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        RuuviToast.cancel()
        resetToasts()
    }

    @After
    fun tearDown() {
        RuuviToast.stopTracking()
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
    fun `with notifications enabled the toast is still drawn inside the activity`() {
        setNotificationsEnabled(true)
        val activity = createActivity()

        activity.showToast("Saved")

        assertNull(latestToast())
        assertEquals("Saved", activity.inAppToastText())
        assertEquals("Saved", latestToastText())
    }

    @Test
    @Config(sdk = [35])
    fun `current Android versions draw the toast inside the activity too`() {
        val activity = createActivity()

        activity.showToast("Saved", ToastDuration.Long)

        assertNull(latestToast())
        assertEquals("Saved", activity.inAppToastText())
        assertEquals(Toast.LENGTH_LONG, latestToastDuration())
    }

    @Test
    fun `without an activity the system toast is used`() {
        context.showToast("Saved")

        assertNotNull(latestToast())
        assertEquals("Saved", latestToastText())
    }

    @Test
    @Config(sdk = [35])
    fun `an app-context toast is drawn in the visible screen`() {
        RuuviToast.init(context as Application)
        val activity = createActivity()

        context.showToast("Measurements may be outdated")

        assertNull(latestToast())
        assertEquals("Measurements may be outdated", activity.inAppToastText())
    }

    @Test
    @Config(sdk = [35])
    fun `a visible toast follows the user to the next screen for the rest of its time`() {
        RuuviToast.init(context as Application)
        val first = createActivity()
        val shownAt = SystemClock.uptimeMillis()
        first.showToast("Saved", ToastDuration.Long)
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(1_000, TimeUnit.MILLISECONDS)

        val second = createActivity()

        assertNull(first.inAppToast())
        assertEquals("Saved", second.inAppToastText())
        // Opening the screen takes a little time, so measure what is left of the toast's time.
        val remaining = shownAt + ToastDuration.Long.visibleMillis - SystemClock.uptimeMillis()
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(remaining - 100, TimeUnit.MILLISECONDS)
        assertNotNull(second.inAppToast())
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(200, TimeUnit.MILLISECONDS)
        assertNull(second.inAppToast())
    }

    @Test
    @Config(sdk = [35])
    fun `a toast about one screen stays with it when another screen opens`() {
        RuuviToast.init(context as Application)
        val first = createActivity()
        RuuviToast.show(first, "Measurements may be outdated", followToNextScreen = false)

        val second = createActivity()

        assertEquals("Measurements may be outdated", first.inAppToastText())
        assertNull(second.inAppToast())
    }

    @Test
    @Config(sdk = [35])
    fun `a result for a stopped screen is shown on the screen the user is on`() {
        RuuviToast.init(context as Application)
        val shareController = Robolectric.buildActivity(AppCompatActivity::class.java).setup()
        val share = shareController.get()
        val previous = createActivity()
        shareController.pause().stop()

        share.showToast("Invalid email address", ToastDuration.Long)

        assertNull(share.inAppToast())
        assertEquals("Invalid email address", previous.inAppToastText())
        assertNull(latestToast())
    }

    @Test
    @Config(sdk = [35])
    fun `a result arriving between screens waits for the next one`() {
        RuuviToast.init(context as Application)
        val shareController = Robolectric.buildActivity(AppCompatActivity::class.java).setup()
        val share = shareController.get()
        val nextController = Robolectric.buildActivity(AppCompatActivity::class.java).create().start()
        shareController.pause().stop()

        share.showToast("Successfully shared sensor")

        assertNull(share.inAppToast())
        assertNull(latestToast())
        assertFalse(RuuviToast.isShowing())

        nextController.resume()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertEquals("Successfully shared sensor", nextController.get().inAppToastText())
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
