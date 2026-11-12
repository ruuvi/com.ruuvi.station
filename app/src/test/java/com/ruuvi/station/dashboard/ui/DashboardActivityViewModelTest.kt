package com.ruuvi.station.dashboard.ui

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.dashboard.DashboardTapAction
import com.ruuvi.station.dashboard.DashboardType
import com.ruuvi.station.network.data.response.MarketingConsentResponseBody
import com.ruuvi.station.network.data.response.MarketingConsentStatus
import com.ruuvi.station.network.domain.MarketingConsentInteractor
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tag.domain.TagInteractor
import com.ruuvi.station.tag.domain.ruuviTagPreview
import com.ruuvi.station.tag.domain.sensorMeasurementsPreview
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
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
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardActivityViewModelTest {
    private val tagInteractor = mockk<TagInteractor>(relaxed = true)
    private val preferencesRepository = mockk<PreferencesRepository>(relaxed = true)
    private val dispatcher = StandardTestDispatcher()
    private val networkInteractor = mockk<RuuviNetworkInteractor>(relaxed = true)
    private val marketingConsentInteractor = mockk<MarketingConsentInteractor>(relaxed = true)
    private var sensors = listOf(sensor())
    private var timeoutSeconds = 60L

    private val viewModel by lazy {
        DashboardActivityViewModel(
            tagInteractor = tagInteractor,
            networkDataSyncInteractor = mockk(relaxed = true),
            networkInteractor = mockk(relaxed = true),
            preferencesRepository = preferencesRepository,
            permissionLogicInteractor = mockk(relaxed = true),
            networkApplicationSettings = mockk(relaxed = true),
            networkSignInInteractor = mockk(relaxed = true),
            nfcResultInteractor = mockk(relaxed = true),
            sortingInteractor = mockk(relaxed = true),
            runtimeBehavior = mockk(relaxed = true),
            bluetoothInteractor = mockk(relaxed = true),
            marketingConsentInteractor = mockk(relaxed = true),
            imageInteractor = mockk(relaxed = true)
        )
    }

    @Before
    fun setUp() {
        every { tagInteractor.getTags() } answers { sensors }
        every { preferencesRepository.getDashboardCardTimeoutSeconds() } answers { timeoutSeconds }
        every { preferencesRepository.getDashboardType() } returns DashboardType.IMAGE_VIEW
        every { preferencesRepository.getDashboardTapAction() } returns DashboardTapAction.getByCode("")
        Dispatchers.setMain(dispatcher)
        every { networkInteractor.signedIn } returns true
        every { marketingConsentInteractor.isEnabled() } returns true
        coEvery {
            marketingConsentInteractor.refresh()
        } returns MarketingConsentResponseBody(consent = false, status = "unsubscribed")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `card becomes stale at the timeout without receiving another update`() {
        viewModel.refreshSensorFreshness(UPDATED_AT + 59_999)
        assertTrue(viewModel.staleSensorIds.value.isEmpty())

        viewModel.refreshSensorFreshness(UPDATED_AT + 60_000)
        assertEquals(setOf("sensor"), viewModel.staleSensorIds.value)
        verify(exactly = 1) { tagInteractor.getTags() }
    }

    @Test
    fun `one second timeout does not mark a card stale early`() {
        timeoutSeconds = 1L

        viewModel.refreshSensorFreshness(UPDATED_AT + 999)
        assertTrue(viewModel.staleSensorIds.value.isEmpty())

        viewModel.refreshSensorFreshness(UPDATED_AT + 1_000)
        assertEquals(setOf("sensor"), viewModel.staleSensorIds.value)
    }

    @Test
    fun `freshness is evaluated independently for local cloud and missing measurements`() {
        sensors = listOf(
            sensor(id = "local-stale", updatedAt = UPDATED_AT - 60_000),
            sensor(id = "local-fresh"),
            sensor(id = "cloud-stale", updatedAt = UPDATED_AT - 60_000).copy(
                networkSensor = true,
                networkLastSync = Date(UPDATED_AT - 60_000)
            ),
            sensor(id = "cloud-fresh").copy(
                networkSensor = true,
                networkLastSync = Date(UPDATED_AT)
            ),
            sensor(id = "no-data").copy(latestMeasurement = null)
        )

        viewModel.refreshSensorFreshness(UPDATED_AT)

        assertEquals(setOf("local-stale", "cloud-stale", "no-data"), viewModel.staleSensorIds.value)
    }

    @Test
    fun `old measurements are stale as soon as the dashboard opens`() {
        assertEquals(setOf("sensor"), viewModel.staleSensorIds.value)
    }

    @Test
    fun `future measurement timestamp is not stale`() {
        viewModel.refreshSensorFreshness(UPDATED_AT - 1_000)

        assertTrue(viewModel.staleSensorIds.value.isEmpty())
    }

    @Test
    fun `updated developer timeout takes effect without recreating the dashboard`() {
        viewModel.refreshSensorFreshness(UPDATED_AT + 30_000)
        assertTrue(viewModel.staleSensorIds.value.isEmpty())

        timeoutSeconds = 5L
        viewModel.refreshSensorFreshness(UPDATED_AT + 30_000)
        assertEquals(setOf("sensor"), viewModel.staleSensorIds.value)

        timeoutSeconds = 300L
        viewModel.refreshSensorFreshness(UPDATED_AT + 30_000)
        assertTrue(viewModel.staleSensorIds.value.isEmpty())
    }

    @Test
    fun `large timeout does not overflow into a stale card`() {
        timeoutSeconds = Long.MAX_VALUE

        viewModel.refreshSensorFreshness(UPDATED_AT + 60_000)

        assertTrue(viewModel.staleSensorIds.value.isEmpty())
    }

    @Test
    fun `new measurement restores freshness even when sensor values are unchanged`() = runBlocking {
        assertEquals(setOf("sensor"), viewModel.staleSensorIds.value)
        sensors = listOf(sensor(updatedAt = System.currentTimeMillis()))

        viewModel.refreshSensors()

        assertTrue(viewModel.staleSensorIds.value.isEmpty())
    }

    @Test
    fun `card still expires while sensor list updates are paused for scrolling`() {
        viewModel.refreshSensorFreshness(UPDATED_AT)
        viewModel.setSensorListScrolling(true)

        viewModel.refreshSensorFreshness(UPDATED_AT + 60_000)

        assertEquals(setOf("sensor"), viewModel.staleSensorIds.value)
    }

    @Test
    fun `resuming scrolling restores freshness from a pending measurement`() = runBlocking {
        assertEquals(setOf("sensor"), viewModel.staleSensorIds.value)
        viewModel.setSensorListScrolling(true)
        sensors = listOf(sensor(updatedAt = System.currentTimeMillis()))

        viewModel.refreshSensors()
        assertEquals(setOf("sensor"), viewModel.staleSensorIds.value)
        viewModel.setSensorListScrolling(false)

        assertTrue(viewModel.staleSensorIds.value.isEmpty())
    }

    @Test
    fun `removed sensors no longer appear in stale sensor state`() = runBlocking {
        assertEquals(setOf("sensor"), viewModel.staleSensorIds.value)
        sensors = emptyList()

        viewModel.refreshSensors()

        assertTrue(viewModel.staleSensorIds.value.isEmpty())
    }


    @Test
    fun `declining marketing consent hides the prompt and starts the cooldown`() = runTest(dispatcher) {
        val viewModel = marketingViewModel()
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
        val viewModel = marketingViewModel()
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
        val viewModel = marketingViewModel()
        coEvery { marketingConsentInteractor.update(true) } returns null
        awaitPrompt(viewModel)

        viewModel.respondToMarketingConsentPrompt(true)
        advanceUntilIdle()

        assertEquals(MarketingConsentPrompt.Error, viewModel.marketingConsentPrompt.value)
        verify(exactly = 0) { preferencesRepository.setMarketingConsentPromptLastShown(any()) }
    }

    @Test
    fun `marketing consent request exception keeps prompt retryable without a cooldown`() = runTest(dispatcher) {
        val viewModel = marketingViewModel()
        coEvery { marketingConsentInteractor.update(true) } throws IOException("Offline")
        awaitPrompt(viewModel)

        viewModel.respondToMarketingConsentPrompt(true)
        advanceUntilIdle()

        assertEquals(MarketingConsentPrompt.Error, viewModel.marketingConsentPrompt.value)
        verify(exactly = 0) { preferencesRepository.setMarketingConsentPromptLastShown(any()) }
    }

    @Test
    fun `marketing consent can be retried after a failed request`() = runTest(dispatcher) {
        val viewModel = marketingViewModel()
        coEvery {
            marketingConsentInteractor.update(true)
        } returnsMany listOf(
            null,
            MarketingConsentResponseBody(consent = true, status = "subscribed"),
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
        val viewModel = marketingViewModel()
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

        requestResult.complete(
            MarketingConsentResponseBody(consent = true, status = "subscribed"),
        )
        advanceUntilIdle()

        assertEquals(MarketingConsentPrompt.Saved, viewModel.marketingConsentPrompt.value)
    }

    @Test
    fun `marketing consent prompt stays hidden when consent is already granted`() = runTest(dispatcher) {
        coEvery {
            marketingConsentInteractor.refresh()
        } returns MarketingConsentResponseBody(consent = true, status = "subscribed")

        val viewModel = marketingViewModel()
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
            status = MarketingConsentStatus.NOT_FOUND.value,
        )

        val viewModel = marketingViewModel()

        awaitPrompt(viewModel)
    }

    @Test
    fun `marketing consent prompt stays hidden for ineligible subscription statuses`() = runTest(dispatcher) {
        val ineligibleStatuses = MarketingConsentStatus.entries - setOf(
            MarketingConsentStatus.UNSUBSCRIBED,
            MarketingConsentStatus.NOT_FOUND,
        )

        ineligibleStatuses.forEach { status ->
            coEvery {
                marketingConsentInteractor.refresh()
            } returns MarketingConsentResponseBody(consent = false, status = status.value)

            val viewModel = marketingViewModel()
            advanceUntilIdle()

            assertEquals(
                "Prompt should stay hidden for $status",
                MarketingConsentPrompt.Hidden,
                viewModel.marketingConsentPrompt.value,
            )
        }
    }

    @Test
    fun `marketing consent prompt stays hidden during the cooldown`() = runTest(dispatcher) {
        every {
            preferencesRepository.getMarketingConsentPromptLastShown()
        } returns System.currentTimeMillis()

        val viewModel = marketingViewModel()
        advanceUntilIdle()

        assertEquals(MarketingConsentPrompt.Hidden, viewModel.marketingConsentPrompt.value)
        coVerify(exactly = 0) { marketingConsentInteractor.update(any()) }
    }

    private suspend fun TestScope.awaitPrompt(viewModel: DashboardActivityViewModel) {
        advanceUntilIdle()
        assertEquals(MarketingConsentPrompt.Ask, viewModel.marketingConsentPrompt.value)
    }

    private fun marketingViewModel() = DashboardActivityViewModel(
        tagInteractor = mockk(relaxed = true),
        networkDataSyncInteractor = mockk(relaxed = true),
        networkInteractor = networkInteractor,
        preferencesRepository = preferencesRepository,
        permissionLogicInteractor = mockk(relaxed = true),
        networkApplicationSettings = mockk(relaxed = true),
        networkSignInInteractor = mockk(relaxed = true),
        nfcResultInteractor = mockk(relaxed = true),
        sortingInteractor = mockk(relaxed = true),
        runtimeBehavior = mockk(relaxed = true),
        bluetoothInteractor = mockk(relaxed = true),
        marketingConsentInteractor = marketingConsentInteractor,
        imageInteractor = mockk(relaxed = true),
    )

    private fun sensor(id: String = "sensor", updatedAt: Long = UPDATED_AT) =
        ruuviTagPreview.copy(
            id = id,
            networkSensor = false,
            networkLastSync = null,
            latestMeasurement = sensorMeasurementsPreview.copy(updatedAt = Date(updatedAt))
        )

    companion object {
        private const val UPDATED_AT = 1_000_000L
    }
}
