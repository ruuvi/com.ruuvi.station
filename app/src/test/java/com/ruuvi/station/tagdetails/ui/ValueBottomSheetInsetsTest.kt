package com.ruuvi.station.tagdetails.ui

import android.app.Application
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.tagdetails.ui.popup.AirValueSheetContent
import com.ruuvi.station.tagdetails.ui.popup.ValueSheetContent
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.units.model.UnitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [30], qualifiers = "en-rUS-mdpi")
class ValueBottomSheetInsetsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `humidity sheet adds navigation space inside its scrolling content`() = verifyInsets(false)

    @Test
    fun `air quality sheet adds navigation space inside its scrolling content`() = verifyInsets(true)

    private fun verifyInsets(airQuality: Boolean) {
        lateinit var contentView: View
        lateinit var scrollState: ScrollState
        WindowCompat.setDecorFitsSystemWindows(compose.activity.window, false)
        val value = EnvironmentValue(
            original = 50.0, value = 50.0, accuracy = Accuracy.Accuracy1,
            valueWithUnit = "50 %", valueWithoutUnit = "50", unitString = "%",
            unitType = if (airQuality) UnitType.AirQuality.AqiIndex else UnitType.HumidityUnit.Relative,
        )
        compose.setContent {
            contentView = LocalView.current
            val state = rememberScrollState()
            SideEffect { scrollState = state }
            RuuviTheme {
                if (airQuality) {
                    AirValueSheetContent(
                        sheetValue = value, maxHeight = 240, lastUpdate = null,
                        chartHistory = null, scrollToChart = {}, onChangeValue = {},
                        scrollState = state,
                    )
                } else {
                    ValueSheetContent(
                        sheetValue = value, maxHeight = 240, chartHistory = null,
                        scrollToChart = {}, scrollState = state,
                    )
                }
            }
        }

        fun applyNavigationInset(bottom: Int) {
            compose.runOnIdle {
                ViewCompat.dispatchApplyWindowInsets(
                    contentView,
                    WindowInsetsCompat.Builder()
                        .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, bottom))
                        .setVisible(WindowInsetsCompat.Type.navigationBars(), bottom > 0)
                        .build(),
                )
            }
            compose.waitForIdle()
        }

        applyNavigationInset(0)
        val initialScrollRange = compose.runOnIdle { scrollState.maxValue }
        val initialViewport = compose.runOnIdle { scrollState.viewportSize }
        assertTrue("Description must overflow the sheet", initialScrollRange > 0)

        applyNavigationInset(48)
        compose.runOnIdle {
            assertEquals("Navigation space must be scrollable", initialScrollRange + 48, scrollState.maxValue)
            assertEquals("Navigation padding must not shrink the viewport", initialViewport, scrollState.viewportSize)
        }

        applyNavigationInset(0)
        compose.runOnIdle {
            assertEquals("Inset changes must not accumulate padding", initialScrollRange, scrollState.maxValue)
        }
    }
}
