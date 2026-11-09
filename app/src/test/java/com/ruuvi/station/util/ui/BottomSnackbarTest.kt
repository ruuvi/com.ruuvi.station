package com.ruuvi.station.util.ui

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.theme.RuuviTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
    fun `RuuviSnackbarHost renders snackbar with action label`() {
        val hostState = SnackbarHostState()

        composeTestRule.setContent {
            RuuviTheme {
                LaunchedEffect(hostState) {
                    hostState.showSnackbar(
                        message = "Message with Action",
                        actionLabel = "RETRY"
                    )
                }
                RuuviSnackbarHost(hostState = hostState)
            }
        }

        composeTestRule.onNodeWithText("Message with Action").assertIsDisplayed()
        composeTestRule.onNodeWithText("RETRY").assertIsDisplayed()
    }

    @Test
    fun `showBottomSnackbar on Activity executes without exception`() {
        val activity = createThemedActivity()

        activity.showBottomSnackbar(
            message = "Test Activity Snackbar",
            actionLabel = "UNDO",
            onAction = {}
        )

        val contentView = activity.findViewById<android.view.View>(android.R.id.content)
        assertTrue(contentView != null)
    }

    @Test
    fun `showBottomSnackbar with StringRes executes without exception`() {
        val activity = createThemedActivity()

        activity.showBottomSnackbar(
            messageRes = R.string.ok,
            actionLabelRes = R.string.cancel,
            onAction = {}
        )

        val contentView = activity.findViewById<android.view.View>(android.R.id.content)
        assertTrue(contentView != null)
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

    private fun createThemedActivity(): ComponentActivity =
        Robolectric.buildActivity(ComponentActivity::class.java)
            .setup()
            .get()
            .apply { setTheme(R.style.AppTheme) }
}

class BottomSnackbarTestApplication : Application()
