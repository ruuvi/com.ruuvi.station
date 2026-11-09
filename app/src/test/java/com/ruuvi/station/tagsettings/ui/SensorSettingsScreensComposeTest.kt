package com.ruuvi.station.tagsettings.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.dashboard.DashboardType
import com.ruuvi.station.tag.domain.ruuviTagPreview
import com.ruuvi.station.tagsettings.ui.notes.*
import com.ruuvi.station.tagsettings.ui.visible_measurements.*
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "en-rUS-mdpi")
class SensorSettingsScreensComposeTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun text(id: Int) = compose.activity.getString(id)

    @Test
    fun `notes edits updates and navigates back only after successful save`() {
        var note by mutableStateOf("Original")
        var updates = 0
        var back = 0
        val effects = MutableSharedFlow<NotesEffect>(extraBufferCapacity = 1)
        compose.setContent {
            RuuviTheme(darkTheme = false) {
                Notes(
                    scaffoldState = rememberScaffoldState(), note = note,
                    onAction = {
                        when (it) {
                            is NotesActions.EditNote -> note = it.note
                            NotesActions.UpdateNote -> updates++
                        }
                    },
                    effects = effects, uiEvent = MutableSharedFlow(),
                    onNavigateBack = { back++ },
                )
            }
        }
        compose.onNode(hasSetTextAction()).performTextReplacement("Updated note")
        compose.onNodeWithText("12/1000").assertIsDisplayed()
        compose.onNodeWithText(text(R.string.update), ignoreCase = true).performClick()
        compose.runOnIdle {
            assertEquals("Updated note", note)
            assertEquals(1, updates)
            assertEquals(0, back)
            assertTrue(effects.tryEmit(NotesEffect.NoteUpdated))
        }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(1, back) }
    }

    @Test
    fun `visible measurements switches between defaults and customisation`() {
        var defaults by mutableStateOf(true)
        val actions = mutableListOf<VisibleMeasurementsActions>()
        compose.setContent {
            RuuviTheme {
                VisibleMeasurements(
                    sensorState = ruuviTagPreview, useDefault = defaults,
                    dashboardType = DashboardType.IMAGE_VIEW,
                    onAction = {
                        actions += it
                        if (it is VisibleMeasurementsActions.ChangeUseDefault) defaults = it.useDefault
                    },
                    effects = MutableSharedFlow(), getUnitName = { it.toString() },
                    getDefaultImageResource = { R.drawable.bg2 },
                    selected = testSelected, allOptions = testAllOptions,
                )
            }
        }
        compose.onNodeWithText(text(R.string.customisation_settings)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.visible_measurements_use_default)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.customisation_settings)).performScrollTo().assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(VisibleMeasurementsActions.ChangeUseDefault(false), actions.single())
        }
    }

    @Test
    fun `measurement add and remove controls dispatch the selected unit`() {
        val added = mutableListOf<ListOption>()
        val removed = mutableListOf<ListOption>()
        compose.setContent {
            RuuviTheme(darkTheme = true) {
                DragAndDropListEdit(testSelected, testAllOptions, { _, _ -> }, {}, added::add, removed::add)
            }
        }
        compose.onAllNodesWithContentDescription("remove")[0].performClick()
        compose.onAllNodesWithContentDescription("add")[0].performClick()
        compose.runOnIdle {
            assertEquals(listOf(testSelected.first()), removed)
            assertEquals(listOf(testAllOptions.first()), added)
        }
    }

    @Test
    fun `removing an active measurement requires confirmation`() {
        val effects = MutableSharedFlow<VisibleMeasurementsEffect>(extraBufferCapacity = 1)
        val actions = mutableListOf<VisibleMeasurementsActions>()
        val unit = testSelected.first().unit
        compose.setContent {
            RuuviTheme {
                VisibleMeasurements(
                    sensorState = ruuviTagPreview, useDefault = true,
                    dashboardType = DashboardType.entries.first { it != DashboardType.IMAGE_VIEW },
                    onAction = actions::add, effects = effects,
                    getUnitName = { "Temperature" }, getDefaultImageResource = { R.drawable.bg2 },
                    selected = testSelected, allOptions = testAllOptions,
                )
            }
        }
        compose.runOnIdle {
            assertTrue(effects.tryEmit(VisibleMeasurementsEffect.AskRemovalConfirmation(unit)))
        }
        compose.onNodeWithText(text(R.string.yes), ignoreCase = true).performClick()
        compose.runOnIdle {
            assertEquals(listOf(VisibleMeasurementsActions.RemoveFromDisplayOrderAndDisableAlert(unit)), actions)
            assertTrue(effects.tryEmit(VisibleMeasurementsEffect.ForbiddenRemoveLast))
        }
        compose.onNodeWithText(text(R.string.visible_measurements_last_element_message)).assertIsDisplayed()
    }
}
