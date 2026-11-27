package com.ruuvi.station.graph

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.util.Period
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "en-rUS-w411dp-h900dp-port")
class HistoryControlsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun `ten days remains selectable for local and cloud preset limits`() {
        for (days in listOf(10, 1096)) {
            val options = historyDurationHours(days, true)
            assertEquals(1, options.count { it == 240 })
            assertTrue(options.indexOf(168) < options.indexOf(240))
            if (days > 10) assertTrue(options.indexOf(240) < options.indexOf(336))
        }
        assertFalse(historyDurationHours(7, true).contains(240))
        var selected: Int? = null
        compose.setContent { RuuviTheme(darkTheme = true) { HistoryDurationMenu(Period.Day1,1096,true,null) { selected = it } } }
        compose.onNodeWithTag("historyDuration").performClick()
        compose.onNodeWithText("10 days").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(240, selected) }
    }
    @Test fun `production menu selects three years using localized preset labels`() {
        var selected: Int? = null
        compose.setContent { RuuviTheme(darkTheme = true) { HistoryDurationMenu(Period.Hour1,1096,true,null) { selected = it } } }
        compose.onNodeWithTag("historyDuration").performClick()
        compose.onNodeWithText("3 years").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(26304, selected) }
    }
    @Test fun `granular menu reaches the final allowed day without composing every row`() {
        var selected: Int? = null
        compose.setContent { RuuviTheme(darkTheme = true) { HistoryDurationMenu(Period.Hour1,1096,false,null) { selected = it } } }
        compose.onNodeWithTag("historyDuration").performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(historyDurationHours(1096,false).lastIndex)
        compose.onNodeWithText("1096 days").performClick()
        compose.runOnIdle { assertEquals(26304, selected) }
    }
    @Test fun `changing preset presentation preserves arbitrary selected durations`() {
        compose.setContent { RuuviTheme(darkTheme = true) { HistoryDurationMenu(Period.getInstance(400 * 24),1096,true,null) {} } }
        compose.onNodeWithText("400 days").assertExists()
    }
}
