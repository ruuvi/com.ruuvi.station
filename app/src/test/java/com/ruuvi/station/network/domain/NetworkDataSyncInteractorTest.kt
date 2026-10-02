package com.ruuvi.station.network.domain

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.bluetooth.BluetoothLibrary
import com.ruuvi.station.bluetooth.contract.FoundRuuviTag
import com.ruuvi.station.calibration.domain.CalibrationInteractor
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.firebase.domain.FirebaseInteractor
import com.ruuvi.station.firebase.domain.PushRegisterInteractor
import com.ruuvi.station.image.ImageInteractor
import com.ruuvi.station.network.data.NetworkSyncEvent
import com.ruuvi.station.network.data.request.GetSensorDataRequest
import com.ruuvi.station.network.data.request.SensorDataMode
import com.ruuvi.station.network.data.request.SensorDenseRequest
import com.ruuvi.station.network.data.request.SortMode
import com.ruuvi.station.network.data.response.GetSensorDataResponse
import com.ruuvi.station.network.data.response.GetSensorDataResponseBody
import com.ruuvi.station.network.data.response.RuuviNetworkResponse
import com.ruuvi.station.network.data.response.SensorDataMeasurementResponse
import com.ruuvi.station.network.data.response.SensorSettings_defaultDisplayOrder
import com.ruuvi.station.network.data.response.SensorSettings_description
import com.ruuvi.station.network.data.response.SensorSettings_displayOrder
import com.ruuvi.station.network.data.response.SensorsDenseInfo
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import java.io.IOException
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds
import com.ruuvi.station.network.data.response.SensorSettings as CloudSensorSettings
import org.mockito.kotlin.any as anyMockito
import org.mockito.kotlin.verify as verifyMockito

class NetworkDataSyncInteractorTest {
    private val preferencesRepository = mockk<PreferencesRepository>(relaxed = true)
    // Avoid creating relaxed DBFlow model mocks for absent tags.
    private val tagRepository = mock<TagRepository>()
    private val networkInteractor = mockk<RuuviNetworkInteractor>(relaxed = true)
    private val imageInteractor = mockk<ImageInteractor>(relaxed = true)
    private val sensorSettingsRepository = mockk<SensorSettingsRepository>(relaxed = true)
    private val sensorHistoryRepository = mockk<SensorHistoryRepository>(relaxed = true)
    private val requestExecutor = mockk<NetworkRequestExecutor>(relaxed = true)
    private val applicationSettings = mockk<NetworkApplicationSettings>(relaxed = true)
    private val alertsSyncInteractor = mockk<NetworkAlertsSyncInteractor>(relaxed = true)
    private val firebaseInteractor = mockk<FirebaseInteractor>(relaxed = true)
    private val shareListInteractor = mockk<NetworkShareListInteractor>(relaxed = true)
    private val subscriptionSyncInteractor = mockk<SubscriptionInfoSyncInteractor>(relaxed = true)
    private val interactor = NetworkDataSyncInteractor(
        preferencesRepository = preferencesRepository,
        tagRepository = tagRepository,
        networkInteractor = networkInteractor,
        imageInteractor = imageInteractor,
        sensorSettingsRepository = sensorSettingsRepository,
        sensorHistoryRepository = sensorHistoryRepository,
        networkRequestExecutor = requestExecutor,
        networkApplicationSettings = applicationSettings,
        networkAlertsSyncInteractor = alertsSyncInteractor,
        calibrationInteractor = mockk<CalibrationInteractor>(relaxed = true),
        firebaseInteractor = firebaseInteractor,
        tagSettingsInteractor = mockk<TagSettingsInteractor>(relaxed = true),
        pushRegisterInteractor = mockk<PushRegisterInteractor>(relaxed = true),
        networkShareListInteractor = shareListInteractor,
        subscriptionInfoSyncInteractor = subscriptionSyncInteractor,
    )

    @Before
    fun setUp() {
        every { networkInteractor.signedIn } returns true
        every { networkInteractor.getEmail() } returns SYNC_USER_EMAIL
        every { sensorSettingsRepository.getSensorSettings() } returns emptyList()
        coEvery { networkInteractor.getSensorDenseLastData(any()) } returns denseResponse()
        coEvery { networkInteractor.getSensorData(any()) } returns null
        mockkObject(BluetoothLibrary)
        every { BluetoothLibrary.decode(any(), any(), any()) } answers {
            FoundRuuviTag(
                id = firstArg(),
                rssi = thirdArg(),
                temperature = 20.5,
                humidity = 45.0,
                pressure = 101_325.0,
                dataFormat = 5,
            )
        }
    }

    @After
    fun tearDown() {
        unmockkObject(BluetoothLibrary)
    }

    @Test
    fun `signed out sync does not start work or mark sync in progress`() = runBlocking {
        every { networkInteractor.signedIn } returns false

        val job = interactor.syncNetworkData()

        assertTrue(job.isCompleted)
        assertFalse(interactor.syncInProgressFlow.value)
        assertNull(interactor.lastResult)
        coVerify(exactly = 0) { requestExecutor.executeScheduledRequests() }
        coVerify(exactly = 0) { networkInteractor.getSensorDenseLastData(any()) }
    }

    @Test
    fun `missing or empty email prevents sync even with a signed in session`() = runBlocking {
        listOf(null, "").forEach { email ->
            every { networkInteractor.getEmail() } returns email

            assertTrue(interactor.syncNetworkData().isCompleted)
            assertFalse(interactor.syncInProgressFlow.value)
        }

        coVerify(exactly = 0) { requestExecutor.executeScheduledRequests() }
        coVerify(exactly = 0) { networkInteractor.getSensorDenseLastData(any()) }
    }

    @Test
    fun `successful sync executes the pipeline and records completion`() = runBlocking {
        val response = denseResponse()
        coEvery { networkInteractor.getSensorDenseLastData(any()) } returns response
        val before = System.currentTimeMillis()

        syncAndJoin()

        coVerifyOrder {
            requestExecutor.executeScheduledRequests()
            subscriptionSyncInteractor.syncSubscriptionInfo()
            requestExecutor.anySettingsRequests()
            applicationSettings.updateSettingsFromNetwork()
            networkInteractor.getSensorDenseLastData(
                SensorDenseRequest(
                    sensor = null,
                    measurements = true,
                    alerts = true,
                    settings = true,
                    sharedToOthers = true,
                    sharedToMe = true,
                ),
            )
            firebaseInteractor.logSync(SYNC_USER_EMAIL, requireNotNull(response.data))
            alertsSyncInteractor.updateAlertsFromNetwork(response)
            shareListInteractor.updateSharingInfo(response)
            requestExecutor.executeScheduledRequests()
        }
        coVerify(exactly = 2) { requestExecutor.executeScheduledRequests() }
        val timestamp = slot<Long>()
        verify(exactly = 1) { preferencesRepository.setLastSyncDate(capture(timestamp)) }
        assertTrue(timestamp.captured in before..System.currentTimeMillis())
        assertSame(NetworkSyncEvent.Success, interactor.lastResult)
        assertFalse(interactor.syncInProgressFlow.value)
    }

    @Test
    fun `pending settings uploads prevent cloud settings from overwriting local changes`() = runBlocking {
        every { requestExecutor.anySettingsRequests() } returns true

        syncAndJoin()

        coVerify(exactly = 0) { applicationSettings.updateSettingsFromNetwork() }
        coVerify(exactly = 1) { networkInteractor.getSensorDenseLastData(any()) }
        assertSame(NetworkSyncEvent.Success, interactor.lastResult)
    }

    @Test
    fun `concurrent sync call reuses the running job`() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        coEvery { networkInteractor.getSensorDenseLastData(any()) } coAnswers {
            started.complete(Unit)
            release.await()
            denseResponse()
        }
        val job = interactor.syncNetworkData()
        try {
            withTimeout(TIMEOUT.milliseconds) { started.await() }

            assertTrue(interactor.syncInProgressFlow.value)
            assertSame(job, interactor.syncNetworkData())
            coVerify(exactly = 1) { networkInteractor.getSensorDenseLastData(any()) }

            release.complete(Unit)
            withTimeout(TIMEOUT.milliseconds) { job.join() }
            assertFalse(interactor.syncInProgressFlow.value)
        } finally {
            job.cancelAndJoin()
        }
    }

    @Test
    fun `null sensor response fails without deleting local sensors or advancing sync date`() = runBlocking {
        coEvery { networkInteractor.getSensorDenseLastData(any()) } returns null

        syncAndJoin(expectSuccess = false)

        assertFailedSync("Unknown error")
        verifyMockito(tagRepository, never()).deleteSensorAndRelatives(anyMockito())
        verify(exactly = 0) { alertsSyncInteractor.updateAlertsFromNetwork(any()) }
        verify(exactly = 0) { shareListInteractor.updateSharingInfo(any()) }
    }

    @Test
    fun `error response is rejected even if it contains sensor data`() = runBlocking {
        coEvery { networkInteractor.getSensorDenseLastData(any()) } returns denseResponse(denseSensor()).copy(
            result = RuuviNetworkResponse.errorResult,
            error = "Server unavailable",
        )

        syncAndJoin(expectSuccess = false)

        assertFailedSync("Server unavailable")
        verify(exactly = 0) { sensorSettingsRepository.getSensorSettingsOrCreate(any()) }
        verifyMockito(tagRepository, never()).deleteSensorAndRelatives(anyMockito())
    }

    @Test
    fun `successful response without data is treated as a failed sync`() = runBlocking {
        coEvery { networkInteractor.getSensorDenseLastData(any()) } returns denseResponse().copy(
            data = null,
            error = "Missing sensor data",
        )

        syncAndJoin(expectSuccess = false)

        assertFailedSync("Missing sensor data")
        verify(exactly = 0) { sensorSettingsRepository.getSensorSettings() }
    }

    @Test
    fun `unauthorized response emits a sign in event and clears progress`() = runBlocking {
        coEvery { networkInteractor.getSensorDenseLastData(any()) } returns denseResponse().copy(
            result = RuuviNetworkResponse.errorResult,
            error = "Session expired",
            code = NetworkResponseLocalizer.ER_UNAUTHORIZED,
        )
        val event = async(context = Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) {
            withTimeout(TIMEOUT.milliseconds) { interactor.syncEvents.first { it is NetworkSyncEvent.Unauthorised } }
        }

        syncAndJoin(expectSuccess = false)

        assertSame(NetworkSyncEvent.Unauthorised, event.await())
        assertFailedSync("Session expired")
    }

    @Test
    fun `exception during queued requests clears progress and preserves the sync date`() = runBlocking {
        coEvery { requestExecutor.executeScheduledRequests() } throws IOException("Offline")

        syncAndJoin(expectSuccess = false)

        assertFailedSync("Offline")
        coVerify(exactly = 0) { networkInteractor.getSensorDenseLastData(any()) }
    }

    @Test
    fun `sensor request exception clears progress without applying partial sync data`() = runBlocking {
        coEvery { networkInteractor.getSensorDenseLastData(any()) } throws IOException("Connection lost")

        syncAndJoin(expectSuccess = false)

        assertFailedSync("Connection lost")
        verify(exactly = 0) { sensorSettingsRepository.getSensorSettingsOrCreate(any()) }
        verify(exactly = 0) { alertsSyncInteractor.updateAlertsFromNetwork(any()) }
        verify(exactly = 0) { shareListInteractor.updateSharingInfo(any()) }
        coVerify(exactly = 1) { requestExecutor.executeScheduledRequests() }
    }

    @Test
    fun `a failed sync can be retried successfully`() = runBlocking {
        coEvery { networkInteractor.getSensorDenseLastData(any()) } returnsMany listOf(null, denseResponse())

        syncAndJoin(expectSuccess = false)
        assertFailedSync("Unknown error")
        syncAndJoin()

        assertSame(NetworkSyncEvent.Success, interactor.lastResult)
        assertFalse(interactor.syncInProgressFlow.value)
        coVerify(exactly = 2) { networkInteractor.getSensorDenseLastData(any()) }
        verify(exactly = 1) { preferencesRepository.setLastSyncDate(any()) }
    }

    @Test
    fun `stopping sync cancels in flight work and queued requests`() = runBlocking {
        val started = CompletableDeferred<Unit>()
        coEvery { networkInteractor.getSensorDenseLastData(any()) } coAnswers {
            started.complete(Unit)
            awaitCancellation()
        }
        val job = interactor.syncNetworkData()
        try {
            withTimeout(TIMEOUT.milliseconds) { started.await() }

            val stopJob = interactor.stopSync()
            try {
                withTimeout(TIMEOUT.milliseconds) { stopJob.join() }
            } finally {
                stopJob.cancelAndJoin()
            }

            assertTrue(job.isCancelled)
            assertFalse(interactor.syncInProgressFlow.value)
            coVerify(exactly = 1) { requestExecutor.cancelAndJoinExecutingRequests() }
            verify(exactly = 0) { preferencesRepository.setLastSyncDate(any()) }
        } finally {
            job.cancelAndJoin()
        }
    }

    @Test
    fun `only cloud sensors absent from the response are removed`() = runBlocking {
        val retained = stubSensor(denseSensor())
        val removed = SensorSettings(id = "removed-cloud-sensor", networkSensor = true)
        val bluetoothOnly = SensorSettings(id = "bluetooth-only", networkSensor = false)
        every { sensorSettingsRepository.getSensorSettings() } returns listOf(retained, removed, bluetoothOnly)

        syncAndJoin()

        verifyMockito(tagRepository).deleteSensorAndRelatives(removed.id)
        verifyMockito(tagRepository).deleteSensorAndRelatives(anyMockito())
        assertSame(NetworkSyncEvent.Success, interactor.lastResult)
    }

    @Test
    fun `ownership is refreshed even when owned local metadata is newer`() = runBlocking {
        val local = localSettings().copy(
            owner = "previous@example.com",
            networkSensor = false,
            canShare = false,
            subscriptionName = "old",
            name = "New local name",
            lastUpdated = 300L,
        )
        val sensor = denseSensor().copy(owner = "OWNER@EXAMPLE.COM")
        val saved = stubSensor(sensor, local)

        syncAndJoin()

        assertEquals(SYNC_USER_EMAIL, saved.owner)
        assertTrue(saved.networkSensor)
        assertEquals(true, saved.canShare)
        assertEquals("free", saved.subscriptionName)
        assertEquals("New local name", saved.name)
        verify(exactly = 1) { networkInteractor.updateSensorToCloud(SYNC_SENSOR_ID) }
    }

    @Test
    fun `newer cloud metadata replaces local name and calibration without uploading`() = runBlocking {
        val sensor = denseSensor().copy(
            name = "Updated cloud name",
            lastUpdated = 200L,
            offsetTemperature = 1.25,
            offsetHumidity = -2.0,
            offsetPressure = 15.0,
        )
        val saved = stubSensor(sensor)

        syncAndJoin()

        assertEquals(sensor.name, saved.name)
        assertEquals(sensor.lastUpdated, saved.lastUpdated)
        assertEquals(sensor.offsetTemperature, requireNotNull(saved.temperatureOffset), 0.0)
        assertEquals(sensor.offsetHumidity, requireNotNull(saved.humidityOffset), 0.0)
        assertEquals(sensor.offsetPressure, requireNotNull(saved.pressureOffset), 0.0)
        verify(exactly = 0) { networkInteractor.updateSensorToCloud(any()) }
    }

    @Test
    fun `shared sensor does not upload newer local metadata as its owner`() = runBlocking {
        stubSensor(
            denseSensor().copy(owner = "someone-else@example.com"),
            localSettings().copy(lastUpdated = 300L),
        )

        syncAndJoin()

        verify(exactly = 0) { networkInteractor.updateSensorToCloud(any()) }
        assertSame(NetworkSyncEvent.Success, interactor.lastResult)
    }

    @Test
    fun `newer local sensor settings are uploaded with their own timestamps`() = runBlocking {
        stubSensor(
            denseSensor().copy(settings = cloudSensorSettings(timestamp = 100L)),
            localSettings().copy(
                displayOrder = "temperature,humidity",
                displayOrderTimestamp = 200L,
                defaultDisplayOrder = false,
                defaultDisplayOrderTimestamp = 300L,
                description = null,
                descriptionTimestamp = 400L,
            ),
        )

        syncAndJoin()

        verify { networkInteractor.updateSensorSetting(SYNC_SENSOR_ID, SensorSettings_displayOrder, "temperature,humidity", 200L) }
        verify { networkInteractor.updateSensorSetting(SYNC_SENSOR_ID, SensorSettings_defaultDisplayOrder, "false", 300L) }
        verify { networkInteractor.updateSensorSetting(SYNC_SENSOR_ID, SensorSettings_description, "", 400L) }
        verify(exactly = 0) { sensorSettingsRepository.newDisplayOrder(any(), any(), any()) }
        verify(exactly = 0) { sensorSettingsRepository.updateUseDefaultSensorOrder(any(), any(), any()) }
        verify(exactly = 0) { sensorSettingsRepository.newDescription(any(), any(), any()) }
    }

    @Test
    fun `newer cloud sensor settings are applied with their original timestamps`() = runBlocking {
        stubSensor(denseSensor().copy(settings = cloudSensorSettings(timestamp = 200L)))

        syncAndJoin()

        verify { sensorSettingsRepository.newDisplayOrder(SYNC_SENSOR_ID, "humidity,temperature", 200L) }
        verify { sensorSettingsRepository.updateUseDefaultSensorOrder(SYNC_SENSOR_ID, false, 200L) }
        verify { sensorSettingsRepository.newDescription(SYNC_SENSOR_ID, "Cloud description", 200L) }
        verify(exactly = 0) { networkInteractor.updateSensorSetting(any(), any(), any(), any()) }
    }

    @Test
    fun `only newest measurement activates the sensor and free plan history`() = runBlocking {
        stubSensor(denseSensor().copy(measurements = listOf(
            measurement(2L),
            measurement(3L, rssi = -40),
            measurement(1L),
        )))

        syncAndJoin()

        val reading = argumentCaptor<TagSensorReading>()
        verifyMockito(tagRepository).activateSensor(reading.capture())
        assertEquals(SYNC_SENSOR_ID, reading.firstValue.ruuviTagId)
        assertEquals(Date(3_000L), reading.firstValue.createdAt)
        assertEquals(-40, reading.firstValue.rssi)
        assertEquals(20.5, requireNotNull(reading.firstValue.temperature), 0.0)
        verify(exactly = 1) { sensorHistoryRepository.insertPoint(reading.firstValue) }
        verify(exactly = 1) { sensorSettingsRepository.updateNetworkLastSync(SYNC_SENSOR_ID, Date(3_000L)) }
        coVerify(exactly = 0) { networkInteractor.getSensorData(any()) }
    }

    @Test
    fun `already synced latest measurement is not inserted into history again`() = runBlocking {
        stubSensor(
            denseSensor().copy(measurements = listOf(measurement(3L))),
            localSettings().copy(networkLastSync = Date(3_000L)),
        )

        syncAndJoin()

        verifyMockito(tagRepository).activateSensor(anyMockito())
        verify(exactly = 0) { sensorHistoryRepository.insertPoint(any()) }
    }

    @Test
    fun `subscribed sensor fetches history instead of separately inserting the latest point`() = runBlocking {
        val sensor = denseSensor()
        stubSensor(sensor.copy(
            subscription = sensor.subscription.copy(maxHistoryDays = 7),
            measurements = listOf(measurement(3L)),
        ))

        syncAndJoin()

        verifyMockito(tagRepository).activateSensor(anyMockito())
        verify(exactly = 0) { sensorHistoryRepository.insertPoint(any()) }
        coVerify(exactly = 1) { networkInteractor.getSensorData(match { it.sensor == SYNC_SENSOR_ID }) }
    }

    @Test
    fun `history download exception fails the overall sync without advancing checkpoints`() = runBlocking {
        val sensor = denseSensor()
        stubSensor(sensor.copy(subscription = sensor.subscription.copy(maxHistoryDays = 7)))
        coEvery { networkInteractor.getSensorData(any()) } throws IOException("History unavailable")

        syncAndJoin(expectSuccess = false)

        assertFailedSync("History unavailable")
        verifyNoHistoryWrites()
        verify(exactly = 0) { alertsSyncInteractor.updateAlertsFromNetwork(any()) }
        verify(exactly = 0) { shareListInteractor.updateSharingInfo(any()) }
        coVerify(exactly = 1) { requestExecutor.executeScheduledRequests() }
    }

    @Test
    fun `queued image upload prevents cloud image download`() = runBlocking {
        stubSensor(denseSensor().copy(picture = "https://example.com/cloud-image.jpg"))
        every { requestExecutor.gotAnyImagesInSync(SYNC_SENSOR_ID) } returns true

        syncAndJoin()

        coVerify(exactly = 0) { imageInteractor.downloadImage(any(), any()) }
        verify(exactly = 0) { sensorSettingsRepository.setDefaultSensorBackground(any(), any()) }
        assertSame(NetworkSyncEvent.Success, interactor.lastResult)
    }

    @Test
    fun `empty cloud picture preserves a local user background`() = runBlocking {
        stubSensor(
            denseSensor(),
            localSettings().copy(userBackground = "file:///local-photo.jpg", imageUrl = "https://example.com/old.jpg"),
        )

        syncAndJoin()

        verify {
            sensorSettingsRepository.updateSensorBackground(
                sensorId = SYNC_SENSOR_ID,
                userBackground = "file:///local-photo.jpg",
                defaultBackground = 0,
                networkBackground = null,
                timestamp = 100L,
                imageUrl = "",
            )
        }
        verify(exactly = 0) { sensorSettingsRepository.setDefaultSensorBackground(any(), any()) }
    }

    @Test
    fun `image download failure restores a default and does not abort sync`() = runBlocking {
        stubSensor(denseSensor().copy(picture = "https://example.com/cloud-image.jpg"))
        coEvery { imageInteractor.downloadImage(any(), any()) } throws IOException("Image unavailable")

        syncAndJoin()

        coVerify(exactly = 1) { imageInteractor.downloadImage(any(), "https://example.com/cloud-image.jpg") }
        verify(exactly = 1) { sensorSettingsRepository.setDefaultSensorBackground(SYNC_SENSOR_ID, 0) }
        assertSame(NetworkSyncEvent.Success, interactor.lastResult)
    }

    @Test
    fun `history is not requested without local sensor settings`() = runBlocking {
        every { sensorSettingsRepository.getSensorSettings(SYNC_SENSOR_ID) } returns null

        interactor.syncSensorDataForPeriod(SYNC_SENSOR_ID, 24)

        coVerify(exactly = 0) { networkInteractor.getSensorData(any()) }
        verifyNoHistoryWrites()
    }

    @Test
    fun `recent history sync skips downloading the last minute again`() = runBlocking {
        every { sensorSettingsRepository.getSensorSettings(SYNC_SENSOR_ID) } returns localSettings().copy(
            networkHistoryLastSync = Date(),
        )

        interactor.syncSensorDataForPeriod(SYNC_SENSOR_ID, 24)

        coVerify(exactly = 0) { networkInteractor.getSensorData(any()) }
        verifyNoHistoryWrites()
    }

    @Test
    fun `history request resumes one second after a newer local watermark`() = runBlocking {
        val lastSync = Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2))
        every { sensorSettingsRepository.getSensorSettings(SYNC_SENSOR_ID) } returns localSettings().copy(
            networkHistoryLastSync = lastSync,
        )

        interactor.syncSensorDataForPeriod(SYNC_SENSOR_ID, 24)

        coVerify(exactly = 1) {
            networkInteractor.getSensorData(GetSensorDataRequest(
                sensor = SYNC_SENSOR_ID,
                since = Date(lastSync.time + 1_000L),
                sort = SortMode.ASCENDING,
                limit = 5_000,
                mode = SensorDataMode.MIXED,
            ))
        }
        verifyNoHistoryWrites()
    }

    @Test
    fun `old watermark does not extend the requested history window`() = runBlocking {
        every { sensorSettingsRepository.getSensorSettings(SYNC_SENSOR_ID) } returns localSettings().copy(
            networkHistoryLastSync = Date(0L),
        )
        val before = System.currentTimeMillis()

        interactor.syncSensorDataForPeriod(SYNC_SENSOR_ID, 24)

        val request = slot<GetSensorDataRequest>()
        coVerify(exactly = 1) { networkInteractor.getSensorData(capture(request)) }
        val day = TimeUnit.HOURS.toMillis(24)
        assertTrue(requireNotNull(request.captured.since).time in (before - day)..(System.currentTimeMillis() - day))
        assertEquals(SortMode.ASCENDING, request.captured.sort)
        assertEquals(SensorDataMode.MIXED, request.captured.mode)
        assertEquals(5_000, request.captured.limit)
        assertNull(request.captured.until)
    }

    @Test
    fun `history pages advance from the maximum timestamp and persist a single batch`() = runBlocking {
        every { sensorSettingsRepository.getSensorSettings(SYNC_SENSOR_ID) } returns localSettings()
        coEvery { networkInteractor.getSensorData(any()) } returnsMany listOf(
            historyResponse(measurement(30L), measurement(10L)),
            historyResponse(measurement(40L)),
            historyResponse(),
        )

        interactor.syncSensorDataForPeriod(SYNC_SENSOR_ID, 24)

        val requests = mutableListOf<GetSensorDataRequest>()
        coVerify(exactly = 2) { networkInteractor.getSensorData(capture(requests)) }
        assertEquals(Date(31_000L), requests[1].since)
        val readings = slot<List<TagSensorReading>>()
        verify(exactly = 1) { sensorHistoryRepository.bulkInsert(SYNC_SENSOR_ID, capture(readings)) }
        assertEquals(listOf(30_000L, 10_000L, 40_000L), readings.captured.map { it.createdAt.time })
        assertTrue(readings.captured.all { it.ruuviTagId == SYNC_SENSOR_ID && it.rssi == -60 })
        verify(exactly = 1) { sensorSettingsRepository.updateNetworkHistoryLastSync(SYNC_SENSOR_ID, Date(40_000L)) }
    }

    @Test
    fun `history pagination stops once the newest point is within the last minute`() = runBlocking {
        every { sensorSettingsRepository.getSensorSettings(SYNC_SENSOR_ID) } returns localSettings()
        val now = System.currentTimeMillis() / 1000
        coEvery { networkInteractor.getSensorData(any()) } returnsMany listOf(
            historyResponse(measurement(now - 10), measurement(now - 5)),
            historyResponse(),
        )

        interactor.syncSensorDataForPeriod(SYNC_SENSOR_ID, 24)

        coVerify(exactly = 1) { networkInteractor.getSensorData(any()) }
        verify(exactly = 1) { sensorHistoryRepository.bulkInsert(SYNC_SENSOR_ID, match { it.size == 2 }) }
        verify { sensorSettingsRepository.updateNetworkHistoryLastSync(SYNC_SENSOR_ID, Date((now - 5) * 1000)) }
    }

    @Test
    fun `empty history page does not advance the watermark`() = runBlocking {
        every { sensorSettingsRepository.getSensorSettings(SYNC_SENSOR_ID) } returns localSettings()
        coEvery { networkInteractor.getSensorData(any()) } returns historyResponse()

        interactor.syncSensorDataForPeriod(SYNC_SENSOR_ID, 24)

        coVerify(exactly = 1) { networkInteractor.getSensorData(any()) }
        verifyNoHistoryWrites()
    }

    @Test
    fun `malformed and legacy air measurements are excluded from history and its watermark`() = runBlocking {
        every { sensorSettingsRepository.getSensorSettings(SYNC_SENSOR_ID) } returns localSettings()
        every { BluetoothLibrary.decode(SYNC_SENSOR_ID, "invalid", any()) } throws IllegalArgumentException("Invalid data")
        every { BluetoothLibrary.decode(SYNC_SENSOR_ID, "legacy-6", any()) } returns FoundRuuviTag(dataFormat = 0x06)
        every { BluetoothLibrary.decode(SYNC_SENSOR_ID, "legacy-f0", any()) } returns FoundRuuviTag(dataFormat = 0xF0)
        coEvery { networkInteractor.getSensorData(any()) } returnsMany listOf(
            historyResponse(
                measurement(10L),
                measurement(20L, data = ""),
                measurement(30L, data = "invalid"),
                measurement(40L, data = "legacy-6"),
                measurement(50L, data = "legacy-f0"),
            ),
            historyResponse(),
        )

        interactor.syncSensorDataForPeriod(SYNC_SENSOR_ID, 24)

        val readings = slot<List<TagSensorReading>>()
        verify(exactly = 1) { sensorHistoryRepository.bulkInsert(SYNC_SENSOR_ID, capture(readings)) }
        assertEquals(listOf(Date(10_000L)), readings.captured.map { it.createdAt })
        verify(exactly = 1) { sensorSettingsRepository.updateNetworkHistoryLastSync(SYNC_SENSOR_ID, Date(10_000L)) }
        verify(exactly = 0) { BluetoothLibrary.decode(any(), "", any()) }
    }

    @Test
    fun `undecodable history does not create an empty batch or update its watermark`() = runBlocking {
        every { sensorSettingsRepository.getSensorSettings(SYNC_SENSOR_ID) } returns localSettings()
        every { BluetoothLibrary.decode(SYNC_SENSOR_ID, "invalid", any()) } throws IllegalArgumentException("Invalid data")
        coEvery { networkInteractor.getSensorData(any()) } returnsMany listOf(
            historyResponse(measurement(10L, data = "invalid")),
            historyResponse(),
        )

        interactor.syncSensorDataForPeriod(SYNC_SENSOR_ID, 24)

        verifyNoHistoryWrites()
    }

    @Test
    fun `getSince preserves the requested sensor date and page size`() = runBlocking {
        val since = Date(123_000L)
        val response = historyResponse()
        coEvery { networkInteractor.getSensorData(any()) } returns response

        val result = interactor.getSince(SYNC_SENSOR_ID, since, 250)

        assertSame(response, result)
        coVerify(exactly = 1) {
            networkInteractor.getSensorData(GetSensorDataRequest(
                sensor = SYNC_SENSOR_ID,
                since = since,
                sort = SortMode.ASCENDING,
                limit = 250,
                mode = SensorDataMode.MIXED,
            ))
        }
    }

    private suspend fun syncAndJoin(expectSuccess: Boolean = true) {
        val job = interactor.syncNetworkData()
        try {
            withTimeout(TIMEOUT.milliseconds) { job.join() }
        } finally {
            job.cancelAndJoin()
        }
        if (expectSuccess) {
            assertSame(
                (interactor.lastResult as? NetworkSyncEvent.Error)?.message,
                NetworkSyncEvent.Success,
                interactor.lastResult,
            )
            assertFalse(interactor.syncInProgressFlow.value)
        }
    }

    private fun assertFailedSync(message: String) {
        assertEquals(message, (interactor.lastResult as? NetworkSyncEvent.Error)?.message)
        assertFalse(interactor.syncInProgressFlow.value)
        verify(exactly = 0) { preferencesRepository.setLastSyncDate(any()) }
    }

    private fun verifyNoHistoryWrites() {
        verify(exactly = 0) { sensorHistoryRepository.bulkInsert(any(), any()) }
        verify(exactly = 0) { sensorSettingsRepository.updateNetworkHistoryLastSync(any(), any()) }
    }

    private fun stubSensor(sensor: SensorsDenseInfo, local: SensorSettings = localSettings()): SensorSettings {
        val saved = spyk(local)
        every { saved.update() } returns true
        every { sensorSettingsRepository.getSensorSettingsOrCreate(sensor.sensor) } returns saved
        every { sensorSettingsRepository.getSensorSettings(sensor.sensor) } returns saved
        coEvery { networkInteractor.getSensorDenseLastData(any()) } returns denseResponse(sensor)
        return saved
    }

    private fun localSettings() = SensorSettings(
        id = SYNC_SENSOR_ID,
        owner = SYNC_USER_EMAIL,
        networkSensor = true,
        canShare = true,
        subscriptionName = "free",
        name = "Local sensor",
        lastUpdated = 100L,
    )

    private fun cloudSensorSettings(timestamp: Long) = CloudSensorSettings(
        displayOrder = "humidity,temperature",
        defaultDisplayOrder = "false",
        displayOrder_lastUpdated = timestamp,
        defaultDisplayOrder_lastUpdated = timestamp,
        description = "Cloud description",
        description_lastUpdated = timestamp,
    )

    private fun measurement(timestamp: Long, data: String = "valid", rssi: Int = -60) = SensorDataMeasurementResponse(
        coordinates = "",
        gwmac = "",
        data = data,
        timestamp = timestamp,
        rssi = rssi,
    )

    private fun historyResponse(vararg measurements: SensorDataMeasurementResponse): GetSensorDataResponse =
        RuuviNetworkResponse(
            result = RuuviNetworkResponse.successResult,
            error = "",
            data = GetSensorDataResponseBody(
                sensor = SYNC_SENSOR_ID,
                offsetTemperature = 0.0,
                offsetHumidity = 0.0,
                offsetPressure = 0.0,
                total = measurements.size,
                measurements = measurements.toList(),
            ),
            code = null,
        )

    companion object {
        private const val TIMEOUT = 5_000L
    }
}
