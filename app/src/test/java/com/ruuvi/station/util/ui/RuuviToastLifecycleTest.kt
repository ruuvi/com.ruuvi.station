package com.ruuvi.station.util.ui

import android.app.Application
import android.content.Context
import android.os.Looper
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ApplicationProvider
import com.ruuvi.station.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class RuuviToastLifecycleTest {
    private val application: Application
        get() = ApplicationProvider.getApplicationContext()
    private val context: Context
        get() = application

    private var now = 0L

    // Every activity a test creates, closed after the test. A live AppCompat activity stays
    // registered with AppCompat, so a later test that changes the night mode would recreate it.
    private val controllers = mutableListOf<ActivityController<AppCompatActivity>>()

    private fun buildActivity(): ActivityController<AppCompatActivity> =
        Robolectric.buildActivity(AppCompatActivity::class.java).also { controllers += it }

    @Before
    fun setUp() {
        RuuviToast.cancel()
        resetToasts()
        RuuviToast.clock = { now }
    }

    @After
    fun tearDown() {
        controllers.forEach { it.close() }
        RuuviToast.stopTracking()
        RuuviToast.cancel()
        RuuviToast.clock = android.os.SystemClock::uptimeMillis
        RuuviToast.recommendedTimeout = { _, millis -> millis }
    }

    @Test
    fun `a result that arrives in the background is shown when the user returns`() {
        RuuviToast.init(application)

        context.showToast("Share unavailable", ToastDuration.Long)

        assertNull(latestToastText())
        assertFalse(RuuviToast.isShowing())

        startActivity()
        assertEquals("Share unavailable", latestToastText())
        assertEquals(Toast.LENGTH_LONG, latestToastDuration())
        assertTrue(RuuviToast.isShowing("Share unavailable"))
    }

    @Test
    fun `only the latest background message is kept`() {
        RuuviToast.init(application)

        context.showToast("Sharing")
        context.showToast("Successfully shared sensor")
        startActivity()

        assertEquals(1, toastShownCount())
        assertEquals("Successfully shared sensor", latestToastText())
    }

    @Test
    fun `cancelling a delayed message drops it`() {
        RuuviToast.init(application)

        context.showToast("Measurements may be outdated")
        RuuviToast.cancel("Something else")
        RuuviToast.cancel("Measurements may be outdated")
        startActivity()

        assertNull(latestToastText())
    }

    @Test
    fun `toasts show immediately while an activity is in the foreground`() {
        RuuviToast.init(application)
        val activity = startActivity()

        activity.showToast("Saved")

        assertEquals("Saved", latestToastText())
    }

    @Test
    fun `visible time follows the accessibility time-to-take-action setting`() {
        RuuviToast.recommendedTimeout = { _, millis -> millis * 3 }

        context.showToast("Permission required", ToastDuration.Long)

        now += ToastDuration.Long.visibleMillis
        assertTrue(RuuviToast.isShowing("Permission required"))
        now += 2 * ToastDuration.Long.visibleMillis - 1
        assertTrue(RuuviToast.isShowing())
        now += 1
        assertFalse(RuuviToast.isShowing())
    }

    private fun startActivity(): AppCompatActivity =
        buildActivity()
            .setup()
            .get()
            .apply {
                setTheme(R.style.AppTheme)
                setContentView(FrameLayout(this))
                Shadows.shadowOf(Looper.getMainLooper()).idle()
            }
}
