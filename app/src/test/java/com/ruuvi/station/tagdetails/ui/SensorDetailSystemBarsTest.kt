package com.ruuvi.station.tagdetails.ui

import android.app.Application
import android.graphics.Color
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.WindowCompat
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28, 30, 35])
class SensorDetailSystemBarsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `system bar icons follow the active backdrop and theme`() {
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

        // Card and history keep light system-bar icons over their dark backdrop in light mode.
        assertSystemBarIcons(darkIcons = false)

        // Leaving that backdrop for a light surface restores dark system-bar icons.
        compose.runOnIdle { sensorBackdrop = false }
        assertSystemBarIcons(darkIcons = true)

        // Both dark surfaces and the sensor backdrop keep light system-bar icons in dark mode.
        compose.runOnIdle { darkTheme = true }
        assertSystemBarIcons(darkIcons = false)
        compose.runOnIdle { sensorBackdrop = true }
        assertSystemBarIcons(darkIcons = false)
    }

    @Suppress("DEPRECATION")
    @Test
    @Config(sdk = [30])
    fun `status bar switches between transparent and themed opaque colors`() {
        var useTransparentStatusBar by mutableStateOf(true)
        var expectedOpaqueColor = Color.TRANSPARENT
        compose.setContent {
            RuuviTheme(darkTheme = false) {
                val systemBarColor = RuuviStationTheme.colors.systemBars.toArgb()
                SideEffect {
                    expectedOpaqueColor = systemBarColor
                }
                SensorDetailSystemBars(
                    useTransparentStatusBar = useTransparentStatusBar,
                    useDarkTheme = false,
                )
            }
        }

        assertStatusBarColor(Color.TRANSPARENT)

        compose.runOnIdle { useTransparentStatusBar = false }
        assertStatusBarColor(expectedOpaqueColor)
    }

    @Test
    fun `system bar effect safely ignores a non activity context`() {
        compose.setContent {
            CompositionLocalProvider(LocalContext provides RuntimeEnvironment.getApplication()) {
                RuuviTheme {
                    SensorDetailSystemBars(
                        useTransparentStatusBar = true,
                        useDarkTheme = false,
                    )
                }
            }
        }

        compose.waitForIdle()
    }

    @Suppress("DEPRECATION")
    private fun assertStatusBarColor(expectedColor: Int) {
        compose.runOnIdle {
            assertEquals(expectedColor, compose.activity.window.statusBarColor)
        }
    }

    private fun assertSystemBarIcons(darkIcons: Boolean) {
        compose.runOnIdle {
            val window = compose.activity.window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            assertEquals(darkIcons, insetsController.isAppearanceLightStatusBars)
            assertEquals(darkIcons, insetsController.isAppearanceLightNavigationBars)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                assertFalse("The navigation bar must stay transparent", window.isNavigationBarContrastEnforced)
            }
        }
    }
}
