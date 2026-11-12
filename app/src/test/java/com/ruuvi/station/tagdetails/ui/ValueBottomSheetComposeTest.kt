package com.ruuvi.station.tagdetails.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlarmSensorStatus
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.tagdetails.ui.popup.ValueBottomSheet
import com.ruuvi.station.tagdetails.ui.popup.got2DaysOfHistory
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.units.model.UnitType
import com.ruuvi.station.vico.model.ChartData
import com.ruuvi.station.vico.model.Segment
import com.ruuvi.station.vico.model.SegmentType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [30], qualifiers = "en-rUS-mdpi")
@OptIn(ExperimentalMaterial3Api::class)
class ValueBottomSheetComposeTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `standard sheet displays its value and missing history state`() {
        val value = environmentValue("52", "%", UnitType.HumidityUnit.Relative)

        showSheet(value)

        compose.onNodeWithText("52").assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.popup_no_data))
            .assertIsDisplayed()
    }

    @Test
    fun `movement sheet omits the history section`() {
        val value = environmentValue("4", "", UnitType.MovementUnit.MovementsCount)

        showSheet(value)

        compose.onNodeWithText("4").assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.popup_no_data))
            .assertDoesNotExist()
    }

    @Test
    fun `AQI sheet displays extra measurements and forwards their selection`() {
        val aqi = environmentValue("87", "", UnitType.AirQuality.AqiIndex)
        val co2 = environmentValue("800", "ppm", UnitType.CO2.Ppm)
        var selected: EnvironmentValue? = null

        showSheet(
            value = aqi,
            extraValues = listOf(co2),
            onChangeValue = { selected = it },
        )

        compose.onNode(hasText("800") and hasClickAction())
            .performScrollTo()
            .performClick()
        compose.runOnIdle {
            assertSame(co2, selected)
        }
    }

    @Test
    fun `two day history requires a timestamp older than 36 hours`() {
        assertFalse(got2DaysOfHistory(null))
        assertFalse(got2DaysOfHistory(ChartData(emptyList(), 0.0, 0.0)))
        assertFalse(got2DaysOfHistory(historyStarting(hoursAgo = 35)))
        assertTrue(got2DaysOfHistory(historyStarting(hoursAgo = 37)))
    }

    private fun showSheet(
        value: EnvironmentValue,
        extraValues: List<EnvironmentValue> = emptyList(),
        onChangeValue: (EnvironmentValue) -> Unit = {},
    ) {
        compose.setContent {
            RuuviTheme {
                ValueBottomSheet(
                    sheetValue = value,
                    extraValues = extraValues,
                    alarmStatus = AlarmSensorStatus.NoAlarms,
                    chartHistory = null,
                    maxHeight = 500,
                    lastUpdate = null,
                    scrollToChart = {},
                    onChangeValue = onChangeValue,
                    onDismiss = {},
                )
            }
        }
        compose.waitForIdle()
    }

    private fun environmentValue(
        value: String,
        unit: String,
        unitType: UnitType,
    ) = EnvironmentValue(
        original = value.toDouble(),
        value = value.toDouble(),
        accuracy = Accuracy.Accuracy1,
        valueWithUnit = "$value $unit",
        valueWithoutUnit = value,
        unitString = unit,
        unitType = unitType,
    )

    private fun historyStarting(hoursAgo: Int): ChartData {
        val timestamp = Date().time - hoursAgo * 60L * 60L * 1_000L
        return ChartData(
            segments = listOf(
                Segment(
                    timestamps = listOf(timestamp),
                    values = listOf(1.0),
                    segmentType = SegmentType.Solid,
                ),
            ),
            minValue = 1.0,
            maxValue = 1.0,
        )
    }
}
