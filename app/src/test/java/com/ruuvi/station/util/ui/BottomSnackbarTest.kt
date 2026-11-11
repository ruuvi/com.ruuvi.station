package com.ruuvi.station.util.ui

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
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.test.core.app.ApplicationProvider
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.theme.RuuviTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
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

    @Before
    fun setUp() {
        StaleSnackbarDebouncer.reset()
    }

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
    fun `showSnackbarIfNoneVisible ignores another request while snackbar is visible`() {
        val hostState = SnackbarHostState()
        var secondResult: SnackbarResult? = SnackbarResult.ActionPerformed

        composeTestRule.setContent {
            RuuviTheme {
                LaunchedEffect(hostState) {
                    launch {
                        hostState.showSnackbarIfNoneVisible("First Message")
                    }
                    delay(50)
                    secondResult = hostState.showSnackbarIfNoneVisible("Second Message")
                }
                RuuviSnackbarHost(hostState = hostState)
            }
        }

        composeTestRule.onNodeWithText("First Message").assertIsDisplayed()
        assertNull(secondResult)
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

        val contentView = activity.findViewById<View>(android.R.id.content)
        assertTrue(contentView != null)
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
    fun `showBottomSnackbar with StringRes executes without exception`() {
        val activity = createThemedActivity()

        activity.showBottomSnackbar(
            messageRes = R.string.ok,
            actionLabelRes = R.string.cancel,
            onAction = {}
        )

        val contentView = activity.findViewById<View>(android.R.id.content)
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
