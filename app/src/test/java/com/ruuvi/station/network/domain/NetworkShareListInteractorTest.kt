package com.ruuvi.station.network.domain

import com.ruuvi.station.database.domain.SensorShareListRepository
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class NetworkShareListInteractorTest {
    private val repository = mockk<SensorShareListRepository>(relaxed = true)
    private val interactor = NetworkShareListInteractor(repository)

    @Test
    fun `confirmed and pending shares are passed to the matching sensor`() {
        val first = denseSensor().copy(
            sharedTo = listOf("accepted@example.com"),
            sharedToPending = listOf("pending@example.com"),
        )
        val second = denseSensor("11:22:33:44:55:66").copy(
            sharedTo = listOf("another@example.com"),
            sharedToPending = emptyList(),
        )

        interactor.updateSharingInfo(denseResponse(first, second))

        verify(exactly = 1) { repository.updateSharingList(first.sensor, first.sharedTo, first.sharedToPending) }
        verify(exactly = 1) { repository.updateSharingList(second.sensor, second.sharedTo, second.sharedToPending) }
        verify(exactly = 2) { repository.updateSharingList(any(), any(), any()) }
    }

    @Test
    fun `empty sharing lists are forwarded to remove stale local shares`() {
        interactor.updateSharingInfo(denseResponse(denseSensor()))

        verify(exactly = 1) { repository.updateSharingList(SYNC_SENSOR_ID, emptyList(), emptyList()) }
    }

    @Test
    fun `empty sensor list leaves sharing data unchanged`() {
        interactor.updateSharingInfo(denseResponse())

        verify(exactly = 0) { repository.updateSharingList(any(), any(), any()) }
    }

    @Test
    fun `missing response data leaves sharing data unchanged`() {
        interactor.updateSharingInfo(denseResponse().copy(data = null))

        verify(exactly = 0) { repository.updateSharingList(any(), any(), any()) }
    }
}
