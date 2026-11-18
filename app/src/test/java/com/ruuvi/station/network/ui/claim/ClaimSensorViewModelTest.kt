package com.ruuvi.station.network.ui.claim

import com.ruuvi.station.app.ui.UiEvent
import com.ruuvi.station.network.data.response.CheckSensorResponse
import com.ruuvi.station.network.data.response.CheckSensorResponseBody
import com.ruuvi.station.network.data.response.RuuviNetworkResponse
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ClaimSensorViewModelTest {
    private val mainDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `claim state is checked once so rotating keeps the force claim instructions`() = runBlocking {
        val networkInteractor = signedInInteractor(ownerEmail = "other@example.com")
        val viewModel = viewModel(networkInteractor)
        val events = mutableListOf<UiEvent>()
        val collector = launch(mainDispatcher) { viewModel.uiEvent.collect { events.add(it) } }

        viewModel.checkClaimState()
        withTimeout(5.seconds) { while (events.isEmpty()) delay(10) }
        // The Activity recreated after a rotation calls it again from onCreate; it must not
        // navigate back to the first force claim page.
        viewModel.checkClaimState()
        delay(200)

        assertEquals(listOf(UiEvent.Navigate(ClaimRoutes.FORCE_CLAIM_INIT, true)), events.toList())
        coVerify(exactly = 1) { networkInteractor.getSensorOwner(SENSOR_ID, any()) }
        collector.cancel()
    }

    @Test
    fun `signed out state is also decided only once`() {
        val networkInteractor = mockk<RuuviNetworkInteractor> { every { signedIn } returns false }
        val viewModel = viewModel(networkInteractor)

        viewModel.checkClaimState()
        viewModel.checkClaimState()

        verify(exactly = 1) { networkInteractor.signedIn }
        assertEquals(UiEvent.Navigate(ClaimRoutes.NOT_SIGNED_IN, true), viewModel.uiEvent.replayCache.single())
    }

    private fun signedInInteractor(ownerEmail: String) = mockk<RuuviNetworkInteractor> {
        every { signedIn } returns true
        every { getEmail() } returns "current@example.com"
        coEvery { getSensorOwner(SENSOR_ID, any()) } answers {
            secondArg<(CheckSensorResponse?) -> Unit>()(
                RuuviNetworkResponse(RuuviNetworkResponse.successResult, "", CheckSensorResponseBody(ownerEmail), null)
            )
        }
    }

    private fun viewModel(networkInteractor: RuuviNetworkInteractor) = ClaimSensorViewModel(
        sensorId = SENSOR_ID,
        ruuviNetworkInteractor = networkInteractor,
        // Mockito: MockK cannot record stubs that return the DBFlow RuuviTagEntity model.
        interactor = mock<TagSettingsInteractor>(),
        sensorInfoInteractor = mockk(),
        sensorClaimInteractor = mockk(),
    )

    private companion object {
        const val SENSOR_ID = "AA:BB:CC:DD:EE:FF"
    }
}
