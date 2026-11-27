package com.ruuvi.station.graph

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import com.github.mikephil.charting.data.Entry
import com.ruuvi.station.alarm.domain.AlarmSensorStatus
import com.ruuvi.station.app.preferences.Preferences
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.graph.model.ChartContainer
import com.ruuvi.station.history.*
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.UnitType
import com.ruuvi.station.util.Period
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "en-rUS-w390dp-h900dp-port-mdpi")
class HistoryLayoutTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun `completed empty period restores existing message and real data restores charts`() {
        verifyEmptyPeriod()
    }

    @Test
    @Config(qualifiers = "en-rUS-w900dp-h390dp-land-mdpi")
    fun `empty period also shows the existing message in landscape`() {
        verifyEmptyPeriod()
    }

    private fun verifyEmptyPeriod() {
        val sensor = mockk<RuuviTag>(relaxed = true)
        every { sensor.id } returns "synthetic-empty"
        every { sensor.alarmSensorStatus } returns AlarmSensorStatus.NoAlarms
        val window = HistoryWindow(100, 200)
        fun models(complete: Boolean, populated: Boolean = false, error: Long = 0,
                   partial: Boolean = false, zoomed: Boolean = false): MutableList<ChartContainer> {
            val rows = if (populated) listOf(HistorySample(150, 20.0, 0)) else emptyList()
            val state = HistoryController.State("synthetic-empty", HistoryPlot(window,
                samples = mapOf("t" to rows), complete = complete, coverage = if (complete) listOf(window) else emptyList(),
                statisticsPartial = partial), visible = if (zoomed) HistoryWindow(160, 170) else window, error = error)
            val entries = rows.map { Entry(((it.timestamp - 100) * 1000).toFloat(), it.value.toFloat(), it) }
            return mutableListOf(ChartContainer(UnitType.TemperatureUnit.Celsius, null, entries.toMutableList(),
                from = 100_000, to = 200_000, history = HistoryChartData(state, "t", listOf(entries), {}, { _, _ -> })))
        }
        val data = MutableStateFlow(models(complete = false))
        val units = UnitsConverter(compose.activity, PreferencesRepository(Preferences(compose.activity)))
        compose.setContent {
            RuuviTheme(darkTheme = true) {
                ChartsView(Modifier.fillMaxSize(), sensor, units, true, false, true, emptyFlow(),
                    Period.Day1, 1, emptyFlow(), { data })
            }
        }
        val message = compose.activity.getString(com.ruuvi.station.R.string.empty_chart_message)
        compose.onNodeWithText(message).assertDoesNotExist()
        compose.runOnIdle { data.value = models(complete = true) }
        compose.onNodeWithText(message).assertIsDisplayed()
        // A failed or incomplete range must not be described as having no data.
        for (value in listOf(models(false), models(true, error = 1), models(true, partial = true), models(true, zoomed = true))) {
            compose.runOnIdle { data.value = value }
            compose.onNodeWithText(message).assertDoesNotExist()
        }
        compose.runOnIdle { data.value = models(true) }
        compose.onNodeWithText(message).assertIsDisplayed()
        compose.runOnIdle { data.value = models(true, populated = true) }
        compose.onNodeWithText(message).assertDoesNotExist()
    }

    @Test fun `new graph data never attaches a zero-height chart after an empty navigation state`() {
        val sensor = mockk<RuuviTag>(relaxed = true)
        every { sensor.id } returns "synthetic"
        every { sensor.alarmSensorStatus } returns AlarmSensorStatus.NoAlarms
        val data = MutableStateFlow(mutableListOf<ChartContainer>())
        val measured = mutableListOf<Int>()
        val attached = mutableSetOf<Int>()
        val units = UnitsConverter(compose.activity, PreferencesRepository(Preferences(compose.activity)))
        compose.setContent {
            RuuviTheme(darkTheme = true) {
                Box(Modifier.width(390.dp).height(600.dp)) {
                    ChartsView(Modifier.fillMaxSize(), sensor, units, true, false, false, emptyFlow(),
                        Period.Day1, 1, emptyFlow(), { data }, onChartCountChanged = {
                            for (container in data.value) container.uiComponent?.let { chart ->
                                if (attached.add(System.identityHashCode(chart))) chart.addOnLayoutChangeListener { v, left, top, right, bottom, _, _, _, _ ->
                                    if (v.isAttachedToWindow && right > left) measured += bottom - top
                                }
                            }
                        })
                }
            }
        }
        val types = listOf(UnitType.TemperatureUnit.Celsius, UnitType.HumidityUnit.Relative,
            UnitType.PressureUnit.Pascal, UnitType.CO2.Ppm, UnitType.VOC.VocIndex)
        for (count in listOf(3, 5, 3)) {
            compose.runOnIdle { data.value = mutableListOf() }
            compose.waitForIdle()
            compose.runOnIdle {
                data.value = types.take(count).map { unit ->
                    val rows = listOf(HistorySample(100, 10.0, 0), HistorySample(200, 20.0, 0))
                    val window = HistoryWindow(100, 200)
                    val state = HistoryController.State("synthetic", HistoryPlot(window, samples = mapOf("t" to rows),
                        coverage = listOf(window), complete = true), visible = window)
                    val entries = rows.map { Entry(((it.timestamp-100)*1000).toFloat(), it.value.toFloat(), it) }
                    ChartContainer(unit, null, entries.toMutableList(), from = 100_000, to = 200_000,
                        history = HistoryChartData(state, "t", listOf(entries), {}, { _, _ -> }))
                }.toMutableList()
            }
            compose.waitForIdle()
        }
        compose.runOnIdle {
            assertTrue("The actual chart views must be measured", measured.isNotEmpty())
            assertTrue("No attached chart may flash as a zero/tiny-height grid: $measured", measured.all { it >= 100 })
        }
    }
}
