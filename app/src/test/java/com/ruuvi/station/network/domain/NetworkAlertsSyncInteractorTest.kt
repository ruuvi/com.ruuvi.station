package com.ruuvi.station.network.domain

import com.ruuvi.station.alarm.domain.AlarmType
import com.ruuvi.station.database.domain.AlarmRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.tables.Alarm
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.network.data.response.NetworkAlertItem
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import java.util.Date

class NetworkAlertsSyncInteractorTest {
    private val alarmRepository = mockk<AlarmRepository>(relaxed = true)
    private val sensorSettingsRepository = mockk<SensorSettingsRepository>(relaxed = true)
    private val networkInteractor = mockk<RuuviNetworkInteractor>(relaxed = true)
    private val interactor = NetworkAlertsSyncInteractor(
        alarmRepository,
        sensorSettingsRepository,
        networkInteractor,
    )

    @Before
    fun setUp() {
        every { sensorSettingsRepository.getSensorSettings(SYNC_SENSOR_ID) } returns SensorSettings(SYNC_SENSOR_ID)
        every { alarmRepository.getForSensor(any()) } returns emptyList()
    }

    @Test
    fun `newer cloud alert replaces local values while preserving the local mute`() {
        val mutedTill = Date(50_000L)
        val local = localAlert(timestamp = 100L).copy(mutedTill = mutedTill)
        every { alarmRepository.getForSensor(SYNC_SENSOR_ID) } returns listOf(local)
        val cloud = cloudAlert(timestamp = 200L).copy(enabled = false)

        syncAlerts(cloud)

        verify(exactly = 1) {
            alarmRepository.upsertAlarm(
                sensorId = SYNC_SENSOR_ID,
                min = cloud.min,
                max = cloud.max,
                enabled = false,
                type = AlarmType.TEMPERATURE.value,
                description = cloud.description,
                mutedTill = mutedTill,
                timestamp = 200L,
            )
        }
        verify(exactly = 0) { networkInteractor.setAlert(any()) }
    }

    @Test
    fun `newer local alert is uploaded without applying stale cloud data`() {
        val local = localAlert(timestamp = 300L)
        every { alarmRepository.getForSensor(SYNC_SENSOR_ID) } returns listOf(local)

        syncAlerts(cloudAlert(timestamp = 200L))

        verify(exactly = 1) { networkInteractor.setAlert(local) }
        verifyNoLocalWrites()
    }

    @Test
    fun `equal alert timestamps cause no writes in either direction`() {
        every { alarmRepository.getForSensor(SYNC_SENSOR_ID) } returns listOf(localAlert(timestamp = 200L))

        syncAlerts(cloudAlert(timestamp = 200L))

        verify(exactly = 0) { networkInteractor.setAlert(any()) }
        verifyNoLocalWrites()
    }

    @Test
    fun `local alert missing from cloud is uploaded including disabled alerts`() {
        val local = localAlert(timestamp = 0L).copy(enabled = false)
        every { alarmRepository.getForSensor(SYNC_SENSOR_ID) } returns listOf(local)

        syncAlerts()

        verify(exactly = 1) { networkInteractor.setAlert(local) }
        verifyNoLocalWrites()
    }

    @Test
    fun `every supported cloud alert type is saved using its database type`() {
        val types = AlarmType.entries.filter { it.networkCode != null }
        val alerts = types.map { cloudAlert(timestamp = 200L).copy(type = requireNotNull(it.networkCode)) }

        syncAlerts(*alerts.toTypedArray())

        types.forEach { type ->
            verify(exactly = 1) {
                alarmRepository.upsertAlarm(
                    sensorId = SYNC_SENSOR_ID,
                    min = -10.0,
                    max = 30.0,
                    enabled = true,
                    type = type.value,
                    description = "Cloud alert",
                    mutedTill = null,
                    timestamp = 200L,
                )
            }
        }
        verify(exactly = 0) { networkInteractor.setAlert(any()) }
    }

    @Test
    fun `unknown cloud alert type is ignored without blocking known alerts`() {
        val known = cloudAlert(timestamp = 200L)

        syncAlerts(known.copy(type = "future-alert"), known)

        verify(exactly = 1) {
            alarmRepository.upsertAlarm(
                SYNC_SENSOR_ID, known.min, known.max, AlarmType.TEMPERATURE.value,
                known.enabled, known.description, null, known.lastUpdated,
            )
        }
        verify(exactly = 1) { alarmRepository.upsertAlarm(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `alerts for sensors without local settings are ignored`() {
        every { sensorSettingsRepository.getSensorSettings(SYNC_SENSOR_ID) } returns null

        syncAlerts(cloudAlert(timestamp = 200L))

        verify(exactly = 0) { alarmRepository.getForSensor(any()) }
        verify(exactly = 0) { networkInteractor.setAlert(any()) }
        verifyNoLocalWrites()
    }

    @Test
    fun `missing response data does not read or change alerts`() {
        interactor.updateAlertsFromNetwork(denseResponse().copy(data = null))

        verify(exactly = 0) { sensorSettingsRepository.getSensorSettings(any()) }
        verify(exactly = 0) { alarmRepository.getForSensor(any()) }
        verify(exactly = 0) { networkInteractor.setAlert(any()) }
        verifyNoLocalWrites()
    }

    @Test
    fun `timestamp conflicts are resolved independently for each alert type`() {
        val temperature = localAlert(timestamp = 300L)
        val humidityMute = Date(60_000L)
        val humidity = localAlert(timestamp = 100L).copy(type = AlarmType.HUMIDITY.value, mutedTill = humidityMute)
        every { alarmRepository.getForSensor(SYNC_SENSOR_ID) } returns listOf(humidity, temperature)

        syncAlerts(
            cloudAlert(timestamp = 200L),
            cloudAlert(timestamp = 200L).copy(type = "humidity"),
        )

        verify(exactly = 1) { networkInteractor.setAlert(temperature) }
        verify(exactly = 1) { networkInteractor.setAlert(any()) }
        verify(exactly = 1) {
            alarmRepository.upsertAlarm(
                SYNC_SENSOR_ID, -10.0, 30.0, AlarmType.HUMIDITY.value,
                true, "Cloud alert", humidityMute, 200L,
            )
        }
        verify(exactly = 1) { alarmRepository.upsertAlarm(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    private fun syncAlerts(vararg alerts: NetworkAlertItem) {
        interactor.updateAlertsFromNetwork(denseResponse(denseSensor().copy(alerts = alerts.toList())))
    }

    private fun verifyNoLocalWrites() {
        verify(exactly = 0) { alarmRepository.upsertAlarm(any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    private fun localAlert(timestamp: Long) = Alarm(
        ruuviTagId = SYNC_SENSOR_ID,
        type = AlarmType.TEMPERATURE.value,
        min = 0.0,
        max = 20.0,
        enabled = true,
        customDescription = "Local alert",
        lastUpdated = timestamp,
    )

    private fun cloudAlert(timestamp: Long) = NetworkAlertItem(
        type = "temperature",
        min = -10.0,
        max = 30.0,
        enabled = true,
        description = "Cloud alert",
        lastUpdated = timestamp,
    )
}
