package com.ruuvi.station.network.ui

import android.app.Application
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.MutableLiveData
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.bluetooth.model.SensorFirmwareResult
import com.ruuvi.station.dfu.ui.DfuUpdateScreen
import com.ruuvi.station.dfu.ui.DfuUpdateStage
import com.ruuvi.station.dfu.ui.DfuUpdateViewModel
import com.ruuvi.station.network.data.response.MarketingConsentStatus
import com.ruuvi.station.tagsettings.ui.BackgroundActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "en-rUS-mdpi")
class ChangedScreensComposeTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun text(id: Int) = compose.activity.getString(id)

    @After fun resetToken() { fcmToken = null }

    @Test
    fun `share form sends typed email and clears input`() {
        val shared = mutableListOf<String>()
        compose.setContent {
            RuuviTheme {
                ShareBody(
                    rememberScaffoldState(), true, emptyList(), emptyList(),
                    0, 20, 5, shared::add, {},
                )
            }
        }
        compose.onNode(hasSetTextAction()).performTextInput("friend@example.com")
        compose.onNodeWithText(text(R.string.share), ignoreCase = true).performClick()
        compose.runOnIdle { assertEquals(listOf("friend@example.com"), shared) }
        compose.onNode(hasSetTextAction()).assertTextContains("")
        compose.onNodeWithText(text(R.string.share_active_access_empty_value)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `active and pending shares render and unshare requires confirmation`() {
        val removed = mutableListOf<String>()
        compose.setContent {
            RuuviTheme {
                ShareBody(
                    rememberScaffoldState(), false, listOf("active@example.com"),
                    listOf("pending@example.com"), 2, 20, 5, {}, removed::add,
                )
            }
        }
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        compose.onNodeWithText("active@example.com").performScrollTo().assertIsDisplayed()
        compose.onAllNodes(hasClickAction() and hasAnySibling(hasText("active@example.com")))[0].performClick()
        compose.runOnIdle { assertTrue(removed.isEmpty()) }
        compose.onNodeWithText(text(R.string.yes), ignoreCase = true).performClick()
        compose.runOnIdle { assertEquals(listOf("active@example.com"), removed) }
        compose.onNodeWithText("pending@example.com").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(text(R.string.share_sensor_description)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `account actions stay accessible and sign out can be cancelled or confirmed`() {
        var deleted = 0
        var signedOut = 0
        compose.setContent {
            RuuviTheme {
                MyAccountBody(
                    "tester@example.com", MarketingConsentUiState(false), {},
                    { signedOut++ }, { deleted++ }, null, null,
                )
            }
        }
        compose.onNodeWithText(text(R.string.delete_account), ignoreCase = true).assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, deleted) }
        compose.onNodeWithText(text(R.string.sign_out), ignoreCase = true).performClick()
        compose.onNodeWithText(text(R.string.sign_out_confirm)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.no), ignoreCase = true).performClick()
        compose.runOnIdle { assertEquals(0, signedOut) }
        compose.onNodeWithText(text(R.string.sign_out), ignoreCase = true).performClick()
        compose.onNodeWithText(text(R.string.yes), ignoreCase = true).performClick()
        compose.runOnIdle { assertEquals(1, signedOut) }
    }

    @Test
    fun `account consent subscription and debug information remain scrollable in dark theme`() {
        var toggled: Boolean? = null
        var consent by mutableStateOf(
            MarketingConsentUiState(false, MarketingConsentStatus.UNSUBSCRIBED, true, true)
        )
        fcmToken = "test-notification-token"
        compose.setContent {
            RuuviTheme(darkTheme = true) {
                MyAccountBody(
                    "tester@example.com", consent, { toggled = it }, {}, {},
                    Subscription("Test Plan", Date(1_700_000_000_000), 10, 20, 5),
                    listOf(1L to "registered-token"),
                )
            }
        }
        compose.onNodeWithText(text(R.string.newsletter_subscription)).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(true, toggled) }
        compose.runOnIdle {
            consent = consent.copy(status = MarketingConsentStatus.UNCONFIRMED, isToggleEnabled = false)
        }
        compose.onNodeWithText(text(R.string.newsletter_subscription_confirmation)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Test Plan").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("1 - registered-token").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(text(R.string.sign_out), ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun `background picker header and final grid row are accessible`() {
        val screen = BackgroundActivity()
        compose.setContent {
            RuuviTheme {
                screen.Body(
                    rememberScaffoldState(),
                    List(30) { R.drawable.bg2 },
                    {}, {},
                    { File("unused-camera-file") to Uri.EMPTY },
                    {},
                )
            }
        }
        compose.onNodeWithText(text(R.string.change_background)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.take_photo)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.select_from_gallery)).assertIsDisplayed()
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(30)
        compose.onNodeWithText(text(R.string.change_background)).assertIsDisplayed()
    }

    @Test
    fun `firmware screen renders progress success and errors as stage changes`() {
        val vm = mockk<DfuUpdateViewModel>(relaxed = true)
        val stage = MutableLiveData(DfuUpdateStage.CHECKING_CURRENT_FW_VERSION)
        every { vm.stage } returns stage
        every { vm.latestFwVersion } returns MutableLiveData("2.0.0")
        every { vm.sensorFwVersion } returns MutableLiveData<SensorFirmwareResult>(null)
        every { vm.canStartUpdate } returns androidx.lifecycle.MediatorLiveData<Boolean>().apply { value = true }
        every { vm.lowBattery } returns MutableLiveData(true)
        every { vm.downloadFwProgress } returns MutableLiveData(50)
        every { vm.updateFwProgress } returns MutableLiveData(75)
        every { vm.deviceDiscovered } returns MutableLiveData(true)
        every { vm.error } returns MutableLiveData("Test firmware error")
        every { vm.errorCode } returns MutableLiveData<Int>(null)
        compose.setContent { RuuviTheme { DfuUpdateScreen(vm) {} } }
        compose.onNodeWithText("2.0.0").assertExists()
        compose.onNodeWithText(text(R.string.start_update_process), ignoreCase = true)
            .performScrollTo().performClick()
        verify { vm.startDownloadProcess(any()) }

        compose.runOnIdle { stage.value = DfuUpdateStage.DOWNLOADING_FW }
        compose.onNodeWithText("50 %").assertExists()
        compose.runOnIdle { stage.value = DfuUpdateStage.UPDATING_FW }
        compose.onNodeWithText("75 %").assertExists()
        compose.runOnIdle { stage.value = DfuUpdateStage.ALREADY_LATEST_VERSION }
        compose.onNodeWithText(text(R.string.already_latest_version)).assertExists()
        compose.runOnIdle { stage.value = DfuUpdateStage.UPDATE_FINISHED }
        compose.onNodeWithText(text(R.string.update_successful_tag)).assertExists()
        compose.runOnIdle { stage.value = DfuUpdateStage.ERROR }
        compose.onNodeWithText("Test firmware error").assertExists()
    }
}
