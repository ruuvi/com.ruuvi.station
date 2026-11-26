package com.ruuvi.station.dashboard.ui

import com.ruuvi.station.app.ui.components.HideToastWhenScreenStopsEffect
import com.ruuvi.station.util.ui.resetToasts
import com.ruuvi.station.util.ui.toastShownCount
import android.app.Application
import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.ruuvi.station.util.ui.RuuviToast
import com.ruuvi.station.util.ui.ToastDuration
import com.ruuvi.station.util.ui.latestToastText
import com.ruuvi.station.util.ui.showToast
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class DashboardToastTest {
    @get:Rule
    val compose = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()
    private val owner = TestLifecycleOwner()
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
    fun `repeated stale requests do not restart the visible toast`() {
        showToastIfIdle(context, STALE)
        showToastIfIdle(context, STALE)
        assertEquals(1, toastShownCount())

        now += ToastDuration.Short.visibleMillis
        showToastIfIdle(context, STALE)
        assertEquals(2, toastShownCount())
    }

    @Test
    fun `stale request is skipped while another toast is visible`() {
        context.showToast("Another message")

        showToastIfIdle(context, STALE)

        assertEquals(1, toastShownCount())
        assertEquals("Another message", latestToastText())
    }

    @Test
    fun `stale toast stays on pause and is hidden on stop`() {
        setContent()
        showToastIfIdle(context, STALE)

        compose.runOnIdle { owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE) }
        compose.runOnIdle { assertTrue(RuuviToast.isShowing(STALE)) }

        compose.runOnIdle { owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP) }
        compose.runOnIdle { assertFalse(RuuviToast.isShowing()) }
    }

    @Test
    fun `leaving the dashboard keeps other toasts visible`() {
        setContent()
        context.showToast("Permission required", ToastDuration.Long)

        compose.runOnIdle {
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
        compose.runOnIdle { assertTrue(RuuviToast.isShowing("Permission required")) }
    }

    private fun setContent() {
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                HideToastWhenScreenStopsEffect(STALE)
            }
        }
        compose.waitForIdle()
    }

    private class TestLifecycleOwner : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this).apply {
            handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            handleLifecycleEvent(Lifecycle.Event.ON_START)
            handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        override val lifecycle: Lifecycle = registry
    }

    private companion object {
        const val STALE = "Measurements may be outdated"
    }
}
