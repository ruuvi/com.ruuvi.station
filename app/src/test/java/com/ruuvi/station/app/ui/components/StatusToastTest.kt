package com.ruuvi.station.app.ui.components

import com.ruuvi.station.util.ui.resetToasts
import com.ruuvi.station.util.ui.latestToastDuration
import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.ViewModel
import androidx.test.core.app.ApplicationProvider
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.UiEvent
import com.ruuvi.station.app.ui.UiText
import com.ruuvi.station.network.domain.OperationStatus
import com.ruuvi.station.util.extensions.processStatus
import com.ruuvi.station.util.ui.RuuviToast
import com.ruuvi.station.util.ui.latestToast
import com.ruuvi.station.util.ui.latestToastText
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowToast
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class StatusToastTest {
    @get:Rule val compose = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @After
    fun clearToast() {
        RuuviToast.cancel()
        resetToasts()
    }

    @Test
    fun `save progress stays visible for the whole save then success shows as a toast`() {
        val events = showStatusToasts()
        val status = MutableSharedFlow<OperationStatus>(extraBufferCapacity = 1)
        val viewModel = object : ViewModel() {}
        var succeeded = false
        compose.runOnIdle {
            viewModel.processStatus(status, events, onSuccess = { succeeded = true })
        }

        compose.runOnIdle { status.tryEmit(OperationStatus.InProgress) }
        compose.waitForIdle()
        compose.onNodeWithTag(SAVE_PROGRESS_TAG)
            .assertExists()
            .assert(hasText(context.getString(R.string.saving_to_cloud)))
        assertNull(latestToastText())

        // A slow save: well past a long toast's 3.5 s, the indicator is still there.
        ShadowLooper.idleMainLooper(10, TimeUnit.SECONDS)
        compose.mainClock.advanceTimeBy(10_000)
        compose.onNodeWithTag(SAVE_PROGRESS_TAG).assertExists()

        compose.runOnIdle { status.tryEmit(OperationStatus.Success) }
        ShadowLooper.idleMainLooper(1, TimeUnit.SECONDS)
        compose.waitForIdle()
        compose.onNodeWithTag(SAVE_PROGRESS_TAG).assertDoesNotExist()
        assertEquals(context.getString(R.string.saving_success), latestToastText())
        assertEquals(Toast.LENGTH_SHORT, latestToastDuration())
        compose.runOnIdle { assertEquals(true, succeeded) }
    }

    @Test
    fun `navigation produces no toast and a failed save shows the failure message`() {
        val events = showStatusToasts()
        compose.runOnIdle { events.tryEmit(UiEvent.NavigateUp) }
        compose.waitForIdle()
        assertNull(latestToastText())

        val viewModel = object : ViewModel() {}
        compose.runOnIdle {
            viewModel.processStatus(flowOf(OperationStatus.Fail(UiText.DynamicString("Offline"))), events)
        }
        compose.waitForIdle()
        compose.onNodeWithTag(SAVE_PROGRESS_TAG).assertDoesNotExist()
        assertEquals(context.getString(R.string.saving_fail), latestToastText())
        assertEquals(Toast.LENGTH_SHORT, latestToastDuration())
    }

    @Test
    fun `a failed save removes the progress indicator`() {
        val events = showStatusToasts()
        val status = MutableSharedFlow<OperationStatus>(extraBufferCapacity = 1)
        val viewModel = object : ViewModel() {}
        compose.runOnIdle { viewModel.processStatus(status, events) }

        compose.runOnIdle { status.tryEmit(OperationStatus.InProgress) }
        compose.waitForIdle()
        compose.onNodeWithTag(SAVE_PROGRESS_TAG).assertExists()

        compose.runOnIdle { status.tryEmit(OperationStatus.Fail(UiText.DynamicString("Offline"))) }
        ShadowLooper.idleMainLooper(1, TimeUnit.SECONDS)
        compose.waitForIdle()
        compose.onNodeWithTag(SAVE_PROGRESS_TAG).assertDoesNotExist()
        assertEquals(context.getString(R.string.saving_fail), latestToastText())
    }

    private fun showStatusToasts(): MutableSharedFlow<UiEvent> =
        MutableSharedFlow<UiEvent>(replay = 1).also { events ->
            compose.setContent { ShowStatusToast(events) }
        }
}
