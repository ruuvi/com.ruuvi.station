package com.ruuvi.station.dashboard.ui

import com.ruuvi.station.app.permissions.PermissionLogicInteractor
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.bluetooth.BluetoothInteractor
import com.ruuvi.station.dashboard.DashboardTapAction
import com.ruuvi.station.dashboard.DashboardType
import com.ruuvi.station.dashboard.domain.SensorsSortingInteractor
import com.ruuvi.station.feature.domain.RuntimeBehavior
import com.ruuvi.station.image.ImageInteractor
import com.ruuvi.station.network.data.response.MarketingConsentResponseBody
import com.ruuvi.station.network.data.response.MarketingConsentStatus
import com.ruuvi.station.network.domain.MarketingConsentInteractor
import com.ruuvi.station.network.domain.NetworkApplicationSettings
import com.ruuvi.station.network.domain.NetworkDataSyncInteractor
import com.ruuvi.station.network.domain.NetworkSignInInteractor
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.nfc.domain.NfcResultInteractor
import com.ruuvi.station.tag.domain.TagInteractor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardActivityViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val preferencesRepository = mockk<PreferencesRepository>(relaxed = true)
    private val networkInteractor = mockk<RuuviNetworkInteractor>(relaxed = true)
    private val marketingConsentInteractor = mockk<MarketingConsentInteractor>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { networkInteractor.signedIn } returns true
        every { marketingConsentInteractor.isEnabled() } returns true
        every { preferencesRepository.getDashboardType() } returns DashboardType.entries.first()
        every { preferencesRepository.getDashboardTapAction() } returns DashboardTapAction.entries.first()
        coEvery {
            marketingConsentInteractor.refresh()
        } returns MarketingConsentResponseBody(consent = false, status = "unsubscribed")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `declining marketing consent hides the prompt and starts the cooldown`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val lastShown = slot<Long>()
        awaitPrompt(viewModel)

        viewModel.respondToMarketingConsentPrompt(false)

        assertEquals(MarketingConsentPrompt.Hidden, viewModel.marketingConsentPrompt.value)
        verify(exactly = 1) {
            preferencesRepository.setMarketingConsentPromptLastShown(capture(lastShown))
        }
        assertTrue(lastShown.captured <= System.currentTimeMillis())
        coVerify(exactly = 0) { marketingConsentInteractor.update(any()) }
    }

    @Test
    fun `successful marketing consent saves the cooldown after the request succeeds`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val lastShown = slot<Long>()
        coEvery {
            marketingConsentInteractor.update(true)
        } returns MarketingConsentResponseBody(consent = true, status = "subscribed")
        awaitPrompt(viewModel)

        viewModel.respondToMarketingConsentPrompt(true)

        assertEquals(MarketingConsentPrompt.Saving, viewModel.marketingConsentPrompt.value)
        advanceUntilIdle()

        assertEquals(MarketingConsentPrompt.Saved, viewModel.marketingConsentPrompt.value)
        verify(exactly = 1) {
            preferencesRepository.setMarketingConsentPromptLastShown(capture(lastShown))
        }
        assertTrue(lastShown.captured <= System.currentTimeMillis())
    }

    @Test
    fun `null marketing consent response keeps prompt retryable without a cooldown`() = runTest(dispatcher) {
        val viewModel = viewModel()
        coEvery { marketingConsentInteractor.update(true) } returns null
        awaitPrompt(viewModel)

        viewModel.respondToMarketingConsentPrompt(true)
        advanceUntilIdle()

        assertEquals(MarketingConsentPrompt.Error, viewModel.marketingConsentPrompt.value)
        verify(exactly = 0) { preferencesRepository.setMarketingConsentPromptLastShown(any()) }
    }

    @Test
    fun `marketing consent request exception keeps prompt retryable without a cooldown`() = runTest(dispatcher) {
        val viewModel = viewModel()
        coEvery { marketingConsentInteractor.update(true) } throws IOException("Offline")
        awaitPrompt(viewModel)

        viewModel.respondToMarketingConsentPrompt(true)
        advanceUntilIdle()

        assertEquals(MarketingConsentPrompt.Error, viewModel.marketingConsentPrompt.value)
        verify(exactly = 0) { preferencesRepository.setMarketingConsentPromptLastShown(any()) }
    }

    @Test
    fun `marketing consent can be retried after a failed request`() = runTest(dispatcher) {
        val viewModel = viewModel()
        coEvery {
            marketingConsentInteractor.update(true)
        } returnsMany listOf(
            null,
            MarketingConsentResponseBody(consent = true, status = "subscribed")
        )
        awaitPrompt(viewModel)

        viewModel.respondToMarketingConsentPrompt(true)
        advanceUntilIdle()
        viewModel.respondToMarketingConsentPrompt(true)
        advanceUntilIdle()

        assertEquals(MarketingConsentPrompt.Saved, viewModel.marketingConsentPrompt.value)
        coVerify(exactly = 2) { marketingConsentInteractor.update(true) }
        verify(exactly = 1) { preferencesRepository.setMarketingConsentPromptLastShown(any()) }
    }

    @Test
    fun `marketing consent ignores duplicate taps while saving`() = runTest(dispatcher) {
        val viewModel = viewModel()
        val requestStarted = CompletableDeferred<Unit>()
        val requestResult = CompletableDeferred<MarketingConsentResponseBody>()
        coEvery {
            marketingConsentInteractor.update(true)
        } coAnswers {
            requestStarted.complete(Unit)
            requestResult.await()
        }
        awaitPrompt(viewModel)

        viewModel.respondToMarketingConsentPrompt(true)
        runCurrent()
        requestStarted.await()
        viewModel.respondToMarketingConsentPrompt(true)

        assertEquals(MarketingConsentPrompt.Saving, viewModel.marketingConsentPrompt.value)
        coVerify(exactly = 1) { marketingConsentInteractor.update(true) }

        requestResult.complete(MarketingConsentResponseBody(consent = true, status = "subscribed"))
        advanceUntilIdle()

        assertEquals(MarketingConsentPrompt.Saved, viewModel.marketingConsentPrompt.value)
    }

    @Test
    fun `marketing consent prompt stays hidden when consent is already granted`() = runTest(dispatcher) {
        coEvery {
            marketingConsentInteractor.refresh()
        } returns MarketingConsentResponseBody(consent = true, status = "subscribed")

        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(MarketingConsentPrompt.Hidden, viewModel.marketingConsentPrompt.value)
        coVerify(exactly = 0) { marketingConsentInteractor.update(any()) }
    }

    @Test
    fun `marketing consent prompt is shown for an address not found in the subscription list`() = runTest(dispatcher) {
        coEvery {
            marketingConsentInteractor.refresh()
        } returns MarketingConsentResponseBody(
            consent = false,
            status = MarketingConsentStatus.NOT_FOUND.value
        )

        val viewModel = viewModel()

        awaitPrompt(viewModel)
    }

    @Test
    fun `marketing consent prompt stays hidden for ineligible subscription statuses`() = runTest(dispatcher) {
        val ineligibleStatuses = MarketingConsentStatus.entries - setOf(
            MarketingConsentStatus.UNSUBSCRIBED,
            MarketingConsentStatus.NOT_FOUND
        )

        ineligibleStatuses.forEach { status ->
            coEvery {
                marketingConsentInteractor.refresh()
            } returns MarketingConsentResponseBody(consent = false, status = status.value)

            val viewModel = viewModel()
            advanceUntilIdle()

            assertEquals(
                "Prompt should stay hidden for $status",
                MarketingConsentPrompt.Hidden,
                viewModel.marketingConsentPrompt.value
            )
        }
    }

    @Test
    fun `marketing consent prompt stays hidden during the cooldown`() = runTest(dispatcher) {
        every {
            preferencesRepository.getMarketingConsentPromptLastShown()
        } returns System.currentTimeMillis()

        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(MarketingConsentPrompt.Hidden, viewModel.marketingConsentPrompt.value)
        coVerify(exactly = 0) { marketingConsentInteractor.update(any()) }
    }

    private suspend fun TestScope.awaitPrompt(viewModel: DashboardActivityViewModel) {
        advanceUntilIdle()
        assertEquals(MarketingConsentPrompt.Ask, viewModel.marketingConsentPrompt.value)
    }

    private fun viewModel() = DashboardActivityViewModel(
        tagInteractor = mockk<TagInteractor>(relaxed = true),
        networkDataSyncInteractor = mockk<NetworkDataSyncInteractor>(relaxed = true),
        networkInteractor = networkInteractor,
        preferencesRepository = preferencesRepository,
        permissionLogicInteractor = mockk<PermissionLogicInteractor>(relaxed = true),
        networkApplicationSettings = mockk<NetworkApplicationSettings>(relaxed = true),
        networkSignInInteractor = mockk<NetworkSignInInteractor>(relaxed = true),
        nfcResultInteractor = mockk<NfcResultInteractor>(relaxed = true),
        sortingInteractor = mockk<SensorsSortingInteractor>(relaxed = true),
        runtimeBehavior = RuntimeBehavior(),
        bluetoothInteractor = mockk<BluetoothInteractor>(relaxed = true),
        marketingConsentInteractor = marketingConsentInteractor,
        imageInteractor = mockk<ImageInteractor>(relaxed = true)
    )
}
