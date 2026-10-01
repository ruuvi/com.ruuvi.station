package com.ruuvi.station.network.domain

import com.ruuvi.station.app.locale.LocaleInteractor
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.network.data.NetworkTokenInfo
import com.ruuvi.station.network.data.response.GetUserSettingsResponseBody
import com.ruuvi.station.network.data.response.NetworkUserSettings
import com.ruuvi.station.network.data.response.RuuviNetworkResponse
import com.ruuvi.station.units.domain.UnitsConverter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NetworkApplicationSettingsTest {
    private val tokenRepository = mockk<NetworkTokenRepository>()
    private val networkRepository = mockk<RuuviNetworkRepository>()
    private val networkInteractor = mockk<RuuviNetworkInteractor>(relaxed = true)
    private val preferencesRepository = mockk<PreferencesRepository>(relaxed = true)
    private val localeInteractor = mockk<LocaleInteractor>()
    private val interactor = NetworkApplicationSettings(
        tokenRepository = tokenRepository,
        networkRepository = networkRepository,
        networkInteractor = networkInteractor,
        preferencesRepository = preferencesRepository,
        unitsConverter = mockk<UnitsConverter>(),
        localeInteractor = localeInteractor,
    )

    @Before
    fun setUp() {
        every { tokenRepository.getTokenInfo() } returns NetworkTokenInfo(SYNC_USER_EMAIL, TOKEN)
        every { networkInteractor.signedIn } returns true
        every { localeInteractor.getCurrentLocaleLanguage() } returns "fi"
        every { preferencesRepository.getNetworkSetting(any()) } returns "local-value"
        every { preferencesRepository.getNetworkSettingLastUpdated(any()) } returns 0L
    }

    @Test
    fun `signed out users do not fetch or change settings`() = runBlocking {
        every { tokenRepository.getTokenInfo() } returns null

        interactor.updateSettingsFromNetwork()

        coVerify(exactly = 0) { networkRepository.getUserSettings(any()) }
        verify(exactly = 0) { preferencesRepository.setNetworkSetting(any(), any(), any()) }
        verify(exactly = 0) { networkInteractor.updateUserSetting(any(), any(), any()) }
    }

    @Test
    fun `null response leaves local settings and upload queue unchanged`() = runBlocking {
        coEvery { networkRepository.getUserSettings(TOKEN) } returns null

        interactor.updateSettingsFromNetwork()

        verify(exactly = 0) { preferencesRepository.setNetworkSetting(any(), any(), any()) }
        verify(exactly = 0) { networkInteractor.updateUserSetting(any(), any(), any()) }
    }

    @Test
    fun `unsuccessful response does not apply even populated settings`() = runBlocking {
        coEvery { networkRepository.getUserSettings(TOKEN) } returns RuuviNetworkResponse(
            result = RuuviNetworkResponse.errorResult,
            error = "Unavailable",
            data = GetUserSettingsResponseBody(temperatureSettings(timestamp = 200L)),
            code = null,
        )

        interactor.updateSettingsFromNetwork()

        verify(exactly = 0) { preferencesRepository.setNetworkSetting(any(), any(), any()) }
        verify(exactly = 0) { networkInteractor.updateUserSetting(any(), any(), any()) }
    }

    @Test
    fun `successful response without data does not initialize cloud settings`() = runBlocking {
        coEvery { networkRepository.getUserSettings(TOKEN) } returns RuuviNetworkResponse(
            result = RuuviNetworkResponse.successResult,
            error = "",
            data = null,
            code = null,
        )

        interactor.updateSettingsFromNetwork()

        verify(exactly = 0) { preferencesRepository.setNetworkSetting(any(), any(), any()) }
        verify(exactly = 0) { networkInteractor.updateUserSetting(any(), any(), any()) }
    }

    @Test
    fun `newer cloud setting is applied with its timestamp without uploading`() = runBlocking {
        every { preferencesRepository.getNetworkSettingLastUpdated(TEMPERATURE) } returns 100L
        stubSettings(temperatureSettings(timestamp = 200L))

        interactor.updateSettingsFromNetwork()

        verify(exactly = 1) { preferencesRepository.setNetworkSetting(TEMPERATURE, "F", 200L) }
        verify(exactly = 0) { networkInteractor.updateUserSetting(any(), any(), any()) }
    }

    @Test
    fun `newer local setting is uploaded without overwriting local data`() = runBlocking {
        every { preferencesRepository.getNetworkSettingLastUpdated(TEMPERATURE) } returns 300L
        every { preferencesRepository.getNetworkSetting(TEMPERATURE) } returns "C"
        stubSettings(temperatureSettings(timestamp = 200L))

        interactor.updateSettingsFromNetwork()

        verify(exactly = 1) { networkInteractor.updateUserSetting(TEMPERATURE, "C", 300L) }
        verify(exactly = 0) { preferencesRepository.setNetworkSetting(any(), any(), any()) }
    }

    @Test
    fun `equal nonzero timestamps cause no writes in either direction`() = runBlocking {
        every { preferencesRepository.getNetworkSettingLastUpdated(TEMPERATURE) } returns 200L
        stubSettings(temperatureSettings(timestamp = 200L))

        interactor.updateSettingsFromNetwork()

        verify(exactly = 0) { preferencesRepository.setNetworkSetting(any(), any(), any()) }
        verify(exactly = 0) { networkInteractor.updateUserSetting(any(), any(), any()) }
    }

    @Test
    fun `each setting resolves its own conflict independently`() = runBlocking {
        every { preferencesRepository.getNetworkSettingLastUpdated(TEMPERATURE) } returns 100L
        every { preferencesRepository.getNetworkSettingLastUpdated(NetworkSettingNames.SENSOR_ORDER) } returns 300L
        every { preferencesRepository.getNetworkSettingLastUpdated(NetworkSettingNames.PROFILE_LANGUAGE_CODE) } returns 200L
        stubSettings(temperatureSettings(timestamp = 200L).copy(
            SENSOR_ORDER = "cloud-order",
            SENSOR_ORDER_lastUpdated = 100L,
            PROFILE_LANGUAGE_CODE = "de",
            PROFILE_LANGUAGE_CODE_lastUpdated = 200L,
        ))

        interactor.updateSettingsFromNetwork()

        verify(exactly = 1) { preferencesRepository.setNetworkSetting(TEMPERATURE, "F", 200L) }
        verify(exactly = 1) { preferencesRepository.setNetworkSetting(any(), any(), any()) }
        verify(exactly = 1) {
            networkInteractor.updateUserSetting(NetworkSettingNames.SENSOR_ORDER, "local-value", 300L)
        }
        verify(exactly = 1) { networkInteractor.updateUserSetting(any(), any(), any()) }
    }

    @Test
    fun `legacy cloud value without timestamps is applied with a current epoch second`() = runBlocking {
        stubSettings(temperatureSettings(timestamp = null))
        val before = System.currentTimeMillis() / 1000

        interactor.updateSettingsFromNetwork()

        val timestamp = slot<Long>()
        verify(exactly = 1) { preferencesRepository.setNetworkSetting(TEMPERATURE, "F", capture(timestamp)) }
        assertTrue(timestamp.captured in before..System.currentTimeMillis() / 1000)
        verify(exactly = 0) { networkInteractor.updateUserSetting(any(), any(), any()) }
    }

    @Test
    fun `missing cloud setting uploads only a locally timestamped value`() = runBlocking {
        every { preferencesRepository.getNetworkSettingLastUpdated(NetworkSettingNames.SENSOR_ORDER) } returns 300L
        every { preferencesRepository.getNetworkSetting(NetworkSettingNames.SENSOR_ORDER) } returns "[\"sensor\"]"
        stubSettings(temperatureSettings(timestamp = 200L))

        interactor.updateSettingsFromNetwork()

        verify(exactly = 1) {
            networkInteractor.updateUserSetting(NetworkSettingNames.SENSOR_ORDER, "[\"sensor\"]", 300L)
        }
        verify(exactly = 1) { networkInteractor.updateUserSetting(any(), any(), any()) }
    }

    @Test
    fun `empty cloud initializes every tracked setting and uses the actual locale`() = runBlocking {
        stubSettings(emptySettings())
        val before = System.currentTimeMillis() / 1000

        interactor.updateSettingsFromNetwork()

        val after = System.currentTimeMillis() / 1000
        NetworkSettingNames.TRACKED_SETTINGS.forEach { name ->
            val expectedValue = if (name == NetworkSettingNames.PROFILE_LANGUAGE_CODE) "fi" else "local-value"
            val timestamp = slot<Long>()
            verify(exactly = 1) {
                preferencesRepository.setNetworkSetting(name, expectedValue, capture(timestamp))
            }
            assertTrue(timestamp.captured in before..after)
            verify(exactly = 1) {
                networkInteractor.updateUserSetting(name, expectedValue, timestamp.captured)
            }
        }
        verify(exactly = NetworkSettingNames.TRACKED_SETTINGS.size) {
            networkInteractor.updateUserSetting(any(), any(), any())
        }
    }

    @Test
    fun `empty cloud preserves existing local timestamps`() = runBlocking {
        every { preferencesRepository.getNetworkSettingLastUpdated(any()) } returns 100L
        stubSettings(emptySettings())

        interactor.updateSettingsFromNetwork()

        NetworkSettingNames.TRACKED_SETTINGS.forEach { name ->
            val expectedValue = if (name == NetworkSettingNames.PROFILE_LANGUAGE_CODE) "fi" else "local-value"
            verify(exactly = 1) { networkInteractor.updateUserSetting(name, expectedValue, 100L) }
        }
        verify(exactly = 0) { preferencesRepository.setNetworkSetting(any(), any(), any()) }
    }

    @Test
    fun `null local value is not uploaded over a cloud value`() = runBlocking {
        every { preferencesRepository.getNetworkSettingLastUpdated(TEMPERATURE) } returns 300L
        every { preferencesRepository.getNetworkSetting(TEMPERATURE) } returns null
        stubSettings(temperatureSettings(timestamp = 200L))

        interactor.updateSettingsFromNetwork()

        verify(exactly = 0) { networkInteractor.updateUserSetting(any(), any(), any()) }
        verify(exactly = 0) { preferencesRepository.setNetworkSetting(any(), any(), any()) }
    }

    @Test
    fun `signing out before conflict resolution prevents uploading local values`() = runBlocking {
        every { networkInteractor.signedIn } returns false
        every { preferencesRepository.getNetworkSettingLastUpdated(TEMPERATURE) } returns 300L
        stubSettings(temperatureSettings(timestamp = 200L))

        interactor.updateSettingsFromNetwork()

        verify(exactly = 0) { networkInteractor.updateUserSetting(any(), any(), any()) }
    }

    @Test
    fun `newer profile language comes from locale rather than a stale preference`() = runBlocking {
        val name = NetworkSettingNames.PROFILE_LANGUAGE_CODE
        every { preferencesRepository.getNetworkSettingLastUpdated(name) } returns 300L
        every { preferencesRepository.getNetworkSetting(name) } returns "en"
        stubSettings(emptySettings().copy(PROFILE_LANGUAGE_CODE = "de", PROFILE_LANGUAGE_CODE_lastUpdated = 200L))

        interactor.updateSettingsFromNetwork()

        verify(exactly = 1) { networkInteractor.updateUserSetting(name, "fi", 300L) }
        verify(exactly = 0) { preferencesRepository.setNetworkSetting(any(), any(), any()) }
    }

    @Test
    fun `offline setting edit is timestamped locally for a later sync`() {
        every { networkInteractor.signedIn } returns false
        val before = System.currentTimeMillis() / 1000

        interactor.updateNetworkSetting(TEMPERATURE)

        val timestamp = slot<Long>()
        verify(exactly = 1) {
            preferencesRepository.setNetworkSetting(TEMPERATURE, "local-value", capture(timestamp))
        }
        assertTrue(timestamp.captured in before..System.currentTimeMillis() / 1000)
        verify(exactly = 0) { networkInteractor.updateUserSetting(any(), any(), any()) }
    }

    @Test
    fun `online setting edit uses the same local and queued timestamps`() {
        interactor.updateNetworkSetting(TEMPERATURE)

        val localTimestamp = slot<Long>()
        val queuedTimestamp = slot<Long>()
        verify { preferencesRepository.setNetworkSetting(TEMPERATURE, "local-value", capture(localTimestamp)) }
        verify { networkInteractor.updateUserSetting(TEMPERATURE, "local-value", capture(queuedTimestamp)) }
        assertEquals(localTimestamp.captured, queuedTimestamp.captured)
    }

    @Test
    fun `profile language edit persists and queues the current locale`() {
        interactor.updateProfileLanguage()

        val timestamp = slot<Long>()
        verify {
            preferencesRepository.setNetworkSetting(NetworkSettingNames.PROFILE_LANGUAGE_CODE, "fi", capture(timestamp))
        }
        verify {
            networkInteractor.updateUserSetting(NetworkSettingNames.PROFILE_LANGUAGE_CODE, "fi", timestamp.captured)
        }
    }

    @Test
    fun `sensor ordering edit persists and queues the same value`() {
        val name = NetworkSettingNames.SENSOR_ORDER
        every { preferencesRepository.getNetworkSetting(name) } returns "[\"first\",\"second\"]"

        interactor.updateSensorsOrder()

        val timestamp = slot<Long>()
        verify { preferencesRepository.setNetworkSetting(name, "[\"first\",\"second\"]", capture(timestamp)) }
        verify { networkInteractor.updateUserSetting(name, "[\"first\",\"second\"]", timestamp.captured) }
    }

    private fun stubSettings(settings: NetworkUserSettings) {
        coEvery { networkRepository.getUserSettings(TOKEN) } returns RuuviNetworkResponse(
            result = RuuviNetworkResponse.successResult,
            error = "",
            data = GetUserSettingsResponseBody(settings),
            code = null,
        )
    }

    private fun temperatureSettings(timestamp: Long?) = emptySettings().copy(
        UNIT_TEMPERATURE = "F",
        UNIT_TEMPERATURE_lastUpdated = timestamp,
    )

    private fun emptySettings() = NetworkUserSettings(
        UNIT_TEMPERATURE = null,
        UNIT_TEMPERATURE_lastUpdated = null,
        UNIT_HUMIDITY = null,
        UNIT_HUMIDITY_lastUpdated = null,
        UNIT_PRESSURE = null,
        UNIT_PRESSURE_lastUpdated = null,
        ACCURACY_TEMPERATURE = null,
        ACCURACY_TEMPERATURE_lastUpdated = null,
        ACCURACY_HUMIDITY = null,
        ACCURACY_HUMIDITY_lastUpdated = null,
        ACCURACY_HUMIDITY_RELATIVE = null,
        ACCURACY_HUMIDITY_RELATIVE_lastUpdated = null,
        ACCURACY_HUMIDITY_ABSOLUTE = null,
        ACCURACY_HUMIDITY_ABSOLUTE_lastUpdated = null,
        ACCURACY_HUMIDITY_DEW_POINT = null,
        ACCURACY_HUMIDITY_DEW_POINT_lastUpdated = null,
        ACCURACY_PRESSURE = null,
        ACCURACY_PRESSURE_lastUpdated = null,
        ACCURACY_PM = null,
        ACCURACY_PM_lastUpdated = null,
        ACCURACY_ACCELERATION = null,
        ACCURACY_ACCELERATION_lastUpdated = null,
        ACCURACY_VOLTAGE = null,
        ACCURACY_VOLTAGE_lastUpdated = null,
        CLOUD_MODE_ENABLED = null,
        CLOUD_MODE_ENABLED_lastUpdated = null,
        CHART_SHOW_ALL_POINTS = null,
        CHART_SHOW_ALL_POINTS_lastUpdated = null,
        CHART_DRAW_DOTS = null,
        CHART_DRAW_DOTS_lastUpdated = null,
        DASHBOARD_TYPE = null,
        DASHBOARD_TYPE_lastUpdated = null,
        DASHBOARD_TAP_ACTION = null,
        DASHBOARD_TAP_ACTION_lastUpdated = null,
        PROFILE_LANGUAGE_CODE = null,
        PROFILE_LANGUAGE_CODE_lastUpdated = null,
        SENSOR_ORDER = null,
        SENSOR_ORDER_lastUpdated = null,
        DISABLE_EMAIL_NOTIFICATIONS = null,
        DISABLE_EMAIL_NOTIFICATIONS_lastUpdated = null,
        DISABLE_PUSH_NOTIFICATIONS = null,
        DISABLE_PUSH_NOTIFICATIONS_lastUpdated = null,
        DISABLE_TELEGRAM_NOTIFICATIONS = null,
        DISABLE_TELEGRAM_NOTIFICATIONS_lastUpdated = null,
        TIPS_ALLOWED = null,
        TIPS_ALLOWED_lastUpdated = null,
    )

    companion object {
        private const val TOKEN = "test-token"
        private const val TEMPERATURE = NetworkSettingNames.UNIT_TEMPERATURE
    }
}
