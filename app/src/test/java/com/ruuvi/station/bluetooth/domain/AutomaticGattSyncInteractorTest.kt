package com.ruuvi.station.bluetooth.domain

import android.app.Application
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.bluetooth.di.BluetoothScannerInjectionModule
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.bind
import org.kodein.di.direct
import org.kodein.di.instance
import org.kodein.di.singleton
import java.util.Date

class AutomaticGattSyncInteractorTest {
    private val gattInteractor = mockk<BluetoothGattInteractor>(relaxed = true)
    private val preferencesRepository = mockk<PreferencesRepository>(relaxed = true)
    private val automaticGattSyncInteractor = AutomaticGattSyncInteractor(
        { gattInteractor },
        preferencesRepository,
    )

    @Before
    fun setUp() {
        every { gattInteractor.isSyncInProgress(any()) } returns false
        every { gattInteractor.readLogs(any(), any(), any()) } returns true
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

        verify { gattInteractor.readLogs(SENSOR_ID, OLD_LAST_SYNC, manualSync = false) }
        verify {
            preferencesRepository.setLastAutomaticGattSyncAttempt(SENSOR_ID, capture(attemptTime))
        }
        assertTrue(attemptTime.captured <= System.currentTimeMillis())
    }

    @Test
    fun `not found sensor remains eligible for immediate retry`() {
        every { gattInteractor.readLogs(SENSOR_ID, any(), manualSync = false) } returns false

        repeat(2) {
            automaticGattSyncInteractor.syncIfEligible(
                sensorId = SENSOR_ID,
                lastSync = OLD_LAST_SYNC,
                isAir = true,
                networkSensor = false,
            )
        }

        verify(exactly = 2) {
            gattInteractor.readLogs(SENSOR_ID, OLD_LAST_SYNC, manualSync = false)
        }
        verify(exactly = 0) {
            preferencesRepository.setLastAutomaticGattSyncAttempt(SENSOR_ID, any())
        }
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

        verify(exactly = 0) { gattInteractor.readLogs(any(), any(), any()) }
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

        verify(exactly = 0) { gattInteractor.readLogs(any(), any(), any()) }
    }

    @Test
    fun `cold first sync resolves GATT from root without retaining the binding dependency chain`() {
        val application = mockk<TestApplication>()
        val di = DI {
            import(BluetoothScannerInjectionModule.module)
            bind<Application>() with singleton { application }
            bind<PreferencesRepository>() with singleton { preferencesRepository }
            bind<BluetoothGattInteractor>(overrides = true) with singleton {
                // Model the GATT -> Bluetooth -> listener -> automatic sync dependency.
                instance<AutomaticGattSyncInteractor>()
                gattInteractor
            }
        }
        every { application.di } returns di

        val automaticSync = di.direct.instance<AutomaticGattSyncInteractor>()
        verify(exactly = 0) { gattInteractor.isSyncInProgress(any()) }

        automaticSync.syncIfEligible(
            sensorId = SENSOR_ID,
            lastSync = OLD_LAST_SYNC,
            isAir = true,
            networkSensor = false,
        )

        verify { gattInteractor.readLogs(SENSOR_ID, OLD_LAST_SYNC, manualSync = false) }
    }

    private class TestApplication : Application(), DIAware {
        override lateinit var di: DI
    }

    private companion object {
        const val SENSOR_ID = "AA:BB:CC:DD:EE:FF"
        val OLD_LAST_SYNC = Date(System.currentTimeMillis() - 10 * 60 * 1000L)
    }
}
