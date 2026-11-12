package com.ruuvi.station.tagdetails.ui

import android.app.Application
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.ruuvi.station.alarm.domain.AlarmSensorStatus
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.tagdetails.ui.popup.AirValueSheetContent
import com.ruuvi.station.tagdetails.ui.popup.ValueBottomSheet
import com.ruuvi.station.tagdetails.ui.popup.ValueSheetContent
import com.ruuvi.station.tagdetails.ui.popup.got2DaysOfHistory
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.units.model.UnitType
import com.ruuvi.station.vico.model.ChartData
import com.ruuvi.station.vico.model.Segment
import com.ruuvi.station.vico.model.SegmentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `humidity sheet adds navigation space inside its scrolling content`() = verifyInsets(airQuality = false)

    @Test
    fun `air quality sheet adds navigation space inside its scrolling content`() = verifyInsets(airQuality = true)

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun `bottom sheet routes standard and air quality values through their matching content`() {
        var value by mutableStateOf(environmentValue(UnitType.HumidityUnit.Relative, "50"))
        val particulateMatter = environmentValue(UnitType.PM.PM25, "12.3")

        compose.setContent {
            RuuviTheme {
                ValueBottomSheet(
                    sheetValue = value,
                    extraValues = listOf(particulateMatter),
                    alarmStatus = AlarmSensorStatus.NoAlarms,
                    chartHistory = null,
                    maxHeight = 800,
                    lastUpdate = null,
                    scrollToChart = {},
                    onChangeValue = {},
                    onDismiss = {},
                )
            }
        }

        compose.onNodeWithText(particulateMatter.valueWithoutUnit).assertDoesNotExist()

        compose.runOnIdle {
            value = environmentValue(UnitType.AirQuality.AqiIndex, "42")
        }
        compose.onNodeWithText(particulateMatter.valueWithoutUnit).assertExists()
    }

    @Test
    fun `history age requires a sample older than thirty six hours`() {
        val now = System.currentTimeMillis()

        assertFalse(got2DaysOfHistory(null))
        assertFalse(got2DaysOfHistory(chartData()))
        assertFalse(got2DaysOfHistory(chartData(now - (35 * HOUR_MILLIS))))
        assertTrue(got2DaysOfHistory(chartData(now - (37 * HOUR_MILLIS))))
    }

    @Test
    fun `movement values skip history while measurements show the empty history state`() {
        var value by mutableStateOf(environmentValue(UnitType.MovementUnit.MovementsCount, "4"))

        compose.setContent {
            RuuviTheme {
                ValueSheetContent(
                    sheetValue = value,
                    maxHeight = 800,
                    chartHistory = null,
                    scrollToChart = {},
                    scrollState = rememberScrollState(),
                )
            }
        }

        compose.onNodeWithText(compose.activity.getString(com.ruuvi.station.R.string.popup_no_data))
            .assertDoesNotExist()

        compose.runOnIdle {
            value = environmentValue(UnitType.HumidityUnit.Relative, "50")
        }
        compose.onNodeWithText(compose.activity.getString(com.ruuvi.station.R.string.popup_no_data))
            .assertExists()
    }

    @Test
    fun `air quality extra measurement can become the selected value`() {
        val aqi = environmentValue(UnitType.AirQuality.AqiIndex, "42")
        val particulateMatter = environmentValue(UnitType.PM.PM25, "12.3")
        var selected: EnvironmentValue? = null

        compose.setContent {
            RuuviTheme {
                AirValueSheetContent(
                    sheetValue = aqi,
                    extraValues = listOf(particulateMatter),
                    maxHeight = 800,
                    lastUpdate = null,
                    chartHistory = null,
                    scrollToChart = {},
                    onChangeValue = { selected = it },
                    scrollState = rememberScrollState(),
                )
            }
        }

        compose.onNodeWithText(particulateMatter.valueWithoutUnit).performClick()
        compose.runOnIdle { assertEquals(particulateMatter, selected) }
    }

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

    private fun environmentValue(unitType: UnitType, value: String) = EnvironmentValue(
        original = value.toDouble(),
        value = value.toDouble(),
        accuracy = Accuracy.Accuracy1,
        valueWithUnit = "$value ${unitType.unitCode}",
        valueWithoutUnit = value,
        unitString = unitType.unitCode,
        unitType = unitType,
    )

    private fun chartData(vararg timestamps: Long) = ChartData(
        segments = if (timestamps.isEmpty()) {
            emptyList()
        } else {
            listOf(
                Segment(
                    timestamps = timestamps.toList(),
                    values = List(timestamps.size) { 1.0 },
                    segmentType = SegmentType.Solid,
                ),
            )
        },
        minValue = 0.0,
        maxValue = 1.0,
    )

    private companion object {
        const val HOUR_MILLIS = 60L * 60L * 1000L
    }
}
