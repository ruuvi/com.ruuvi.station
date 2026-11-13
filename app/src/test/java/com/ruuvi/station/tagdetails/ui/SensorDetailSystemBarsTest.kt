package com.ruuvi.station.tagdetails.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.WindowCompat
import com.ruuvi.station.app.ui.theme.RuuviTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [30, 35])
class SensorDetailSystemBarsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `navigation icons follow the dark sensor backdrop and update when leaving it`() {
        var sensorBackdrop by mutableStateOf(true)
        var darkTheme by mutableStateOf(false)
        compose.setContent {
            RuuviTheme(darkTheme = darkTheme) {
                SensorDetailSystemBars(
                    useTransparentStatusBar = sensorBackdrop,
                    useDarkTheme = darkTheme,
                )
            }
        }

        // Card and history keep light buttons over their dark backdrop in light mode.
        assertNavigationIcons(darkIcons = false)

        // Leaving that backdrop for a light surface restores dark buttons.
        compose.runOnIdle { sensorBackdrop = false }
        assertNavigationIcons(darkIcons = true)

        // Both dark surfaces and the sensor backdrop keep light buttons in dark mode.
        compose.runOnIdle { darkTheme = true }
        assertNavigationIcons(darkIcons = false)
        compose.runOnIdle { sensorBackdrop = true }
        assertNavigationIcons(darkIcons = false)
    }

    private fun assertNavigationIcons(darkIcons: Boolean) {
        compose.runOnIdle {
            val window = compose.activity.window
            assertEquals(
                darkIcons,
                WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars,
            )
            assertFalse("The navigation bar must stay transparent", window.isNavigationBarContrastEnforced)
        }
    }
}
