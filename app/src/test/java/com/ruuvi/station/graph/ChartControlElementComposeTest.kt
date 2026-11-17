package com.ruuvi.station.graph

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.app.ui.theme.darkPalette
import com.ruuvi.station.app.ui.theme.provideTypography
import com.ruuvi.station.util.Period
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "en-rUS-w411dp-h1200dp-port-notnight-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ChartControlElementComposeTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `period popup stays dark in a light theme and selecting a period dismisses it`() {
        var selected: Int? = null
        setLightContent {
            ViewPeriodMenu(viewPeriod = Period.Hour1, setViewPeriod = { selected = it })
        }

        compose.onNodeWithText(text(R.string.hour_1)).performClick()
        assertDarkPopup(R.string.all)
        compose.onNodeWithText(text(R.string.hour_2)).performClick()

        compose.runOnIdle { assertEquals(Period.Hour2.value, selected) }
        compose.onNode(isPopup()).assertDoesNotExist()
    }

    @Test
    fun `custom period formats its hour count and more opens the longer history dialog`() {
        setLightContent {
            ViewPeriodMenu(viewPeriod = Period.HourX(42), setViewPeriod = {})
        }

        compose.onNodeWithText(compose.activity.getString(R.string.hour_x, 42)).performClick()
        compose.onNodeWithText(text(R.string.more)).performScrollTo().performClick()

        compose.onNode(isPopup()).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.longer_history_title)).assertExists()
        compose.onNodeWithText(text(R.string.longer_history_message)).assertExists()
    }

    @Test
    fun `actions popup stays dark in a light theme and statistics action updates its caption`() {
        var showStats by mutableStateOf(false)
        var changes = 0
        setLightContent {
            ActionsMenu(
                showStats = showStats,
                changeShowStats = {
                    changes++
                    showStats = !showStats
                },
            )
        }

        openActions()
        assertDarkPopup(R.string.export_history)
        compose.onNodeWithText(text(R.string.chart_stat_show)).performClick()
        compose.onNode(isPopup()).assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, changes) }

        openActions()
        compose.onNodeWithText(text(R.string.chart_stat_hide)).assertExists()
        compose.onNodeWithText(text(R.string.chart_stat_show)).assertDoesNotExist()
    }

    @Test
    fun `export actions invoke their callbacks and dismiss even when no file is available`() {
        var csvCalls = 0
        var xlsxCalls = 0
        setLightContent {
            ThreeDotsMenu(
                sensorId = "sensor",
                showChartStats = false,
                chartSizeLevel = 1,
                hideIncreaseChartSize = false,
                increaseChartSize = {},
                decreaseChartSize = {},
                exportToCsv = { csvCalls++; null },
                exportToXlsx = { xlsxCalls++; null },
                clearHistory = {},
                changeShowStats = {},
            )
        }

        openActions()
        compose.onNodeWithText(text(R.string.export_history)).performClick()
        compose.onNode(isPopup()).assertDoesNotExist()
        openActions()
        compose.onNodeWithText(text(R.string.export_history_xlsx)).performClick()
        compose.onNode(isPopup()).assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(1, csvCalls)
            assertEquals(1, xlsxCalls)
        }
    }

    @Test
    fun `clear history requires confirmation and cancel preserves history`() {
        var clearCalls = 0
        setLightContent { ActionsMenu(clearHistory = { clearCalls++ }) }

        openActions()
        compose.onNodeWithText(text(R.string.clear_view)).performClick()
        compose.onNode(isPopup()).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.clear_local_history_description)).assertExists()
        compose.runOnIdle { assertEquals(0, clearCalls) }
        compose.onNodeWithText(text(R.string.cancel)).performClick()
        compose.runOnIdle { assertEquals(0, clearCalls) }

        openActions()
        compose.onNodeWithText(text(R.string.clear_view)).performClick()
        compose.onNodeWithText(text(R.string.clear)).performClick()
        compose.runOnIdle { assertEquals(1, clearCalls) }
        compose.onNodeWithText(text(R.string.clear_local_history_description)).assertDoesNotExist()
    }

    @Test
    fun `portrait chart size actions honor minimum and maximum levels`() {
        var level by mutableStateOf(1)
        setLightContent {
            ActionsMenu(
                sizeLevel = level,
                increase = { level++ },
                decrease = { level-- },
            )
        }

        openActions()
        compose.onNodeWithText(text(R.string.decrease_graph_size)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.increase_graph_size)).performClick()
        compose.onNode(isPopup()).assertDoesNotExist()
        compose.runOnIdle { assertEquals(2, level) }

        openActions()
        compose.onNodeWithText(text(R.string.decrease_graph_size)).assertExists()
        compose.onNodeWithText(text(R.string.increase_graph_size)).performClick()
        openActions()
        compose.onNodeWithText(text(R.string.increase_graph_size)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.decrease_graph_size)).performClick()
        compose.runOnIdle { assertEquals(2, level) }
    }

    @Test
    fun `chart size actions are hidden when too few charts are available`() {
        setLightContent { ActionsMenu(sizeLevel = 2, hideSize = true) }

        openActions()
        assertSizeActionsHidden()
    }

    @Test
    @Config(qualifiers = "en-rUS-land-notnight-mdpi")
    fun `landscape hides chart size actions even at an intermediate level`() {
        setLightContent { ActionsMenu(sizeLevel = 2) }

        openActions()
        assertSizeActionsHidden()
    }

    private fun setLightContent(content: @Composable () -> Unit) {
        compose.setContent {
            MaterialTheme(colors = lightColors()) {
                RuuviTheme(darkTheme = false, content = content)
            }
        }
    }

    @Composable
    private fun ActionsMenu(
        showStats: Boolean = false,
        sizeLevel: Int = 1,
        hideSize: Boolean = false,
        increase: () -> Unit = {},
        decrease: () -> Unit = {},
        clearHistory: () -> Unit = {},
        changeShowStats: () -> Unit = {},
    ) {
        ThreeDotsMenu(
            sensorId = "sensor",
            showChartStats = showStats,
            chartSizeLevel = sizeLevel,
            hideIncreaseChartSize = hideSize,
            increaseChartSize = increase,
            decreaseChartSize = decrease,
            exportToCsv = { null },
            exportToXlsx = { null },
            clearHistory = clearHistory,
            changeShowStats = changeShowStats,
        )
    }

    private fun openActions() {
        compose.onAllNodes(hasClickAction()).onFirst().performClick()
    }

    private fun assertDarkPopup(textRes: Int) {
        val pixels = compose.onNodeWithText(text(textRes)).captureToImage().toPixelMap()
        // Sample the menu row's start padding, away from text and the popup edges.
        assertEquals(darkPalette.background, pixels[2, pixels.height / 2])

        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text(textRes), useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(provideTypography(darkPalette).paragraph.color, layouts.single().layoutInput.style.color)
    }

    private fun assertSizeActionsHidden() {
        compose.onNodeWithText(text(R.string.increase_graph_size)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.decrease_graph_size)).assertDoesNotExist()
    }

    private fun text(res: Int) = compose.activity.getString(res)
}
