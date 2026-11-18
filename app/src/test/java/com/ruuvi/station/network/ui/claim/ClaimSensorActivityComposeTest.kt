package com.ruuvi.station.network.ui.claim

import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.UiEvent
import com.ruuvi.station.util.ui.UiTestApplication
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNfcAdapter

@RunWith(RobolectricTestRunner::class)
@Config(application = UiTestApplication::class, sdk = [35], qualifiers = "en-rUS-notnight-mdpi")
class ClaimSensorActivityComposeTest {
    @get:Rule
    val compose = createComposeRule()

    private val application get() = RuntimeEnvironment.getApplication() as UiTestApplication
    private val events = MutableSharedFlow<UiEvent>(replay = 1)
    private val bluetoothInProgress = MutableStateFlow(false)
    private val nfcInProgress = MutableStateFlow(false)

    @Before
    fun setUp() {
        setNfcHardwareExists(true)
    }

    @Test
    fun `force claim opens the Ruuvi Air instructions with both methods`() =
        withActivity(isAir = true, route = ClaimRoutes.FORCE_CLAIM_INIT) { viewModel ->
            button(R.string.force_claim).assertIsDisplayed().performClick()
            verify(viewModel).getSensorId()
            verify(viewModel, never()).useBluetooth()
            verify(viewModel, never()).useNfc()

            showRoute(ClaimRoutes.FORCE_CLAIM_GETTING_ID)

            instructions(R.string.force_claim_air_description2)
            button(R.string.use_nfc).assertIsDisplayed().assertIsEnabled()
            button(R.string.use_bluetooth).assertIsDisplayed().assertIsEnabled()
        }

    @Test
    fun `without NFC hardware the tag instructions only offer Bluetooth`() {
        setNfcHardwareExists(false)

        withActivity { viewModel ->
            instructions(R.string.force_claim_sensor_description2)
            button(R.string.use_nfc).assertDoesNotExist()
            button(R.string.use_bluetooth).assertIsDisplayed().assertIsEnabled().performClick()
            verify(viewModel).useBluetooth()
        }
    }

    @Test
    fun `Use NFC starts waiting when NFC is enabled`() {
        shadowOf(checkNotNull(NfcAdapter.getDefaultAdapter(application))).setEnabled(true)

        withActivity { viewModel ->
            button(R.string.use_nfc).performClick()
            verify(viewModel).useNfc()
            verify(viewModel, never()).useBluetooth()
        }
    }

    @Test
    fun `Use NFC opens NFC settings when NFC is disabled`() {
        shadowOf(checkNotNull(NfcAdapter.getDefaultAdapter(application))).setEnabled(false)

        withActivity { viewModel ->
            button(R.string.use_nfc).performClick()

            assertEquals(
                Settings.ACTION_NFC_SETTINGS,
                shadowOf(application).nextStartedActivity.action,
            )
            verify(viewModel, never()).useNfc()
        }
    }

    @Test
    @Config(qualifiers = "+land")
    fun `both methods remain visible in landscape and re-enable after either wait ends`() =
        withActivity { _ ->
            button(R.string.use_nfc).assertIsDisplayed()
            button(R.string.use_bluetooth).assertIsDisplayed()

            bluetoothInProgress.value = true
            assertMethodsDisabled()

            bluetoothInProgress.value = false
            assertMethodsEnabled()

            nfcInProgress.value = true
            assertMethodsDisabled()

            nfcInProgress.value = false
            assertMethodsEnabled()
        }

    private fun setNfcHardwareExists(exists: Boolean) {
        ShadowNfcAdapter.setNfcHardwareExists(exists)
        shadowOf(application.packageManager).setSystemFeature(PackageManager.FEATURE_NFC, exists)
    }

    private fun assertMethodsDisabled() {
        button(R.string.use_nfc).assertIsNotEnabled()
        button(R.string.use_bluetooth).assertIsNotEnabled()
    }

    private fun assertMethodsEnabled() {
        button(R.string.use_nfc).assertIsEnabled()
        button(R.string.use_bluetooth).assertIsEnabled()
    }

    private fun instructions(resource: Int) {
        compose.onNodeWithText(application.getString(resource)).assertIsDisplayed()
    }

    private fun button(resource: Int) =
        compose.onNode(hasClickAction() and hasText(application.getString(resource)))

    private fun showRoute(route: String) {
        check(events.tryEmit(UiEvent.Navigate(route, popBackStack = true)))
        compose.waitForIdle()
    }

    private fun withActivity(
        isAir: Boolean = false,
        route: String = ClaimRoutes.FORCE_CLAIM_GETTING_ID,
        assertions: (ClaimSensorViewModel) -> Unit,
    ) {
        val viewModel = mock<ClaimSensorViewModel> {
            on { this.isAir } doReturn isAir
            on { uiEvent } doReturn events
            on { this.bluetoothInProgress } doReturn bluetoothInProgress
            on { this.nfcInProgress } doReturn nfcInProgress
        }
        application.claimSensorViewModel = viewModel
        showRoute(route)
        val controller = Robolectric.buildActivity(ClaimSensorActivity::class.java).setup().visible()
        try {
            compose.waitForIdle()
            assertions(viewModel)
        } finally {
            controller.pause().stop().destroy()
        }
    }
}
