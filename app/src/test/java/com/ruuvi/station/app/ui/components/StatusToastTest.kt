package com.ruuvi.station.app.ui.components

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.ViewModel
import androidx.test.core.app.ApplicationProvider
import com.ruuvi.station.R
import com.ruuvi.station.app.permissions.permissionToast
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
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class StatusToastTest {
    @get:Rule val compose = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @After
    fun clearToast() {
        RuuviToast.cancel()
        ShadowToast.reset()
    }

    @Test
    fun `save reports progress as a long toast then shows success`() {
        val events = showStatusToasts()
        val viewModel = object : ViewModel() {}
        compose.runOnIdle {
            viewModel.processStatus(flowOf(OperationStatus.InProgress), events)
        }
        compose.waitForIdle()
        assertEquals(context.getString(R.string.saving_to_cloud), latestToastText())
        assertEquals(Toast.LENGTH_LONG, latestToast()?.duration)

        var succeeded = false
        compose.runOnIdle {
            viewModel.processStatus(flowOf(OperationStatus.Success), events, onSuccess = { succeeded = true })
        }
        compose.waitForIdle()
        assertEquals(context.getString(R.string.saving_success), latestToastText())
        assertEquals(Toast.LENGTH_SHORT, latestToast()?.duration)
        compose.runOnIdle { assertEquals(true, succeeded) }
    }

    @Test
    fun `navigation produces no toast and a failed save shows the failure message`() {
        val events = showStatusToasts()
        compose.runOnIdle { events.tryEmit(UiEvent.NavigateUp) }
        compose.waitForIdle()
        assertNull(latestToast())

        val viewModel = object : ViewModel() {}
        compose.runOnIdle {
            viewModel.processStatus(flowOf(OperationStatus.Fail(UiText.DynamicString("Offline"))), events)
        }
        compose.waitForIdle()
        assertEquals(context.getString(R.string.saving_fail), latestToastText())
        assertEquals(Toast.LENGTH_SHORT, latestToast()?.duration)
    }

    @Test
    fun `permission guidance uses a long toast`() {
        val message = context.getString(R.string.permission_location_needed_api31)
        permissionToast(context, message)

        assertEquals(message, latestToastText())
        assertEquals(Toast.LENGTH_LONG, latestToast()?.duration)
    }

    private fun showStatusToasts(): MutableSharedFlow<UiEvent> =
        MutableSharedFlow<UiEvent>(replay = 1).also { events ->
            compose.setContent { ShowStatusToast(events) }
        }
}
