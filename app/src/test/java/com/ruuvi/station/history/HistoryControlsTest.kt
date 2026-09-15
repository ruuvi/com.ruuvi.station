package com.ruuvi.station.history

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.graph.HistoryCalendarControl
import com.ruuvi.station.graph.HistoryRangeCaption
import com.ruuvi.station.graph.ViewPeriodMenu
import com.ruuvi.station.graph.VisibleHistorySync
import kotlinx.coroutines.awaitCancellation
import com.ruuvi.station.util.Period
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "en-rUS-w411dp-h891dp")
class HistoryControlsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun `history and account loading share one top spinner while errors remain retryable`() {
        val history = mutableStateOf(com.ruuvi.station.network.domain.HistorySyncState(sensorId = "A", loading = true))
        val account = mutableStateOf(true)
        var retried = false
        compose.setContent {
            RuuviTheme {
                Column {
                    com.ruuvi.station.tagdetails.ui.SensorCardTopAppBar(
                        navigationCallback = {}, chartsEnabled = true,
                        syncInProgress = com.ruuvi.station.graph.cloudSpinnerVisible(account.value, "A", history.value),
                        alarmStatus = com.ruuvi.station.alarm.domain.AlarmSensorStatus.NoAlarms,
                        alarmAction = {}, chartsAction = {}, settingsAction = {}
                    )
                    com.ruuvi.station.graph.CloudHistoryStatus(history.value) { retried = true }
                }
            }
        }
        compose.onAllNodes(hasProgressBarRangeInfo(androidx.compose.ui.semantics.ProgressBarRangeInfo.Indeterminate)).assertCountEquals(1)
        compose.onNodeWithText("Loading cloud history…").assertDoesNotExist()
        compose.runOnIdle { history.value = history.value.copy(loading = false, failed = true) }
        compose.onAllNodes(hasProgressBarRangeInfo(androidx.compose.ui.semantics.ProgressBarRangeInfo.Indeterminate)).assertCountEquals(1)
        compose.runOnIdle { account.value = false }
        compose.onAllNodes(hasProgressBarRangeInfo(androidx.compose.ui.semantics.ProgressBarRangeInfo.Indeterminate)).assertCountEquals(0)
        compose.onNodeWithText("Try again").performClick()
        compose.runOnIdle { assertTrue(retried) }
    }

    @Test fun `only resumed selected history subscribes and leaving cancels it`() {
        val activeSensor = mutableStateOf<String?>(null)
        val started = mutableListOf<String>()
        val stopped = mutableListOf<String>()
        val owner = object : LifecycleOwner {
            val registry = LifecycleRegistry.createUnsafe(this)
            override val lifecycle: Lifecycle = registry
        }
        owner.registry.currentState = Lifecycle.State.RESUMED
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                androidx.compose.material.Text(activeSensor.value ?: "No history open")
                VisibleHistorySync(activeSensor.value) { sensor ->
                    started += sensor
                    try { awaitCancellation() } finally { stopped += sensor }
                }
            }
        }
        compose.runOnIdle { assertTrue(started.isEmpty()); activeSensor.value = "A" }
        compose.waitForIdle()
        compose.waitUntil { started == listOf("A") }
        compose.runOnIdle { activeSensor.value = "B" }
        compose.waitForIdle()
        compose.waitUntil { started == listOf("A", "B") && stopped == listOf("A") }
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.STARTED }
        compose.waitForIdle()
        compose.waitUntil { stopped == listOf("A", "B") }
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        compose.waitForIdle()
        compose.waitUntil { started == listOf("A", "B", "B") }
        compose.runOnIdle { activeSensor.value = null }
        compose.waitForIdle()
        compose.waitUntil { stopped == listOf("A", "B", "B") }
    }

    @Test fun `timespan menu offers each longer shortcut`() {
        var selected: Int? = null
        compose.setContent { RuuviTheme { ViewPeriodMenu(Period.All, { selected = it }) } }
        compose.onNodeWithText("All").performClick()
        compose.onNodeWithText("100 days").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(2400, selected) }
        compose.onNodeWithText("All").performClick()
        compose.onNodeWithText("30 days").performScrollTo().assertExists()
        compose.onNodeWithText("60 days").performScrollTo().assertExists()
    }

    @Test fun `cancel leaves range unchanged and apply converts the selection to custom`() {
        val selection = mutableStateOf<HistorySelection>(HistorySelection.Rolling(7 * 24))
        compose.setContent {
            RuuviTheme {
                Column {
                    HistoryCalendarControl(selection.value) { start, end -> selection.value = HistorySelection.Custom(start, end) }
                    HistoryRangeCaption(selection.value)
                }
            }
        }
        compose.onNodeWithTag("historyCalendar").performClick()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(HistorySelection.Rolling(168), selection.value) }
        compose.onNodeWithTag("historyCalendar").performClick()
        compose.onNodeWithTag("historyCalendarApply").performClick()
        compose.runOnIdle { assertTrue(selection.value is HistorySelection.Custom) }
        compose.onNodeWithTag("historyRangeCaption").assertExists()
    }

    @Test fun `custom caption shows both dates and shortcut returns to rolling`() {
        val selection = mutableStateOf<HistorySelection>(HistorySelection.Custom(
            Instant.parse("2026-09-01T00:00:00Z").toEpochMilli(), Instant.parse("2026-09-07T00:00:00Z").toEpochMilli()))
        compose.setContent {
            RuuviTheme {
                Column {
                    ViewPeriodMenu(Period.All, { selection.value = HistorySelection.Rolling(it) }, custom = selection.value is HistorySelection.Custom)
                    HistoryRangeCaption(selection.value)
                }
            }
        }
        compose.onNodeWithTag("historyRangeCaption").assertTextContains("2026", substring = true)
        compose.onNodeWithText("Custom").performClick()
        compose.onNodeWithText("30 days").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(HistorySelection.Rolling(720), selection.value) }
        compose.onNodeWithTag("historyRangeCaption").assertDoesNotExist()
    }
}
