package com.ruuvi.station.bluetooth

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.raizlabs.android.dbflow.config.FlowManager
import com.ruuvi.station.alarm.domain.AlarmCheckInteractor
import com.ruuvi.station.app.preferences.GlobalSettings
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.bluetooth.contract.FoundRuuviTag
import com.ruuvi.station.bluetooth.domain.AutomaticGattSyncInteractor
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.dataforwarding.domain.DataForwardingSender
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any as anyMockito
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify as verifyMockito
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class DefaultOnTagFoundListenerTest {
    private val context = mockk<Context>(relaxed = true)
    private val preferencesRepository = mockk<PreferencesRepository>(relaxed = true)
    private val dataForwardingSender = mockk<DataForwardingSender>(relaxed = true)
    private val repository = mock<TagRepository>()
    private val alarmCheckInteractor = mockk<AlarmCheckInteractor>(relaxed = true)
    private val sensorSettingsRepository = mockk<SensorSettingsRepository>(relaxed = true)
    private val sensorHistoryRepository = mockk<SensorHistoryRepository>(relaxed = true)
    private val automaticGattSyncInteractor = mockk<AutomaticGattSyncInteractor>(relaxed = true)
    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)
    private val persistedReadings = mutableListOf<TagSensorReading>()
    private val refreshedSensors = mutableListOf<String>()
    private var now = 1_000_000L

    @Before
    fun setUp() {
        FlowManager.init(ApplicationProvider.getApplicationContext())
        every { preferencesRepository.getBackgroundScanInterval() } returns 60
    }

    @Test
    fun `new valid sensor is saved with the current timestamp`() {
        whenever(repository.getTagById(SENSOR_ID)).thenReturn(null)
        val listener = listener()

        listener.onTagFound(tag())
        scope.advanceUntilIdle()

        verifyMockito(repository).saveTag(
            argThat { id == SENSOR_ID && updateAt == Date(now) },
        )
        verify { sensorSettingsRepository wasNot Called }
    }

    @Test
    fun `invalid RSSI and descending air particulate values are dropped`() {
        val listener = listener()

        listener.onTagFound(tag(rssi = 1))
        listOf(
            listOf(2.0, 1.0, 3.0, 4.0),
            listOf(1.0, 3.0, 2.0, 4.0),
            listOf(1.0, 2.0, 4.0, 3.0),
        ).forEach { particulateValues ->
            listener.onTagFound(
                tag(
                    dataFormat = AIR_DATA_FORMAT,
                    pm1 = particulateValues[0],
                    pm25 = particulateValues[1],
                    pm4 = particulateValues[2],
                    pm10 = particulateValues[3],
                ),
            )
        }
        scope.advanceUntilIdle()

        verifyMockito(repository, never()).getTagById(anyMockito())
    }

    @Test
    fun `existing sensor without settings only updates latest advertisement`() {
        whenever(repository.getTagById(SENSOR_ID)).thenReturn(RuuviTagEntity(id = SENSOR_ID))
        every { sensorSettingsRepository.getSensorSettings(SENSOR_ID) } returns null
        val listener = listener()

        listener.onTagFound(tag())
        scope.advanceUntilIdle()

        verifyMockito(repository).updateTag(anyMockito())
        verify { automaticGattSyncInteractor wasNot Called }
        verify { alarmCheckInteractor wasNot Called }
        assertTrue(persistedReadings.isEmpty())
    }

    @Test
    fun `favorite readings are throttled in background and resume after interval`() {
        val existingTag = RuuviTagEntity(id = SENSOR_ID, favorite = true)
        val settings = SensorSettings(id = SENSOR_ID)
        whenever(repository.getTagById(SENSOR_ID)).thenReturn(existingTag)
        every { sensorSettingsRepository.getSensorSettings(SENSOR_ID) } returns settings
        val listener = listener()

        listener.onTagFound(tag())
        scope.advanceUntilIdle()
        listener.onTagFound(tag())
        scope.advanceUntilIdle()
        now += 61_000L
        listener.onTagFound(tag())
        scope.advanceUntilIdle()

        assertEquals(2, persistedReadings.size)
        assertEquals(listOf(SENSOR_ID, SENSOR_ID), refreshedSensors)
        verify(exactly = 2) { dataForwardingSender.sendData(any(), settings) }
        verifyMockito(repository, org.mockito.kotlin.times(3)).updateTag(anyMockito())
        verify(exactly = 3) { alarmCheckInteractor.checkAlarmsForSensor(any(), settings) }
    }

    @Test
    fun `foreground reading starts automatic sync and bypasses background throttle`() {
        val settings = SensorSettings(id = SENSOR_ID, lastSync = Date(500L))
        whenever(repository.getTagById(SENSOR_ID)).thenReturn(RuuviTagEntity(id = SENSOR_ID))
        every { sensorSettingsRepository.getSensorSettings(SENSOR_ID) } returns settings
        val listener = listener().apply { isForeground = true }

        listener.onTagFound(
            tag(
                dataFormat = AIR_DATA_FORMAT,
                pm1 = 1.0,
                pm25 = 2.0,
                pm4 = 3.0,
                pm10 = 4.0,
            ),
        )
        scope.advanceUntilIdle()

        verify {
            automaticGattSyncInteractor.syncIfEligible(
                sensorId = SENSOR_ID,
                lastSync = settings.lastSync,
                isAir = true,
                networkSensor = false,
            )
        }
        assertEquals(1, persistedReadings.size)
    }

    @Test
    fun `cloud mode leaves synced network sensor unchanged after automatic sync check`() {
        val settings = SensorSettings(
            id = SENSOR_ID,
            networkSensor = true,
            networkLastSync = Date(500L),
        )
        every { preferencesRepository.signedIn() } returns true
        every { preferencesRepository.isCloudModeEnabled() } returns true
        whenever(repository.getTagById(SENSOR_ID)).thenReturn(RuuviTagEntity(id = SENSOR_ID))
        every { sensorSettingsRepository.getSensorSettings(SENSOR_ID) } returns settings
        val listener = listener().apply { isForeground = true }

        listener.onTagFound(tag())
        scope.advanceUntilIdle()

        verify { automaticGattSyncInteractor.syncIfEligible(SENSOR_ID, null, false, true) }
        verifyMockito(repository, never()).updateTag(anyMockito())
        verify(exactly = 0) {
            alarmCheckInteractor.checkAlarmsForSensor(any(), any())
        }
        assertTrue(persistedReadings.isEmpty())
    }

    @Test
    fun `legacy air format updates sensor and alarms without saving history`() {
        val settings = SensorSettings(id = SENSOR_ID)
        whenever(repository.getTagById(SENSOR_ID)).thenReturn(RuuviTagEntity(id = SENSOR_ID))
        every { sensorSettingsRepository.getSensorSettings(SENSOR_ID) } returns settings
        val listener = listener()

        listener.onTagFound(tag(dataFormat = 0xF0))
        scope.advanceUntilIdle()

        verifyMockito(repository).updateTag(anyMockito())
        verify { alarmCheckInteractor.checkAlarmsForSensor(any(), settings) }
        assertTrue(persistedReadings.isEmpty())
        verify { dataForwardingSender wasNot Called }
    }

    @Test
    fun `old history cleanup runs at most once per ten minute window`() {
        val listener = listener()
        now += 10 * 60 * 1_000L + 1

        listener.onTagFound(FoundRuuviTag())
        listener.onTagFound(FoundRuuviTag())
        scope.advanceUntilIdle()

        verify(exactly = 1) {
            sensorHistoryRepository.removeOlderThan(GlobalSettings.historyLengthHours)
        }
    }

    private fun listener(): DefaultOnTagFoundListener = DefaultOnTagFoundListener(
        context = context,
        preferencesRepository = preferencesRepository,
        dataForwardingSender = dataForwardingSender,
        repository = repository,
        alarmCheckInteractor = alarmCheckInteractor,
        sensorSettingsRepository = sensorSettingsRepository,
        sensorHistoryRepository = sensorHistoryRepository,
        automaticGattSyncInteractor = { automaticGattSyncInteractor },
        ioScope = CoroutineScope(scope.coroutineContext),
        currentTimeMillis = { now },
        persistReading = persistedReadings::add,
        refreshWidgets = { _, sensorId -> refreshedSensors.add(sensorId) },
    )

    private fun tag(
        rssi: Int = -50,
        dataFormat: Int = 5,
        pm1: Double? = null,
        pm25: Double? = null,
        pm4: Double? = null,
        pm10: Double? = null,
    ) = FoundRuuviTag(
        id = SENSOR_ID,
        rssi = rssi,
        dataFormat = dataFormat,
        pm1 = pm1,
        pm25 = pm25,
        pm4 = pm4,
        pm10 = pm10,
    )

    private companion object {
        const val SENSOR_ID = "AA:BB:CC:DD:EE:FF"
        const val AIR_DATA_FORMAT = 0xE0
    }
}
