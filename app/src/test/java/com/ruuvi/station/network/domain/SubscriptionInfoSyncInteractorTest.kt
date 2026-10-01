package com.ruuvi.station.network.domain

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.network.data.response.GetSubscriptionResponse
import com.ruuvi.station.network.data.response.GetSubscriptionResponseBody
import com.ruuvi.station.network.data.response.RuuviNetworkResponse
import com.ruuvi.station.network.data.response.SubscriptionInfo
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Test

class SubscriptionInfoSyncInteractorTest {
    private val preferencesRepository = mockk<PreferencesRepository>(relaxed = true)
    private val networkInteractor = mockk<RuuviNetworkInteractor>()
    private val interactor = SubscriptionInfoSyncInteractor(preferencesRepository, networkInteractor)

    @Before
    fun setUp() {
        every { preferencesRepository.getSubscriptionRefreshDate() } returns 0L
    }

    @Test
    fun `stale subscription refresh uses limits from the first active subscription`() = runBlocking {
        val onResult = requestRefresh()
        val active = subscription(active = true, maxShares = 10, maxSharesPerSensor = 3)

        onResult(response(
            subscription(active = false, maxShares = 100, maxSharesPerSensor = 50),
            active,
            subscription(active = true, maxShares = 20, maxSharesPerSensor = 5),
        ))

        verify(exactly = 1) { preferencesRepository.setSubscriptionMaxSharesTotal(active.maxShares) }
        verify(exactly = 1) { preferencesRepository.setSubscriptionMaxSharesTotal(any()) }
        verify(exactly = 1) { preferencesRepository.setSubscriptionMaxSharesPerSensor(active.maxSharesPerSensor) }
        verify(exactly = 1) { preferencesRepository.setSubscriptionMaxSharesPerSensor(any()) }
    }

    @Test
    fun `inactive subscriptions do not overwrite saved limits`() = runBlocking {
        val onResult = requestRefresh()

        onResult(response(subscription(active = false)))

        verifyNoLimitChanges()
    }

    @Test
    fun `empty subscription list leaves saved limits unchanged`() = runBlocking {
        val onResult = requestRefresh()

        onResult(response())

        verifyNoLimitChanges()
    }

    @Test
    fun `error response does not apply even an active subscription`() = runBlocking {
        val onResult = requestRefresh()

        onResult(response(subscription(active = true)).copy(
            result = RuuviNetworkResponse.errorResult,
            error = "Unavailable",
        ))

        verifyNoLimitChanges()
    }

    @Test
    fun `null subscription response leaves saved limits unchanged`() = runBlocking {
        val onResult = requestRefresh()

        onResult(null)

        verifyNoLimitChanges()
    }

    @Test
    fun `successful subscription response without data leaves saved limits unchanged`() = runBlocking {
        val onResult = requestRefresh()

        onResult(response().copy(data = null))

        verifyNoLimitChanges()
    }

    private suspend fun requestRefresh(): (GetSubscriptionResponse?) -> Unit {
        val callback = CompletableDeferred<(GetSubscriptionResponse?) -> Unit>()
        coEvery { networkInteractor.getSubscription(any()) } coAnswers {
            callback.complete(firstArg())
            Unit
        }

        interactor.syncSubscriptionInfo()

        return withTimeout(5_000L) { callback.await() }
    }

    private fun verifyNoLimitChanges() {
        verify(exactly = 0) { preferencesRepository.setSubscriptionMaxSharesTotal(any()) }
        verify(exactly = 0) { preferencesRepository.setSubscriptionMaxSharesPerSensor(any()) }
    }

    private fun response(vararg subscriptions: SubscriptionInfo): GetSubscriptionResponse =
        RuuviNetworkResponse(
            result = RuuviNetworkResponse.successResult,
            error = "",
            data = GetSubscriptionResponseBody(subscriptions.toList()),
            code = null,
        )

    private fun subscription(active: Boolean, maxShares: Int = 10, maxSharesPerSensor: Int = 3) = SubscriptionInfo(
        validDays = 30,
        maxClaims = 5,
        maxShares = maxShares,
        maxSharesPerSensor = maxSharesPerSensor,
        maxHistoryDays = 7,
        maxResolutionMinutes = 1,
        subscriptionName = "test-plan",
        claimCode = "",
        creatorId = "",
        isActive = active,
        startTime = 0L,
        endTime = null,
    )
}
