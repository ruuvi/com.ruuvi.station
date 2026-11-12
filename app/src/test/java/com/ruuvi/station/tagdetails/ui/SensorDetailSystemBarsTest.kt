package com.ruuvi.station.tagdetails.ui

import android.app.Application
import android.graphics.Color
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.WindowCompat
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.tagsettings.ui.SensorSettingsRoutes
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
        assertSystemBarIcons(darkNavigationIcons = false)

        // Leaving that backdrop for a light surface restores dark navigation buttons.
        compose.runOnIdle { sensorBackdrop = false }
        assertSystemBarIcons(darkNavigationIcons = true)

        // Both dark surfaces and the sensor backdrop keep light buttons in dark mode.
        compose.runOnIdle { darkTheme = true }
        assertSystemBarIcons(darkNavigationIcons = false)
        compose.runOnIdle { sensorBackdrop = true }
        assertSystemBarIcons(darkNavigationIcons = false)
    }

    @Suppress("DEPRECATION")
    @Test
    @Config(sdk = [30])
    fun `status bar is transparent for sensor destinations but opaque in nested settings`() {
        var destination by mutableStateOf(SensorDetailDestination.CARD)
        var settingsRoute by mutableStateOf(SensorSettingsRoutes.SENSOR_SETTINGS_ROOT)
        var expectedOpaqueStatusBarColor = Color.TRANSPARENT

        compose.setContent {
            val useDarkTheme = destination.forcesDarkTheme
            RuuviTheme(darkTheme = useDarkTheme) {
                expectedOpaqueStatusBarColor = RuuviStationTheme.colors.systemBars.toArgb()
                SensorDetailSystemBars(
                    destination = destination,
                    settingsRoute = settingsRoute,
                    useDarkTheme = useDarkTheme,
                )
            }
        }

        SensorDetailDestination.entries.forEach { rootDestination ->
            compose.runOnIdle {
                destination = rootDestination
                settingsRoute = SensorSettingsRoutes.SENSOR_SETTINGS_ROOT
            }
            assertTransparentStatusBar()
        }

        // A non-root route is only nested when SETTINGS is the active destination.
        compose.runOnIdle {
            destination = SensorDetailDestination.CARD
            settingsRoute = SensorSettingsRoutes.SENSOR_REMOVE
        }
        assertTransparentStatusBar()

        compose.runOnIdle { destination = SensorDetailDestination.SETTINGS }
        compose.runOnIdle {
            assertEquals(expectedOpaqueStatusBarColor, compose.activity.window.statusBarColor)
        }
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
    private fun assertTransparentStatusBar() {
        compose.runOnIdle {
            assertEquals(Color.TRANSPARENT, compose.activity.window.statusBarColor)
        }
    }

    private fun assertSystemBarIcons(darkNavigationIcons: Boolean) {
        compose.runOnIdle {
            val window = compose.activity.window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            assertFalse("The status bar must keep light icons", insetsController.isAppearanceLightStatusBars)
            assertEquals(darkNavigationIcons, insetsController.isAppearanceLightNavigationBars)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                assertFalse("The navigation bar must stay transparent", window.isNavigationBarContrastEnforced)
            }
        }
    }
}
