package com.ruuvi.station.network.domain

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.ruuvi.station.app.ui.UiText
import com.ruuvi.station.database.domain.NetworkRequestRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.model.NetworkRequestStatus
import com.ruuvi.station.database.model.NetworkRequestType
import com.ruuvi.station.database.tables.NetworkRequest
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.network.data.NetworkTokenInfo
import com.ruuvi.station.network.data.request.SetAlertRequest
import com.ruuvi.station.network.data.request.UnclaimSensorRequest
import com.ruuvi.station.network.data.request.UnshareSensorRequest
import com.ruuvi.station.network.data.request.UpdateSensorRequest
import com.ruuvi.station.network.data.request.UpdateSensorSettingRequest
import com.ruuvi.station.network.data.request.UpdateUserSettingRequest
import com.ruuvi.station.network.data.request.UploadImageRequest
import com.ruuvi.station.network.data.requestWrappers.UploadImageRequestWrapper
import com.ruuvi.station.network.data.response.RuuviNetworkResponse
import com.ruuvi.station.network.data.response.UploadImageResponseBody
import com.ruuvi.station.network.domain.NetworkRequestExecutor.NetworkJobManager
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import io.mockk.verifySequence
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds

class NetworkRequestExecutorTest {
    private val tokenRepository = mockk<NetworkTokenRepository>()
    private val networkRepository = mockk<RuuviNetworkRepository>()
    private val requestRepository = mockk<NetworkRequestRepository>(relaxUnitFun = true)
    private val sensorSettingsRepository = mockk<SensorSettingsRepository>(relaxUnitFun = true)
    private val jobManager = NetworkJobManager()
    private val executor = NetworkRequestExecutor(
        tokenRepository,
        networkRepository,
        requestRepository,
        sensorSettingsRepository,
        jobManager,
    )

    @Before
    fun setUp() {
        every { tokenRepository.getTokenInfo() } returns NetworkTokenInfo("owner@example.com", TOKEN)
        every { requestRepository.startExecuting(any()) } returns true
        every { requestRepository.getScheduledRequests() } returns emptyList()
        every { requestRepository.getSimilar(any()) } returns emptyList()
    }

    @After
    fun cancelOutstandingJobs() = runBlocking {
        withTimeout(TIMEOUT) { executor.cancelAndJoinExecutingRequests() }
    }

    @Test
    fun `scheduled requests deserialize and route every request type in queue order`() = runBlocking {
        val unclaim = UnclaimSensorRequest(SENSOR_ID, deleteData = true)
        val update = UpdateSensorRequest(
            sensor = SENSOR_ID,
            name = "Kitchen",
            public = true,
            offsetTemperature = 1.25,
            offsetHumidity = -2.0,
            offsetPressure = 3.5,
            timestamp = 123L,
        )
        val upload = imageRequest()
        val settings = UpdateUserSettingRequest("temperatureUnit", "fahrenheit", timestamp = 124L)
        val unshare = UnshareSensorRequest("guest@example.com", SENSOR_ID)
        val reset = UploadImageRequest.getResetImageRequest(SENSOR_ID)
        val alert = SetAlertRequest(SENSOR_ID, "temperature", -5.0, 30.0, true, "Too warm", 125L)
        val sensorSettings = UpdateSensorSettingRequest(
            SENSOR_ID,
            listOf("description", "displayOrder"),
            listOf("Kitchen sensor", "temperature,humidity"),
            timestamp = 126L,
        )
        val requests = listOf(
            queuedRequest(1, NetworkRequestType.UNCLAIM, unclaim),
            queuedRequest(2, NetworkRequestType.UPDATE_SENSOR, update),
            queuedRequest(3, NetworkRequestType.UPLOAD_IMAGE, upload),
            queuedRequest(4, NetworkRequestType.SETTINGS, settings),
            queuedRequest(5, NetworkRequestType.UNSHARE, unshare),
            queuedRequest(6, NetworkRequestType.RESET_IMAGE, reset),
            queuedRequest(7, NetworkRequestType.SET_ALERT, alert),
            queuedRequest(8, NetworkRequestType.SENSOR_SETTINGS, sensorSettings),
        )
        every { requestRepository.getScheduledRequests() } returns requests
        every { sensorSettingsRepository.getSensorSettings(SENSOR_ID) } returns currentBackground()
        coEvery { networkRepository.unclaimSensor(unclaim, TOKEN) } returns response()
        coEvery { networkRepository.updateSensor(update, TOKEN) } returns response()
        coEvery { networkRepository.uploadImage(IMAGE_FILENAME, upload.request, TOKEN) } returns
            response(UploadImageResponseBody(uploadURL = "", guid = "uploaded-guid"))
        coEvery { networkRepository.updateUserSettings(settings, TOKEN) } returns response()
        coEvery { networkRepository.unshareSensor(unshare, TOKEN) } returns response()
        coEvery { networkRepository.resetImage(reset, TOKEN) } returns response()
        coEvery { networkRepository.setAlert(alert, TOKEN) } returns response()
        coEvery { networkRepository.updateSensorSettings(sensorSettings, TOKEN) } returns response()

        executeScheduled()

        coVerifySequence {
            networkRepository.unclaimSensor(unclaim, TOKEN)
            networkRepository.updateSensor(update, TOKEN)
            networkRepository.uploadImage(IMAGE_FILENAME, upload.request, TOKEN)
            networkRepository.updateUserSettings(settings, TOKEN)
            networkRepository.unshareSensor(unshare, TOKEN)
            networkRepository.resetImage(reset, TOKEN)
            networkRepository.setAlert(alert, TOKEN)
            networkRepository.updateSensorSettings(sensorSettings, TOKEN)
        }
        requests.forEach { request ->
            verifyOrder {
                requestRepository.startExecuting(request)
                requestRepository.disableRequest(request, NetworkRequestStatus.SUCCESS)
            }
            verify(exactly = 1) { requestRepository.startExecuting(request) }
            verifyTerminal(request, NetworkRequestStatus.SUCCESS)
            assertFalse(jobManager.isJobRunning(request.id))
        }
        verifySequence {
            sensorSettingsRepository.getSensorSettings(SENSOR_ID)
            sensorSettingsRepository.updateNetworkBackground(SENSOR_ID, "uploaded-guid", null)
        }
    }

    @Test
    fun `empty schedule does not acquire a token or start network work`() = runBlocking {
        executeScheduled()

        verify { tokenRepository wasNot Called }
        verify { networkRepository wasNot Called }
        verify { sensorSettingsRepository wasNot Called }
        verify(exactly = 0) { requestRepository.startExecuting(any()) }
    }

    @Test
    fun `request rejected by the repository is not executed or marked failed`() = runBlocking {
        val request = queuedRequest()
        every { requestRepository.getScheduledRequests() } returns listOf(request)
        every { requestRepository.startExecuting(request) } returns false

        executeScheduled()

        verify(exactly = 1) { requestRepository.startExecuting(request) }
        verify(exactly = 0) { requestRepository.disableRequest(any(), any()) }
        verify(exactly = 0) { requestRepository.registerFailedAttempt(any()) }
        verify(exactly = 1) { tokenRepository.getTokenInfo() }
        verify { networkRepository wasNot Called }
    }

    @Test
    fun `another scheduler pass does not duplicate an already running request`() = runBlocking {
        val request = queuedRequest()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        every { requestRepository.getScheduledRequests() } returns listOf(request)
        coEvery { networkRepository.unclaimSensor(any(), TOKEN) } coAnswers {
            started.complete(Unit)
            release.await()
            response()
        }

        withTimeout(TIMEOUT.milliseconds) {
            val execution = launch { executor.executeScheduledRequests() }
            try {
                started.await()
                assertTrue(jobManager.isJobRunning(request.id))

                executor.executeScheduledRequests()

                verify(exactly = 1) { requestRepository.startExecuting(request) }
                coVerify(exactly = 1) { networkRepository.unclaimSensor(any(), TOKEN) }
                verify(exactly = 0) { requestRepository.disableRequest(any(), any()) }
                verify(exactly = 0) { requestRepository.registerFailedAttempt(any()) }

                release.complete(Unit)
                execution.join()
                verifyTerminal(request, NetworkRequestStatus.SUCCESS)
                assertFalse(jobManager.isJobRunning(request.id))
            } finally {
                release.complete(Unit)
                execution.cancel()
            }
        }
    }

    @Test
    fun `concurrent scheduler passes claim and execute a request only once`() = runBlocking {
        val request = queuedRequest()
        val callersReady = CountDownLatch(2)
        val firstClaimEntered = CountDownLatch(1)
        val releaseFirstClaim = CountDownLatch(1)
        val firstClaim = AtomicBoolean(true)
        val requestIsReady = AtomicBoolean(true)
        every { requestRepository.getScheduledRequests() } returns listOf(request)
        every { requestRepository.startExecuting(request) } answers {
            val wasReady = requestIsReady.get()
            if (firstClaim.compareAndSet(true, false)) {
                firstClaimEntered.countDown()
                releaseFirstClaim.await()
            }
            requestIsReady.set(false)
            wasReady
        }
        coEvery { networkRepository.unclaimSensor(any(), TOKEN) } returns response()

        try {
            withTimeout(TIMEOUT.milliseconds) {
                val executions = List(2) {
                    async(Dispatchers.Default) {
                        callersReady.countDown()
                        executor.executeScheduledRequests()
                    }
                }
                callersReady.await()
                firstClaimEntered.await()
                releaseFirstClaim.countDown()
                executions.awaitAll()
            }
        } finally {
            releaseFirstClaim.countDown()
        }

        verify(exactly = 2) { requestRepository.startExecuting(request) }
        coVerify(exactly = 1) { networkRepository.unclaimSensor(any(), TOKEN) }
        verifyTerminal(request, NetworkRequestStatus.SUCCESS)
    }

    @Test
    fun `conflict is terminal rather than a retryable failure`() = runBlocking {
        val request = queuedRequest()
        every { requestRepository.getScheduledRequests() } returns listOf(request)
        coEvery { networkRepository.unclaimSensor(any(), TOKEN) } returns
            response(result = RuuviNetworkResponse.errorResult, code = "ER_CONFLICT")

        executeScheduled()

        verifyTerminal(request, NetworkRequestStatus.CONFLICT)
    }

    @Test
    fun `non-conflict error is retryable`() = runBlocking {
        val request = queuedRequest()
        every { requestRepository.getScheduledRequests() } returns listOf(request)
        coEvery { networkRepository.unclaimSensor(any(), TOKEN) } returns
            response(result = RuuviNetworkResponse.errorResult, code = "ER_UNAVAILABLE")

        executeScheduled()

        verifyRetry(request)
    }

    @Test
    fun `null response is retryable`() = runBlocking {
        val request = queuedRequest()
        every { requestRepository.getScheduledRequests() } returns listOf(request)
        coEvery { networkRepository.unclaimSensor(any(), TOKEN) } returns null

        executeScheduled()

        verifyRetry(request)
    }

    @Test
    fun `network exception is retryable and does not stop later scheduled requests`() = runBlocking {
        val failedPayload = UnclaimSensorRequest(SENSOR_ID)
        val nextPayload = UnclaimSensorRequest("11:22:33:44:55:66")
        val failed = queuedRequest(1, payload = failedPayload)
        val next = queuedRequest(2, payload = nextPayload)
        every { requestRepository.getScheduledRequests() } returns listOf(failed, next)
        coEvery { networkRepository.unclaimSensor(failedPayload, TOKEN) } throws IOException("Offline")
        coEvery { networkRepository.unclaimSensor(nextPayload, TOKEN) } returns response()

        executeScheduled()

        verifyRetry(failed)
        verifyTerminal(next, NetworkRequestStatus.SUCCESS)
        coVerifySequence {
            networkRepository.unclaimSensor(failedPayload, TOKEN)
            networkRepository.unclaimSensor(nextPayload, TOKEN)
        }
    }

    @Test
    fun `completed failed job can be retried successfully by a later scheduler pass`() = runBlocking {
        val request = queuedRequest()
        every { requestRepository.getScheduledRequests() } returns listOf(request)
        coEvery { networkRepository.unclaimSensor(any(), TOKEN) } returnsMany listOf(
            response(result = RuuviNetworkResponse.errorResult),
            response(),
        )

        executeScheduled()
        verifyRetry(request)
        assertFalse(jobManager.isJobRunning(request.id))

        executeScheduled()

        coVerify(exactly = 2) { networkRepository.unclaimSensor(any(), TOKEN) }
        verify(exactly = 2) { requestRepository.startExecuting(request) }
        verify(exactly = 1) { requestRepository.registerFailedAttempt(request) }
        verify(exactly = 1) { requestRepository.disableRequest(request, NetworkRequestStatus.SUCCESS) }
        assertFalse(jobManager.isJobRunning(request.id))
    }

    @Test
    fun `malformed JSON is terminal and does not stop later scheduled requests`() = runBlocking {
        val malformed = queuedRequest(1).copy(requestData = "{\"sensor\":")
        val next = queuedRequest(2)
        val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
        every { requestRepository.getScheduledRequests() } returns listOf(malformed, next)
        coEvery { networkRepository.unclaimSensor(any(), TOKEN) } returns response()
        mockkStatic(FirebaseCrashlytics::class)
        try {
            every { FirebaseCrashlytics.getInstance() } returns crashlytics

            executeScheduled()

            verifyTerminal(malformed, NetworkRequestStatus.PARSE_FAIL)
            verifyTerminal(next, NetworkRequestStatus.SUCCESS)
            coVerify(exactly = 1) { networkRepository.unclaimSensor(any(), TOKEN) }
            verify(exactly = 1) { crashlytics.recordException(any()) }
        } finally {
            unmockkStatic(FirebaseCrashlytics::class)
        }
    }

    @Test
    fun `null and empty JSON payloads are terminal parse failures without network calls`() = runBlocking {
        val requests = listOf(
            queuedRequest(1).copy(requestData = "null"),
            queuedRequest(2).copy(requestData = ""),
        )
        every { requestRepository.getScheduledRequests() } returns requests

        executeScheduled()

        requests.forEach { verifyTerminal(it, NetworkRequestStatus.PARSE_FAIL) }
        verify { networkRepository wasNot Called }
    }

    @Test
    fun `missing token leaves scheduled request ready without sending it`() = runBlocking {
        val request = queuedRequest()
        every { tokenRepository.getTokenInfo() } returns null
        every { requestRepository.getScheduledRequests() } returns listOf(request)

        executeScheduled()

        verify(exactly = 1) { tokenRepository.getTokenInfo() }
        verify(exactly = 0) { requestRepository.startExecuting(request) }
        verify(exactly = 0) { requestRepository.disableRequest(any(), any()) }
        verify(exactly = 0) { requestRepository.registerFailedAttempt(any()) }
        verify { networkRepository wasNot Called }
        verify { sensorSettingsRepository wasNot Called }
    }

    @Test
    fun `queued registration cancels similar jobs before disabling and saving without execution`() = runBlocking {
        val replacement = queuedRequest(3)
        val similar = listOf(queuedRequest(1), queuedRequest(2))
        val oldJobs = similar.map { request -> Job().also { jobManager.registerJob(request.id, it) } }
        val unrelated = Job()
        jobManager.registerJob(99, unrelated)
        every { requestRepository.getSimilar(replacement) } returns similar
        every { requestRepository.disableSimilar(replacement) } answers {
            assertTrue(oldJobs.all { it.isCancelled })
        }

        val registration = executor.registerRequest(replacement, executeNow = false)
        withTimeout(TIMEOUT.milliseconds) { registration.join() }

        assertFalse(registration.isCancelled)
        assertTrue(oldJobs.all { it.isCompleted })
        assertTrue(unrelated.isActive)
        verifySequence {
            requestRepository.getSimilar(replacement)
            requestRepository.disableSimilar(replacement)
            requestRepository.saveRequest(replacement)
        }
        verify { tokenRepository wasNot Called }
        verify { networkRepository wasNot Called }
    }

    @Test
    fun `status registration saves then reports progress and success`() = runBlocking {
        val request = queuedRequest()
        coEvery { networkRepository.unclaimSensor(any(), TOKEN) } returns response()

        val statuses = collectStatuses(request)

        assertEquals(listOf(OperationStatus.InProgress, OperationStatus.Success), statuses)
        verifySequence {
            requestRepository.getSimilar(request)
            requestRepository.disableSimilar(request)
            requestRepository.saveRequest(request)
            requestRepository.startExecuting(request)
            requestRepository.disableRequest(request, NetworkRequestStatus.SUCCESS)
        }
        coVerify(exactly = 1) { networkRepository.unclaimSensor(any(), TOKEN) }
    }

    @Test
    fun `status registration reports progress and failure for a retryable response`() = runBlocking {
        val request = queuedRequest()
        coEvery { networkRepository.unclaimSensor(any(), TOKEN) } returns
            response(result = RuuviNetworkResponse.errorResult)

        val statuses = collectStatuses(request)

        assertEquals(
            listOf(OperationStatus.InProgress, OperationStatus.Fail(UiText.EmptyString)),
            statuses,
        )
        verifyRetry(request)
    }

    @Test
    fun `status registration reports failure without sending work when start is rejected`() = runBlocking {
        val request = queuedRequest()
        every { requestRepository.startExecuting(request) } returns false

        val statuses = collectStatuses(request)

        assertEquals(
            listOf(OperationStatus.InProgress, OperationStatus.Fail(UiText.EmptyString)),
            statuses,
        )
        verify(exactly = 1) { requestRepository.saveRequest(request) }
        verify(exactly = 0) { requestRepository.registerFailedAttempt(any()) }
        verify(exactly = 0) { requestRepository.disableRequest(any(), any()) }
        verify { networkRepository wasNot Called }
    }

    @Test
    fun `cancelling executing requests waits for network cancellation and does not report success`() = runBlocking {
        val request = queuedRequest()
        val started = CompletableDeferred<Unit>()
        val stopped = CompletableDeferred<Unit>()
        every { requestRepository.getScheduledRequests() } returns listOf(request)
        coEvery { networkRepository.unclaimSensor(any(), TOKEN) } coAnswers {
            started.complete(Unit)
            try {
                awaitCancellation()
            } finally {
                stopped.complete(Unit)
            }
        }

        withTimeout(TIMEOUT.milliseconds) {
            val execution = launch { executor.executeScheduledRequests() }
            started.await()

            executor.cancelAndJoinExecutingRequests()

            assertTrue(stopped.isCompleted)
            assertFalse(jobManager.isJobRunning(request.id))
            execution.join()
        }
        verifyRetry(request)
        coVerify(exactly = 1) { networkRepository.unclaimSensor(any(), TOKEN) }
    }

    @Test
    fun `upload for missing sensor settings is retired without network or background writes`() = runBlocking {
        verifyStaleUploadSkipped(null)
    }

    @Test
    fun `upload replaced by a default background is retired without network or background writes`() = runBlocking {
        verifyStaleUploadSkipped(SensorSettings(id = SENSOR_ID, defaultBackground = 2))
    }

    @Test
    fun `upload replaced by another user image is retired without network or background writes`() = runBlocking {
        verifyStaleUploadSkipped(SensorSettings(id = SENSOR_ID, userBackground = "newer-image.jpg"))
    }

    @Test
    fun `successful uploads without a guid preserve the stored network background`() = runBlocking {
        val upload = imageRequest()
        val requests = listOf(
            queuedRequest(1, NetworkRequestType.UPLOAD_IMAGE, upload),
            queuedRequest(2, NetworkRequestType.UPLOAD_IMAGE, upload),
        )
        every { requestRepository.getScheduledRequests() } returns requests
        every { sensorSettingsRepository.getSensorSettings(SENSOR_ID) } returns currentBackground()
        coEvery { networkRepository.uploadImage(IMAGE_FILENAME, upload.request, TOKEN) } returnsMany listOf(
            response(),
            response(UploadImageResponseBody(uploadURL = "", guid = "")),
        )

        executeScheduled()

        requests.forEach { verifyTerminal(it, NetworkRequestStatus.SUCCESS) }
        coVerify(exactly = 2) { networkRepository.uploadImage(IMAGE_FILENAME, upload.request, TOKEN) }
        verify(exactly = 0) { sensorSettingsRepository.updateNetworkBackground(any(), any(), any()) }
    }

    @Test
    fun `failed upload is retryable and does not apply a guid from an error response`() = runBlocking {
        val upload = imageRequest()
        val request = queuedRequest(type = NetworkRequestType.UPLOAD_IMAGE, payload = upload)
        every { requestRepository.getScheduledRequests() } returns listOf(request)
        every { sensorSettingsRepository.getSensorSettings(SENSOR_ID) } returns currentBackground()
        coEvery { networkRepository.uploadImage(IMAGE_FILENAME, upload.request, TOKEN) } returns response(
            data = UploadImageResponseBody(uploadURL = "", guid = "must-not-be-saved"),
            result = RuuviNetworkResponse.errorResult,
        )

        executeScheduled()

        verifyRetry(request)
        verify(exactly = 0) { sensorSettingsRepository.updateNetworkBackground(any(), any(), any()) }
    }

    @Test
    fun `image sync lookup is restricted to active uploads for the requested sensor`() {
        val otherSensor = "11:22:33:44:55:66"
        val upload = queuedRequest(type = NetworkRequestType.UPLOAD_IMAGE, payload = imageRequest())
        every {
            requestRepository.getActiveRequestsForKeyType(SENSOR_ID, NetworkRequestType.UPLOAD_IMAGE)
        } returns listOf(upload)
        every {
            requestRepository.getActiveRequestsForKeyType(otherSensor, NetworkRequestType.UPLOAD_IMAGE)
        } returns emptyList()

        assertTrue(executor.gotAnyImagesInSync(SENSOR_ID))
        assertFalse(executor.gotAnyImagesInSync(otherSensor))

        verifySequence {
            requestRepository.getActiveRequestsForKeyType(SENSOR_ID, NetworkRequestType.UPLOAD_IMAGE)
            requestRepository.getActiveRequestsForKeyType(otherSensor, NetworkRequestType.UPLOAD_IMAGE)
        }
        verify { networkRepository wasNot Called }
    }

    @Test
    fun `settings query distinguishes user settings from all other scheduled types`() {
        val nonSettingsRequests = NetworkRequestType.entries
            .filter { it != NetworkRequestType.SETTINGS }
            .mapIndexed { index, type -> queuedRequest(index + 1, type) }
        every { requestRepository.getScheduledRequests() } returnsMany listOf(
            emptyList(),
            nonSettingsRequests,
            nonSettingsRequests + queuedRequest(99, NetworkRequestType.SETTINGS),
        )

        assertFalse(executor.anySettingsRequests())
        assertFalse(executor.anySettingsRequests())
        assertTrue(executor.anySettingsRequests())

        verify(exactly = 3) { requestRepository.getScheduledRequests() }
        verify(exactly = 0) { requestRepository.startExecuting(any()) }
        verify { networkRepository wasNot Called }
    }

    private suspend fun executeScheduled() {
        withTimeout(TIMEOUT.milliseconds) { executor.executeScheduledRequests() }
    }

    private suspend fun collectStatuses(request: NetworkRequest): List<OperationStatus> =
        withTimeout(TIMEOUT.milliseconds) { executor.registerRequestWithStatus(request).take(2).toList() }

    private suspend fun verifyStaleUploadSkipped(settings: SensorSettings?) {
        val request = queuedRequest(type = NetworkRequestType.UPLOAD_IMAGE, payload = imageRequest())
        every { requestRepository.getScheduledRequests() } returns listOf(request)
        every { sensorSettingsRepository.getSensorSettings(SENSOR_ID) } returns settings

        executeScheduled()

        verifyTerminal(request, NetworkRequestStatus.SUCCESS)
        verify { networkRepository wasNot Called }
        verify(exactly = 1) { sensorSettingsRepository.getSensorSettings(SENSOR_ID) }
        verify(exactly = 0) { sensorSettingsRepository.updateNetworkBackground(any(), any(), any()) }
    }

    private fun verifyTerminal(request: NetworkRequest, status: NetworkRequestStatus) {
        verify(exactly = 1) { requestRepository.disableRequest(request, status) }
        verify(exactly = 1) { requestRepository.disableRequest(request, any()) }
        verify(exactly = 0) { requestRepository.registerFailedAttempt(request) }
    }

    private fun verifyRetry(request: NetworkRequest) {
        verify(exactly = 1) { requestRepository.registerFailedAttempt(request) }
        verify(exactly = 0) { requestRepository.disableRequest(request, any()) }
    }

    private fun queuedRequest(
        id: Int = 1,
        type: NetworkRequestType = NetworkRequestType.UNCLAIM,
        payload: Any = UnclaimSensorRequest(SENSOR_ID, deleteData = true),
    ) = NetworkRequest(type, SENSOR_ID, payload).apply {
        this.id = id
        requestDate = Date(0L)
    }

    private fun imageRequest() = UploadImageRequestWrapper(IMAGE_FILENAME, UploadImageRequest(SENSOR_ID))

    private fun currentBackground() = SensorSettings(
        id = SENSOR_ID,
        userBackground = IMAGE_FILENAME,
        networkBackground = "previous-guid",
    )

    private fun <T> response(
        data: T? = null,
        result: String = RuuviNetworkResponse.successResult,
        code: String? = null,
    ) = RuuviNetworkResponse(
        result = result,
        error = if (result == RuuviNetworkResponse.errorResult) "Unavailable" else "",
        data = data,
        code = code,
    )

    companion object {
        private const val SENSOR_ID = "AA:BB:CC:DD:EE:FF"
        private const val TOKEN = "executor-test-token"
        private const val IMAGE_FILENAME = "user-background.jpg"
        private const val TIMEOUT = 5_000L
    }
}
