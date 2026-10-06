package com.ruuvi.station.settings.domain

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.dataforwarding.domain.DataForwardingSender
import com.ruuvi.station.network.domain.NetworkApplicationSettings
import com.ruuvi.station.network.domain.NetworkSettingNames
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.UnitType.TemperatureUnit
import com.ruuvi.station.util.BackgroundScanModes
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppSettingsInteractorTest {
    private val preferencesRepository = mockk<PreferencesRepository>(relaxed = true)
    private val networkApplicationSettings = mockk<NetworkApplicationSettings>(relaxed = true)
    private val sensorSettingsRepository = mockk<SensorSettingsRepository>()
    private val interactor = AppSettingsInteractor(
        preferencesRepository = preferencesRepository,
        dataForwardingSender = mockk(),
        unitsConverter = mockk<UnitsConverter>(),
        networkApplicationSettings = networkApplicationSettings,
        sensorSettingsRepository = sensorSettingsRepository,
        tagRepository = mockk<TagRepository>(),
    )

    @Before
    fun setUp() {
        every { preferencesRepository.isShowAllGraphPoint() } returns false
        every { preferencesRepository.graphDrawDots() } returns false
        every { preferencesRepository.getBackgroundScanMode() } returns BackgroundScanModes.BACKGROUND
        every { preferencesRepository.getBackgroundScanInterval() } returns 15
        every { preferencesRepository.getGraphViewPeriodHours() } returns 24
    }

    @Test
    fun `temperature unit setting is persisted and scheduled for cloud sync`() {
        val before = System.currentTimeMillis() / 1000

        interactor.setTemperatureUnit(TemperatureUnit.Fahrenheit)

        val timestamp = slot<Long>()
        verify {
            preferencesRepository.setNetworkSetting(
                NetworkSettingNames.UNIT_TEMPERATURE,
                TemperatureUnit.Fahrenheit.unitCode,
                capture(timestamp),
            )
        }
        assertTrue(timestamp.captured in before..System.currentTimeMillis() / 1000)
        verify { networkApplicationSettings.updateNetworkSetting(NetworkSettingNames.UNIT_TEMPERATURE) }
    }

    @Test
    fun `humidity accuracy updates all dependent cloud settings`() {
        interactor.setHumidityAccuracy(Accuracy.Accuracy1)

        verify {
            preferencesRepository.setNetworkSetting(
                NetworkSettingNames.ACCURACY_HUMIDITY,
                Accuracy.Accuracy1.code.toString(),
                any(),
            )
        }
        verify {
            networkApplicationSettings.updateNetworkSetting(NetworkSettingNames.ACCURACY_HUMIDITY)
            networkApplicationSettings.updateNetworkSetting(NetworkSettingNames.ACCURACY_HUMIDITY_RELATIVE)
            networkApplicationSettings.updateNetworkSetting(NetworkSettingNames.ACCURACY_HUMIDITY_ABSOLUTE)
            networkApplicationSettings.updateNetworkSetting(NetworkSettingNames.ACCURACY_HUMIDITY_DEW_POINT)
        }
    }

    @Test
    fun `setting a routed resolution persists its matching network setting`() {
        interactor.setAccuracy(ResolutionSettingsTarget.Voltage, Accuracy.Accuracy0)

        verify {
            preferencesRepository.setNetworkSetting(
                NetworkSettingNames.ACCURACY_VOLTAGE,
                Accuracy.Accuracy0.code.toString(),
                any(),
            )
        }
        verify { networkApplicationSettings.updateNetworkSetting(NetworkSettingNames.ACCURACY_VOLTAGE) }
    }

    @Test
    fun `unchanged chart and scan preferences are not rewritten`() {
        interactor.setIsShowAllGraphPoint(false)
        interactor.setGraphDrawDots(false)
        interactor.setBackgroundScanMode(BackgroundScanModes.BACKGROUND)
        interactor.setBackgroundScanInterval(15)
        interactor.setGraphViewPeriod(24)

        verify(exactly = 0) { preferencesRepository.setNetworkSetting(any(), any(), any()) }
        verify(exactly = 0) { preferencesRepository.setBackgroundScanMode(any()) }
        verify(exactly = 0) { preferencesRepository.setBackgroundScanInterval(any()) }
        verify(exactly = 0) { preferencesRepository.setGraphViewPeriodHours(any()) }
    }

    @Test
    fun `changed chart display setting is persisted and scheduled for cloud sync`() {
        interactor.setGraphDrawDots(true)

        verify {
            preferencesRepository.setNetworkSetting(
                NetworkSettingNames.CHART_DRAW_DOTS,
                "true",
                any(),
            )
        }
        verify { networkApplicationSettings.updateNetworkSetting(NetworkSettingNames.CHART_DRAW_DOTS) }
    }

    @Test
    fun `cloud settings are shown only for signed in users with synced network sensors`() {
        every { preferencesRepository.signedIn() } returns true
        every { sensorSettingsRepository.getSensorSettings() } returns listOf(
            SensorSettings(networkSensor = true, networkLastSync = java.util.Date()),
        )

        assertTrue(interactor.shouldShowCloudMode())

        every { preferencesRepository.signedIn() } returns false
        assertFalse(interactor.shouldShowCloudMode())
    }

    @Test
    fun `alert toggles store inverse disable flags and queue both settings`() {
        interactor.setEmailAlerts(true)
        interactor.setPushAlerts(false)

        verify {
            preferencesRepository.setNetworkSetting(
                NetworkSettingNames.DISABLE_EMAIL_NOTIFICATIONS,
                "false",
                any(),
            )
            preferencesRepository.setNetworkSetting(
                NetworkSettingNames.DISABLE_PUSH_NOTIFICATIONS,
                "true",
                any(),
            )
        }
        verify {
            networkApplicationSettings.updateNetworkSetting(NetworkSettingNames.DISABLE_EMAIL_NOTIFICATIONS)
            networkApplicationSettings.updateNetworkSetting(NetworkSettingNames.DISABLE_PUSH_NOTIFICATIONS)
        }
    }
}
