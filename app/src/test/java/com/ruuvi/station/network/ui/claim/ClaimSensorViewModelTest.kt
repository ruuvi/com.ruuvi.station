package com.ruuvi.station.network.ui.claim

import androidx.lifecycle.viewModelScope
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.UiEvent
import com.ruuvi.station.app.ui.UiText
import com.ruuvi.station.bluetooth.domain.SensorInfoInteractor
import com.ruuvi.station.bluetooth.model.SensorFirmwareResult
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.network.data.response.ContestSensorResponse
import com.ruuvi.station.network.data.response.ContestSensorResponseBody
import com.ruuvi.station.network.data.response.RuuviNetworkResponse
import com.ruuvi.station.network.data.response.CheckSensorResponse
import com.ruuvi.station.network.data.response.CheckSensorResponseBody
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.network.domain.SensorClaimInteractor
import com.ruuvi.station.nfc.NfcScanReciever
import com.ruuvi.gateway.tester.nfc.model.SensorNfсScanInfo
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ClaimSensorViewModelTest {
    private val sensorInfoInteractor = mockk<SensorInfoInteractor>()
    private val sensorClaimInteractor = mockk<SensorClaimInteractor> {
        coEvery { contestSensor(any(), any(), any(), any()) } returns Unit
    }

    private val mainDispatcher = UnconfinedTestDispatcher()
    private val viewModels = mutableListOf<ClaimSensorViewModel>()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        viewModels.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    @Test
    fun `force claim listens for NFC but waits for Use BT before reading Bluetooth`() = runBlocking {
        val viewModel = forceClaimViewModel()

        viewModel.getSensorId()

        assertEquals(
            UiEvent.Navigate(ClaimRoutes.FORCE_CLAIM_GETTING_ID, true),
            withTimeout(5.seconds) { viewModel.uiEvent.first() },
        )
        coVerify(exactly = 0) { sensorInfoInteractor.getSensorFirmwareVersion(any()) }
        assertFalse(viewModel.bluetoothInProgress.value)
    }

    @Test
    fun `Use BT reads the sensor id and contests the sensor once`() = runBlocking {
        coEvery { sensorInfoInteractor.getSensorFirmwareVersion(SENSOR_ID) } returns
            SensorFirmwareResult(isSuccess = true, fw = "3.31.0", error = "", id = SECRET)
        val viewModel = forceClaimViewModel()
        viewModel.getSensorId()

        viewModel.useBluetooth()
        viewModel.useBluetooth()

        coVerify(timeout = 5_000, exactly = 1) {
            sensorClaimInteractor.contestSensor(SENSOR_ID, SENSOR_NAME, SECRET, any())
        }
        coVerify(exactly = 1) { sensorInfoInteractor.getSensorFirmwareVersion(SENSOR_ID) }
        assertFalse(viewModel.bluetoothInProgress.value)
    }

    @Test
    fun `Use BT keeps reading until the sensor exposes its id after the button press`() = runBlocking {
        coEvery { sensorInfoInteractor.getSensorFirmwareVersion(SENSOR_ID) } returnsMany listOf(
            // RuuviTag before the button press: no ID characteristic at all.
            SensorFirmwareResult(isSuccess = true, fw = "3.31.1", error = "", id = null),
            // Ruuvi Air before the button press: masked ID.
            SensorFirmwareResult(isSuccess = true, fw = "1.3.3", error = "", id = UNKNOWN_ID),
            SensorFirmwareResult(isSuccess = true, fw = "3.31.1", error = "", id = SECRET),
        )
        val viewModel = forceClaimViewModel()
        viewModel.getSensorId()

        viewModel.useBluetooth()
        mainDispatcher.scheduler.advanceTimeBy(2_001)

        coVerify(timeout = 5_000, exactly = 1) {
            sensorClaimInteractor.contestSensor(SENSOR_ID, SENSOR_NAME, SECRET, any())
        }
        coVerify(exactly = 3) { sensorInfoInteractor.getSensorFirmwareVersion(SENSOR_ID) }
    }

    @Test
    fun `Use BT shows sensor not found after 15 seconds without an id and can be tried again`() = runBlocking {
        coEvery { sensorInfoInteractor.getSensorFirmwareVersion(SENSOR_ID) } returns
            SensorFirmwareResult(isSuccess = true, fw = "3.31.1", error = "", id = null)
        val viewModel = forceClaimViewModel()
        val events = mutableListOf<UiEvent>()
        val collector = launch(mainDispatcher) { viewModel.uiEvent.collect { events.add(it) } }
        viewModel.getSensorId()
        val notFound = UiEvent.ShowSnackbar(UiText.StringResource(R.string.sensor_not_found_error))

        viewModel.useBluetooth()
        mainDispatcher.scheduler.advanceTimeBy(14_000)
        assertTrue(viewModel.bluetoothInProgress.value)
        assertFalse(notFound in events)

        mainDispatcher.scheduler.advanceTimeBy(1_001)
        assertFalse(viewModel.bluetoothInProgress.value)
        assertEquals(1, events.count { it == notFound })
        coVerify(exactly = 0) { sensorClaimInteractor.contestSensor(any(), any(), any(), any()) }

        coEvery { sensorInfoInteractor.getSensorFirmwareVersion(SENSOR_ID) } returns
            SensorFirmwareResult(isSuccess = true, fw = "3.31.1", error = "", id = SECRET)
        viewModel.useBluetooth()
        coVerify(timeout = 5_000, exactly = 1) {
            sensorClaimInteractor.contestSensor(SENSOR_ID, SENSOR_NAME, SECRET, any())
        }
        collector.cancel()
    }

    @Test
    fun `NFC scan of the sensor contests it and Use BT is ignored afterwards`() = runBlocking {
        val viewModel = forceClaimViewModel()
        viewModel.getSensorId()

        nfcScans().emit(SensorNfсScanInfo(id = SECRET, mac = SENSOR_ID, sw = "3.31.0"))
        viewModel.useBluetooth()

        coVerify(timeout = 5_000, exactly = 1) {
            sensorClaimInteractor.contestSensor(SENSOR_ID, SENSOR_NAME, SECRET, any())
        }
        coVerify(exactly = 0) { sensorInfoInteractor.getSensorFirmwareVersion(any()) }
    }

    @Test
    fun `failed force claim returns to the instructions with the error and can be retried`() = runTest(mainDispatcher) {
        coEvery { sensorClaimInteractor.contestSensor(any(), any(), any(), any()) } answers {
            arg<(ContestSensorResponse?) -> Unit>(3)(
                RuuviNetworkResponse(RuuviNetworkResponse.errorResult, CONTEST_ERROR, null, null)
            )
        } andThenAnswer {
            arg<(ContestSensorResponse?) -> Unit>(3)(
                RuuviNetworkResponse(RuuviNetworkResponse.successResult, "", ContestSensorResponseBody(SENSOR_ID), null)
            )
        }
        val viewModel = forceClaimViewModel()
        val events = Channel<UiEvent>(Channel.UNLIMITED)
        backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            viewModel.uiEvent.collect { events.send(it) }
        }
        val scans = nfcScans()
        val instructions = UiEvent.Navigate(ClaimRoutes.FORCE_CLAIM_GETTING_ID, true)
        val progress = UiEvent.Navigate(ClaimRoutes.CLAIM_IN_PROGRESS, true)
        val error = UiEvent.ShowSnackbar(UiText.DynamicString(CONTEST_ERROR))

        withTimeout(5.seconds) {
            viewModel.getSensorId()
            assertEquals(instructions, events.receive())
            scans.subscriptionCount.first { it > 0 }
            scans.emit(SCAN)

            assertEquals(progress, events.receive())
            assertEquals(instructions, events.receive())
            assertEquals(error, events.receive())

            scans.subscriptionCount.first { it > 0 }
            scans.emit(SCAN)

            assertEquals(progress, events.receive())
            assertEquals(UiEvent.NavigateUp, events.receive())
        }
        coVerify(exactly = 2) { sensorClaimInteractor.contestSensor(SENSOR_ID, SENSOR_NAME, SECRET, any()) }
    }

    @Test
    fun `Use NFC blocks Use BT until the wait times out with sensor not found`() = runBlocking {
        val viewModel = forceClaimViewModel()
        val events = mutableListOf<UiEvent>()
        val collector = launch(mainDispatcher) { viewModel.uiEvent.collect { events.add(it) } }
        viewModel.getSensorId()

        viewModel.useNfc()
        viewModel.useBluetooth()

        assertTrue(viewModel.nfcInProgress.value)
        assertFalse(viewModel.bluetoothInProgress.value)
        coVerify(exactly = 0) { sensorInfoInteractor.getSensorFirmwareVersion(any()) }

        mainDispatcher.scheduler.advanceTimeBy(60_001)

        assertFalse(viewModel.nfcInProgress.value)
        assertTrue(UiEvent.ShowSnackbar(UiText.StringResource(R.string.sensor_not_found_error)) in events)
        collector.cancel()
    }

    @Test
    fun `wrong sensor scanned while waiting for NFC enables both options again`() = runBlocking {
        val viewModel = forceClaimViewModel()
        viewModel.getSensorId()
        viewModel.useNfc()

        nfcScans().emit(SCAN.copy(mac = "11:11:11:11:11:11"))

        assertFalse(viewModel.nfcInProgress.value)
        coEvery { sensorInfoInteractor.getSensorFirmwareVersion(SENSOR_ID) } returns
            SensorFirmwareResult(isSuccess = true, fw = "3.31.0", error = "", id = SECRET)
        viewModel.useBluetooth()
        coVerify(timeout = 5_000, exactly = 1) {
            sensorClaimInteractor.contestSensor(SENSOR_ID, SENSOR_NAME, SECRET, any())
        }
    }

    @Test
    fun `Use BT in progress blocks Use NFC`() = runBlocking {
        val gate = CompletableDeferred<SensorFirmwareResult>()
        coEvery { sensorInfoInteractor.getSensorFirmwareVersion(SENSOR_ID) } coAnswers { gate.await() }
        val viewModel = forceClaimViewModel()
        viewModel.getSensorId()

        viewModel.useBluetooth()
        viewModel.useNfc()

        assertTrue(viewModel.bluetoothInProgress.value)
        assertFalse(viewModel.nfcInProgress.value)
        gate.complete(SensorFirmwareResult(isSuccess = false, fw = "", error = "timeout"))
        // Bluetooth keeps retrying until its 15 s timeout before both options are enabled again.
        mainDispatcher.scheduler.advanceTimeBy(15_001)
        assertFalse(viewModel.bluetoothInProgress.value)
        viewModel.useNfc()
        assertTrue(viewModel.nfcInProgress.value)
    }

    @Test
    fun `missing NFC scan data does not stop an active wait or claim a sensor`() = runBlocking {
        val viewModel = forceClaimViewModel()
        viewModel.getSensorId()
        viewModel.useNfc()

        nfcScans().emit(null)
        nfcScans().emit(SCAN.copy(id = ""))

        assertTrue(viewModel.nfcInProgress.value)
        coVerify(exactly = 0) { sensorClaimInteractor.contestSensor(any(), any(), any(), any()) }

        nfcScans().emit(SCAN)

        assertFalse(viewModel.nfcInProgress.value)
        mainDispatcher.scheduler.advanceTimeBy(60_001)
        assertEquals(
            UiEvent.Navigate(ClaimRoutes.CLAIM_IN_PROGRESS, true),
            viewModel.uiEvent.replayCache.single(),
        )
        coVerify(timeout = 5_000, exactly = 1) {
            sensorClaimInteractor.contestSensor(SENSOR_ID, SENSOR_NAME, SECRET, any())
        }
    }

    @Test
    fun `wrong NFC scan reports the wrong sensor and cancels the timeout`() = runBlocking {
        val viewModel = forceClaimViewModel()
        viewModel.getSensorId()
        viewModel.useNfc()

        nfcScans().emit(SCAN.copy(mac = "11:11:11:11:11:11"))
        mainDispatcher.scheduler.advanceTimeBy(60_001)

        assertFalse(viewModel.nfcInProgress.value)
        assertEquals(
            UiEvent.ShowSnackbar(UiText.StringResource(R.string.claim_wrong_sensor_scanned)),
            viewModel.uiEvent.replayCache.single(),
        )
        coVerify(exactly = 0) { sensorClaimInteractor.contestSensor(any(), any(), any(), any()) }
    }

    @Test
    fun `second NFC tap does not extend the original timeout`() {
        val viewModel = forceClaimViewModel()
        viewModel.getSensorId()
        viewModel.useNfc()
        mainDispatcher.scheduler.advanceTimeBy(30_000)

        viewModel.useNfc()
        mainDispatcher.scheduler.advanceTimeBy(30_001)

        assertFalse(viewModel.nfcInProgress.value)
        assertEquals(
            UiEvent.ShowSnackbar(UiText.StringResource(R.string.sensor_not_found_error)),
            viewModel.uiEvent.replayCache.single(),
        )
    }

    @Test
    fun `NFC scan cancels an in-flight Bluetooth read and uses the NFC secret`() = runBlocking {
        val read = CompletableDeferred<SensorFirmwareResult>()
        val cancelled = CompletableDeferred<Unit>()
        coEvery { sensorInfoInteractor.getSensorFirmwareVersion(SENSOR_ID) } coAnswers {
            try {
                read.await()
            } finally {
                cancelled.complete(Unit)
            }
        }
        val viewModel = forceClaimViewModel()
        viewModel.getSensorId()
        viewModel.useBluetooth()
        viewModel.useBluetooth()
        assertTrue(viewModel.bluetoothInProgress.value)

        nfcScans().emit(SCAN)

        withTimeout(5.seconds) { cancelled.await() }
        assertFalse(viewModel.bluetoothInProgress.value)
        coVerify(exactly = 1) { sensorInfoInteractor.getSensorFirmwareVersion(SENSOR_ID) }
        coVerify(timeout = 5_000, exactly = 1) {
            sensorClaimInteractor.contestSensor(SENSOR_ID, SENSOR_NAME, SECRET, any())
        }
    }

    @Test
    fun `clearing the view model stops scanning and cancels the NFC timeout`() = runBlocking {
        val viewModel = forceClaimViewModel()
        viewModel.getSensorId()
        viewModel.useNfc()

        viewModel.viewModelScope.cancel()
        nfcScans().emit(SCAN)
        mainDispatcher.scheduler.advanceTimeBy(60_001)

        coVerify(exactly = 0) { sensorClaimInteractor.contestSensor(any(), any(), any(), any()) }
        assertEquals(
            UiEvent.Navigate(ClaimRoutes.FORCE_CLAIM_GETTING_ID, true),
            viewModel.uiEvent.replayCache.single(),
        )
    }

    @Test
    fun `missing settings return to the instructions without calling the claim endpoint`() = runBlocking {
        val viewModel = forceClaimViewModel(settings = null)
        viewModel.getSensorId()

        nfcScans().emit(SCAN)

        val error = UiEvent.ShowSnackbar(UiText.DynamicString("Error"))
        withTimeout(5.seconds) { viewModel.uiEvent.first { it == error } }
        assertFalse(viewModel.bluetoothInProgress.value)
        assertFalse(viewModel.nfcInProgress.value)
        coVerify(exactly = 0) { sensorClaimInteractor.contestSensor(any(), any(), any(), any()) }
    }

    @Test
    fun `claim state is checked once so rotating keeps the force claim instructions`() = runBlocking {
        val networkInteractor = mockk<RuuviNetworkInteractor> {
            every { signedIn } returns true
            every { getEmail() } returns "current@example.com"
            coEvery { getSensorOwner(SENSOR_ID, any()) } answers {
                secondArg<(CheckSensorResponse?) -> Unit>()(
                    RuuviNetworkResponse(RuuviNetworkResponse.successResult, "", CheckSensorResponseBody("other@example.com"), null)
                )
            }
        }
        val viewModel = forceClaimViewModel(networkInteractor)
        val events = mutableListOf<UiEvent>()
        val collector = launch(mainDispatcher) { viewModel.uiEvent.collect { events.add(it) } }

        viewModel.checkClaimState()
        withTimeout(5.seconds) { while (events.isEmpty()) delay(10) }
        viewModel.getSensorId()
        // The Activity recreated after a rotation calls it again from onCreate.
        viewModel.checkClaimState()
        delay(200)

        assertEquals(
            listOf(
                UiEvent.Navigate(ClaimRoutes.FORCE_CLAIM_INIT, true),
                UiEvent.Navigate(ClaimRoutes.FORCE_CLAIM_GETTING_ID, true),
            ),
            events.toList(),
        )
        coVerify(exactly = 1) { networkInteractor.getSensorOwner(SENSOR_ID, any()) }
        collector.cancel()
    }

    private fun forceClaimViewModel(
        settings: SensorSettings? = SensorSettings(id = SENSOR_ID, name = SENSOR_NAME),
    ) = forceClaimViewModel(mockk<RuuviNetworkInteractor>(relaxed = true), settings)

    private fun forceClaimViewModel(
        networkInteractor: RuuviNetworkInteractor,
        settings: SensorSettings? = SensorSettings(id = SENSOR_ID, name = SENSOR_NAME),
    ) = ClaimSensorViewModel(
        sensorId = SENSOR_ID,
        ruuviNetworkInteractor = networkInteractor,
        // Mockito: MockK cannot record stubs that return the DBFlow SensorSettings model.
        interactor = mock<TagSettingsInteractor> {
            on { getSensorSettings(SENSOR_ID) } doReturn settings
        },
        sensorInfoInteractor = sensorInfoInteractor,
        sensorClaimInteractor = sensorClaimInteractor,
        contestDispatcher = mainDispatcher,
    ).also(viewModels::add)

    @Suppress("UNCHECKED_CAST")
    private fun nfcScans(): MutableSharedFlow<SensorNfсScanInfo?> =
        NfcScanReciever::class.java.getDeclaredField("_nfcSensorScanned")
            .apply { isAccessible = true }
            .get(null) as MutableSharedFlow<SensorNfсScanInfo?>

    private val SCAN = SensorNfсScanInfo(id = SECRET, mac = SENSOR_ID, sw = "3.31.0")

    private companion object {
        const val SENSOR_ID = "AA:BB:CC:DD:EE:FF"
        const val SENSOR_NAME = "Ruuvi EEFF"
        const val SECRET = "11:22:33:44:55:66:77:88"
        const val UNKNOWN_ID = "XX:XX:XX:XX:XX:XX:XX:XX"
        const val CONTEST_ERROR = "Wrong secret"
    }
}
