package com.ruuvi.station.network.ui

import com.ruuvi.station.R
import com.ruuvi.station.app.ui.UiEvent
import com.ruuvi.station.app.ui.UiText
import com.ruuvi.station.network.data.NetworkSyncEvent
import com.ruuvi.station.network.data.request.UserRegisterRequest
import com.ruuvi.station.network.domain.NetworkDataSyncInteractor
import com.ruuvi.station.network.domain.NetworkSignInInteractor
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val networkInteractor = mockk<RuuviNetworkInteractor>(relaxed = true)
    private val networkSignInInteractor = mockk<NetworkSignInInteractor>(relaxed = true)
    private val networkDataSyncInteractor = mockk<NetworkDataSyncInteractor>(relaxed = true)
    private lateinit var viewModel: SignInViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = SignInViewModel(networkInteractor, networkSignInInteractor, networkDataSyncInteractor)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `code is verified with the trimmed e-mail the code was requested for`() = runTest(dispatcher) {
        viewModel.submitEmail("  $EMAIL \n")

        assertEquals(EMAIL, viewModel.email.value)
        verify(exactly = 1) { networkInteractor.registerUser(UserRegisterRequest(EMAIL), any()) }

        viewModel.verifyCode(CODE)

        verify(exactly = 1) { networkSignInInteractor.signIn(CODE, EMAIL, any()) }
    }

    @Test
    fun `code is not verified without an e-mail and the user is sent back to the e-mail page`() = runTest(dispatcher) {
        val events = collectEvents()

        viewModel.verifyCode(CODE)

        verify(exactly = 0) { networkSignInInteractor.signIn(any(), any(), any()) }
        assertEquals(listOf<UiEvent>(UiEvent.NavigateUp), events)
    }

    @Test
    fun `code is not verified with a blank e-mail`() = runTest(dispatcher) {
        viewModel.submitEmail("   ")
        val events = collectEvents()

        viewModel.verifyCode(CODE)

        verify(exactly = 0) { networkSignInInteractor.signIn(any(), any(), any()) }
        assertEquals(listOf<UiEvent>(UiEvent.NavigateUp), events)
    }

    @Test
    fun `successful verification finishes after sensors sync and clears the code`() = runTest(dispatcher) {
        val syncEvents = MutableSharedFlow<NetworkSyncEvent>()
        every { networkDataSyncInteractor.syncEvents } returns syncEvents
        every { networkSignInInteractor.signIn(CODE, EMAIL, any()) } answers {
            thirdArg<(UiText?) -> Unit>()(null)
        }
        viewModel.submitEmail(EMAIL)
        val events = collectEvents()
        val tokenProcessed = mutableListOf<Boolean>()
        backgroundScope.launch(dispatcher) { viewModel.tokenProcessed.toList(tokenProcessed) }

        viewModel.verifyCode(CODE)

        verify(exactly = 1) { networkDataSyncInteractor.syncNetworkData() }
        assertEquals(listOf<UiEvent>(UiEvent.Progress(true, null)), events)
        assertTrue(tokenProcessed.isEmpty())

        syncEvents.emit(NetworkSyncEvent.SensorsSynced)

        assertEquals(
            listOf(UiEvent.Progress(true, null), UiEvent.Progress(false, null), UiEvent.Finish),
            events,
        )
        assertEquals(listOf(true), tokenProcessed)
        assertEquals(0, syncEvents.subscriptionCount.value)
    }

    @Test
    fun `failed verification shows the error and clears the code without navigating`() = runTest(dispatcher) {
        val error = UiText.StringResource(R.string.cloud_er_sub_code_expired)
        val onResult = slot<(UiText?) -> Unit>()
        every { networkSignInInteractor.signIn(any(), any(), capture(onResult)) } answers {
            onResult.captured(error)
        }
        viewModel.submitEmail(EMAIL)
        val events = collectEvents()
        val tokenProcessed = mutableListOf<Boolean>()
        backgroundScope.launch(dispatcher) { viewModel.tokenProcessed.toList(tokenProcessed) }

        viewModel.verifyCode(CODE)

        assertEquals(listOf(UiEvent.ShowToast(error)), events.filterIsInstance<UiEvent.ShowToast>())
        assertTrue(events.none { it is UiEvent.Navigate || it is UiEvent.NavigateUp || it is UiEvent.Finish })
        assertEquals(UiEvent.Progress(false, null), events.last { it is UiEvent.Progress })
        assertEquals(listOf(true), tokenProcessed)
        verify(exactly = 0) { networkDataSyncInteractor.syncNetworkData() }
    }

    private fun TestScope.collectEvents(): List<UiEvent> {
        val events = mutableListOf<UiEvent>()
        backgroundScope.launch(dispatcher) { viewModel.uiEvent.toList(events) }
        return events
    }

    private companion object {
        const val CODE = "AB12"
        const val EMAIL = "user@example.com"
    }
}
