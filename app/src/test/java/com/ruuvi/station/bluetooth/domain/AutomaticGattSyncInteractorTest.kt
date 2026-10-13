package com.ruuvi.station.bluetooth.domain

import com.ruuvi.station.app.preferences.PreferencesRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

class AutomaticGattSyncInteractorTest {
    private val gattInteractor = mockk<BluetoothGattInteractor>(relaxed = true)
    private val preferencesRepository = mockk<PreferencesRepository>(relaxed = true)
    private val automaticGattSyncInteractor = AutomaticGattSyncInteractor(
        gattInteractor,
        preferencesRepository,
    )

    @Before
    fun setUp() {
        every { gattInteractor.isSyncInProgress(any()) } returns false
        every { gattInteractor.readLogs(any(), any()) } returns true
        every { preferencesRepository.getLastAutomaticGattSyncAttempt(any()) } returns 0L
    }

    @Test
    fun `syncs a present local Air sensor and records the attempt`() {
        val attemptTime = slot<Long>()

        automaticGattSyncInteractor.syncIfEligible(
            sensorId = SENSOR_ID,
            lastSync = OLD_LAST_SYNC,
            isAir = true,
            networkSensor = false,
        )

        verify { gattInteractor.readLogs(SENSOR_ID, OLD_LAST_SYNC) }
        verify {
            preferencesRepository.setLastAutomaticGattSyncAttempt(SENSOR_ID, capture(attemptTime))
        }
        assertTrue(attemptTime.captured <= System.currentTimeMillis())
    }

    @Test
    fun `does not sync a recently attempted sensor`() {
        every {
            preferencesRepository.getLastAutomaticGattSyncAttempt(SENSOR_ID)
        } returns System.currentTimeMillis() - 4 * 60 * 1000L

        automaticGattSyncInteractor.syncIfEligible(
            sensorId = SENSOR_ID,
            lastSync = OLD_LAST_SYNC,
            isAir = true,
            networkSensor = false,
        )

        verify(exactly = 0) { gattInteractor.readLogs(any(), any()) }
    }

    @Test
    fun `does not sync non-Air cloud or active sensors`() {
        automaticGattSyncInteractor.syncIfEligible(
            sensorId = SENSOR_ID,
            lastSync = OLD_LAST_SYNC,
            isAir = false,
            networkSensor = false,
        )
        automaticGattSyncInteractor.syncIfEligible(
            sensorId = SENSOR_ID,
            lastSync = OLD_LAST_SYNC,
            isAir = true,
            networkSensor = true,
        )
        every { gattInteractor.isSyncInProgress(SENSOR_ID) } returns true
        automaticGattSyncInteractor.syncIfEligible(
            sensorId = SENSOR_ID,
            lastSync = OLD_LAST_SYNC,
            isAir = true,
            networkSensor = false,
        )

        verify(exactly = 0) { gattInteractor.readLogs(any(), any()) }
    }

    private companion object {
        const val SENSOR_ID = "AA:BB:CC:DD:EE:FF"
        val OLD_LAST_SYNC = Date(System.currentTimeMillis() - 10 * 60 * 1000L)
    }
}
