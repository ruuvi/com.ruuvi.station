package com.ruuvi.station.network.ui.claim

import com.ruuvi.station.R
import com.ruuvi.station.app.ui.UiEvent
import com.ruuvi.station.app.ui.UiText
import com.ruuvi.station.bluetooth.domain.SensorInfoInteractor
import com.ruuvi.station.bluetooth.model.SensorFirmwareResult
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.network.data.response.CheckSensorResponse
import com.ruuvi.station.network.data.response.CheckSensorResponseBody
import com.ruuvi.station.network.data.response.ClaimSensorResponse
import com.ruuvi.station.network.data.response.ClaimSensorResponseBody
import com.ruuvi.station.network.data.response.ContestSensorResponse
import com.ruuvi.station.network.data.response.ContestSensorResponseBody
import com.ruuvi.station.network.data.response.RuuviNetworkResponse
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.network.domain.SensorClaimInteractor
import com.ruuvi.station.nfc.NfcScanReciever
import com.ruuvi.gateway.tester.nfc.model.SensorNfсScanInfo
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
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
        viewModel.checkClaimState()
        delay(200)

        assertEquals(listOf(UiEvent.Navigate(ClaimRoutes.FORCE_CLAIM_INIT, true)), events.toList())
        coVerify(exactly = 1) { networkInteractor.getSensorOwner(SENSOR_ID, any()) }
        collector.cancel()
    }

    @Test
    fun `checkClaimState navigates to FREE_TO_CLAIM when owner email is empty`() = runBlocking {
        val networkInteractor = signedInInteractor(ownerEmail = "")
        val viewModel = viewModel(networkInteractor)
        val events = mutableListOf<UiEvent>()
        val collector = launch(mainDispatcher) { viewModel.uiEvent.collect { events.add(it) } }

        viewModel.checkClaimState()
        withTimeout(5.seconds) { while (events.isEmpty()) delay(10) }

        assertEquals(listOf(UiEvent.Navigate(ClaimRoutes.FREE_TO_CLAIM, true)), events.toList())
        collector.cancel()
    }

    @Test
    fun `checkClaimState navigates to UNCLAIM when sensor is owned by current user`() = runBlocking {
        val networkInteractor = signedInInteractor(ownerEmail = "current@example.com")
        val viewModel = viewModel(networkInteractor)
        val events = mutableListOf<UiEvent>()
        val collector = launch(mainDispatcher) { viewModel.uiEvent.collect { events.add(it) } }

        viewModel.checkClaimState()
        withTimeout(5.seconds) { while (events.isEmpty()) delay(10) }

        assertEquals(listOf(UiEvent.Navigate(ClaimRoutes.UNCLAIM, true)), events.toList())
        collector.cancel()
    }

    @Test
    fun `checkClaimState navigates to FORCE_CLAIM_ERROR when getSensorOwner fails`() = runBlocking {
        val networkInteractor = mockk<RuuviNetworkInteractor> {
            every { signedIn } returns true
            coEvery { getSensorOwner(SENSOR_ID, any()) } answers {
                secondArg<(CheckSensorResponse?) -> Unit>()(
                    RuuviNetworkResponse(RuuviNetworkResponse.errorResult, "Network error", null, null)
                )
            }
        }
        val viewModel = viewModel(networkInteractor)
        val events = mutableListOf<UiEvent>()
        val collector = launch(mainDispatcher) { viewModel.uiEvent.collect { events.add(it) } }

        viewModel.checkClaimState()
        withTimeout(5.seconds) { while (events.isEmpty()) delay(10) }

        assertEquals(listOf(UiEvent.Navigate(ClaimRoutes.FORCE_CLAIM_ERROR, true)), events.toList())
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

    @Test
    fun `claimSensor navigates to CLAIM_IN_PROGRESS then NavigateUp on success`() = runBlocking {
        val sensorClaimInteractor = mockk<SensorClaimInteractor> {
            coEvery { claimSensor(SENSOR_ID, SENSOR_NAME, any()) } answers {
                thirdArg<(ClaimSensorResponse?) -> Unit>()(
                    RuuviNetworkResponse(RuuviNetworkResponse.successResult, "", ClaimSensorResponseBody(SENSOR_ID), null)
                )
            }
        }
        val viewModel = viewModel(sensorClaimInteractor = sensorClaimInteractor)
        val events = mutableListOf<UiEvent>()
        val collector = launch(mainDispatcher) { viewModel.uiEvent.collect { events.add(it) } }

        viewModel.claimSensor()
        withTimeout(5.seconds) { while (events.size < 2) delay(10) }

        assertEquals(
            listOf(
                UiEvent.Navigate(ClaimRoutes.CLAIM_IN_PROGRESS, true),
                UiEvent.NavigateUp
            ),
            events.toList()
        )
        collector.cancel()
    }

    @Test
    fun `claimSensor shows error snackbar and NavigateUp on API failure`() = runBlocking {
        val sensorClaimInteractor = mockk<SensorClaimInteractor> {
            coEvery { claimSensor(SENSOR_ID, SENSOR_NAME, any()) } answers {
                thirdArg<(ClaimSensorResponse?) -> Unit>()(
                    RuuviNetworkResponse(RuuviNetworkResponse.errorResult, "Claim error", null, null)
                )
            }
        }
        val viewModel = viewModel(sensorClaimInteractor = sensorClaimInteractor)
        val events = mutableListOf<UiEvent>()
        val collector = launch(mainDispatcher) { viewModel.uiEvent.collect { events.add(it) } }

        viewModel.claimSensor()
        withTimeout(5.seconds) { while (events.size < 3) delay(10) }

        assertEquals(
            listOf(
                UiEvent.Navigate(ClaimRoutes.CLAIM_IN_PROGRESS, true),
                UiEvent.ShowSnackbar(UiText.DynamicString("Claim error")),
                UiEvent.NavigateUp
            ),
            events.toList()
        )
        collector.cancel()
    }

    @Test
    fun `claimSensor shows error snackbar when exception occurs`() = runBlocking {
        val tagSettingsInteractor = mock<TagSettingsInteractor> {
            on { getSensorSettings(SENSOR_ID) } doThrow RuntimeException("Settings error")
        }
        val viewModel = viewModel(tagSettingsInteractor = tagSettingsInteractor)
        val events = mutableListOf<UiEvent>()
        val collector = launch(mainDispatcher) { viewModel.uiEvent.collect { events.add(it) } }

        viewModel.claimSensor()
        withTimeout(5.seconds) { while (events.size < 2) delay(10) }

        assertEquals(
            listOf(
                UiEvent.Navigate(ClaimRoutes.CLAIM_IN_PROGRESS, true),
                UiEvent.ShowSnackbar(UiText.DynamicString("Settings error"))
            ),
            events.toList()
        )
        collector.cancel()
    }

    @Test
    fun `unclaimSensor navigates to CLAIM_IN_PROGRESS then NavigateUp on success`() = runBlocking {
        val sensorClaimInteractor = mockk<SensorClaimInteractor> {
            coEvery { unclaimSensor(SENSOR_ID, true, any()) } answers {
                thirdArg<(ClaimSensorResponse?) -> Unit>()(
                    RuuviNetworkResponse(RuuviNetworkResponse.successResult, "", ClaimSensorResponseBody(SENSOR_ID), null)
                )
            }
        }
        val viewModel = viewModel(sensorClaimInteractor = sensorClaimInteractor)
        val events = mutableListOf<UiEvent>()
        val collector = launch(mainDispatcher) { viewModel.uiEvent.collect { events.add(it) } }

        viewModel.unclaimSensor(deleteData = true)
        withTimeout(5.seconds) { while (events.size < 2) delay(10) }

        assertEquals(
            listOf(
                UiEvent.Navigate(ClaimRoutes.CLAIM_IN_PROGRESS, true),
                UiEvent.NavigateUp
            ),
            events.toList()
        )
        coVerify { sensorClaimInteractor.unclaimSensor(SENSOR_ID, true, any()) }
        collector.cancel()
    }

    @Test
    fun `unclaimSensor shows error snackbar and NavigateUp on API failure`() = runBlocking {
        val sensorClaimInteractor = mockk<SensorClaimInteractor> {
            coEvery { unclaimSensor(SENSOR_ID, false, any()) } answers {
                thirdArg<(ClaimSensorResponse?) -> Unit>()(
                    RuuviNetworkResponse(RuuviNetworkResponse.errorResult, "Unclaim error", null, null)
                )
            }
        }
        val viewModel = viewModel(sensorClaimInteractor = sensorClaimInteractor)
        val events = mutableListOf<UiEvent>()
        val collector = launch(mainDispatcher) { viewModel.uiEvent.collect { events.add(it) } }

        viewModel.unclaimSensor(deleteData = false)
        withTimeout(5.seconds) { while (events.size < 3) delay(10) }

        assertEquals(
            listOf(
                UiEvent.Navigate(ClaimRoutes.CLAIM_IN_PROGRESS, true),
                UiEvent.ShowSnackbar(UiText.DynamicString("Unclaim error")),
                UiEvent.NavigateUp
            ),
            events.toList()
        )
        coVerify { sensorClaimInteractor.unclaimSensor(SENSOR_ID, false, any()) }
        collector.cancel()
    }

    @Test
    fun `unclaimSensor shows error snackbar when exception occurs`() = runBlocking {
        val tagSettingsInteractor = mock<TagSettingsInteractor> {
            on { getSensorSettings(SENSOR_ID) } doThrow RuntimeException("Settings error")
        }
        val viewModel = viewModel(tagSettingsInteractor = tagSettingsInteractor)
        val events = mutableListOf<UiEvent>()
        val collector = launch(mainDispatcher) { viewModel.uiEvent.collect { events.add(it) } }

        viewModel.unclaimSensor(deleteData = true)
        withTimeout(5.seconds) { while (events.size < 2) delay(10) }

        assertEquals(
            listOf(
                UiEvent.Navigate(ClaimRoutes.CLAIM_IN_PROGRESS, true),
                UiEvent.ShowSnackbar(UiText.DynamicString("Settings error"))
            ),
            events.toList()
        )
        collector.cancel()
    }

    @Test
    fun `contestSensor navigates to CLAIM_IN_PROGRESS then NavigateUp on success`() = runBlocking {
        val sensorClaimInteractor = mockk<SensorClaimInteractor> {
            coEvery { contestSensor(SENSOR_ID, SENSOR_NAME, SECRET, any()) } answers {
                arg<(ContestSensorResponse?) -> Unit>(3)(
                    RuuviNetworkResponse(RuuviNetworkResponse.successResult, "", ContestSensorResponseBody(SENSOR_ID), null)
                )
            }
        }
        val viewModel = viewModel(sensorClaimInteractor = sensorClaimInteractor)
        val events = mutableListOf<UiEvent>()
        val collector = launch(mainDispatcher) { viewModel.uiEvent.collect { events.add(it) } }

        viewModel.contestSensor(SECRET)
        withTimeout(5.seconds) { while (events.size < 2) delay(10) }

        assertEquals(
            listOf(
                UiEvent.Navigate(ClaimRoutes.CLAIM_IN_PROGRESS, true),
                UiEvent.NavigateUp
            ),
            events.toList()
        )
        collector.cancel()
    }

    @Test
    fun `contestSensor shows error snackbar and NavigateUp on API failure`() = runBlocking {
        val sensorClaimInteractor = mockk<SensorClaimInteractor> {
            coEvery { contestSensor(SENSOR_ID, SENSOR_NAME, SECRET, any()) } answers {
                arg<(ContestSensorResponse?) -> Unit>(3)(
                    RuuviNetworkResponse(RuuviNetworkResponse.errorResult, "Contest error", null, null)
                )
            }
        }
        val viewModel = viewModel(sensorClaimInteractor = sensorClaimInteractor)
        val events = mutableListOf<UiEvent>()
        val collector = launch(mainDispatcher) { viewModel.uiEvent.collect { events.add(it) } }

        viewModel.contestSensor(SECRET)
        withTimeout(5.seconds) { while (events.size < 3) delay(10) }

        assertEquals(
            listOf(
                UiEvent.Navigate(ClaimRoutes.CLAIM_IN_PROGRESS, true),
                UiEvent.ShowSnackbar(UiText.DynamicString("Contest error")),
                UiEvent.NavigateUp
            ),
            events.toList()
        )
        collector.cancel()
    }

    @Test
    fun `contestSensor shows error snackbar when exception occurs`() = runBlocking {
        val tagSettingsInteractor = mock<TagSettingsInteractor> {
            on { getSensorSettings(SENSOR_ID) } doThrow RuntimeException("Settings error")
        }
        val viewModel = viewModel(tagSettingsInteractor = tagSettingsInteractor)
        val events = mutableListOf<UiEvent>()
        val collector = launch(mainDispatcher) { viewModel.uiEvent.collect { events.add(it) } }

        viewModel.contestSensor(SECRET)
        withTimeout(5.seconds) { while (events.size < 2) delay(10) }

        assertEquals(
            listOf(
                UiEvent.Navigate(ClaimRoutes.CLAIM_IN_PROGRESS, true),
                UiEvent.ShowSnackbar(UiText.DynamicString("Settings error"))
            ),
            events.toList()
        )
        collector.cancel()
    }

    @Test
    fun `NFC scan with wrong MAC address shows wrong sensor scanned snackbar`() = runBlocking {
        val sensorInfoInteractor = mockk<SensorInfoInteractor> {
            coEvery { getSensorFirmwareVersion(SENSOR_ID) } coAnswers { awaitCancellation() }
        }
        val viewModel = viewModel(sensorInfoInteractor = sensorInfoInteractor)
        val events = mutableListOf<UiEvent>()
        val collector = launch(mainDispatcher) { viewModel.uiEvent.collect { events.add(it) } }

        viewModel.getSensorId()
        nfcScans().emit(SensorNfсScanInfo(id = SECRET, mac = "WRONG:MAC", sw = "3.31.1"))
        withTimeout(5.seconds) { while (events.size < 2) delay(10) }

        assertEquals(
            listOf(
                UiEvent.Navigate(ClaimRoutes.FORCE_CLAIM_GETTING_ID, true),
                UiEvent.ShowSnackbar(UiText.StringResource(R.string.claim_wrong_sensor_scanned))
            ),
            events.toList()
        )
        collector.cancel()
    }

    @Test
    fun `Bluetooth scanning retries on placeholder ID until valid ID is returned`() = runBlocking {
        var calls = 0
        val sensorInfoInteractor = mockk<SensorInfoInteractor> {
            coEvery { getSensorFirmwareVersion(SENSOR_ID) } answers {
                calls++
                if (calls == 1) {
                    SensorFirmwareResult(isSuccess = true, fw = "3.31.1", error = "", id = "XX:XX:XX:XX:XX:XX:XX:XX")
                } else {
                    SensorFirmwareResult(isSuccess = true, fw = "3.31.1", error = "", id = SECRET)
                }
            }
        }
        val sensorClaimInteractor = mockk<SensorClaimInteractor> {
            coEvery { contestSensor(SENSOR_ID, SENSOR_NAME, SECRET, any()) } returns Unit
        }
        val viewModel = viewModel(
            sensorInfoInteractor = sensorInfoInteractor,
            sensorClaimInteractor = sensorClaimInteractor
        )

        viewModel.getSensorId()
        mainDispatcher.scheduler.advanceTimeBy(3000)

        coVerify(timeout = 5_000, exactly = 1) {
            sensorClaimInteractor.contestSensor(SENSOR_ID, SENSOR_NAME, SECRET, any())
        }
    }

    @Test
    fun `tapping force claim repeatedly starts only one NFC and Bluetooth job`() = runBlocking {
        val sensorInfoInteractor = mockk<SensorInfoInteractor> {
            coEvery { getSensorFirmwareVersion(SENSOR_ID) } coAnswers { awaitCancellation() }
        }
        val sensorClaimInteractor = mockk<SensorClaimInteractor> {
            coEvery { contestSensor(any(), any(), any(), any()) } returns Unit
        }
        val viewModel = viewModel(
            sensorInfoInteractor = sensorInfoInteractor,
            sensorClaimInteractor = sensorClaimInteractor,
        )
        val events = mutableListOf<UiEvent>()
        val collector = launch(mainDispatcher) { viewModel.uiEvent.collect { events.add(it) } }

        repeat(3) { viewModel.getSensorId() }

        assertEquals(1, events.count { it == UiEvent.Navigate(ClaimRoutes.FORCE_CLAIM_GETTING_ID, true) })
        coVerify(exactly = 1) { sensorInfoInteractor.getSensorFirmwareVersion(SENSOR_ID) }

        nfcScans().emit(SCAN)
        coVerify(timeout = 5_000, exactly = 1) {
            sensorClaimInteractor.contestSensor(SENSOR_ID, SENSOR_NAME, SECRET, any())
        }
        delay(200)
        coVerify(exactly = 1) { sensorClaimInteractor.contestSensor(any(), any(), any(), any()) }
        collector.cancel()
    }

    @Test
    fun `touching the sensor again after the NFC claim started sends no second request`() = runBlocking {
        val sensorInfoInteractor = mockk<SensorInfoInteractor> {
            coEvery { getSensorFirmwareVersion(SENSOR_ID) } coAnswers { awaitCancellation() }
        }
        val sensorClaimInteractor = mockk<SensorClaimInteractor> {
            coEvery { contestSensor(any(), any(), any(), any()) } returns Unit
        }
        val viewModel = viewModel(
            sensorInfoInteractor = sensorInfoInteractor,
            sensorClaimInteractor = sensorClaimInteractor,
        )
        viewModel.getSensorId()

        nfcScans().emit(SCAN)
        coVerify(timeout = 5_000, exactly = 1) {
            sensorClaimInteractor.contestSensor(SENSOR_ID, SENSOR_NAME, SECRET, any())
        }

        nfcScans().emit(SCAN)
        nfcScans().emit(SCAN)
        viewModel.getSensorId()
        nfcScans().emit(SCAN)
        delay(200)

        coVerify(exactly = 1) { sensorClaimInteractor.contestSensor(any(), any(), any(), any()) }
    }

    @Test
    fun `Bluetooth result after the NFC claim started sends no second request`() = runBlocking {
        val bluetoothResult = CompletableDeferred<SensorFirmwareResult>()
        val sensorInfoInteractor = mockk<SensorInfoInteractor> {
            coEvery { getSensorFirmwareVersion(SENSOR_ID) } coAnswers { bluetoothResult.await() }
        }
        val sensorClaimInteractor = mockk<SensorClaimInteractor> {
            coEvery { contestSensor(any(), any(), any(), any()) } returns Unit
        }
        val viewModel = viewModel(
            sensorInfoInteractor = sensorInfoInteractor,
            sensorClaimInteractor = sensorClaimInteractor,
        )
        viewModel.getSensorId()

        nfcScans().emit(SCAN)
        bluetoothResult.complete(SensorFirmwareResult(isSuccess = true, fw = "3.31.1", error = "", id = SECRET))
        delay(200)

        coVerify(exactly = 1) { sensorClaimInteractor.contestSensor(any(), any(), any(), any()) }
    }

    @Test
    fun `isAir property is null when tag does not exist`() {
        val tagSettingsNull = mock<TagSettingsInteractor> {
            on { getTagById(SENSOR_ID) } doReturn null
        }
        val viewModelNull = viewModel(tagSettingsInteractor = tagSettingsNull)
        assertNull(viewModelNull.isAir)
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

    private fun viewModel(
        networkInteractor: RuuviNetworkInteractor = mockk(relaxed = true),
        sensorInfoInteractor: SensorInfoInteractor = mockk(),
        sensorClaimInteractor: SensorClaimInteractor = mockk(),
        tagSettingsInteractor: TagSettingsInteractor = mock {
            on { getSensorSettings(SENSOR_ID) } doReturn SensorSettings(id = SENSOR_ID, name = SENSOR_NAME)
        },
    ) = ClaimSensorViewModel(
        sensorId = SENSOR_ID,
        ruuviNetworkInteractor = networkInteractor,
        interactor = tagSettingsInteractor,
        sensorInfoInteractor = sensorInfoInteractor,
        sensorClaimInteractor = sensorClaimInteractor,
    )

    @Suppress("UNCHECKED_CAST")
    private fun nfcScans(): MutableSharedFlow<SensorNfсScanInfo?> =
        NfcScanReciever::class.java.getDeclaredField("_nfcSensorScanned")
            .apply { isAccessible = true }
            .get(null) as MutableSharedFlow<SensorNfсScanInfo?>

    private val SCAN = SensorNfсScanInfo(id = SECRET, mac = SENSOR_ID, sw = "3.31.1")

    private companion object {
        const val SENSOR_ID = "AA:BB:CC:DD:EE:FF"
        const val SENSOR_NAME = "Ruuvi EEFF"
        const val SECRET = "11:22:33:44:55:66:77:88"
    }
}
