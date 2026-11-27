package com.ruuvi.station.graph

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.ruuvi.station.app.ui.UiText
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.tagdetails.ui.SyncStatus
import com.ruuvi.station.util.Period
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "en-rUS-w411dp-h900dp-port")
class HistorySyncControlTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun `first statistics reserve their row without showing or announcing dashes`() {
        var display by mutableStateOf(com.ruuvi.station.history.HistoryStatisticsDisplay(
            null, com.ruuvi.station.history.HistoryStatisticsPhase.UPDATING))
        var width by mutableStateOf(390.dp)
        compose.setContent {
            Column(Modifier.width(width).height(500.dp)) {
                HistoryStatisticsText(display, { it.toInt().toString() },
                    androidx.compose.ui.text.TextStyle(fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp)),
                    Modifier.fillMaxWidth().testTag("statistics"))
                Box(Modifier.fillMaxWidth().weight(1f).testTag("statisticsPlot"))
            }
        }
        for (size in listOf(390.dp, 320.dp)) {
            compose.runOnIdle { width = size }
            val plot = compose.onNodeWithTag("statisticsPlot").fetchSemanticsNode().boundsInRoot
            for (state in listOf(com.ruuvi.station.history.HistoryStatisticsPhase.UPDATING,
                com.ruuvi.station.history.HistoryStatisticsPhase.PARTIAL, com.ruuvi.station.history.HistoryStatisticsPhase.FAILED)) {
                compose.runOnIdle { display = display.copy(values = null, phase = state) }
                compose.onNodeWithText("Min: — Max: — Average: —").assertDoesNotExist()
                assertFalse(compose.onNodeWithTag("statistics").fetchSemanticsNode().config
                    .contains(androidx.compose.ui.semantics.SemanticsProperties.Text))
                assertTrue(compose.onNodeWithTag("statistics").fetchSemanticsNode().boundsInRoot.height > 0)
                assertEquals(plot, compose.onNodeWithTag("statisticsPlot").fetchSemanticsNode().boundsInRoot)
            }
            for (state in listOf(com.ruuvi.station.history.HistoryStatisticsPhase.PARTIAL,
                com.ruuvi.station.history.HistoryStatisticsPhase.READY, com.ruuvi.station.history.HistoryStatisticsPhase.UPDATING)) {
                compose.runOnIdle { display = display.copy(values = com.ruuvi.station.history.HistoryStatistics(1.0, 9.0, 4.0), phase = state) }
                compose.onNodeWithText("Min: 1 Max: 9 Average: 4").assertIsDisplayed()
                assertEquals(plot, compose.onNodeWithTag("statisticsPlot").fetchSemanticsNode().boundsInRoot)
            }
        }
    }

    @Test fun `statistics states preserve text and layout while fading outdated values`() {
        var phase by mutableStateOf(com.ruuvi.station.history.HistoryStatisticsPhase.READY)
        var width by mutableStateOf(390.dp)
        compose.setContent {
            Column(Modifier.width(width).height(500.dp)) {
                HistoryStatisticsText(com.ruuvi.station.history.HistoryStatisticsDisplay(
                    com.ruuvi.station.history.HistoryStatistics(1.0, 9.0, 4.0), phase), { it.toInt().toString() },
                    androidx.compose.ui.text.TextStyle(fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp)),
                    Modifier.fillMaxWidth().testTag("statistics"))
                Box(Modifier.fillMaxWidth().weight(1f).testTag("statisticsPlot"))
            }
        }
        for (size in listOf(390.dp, 320.dp)) {
            compose.runOnIdle { width = size }
            val plot = compose.onNodeWithTag("statisticsPlot").fetchSemanticsNode().boundsInRoot
            for (state in com.ruuvi.station.history.HistoryStatisticsPhase.entries) {
                compose.runOnIdle { phase = state }
                val layouts = mutableListOf<TextLayoutResult>()
                compose.onNodeWithTag("statistics").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                assertFalse("state=$state width=$size size=${layouts.single().size} paragraph=${layouts.single().multiParagraph.width}x${layouts.single().multiParagraph.height} font=${layouts.single().layoutInput.style.fontSize}", layouts.single().hasVisualOverflow)
                assertEquals("Min: 1 Max: 9 Average: 4", layouts.single().layoutInput.text.text)
                val expectedAlpha = if (state == com.ruuvi.station.history.HistoryStatisticsPhase.READY) .5f else .38f
                assertEquals(expectedAlpha, layouts.single().layoutInput.style.color.alpha, .005f)
                assertEquals(plot, compose.onNodeWithTag("statisticsPlot").fetchSemanticsNode().boundsInRoot)
            }
        }
    }

    @Test fun `cloud progress keeps the plot and duration still while Bluetooth retains its own status`() {
        var progress by mutableStateOf<Float?>(null)
        var width by mutableStateOf(390.dp)
        var failed by mutableStateOf(false)
        var exporting by mutableStateOf(false)
        val bluetooth = MutableStateFlow(SyncStatus("synthetic", syncInProgress = false, statusMessage = UiText.EmptyString))
        var syncs = 0
        compose.setContent {
            RuuviTheme(darkTheme = true) {
                Column(Modifier.width(width).height(500.dp)) {
                    ChartControlElement2(
                        sensorId = "synthetic", viewPeriod = Period.getInstance(1096 * 24), showChartStats = true,
                        syncStatus = bluetooth, chartSizeLevel = 1, hideIncreaseChartSize = false,
                        increaseChartSize = {}, decreaseChartSize = {}, disconnectGattAction = {},
                        shouldSkipGattSyncDialog = { true }, syncGatt = { syncs++ }, setViewPeriod = {},
                        exportToCsv = { null }, exportToXlsx = { null }, removeTagData = {}, refreshStatus = {},
                        dontShowGattSyncDescription = {}, changeShowStats = {},
                        historyEnabled = true, historyDays = 1096, historyPresets = false, historyProgress = progress
                    )
                    HistoryStatus(failed, exporting, null, { failed = false }, { exporting = false })
                    Box(Modifier.fillMaxWidth().weight(1f).testTag("plot"))
                }
            }
        }
        for (size in listOf(390.dp, 320.dp)) {
            compose.runOnIdle { width = size }
            val plot = compose.onNodeWithTag("plot").fetchSemanticsNode().boundsInRoot
            val duration = compose.onNodeWithTag("historyDuration").fetchSemanticsNode().boundsInRoot
            for ((fraction, title) in listOf(0f to "Downloading (0%)", .38f to "Downloading (38%)", 1f to "Downloading (100%)", null to "Sync")) {
                compose.runOnIdle { progress = fraction }
                compose.onNodeWithText(title).assertIsDisplayed()
                val layouts = mutableListOf<TextLayoutResult>()
                compose.onNodeWithText(title, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                assertFalse("Full progress must fit at $size: $title", layouts.single().hasVisualOverflow)
                assertEquals(plot, compose.onNodeWithTag("plot").fetchSemanticsNode().boundsInRoot)
                assertEquals(duration, compose.onNodeWithTag("historyDuration").fetchSemanticsNode().boundsInRoot)
            }
        }
        compose.runOnIdle { progress = .38f }
        compose.onNodeWithText("Downloading (38%)").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(0, syncs); bluetooth.value = bluetooth.value.copy(syncInProgress = true, statusMessage = UiText.DynamicString("120 points")) }
        compose.onNodeWithText("120 points").assertIsDisplayed()
        compose.onNodeWithContentDescription("Cancel").assertHasClickAction()
        compose.runOnIdle { bluetooth.value = bluetooth.value.copy(syncInProgress = false) }
        compose.onNodeWithText("Downloading (38%)").assertIsDisplayed()
        compose.runOnIdle { progress = null }
        compose.onNodeWithText("Sync").performClick()
        compose.runOnIdle { assertEquals(1, syncs) }
        val stablePlot = compose.onNodeWithTag("plot").fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { exporting = true }
        compose.onNodeWithText("Preparing export").assertIsDisplayed()
        assertEquals(stablePlot, compose.onNodeWithTag("plot").fetchSemanticsNode().boundsInRoot)
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertFalse(exporting); failed = true }
        compose.onNodeWithText("Could not load history").assertIsDisplayed()
        assertEquals(stablePlot, compose.onNodeWithTag("plot").fetchSemanticsNode().boundsInRoot)
        compose.onNodeWithText("Try again").performClick()
        compose.runOnIdle { assertFalse(failed) }
    }
}
