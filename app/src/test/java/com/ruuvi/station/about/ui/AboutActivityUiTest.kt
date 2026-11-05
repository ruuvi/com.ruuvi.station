package com.ruuvi.station.about.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Looper
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.ruuvi.station.R
import com.ruuvi.station.about.model.AppStats
import com.ruuvi.station.util.ui.UiTestApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = UiTestApplication::class, sdk = [24, 25, 35], qualifiers = "en-rUS-notnight-mdpi")
class AboutActivityUiTest {
    private val stats = MutableStateFlow<AppStats?>(AppStats(2, 5, 100))
    private val viewModel = mockk<AboutActivityViewModel>(relaxed = true)
    private var oldMode = AppCompatDelegate.MODE_NIGHT_UNSPECIFIED

    @Before fun setUp() {
        oldMode = AppCompatDelegate.getDefaultNightMode()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        every { viewModel.appStats } returns stats
        every { viewModel.isDeveloperSettingsEnabled() } returns false
        (RuntimeEnvironment.getApplication() as UiTestApplication).aboutViewModel = viewModel
    }

    @After fun tearDown() { AppCompatDelegate.setDefaultNightMode(oldMode) }

    @Test
    fun `light theme keeps header white and applies legacy navigation fallback`() {
        withActivity { activity ->
            assertEquals(Color.WHITE, (activity.binding.root.background as ColorDrawable).color)
            assertEquals(activity.getColor(R.color.titan), activity.binding.toolbar.children.filterIsInstance<android.widget.TextView>().first().currentTextColor)
            val icons = WindowCompat.getInsetsController(activity.window, activity.binding.root)
            assertTrue(icons.isAppearanceLightStatusBars)
            assertEquals(
                if (Build.VERSION.SDK_INT < 26) activity.getColor(R.color.titan) else Color.TRANSPARENT,
                activity.window.navigationBarColor,
            )
            if (Build.VERSION.SDK_INT < 26) assertFalse(icons.isAppearanceLightNavigationBars)
        }
    }

    @Test
    @Config(qualifiers = "en-rUS-night-mdpi")
    fun `dark theme keeps dark header and light system icons`() {
        withActivity { activity ->
            assertEquals(activity.getColor(R.color.titan), (activity.binding.root.background as ColorDrawable).color)
            val icons = WindowCompat.getInsetsController(activity.window, activity.binding.root)
            assertFalse(icons.isAppearanceLightStatusBars)
            assertFalse(icons.isAppearanceLightNavigationBars)
        }
    }

    @Test
    @Config(sdk = [35])
    fun `navigation inset is inside scroll content while background reaches screen edge`() {
        withActivity { activity ->
            val insets = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 24, 0, 48))
                .build()
            ViewCompat.dispatchApplyWindowInsets(activity.binding.root, insets)
            assertEquals(24, activity.binding.root.paddingTop)
            assertEquals(0, activity.binding.root.paddingBottom)
            assertEquals(48, activity.binding.content.root.paddingBottom)
            assertFalse(activity.binding.content.root.clipToPadding)
        }
    }

    @Test
    fun `stats update and developer settings can be enabled`() {
        withActivity { activity ->
            assertTrue(activity.binding.content.debugInfo.text.contains(activity.getString(R.string.help_added_tags, 2)))
            stats.value = null
            shadowOf(Looper.getMainLooper()).idle()
            assertFalse(activity.binding.content.debugInfo.text.contains(activity.getString(R.string.help_added_tags, 2)))
            repeat(20) { activity.binding.content.debugInfo.performClick() }
            verify(exactly = 1) { viewModel.enableDeveloperSettings() }
        }
    }

    @Test
    fun `already enabled developer settings remain enabled and back finishes the page`() {
        every { viewModel.isDeveloperSettingsEnabled() } returns true
        withActivity { activity ->
            repeat(20) { activity.binding.content.debugInfo.performClick() }
            verify(exactly = 0) { viewModel.enableDeveloperSettings() }
            val item = mockk<android.view.MenuItem>()
            every { item.itemId } returns android.R.id.home
            assertTrue(activity.onOptionsItemSelected(item))
            assertTrue(activity.isFinishing)
        }
    }

    private fun withActivity(assertions: (AboutActivity) -> Unit) {
        val controller = Robolectric.buildActivity(AboutActivity::class.java).setup().visible()
        try {
            shadowOf(Looper.getMainLooper()).idle()
            assertions(controller.get())
        } finally {
            controller.pause().stop().destroy()
        }
    }
}
