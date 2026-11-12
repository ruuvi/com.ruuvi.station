package com.ruuvi.station.util.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.test.core.app.ApplicationProvider
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.theme.RuuviTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(application = BottomSnackbarTestApplication::class, sdk = [35])
class BottomSnackbarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `RuuviSnackbarHost renders snackbar message`() {
        val hostState = SnackbarHostState()

        composeTestRule.setContent {
            RuuviTheme {
                LaunchedEffect(hostState) {
                    hostState.showSnackbar("Hello Snackbar")
                }
                RuuviSnackbarHost(hostState = hostState)
            }
        }

        composeTestRule.onNodeWithText("Hello Snackbar").assertIsDisplayed()
    }

    @Test
    fun `RuuviSnackbarHost performs snackbar action`() {
        val hostState = SnackbarHostState()
        var result: SnackbarResult? = null

        composeTestRule.setContent {
            RuuviTheme {
                LaunchedEffect(hostState) {
                    result = hostState.showSnackbar(
                        message = "Message with Action",
                        actionLabel = "RETRY",
                    )
                }
                RuuviSnackbarHost(hostState = hostState)
            }
        }

        composeTestRule.onNodeWithText("Message with Action").assertIsDisplayed()
        composeTestRule.onNodeWithText("RETRY").performClick()

        composeTestRule.runOnIdle {
            assertEquals(SnackbarResult.ActionPerformed, result)
        }
    }

    @Test
    fun `showBottomSnackbar on Activity performs action`() {
        val activity = createThemedActivity()
        var actionPerformed = false

        activity.showBottomSnackbar(
            message = "Test Activity Snackbar",
            actionLabel = "UNDO",
            onAction = { actionPerformed = true },
        )
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(100, TimeUnit.MILLISECONDS)

        activity.findViewById<View>(
            com.google.android.material.R.id.snackbar_action,
        ).performClick()

        assertTrue(actionPerformed)
    }

    @Test
    fun `showBottomSnackbar uses activity layout root to find CoordinatorLayout`() {
        val activity = createThemedActivity()
        val coordinatorLayout = CoordinatorLayout(activity)
        activity.setContentView(coordinatorLayout)

        activity.showBottomSnackbar("Coordinator snackbar")
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(100, TimeUnit.MILLISECONDS)

        val snackbarText = activity.findViewById<TextView>(
            com.google.android.material.R.id.snackbar_text,
        )
        val snackbarLayout = (snackbarText.parent as View).parent as View
        assertSame(coordinatorLayout, snackbarLayout.parent)
    }

    @Test
    fun `showBottomSnackbar resolves StringRes message and action`() {
        val activity = createThemedActivity()
        var actionPerformed = false

        activity.showBottomSnackbar(
            messageRes = R.string.ok,
            actionLabelRes = R.string.cancel,
            onAction = { actionPerformed = true },
        )
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(100, TimeUnit.MILLISECONDS)

        assertEquals(
            activity.getString(R.string.ok),
            activity.findViewById<TextView>(
                com.google.android.material.R.id.snackbar_text,
            ).text.toString(),
        )
        val action = activity.findViewById<TextView>(
            com.google.android.material.R.id.snackbar_action,
        )
        assertEquals(activity.getString(R.string.cancel), action.text.toString())

        action.performClick()

        assertTrue(actionPerformed)
    }

    @Test
    fun `showBottomSnackbar with StringRes supports no action`() {
        val activity = createThemedActivity()

        activity.showBottomSnackbar(messageRes = R.string.ok)
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(100, TimeUnit.MILLISECONDS)

        assertEquals(
            activity.getString(R.string.ok),
            activity.findViewById<TextView>(
                com.google.android.material.R.id.snackbar_text,
            ).text.toString(),
        )
    }

    @Test
    fun `showBottomSnackbar ignores action label without callback`() {
        val activity = createThemedActivity()

        activity.showBottomSnackbar(
            message = "No callback",
            actionLabel = "IGNORED",
        )
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(100, TimeUnit.MILLISECONDS)

        val action = activity.findViewById<TextView>(
            com.google.android.material.R.id.snackbar_action,
        )
        assertTrue(action.text.isEmpty())
    }

    @Test
    fun `showBottomSnackbar works with ContextWrapper`() {
        val activity = createThemedActivity()
        val wrapper = ContextWrapper(activity)

        wrapper.showBottomSnackbar("Wrapped Context Message")
    }

    @Test
    fun `showBottomSnackbar safely handles non activity context`() {
        context.showBottomSnackbar("App Context Message")
    }

    @Test
    fun `showBottomSnackbar safely handles activity without content`() {
        val activity = mock<Activity>()

        activity.showBottomSnackbar("Missing content")
    }

    @Test
    fun `showBottomSnackbar safely handles self referencing ContextWrapper`() {
        val wrapper = object : ContextWrapper(context) {
            override fun getBaseContext(): Context = this
        }

        wrapper.showBottomSnackbar("Self-referencing wrapper")
    }

    @Test
    fun `showSnackbarIfNoneVisible shows snackbar when no snackbar is visible`() {
        val hostState = SnackbarHostState()

        composeTestRule.setContent {
            RuuviTheme {
                LaunchedEffect(hostState) {
                    hostState.showSnackbarIfNoneVisible("First Message")
                }
                RuuviSnackbarHost(hostState = hostState)
            }
        }

        composeTestRule.onNodeWithText("First Message").assertIsDisplayed()
    }

    @Test
    fun `showSnackbarIfNoneVisible returns null when snackbar is already visible`() {
        val hostState = SnackbarHostState()
        var secondResult: SnackbarResult? = SnackbarResult.Dismissed

        composeTestRule.setContent {
            RuuviTheme {
                LaunchedEffect(hostState) {
                    hostState.showSnackbar(
                        "First Message",
                        duration = androidx.compose.material.SnackbarDuration.Indefinite
                    )
                }
                RuuviSnackbarHost(hostState = hostState)
            }
        }

        composeTestRule.onNodeWithText("First Message").assertIsDisplayed()

        composeTestRule.runOnIdle {
            kotlinx.coroutines.runBlocking {
                secondResult = hostState.showSnackbarIfNoneVisible("Second Message")
            }
        }

        assertNull(secondResult)
        composeTestRule.onNodeWithText("Second Message").assertDoesNotExist()
    }

    private fun createThemedActivity(): ComponentActivity =
        Robolectric.buildActivity(ComponentActivity::class.java)
            .setup()
            .get()
            .apply { setTheme(R.style.AppTheme) }
}

class BottomSnackbarTestApplication : Application()
