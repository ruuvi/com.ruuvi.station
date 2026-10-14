package com.ruuvi.station.settings.ui

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.feature.provider.RuntimeFeatureFlagProvider
import com.ruuvi.station.network.domain.NetworkTokenRepository
import com.ruuvi.station.network.domain.RuuviNetworkRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for DeveloperSettingsViewModel.
 *
 * Tests verify that:
 * - When dev server setting is changed, RuuviNetworkRepository is reinitialized
 * - Preference values are updated correctly
 */
class DeveloperSettingsViewModelTest {

    private lateinit var mockPreferencesRepository: PreferencesRepository
    private lateinit var mockRuuviNetworkRepository: RuuviNetworkRepository
    private lateinit var mockNetworkTokenRepository: NetworkTokenRepository
    private lateinit var mockRuntimeFeatureFlagProvider: RuntimeFeatureFlagProvider
    private lateinit var viewModel: DeveloperSettingsViewModel
    private var savedDashboardCardTimeoutSeconds = 60L

    @Before
    fun setUp() {
        mockPreferencesRepository = mockk(relaxed = true)
        mockRuuviNetworkRepository = mockk(relaxed = true)
        mockNetworkTokenRepository = mockk(relaxed = true)
        mockRuntimeFeatureFlagProvider = mockk(relaxed = true)

        every { mockPreferencesRepository.isDevServerEnabled() } returns false
        every { mockPreferencesRepository.getDashboardCardTimeoutSeconds() } answers {
            savedDashboardCardTimeoutSeconds
        }
        every { mockPreferencesRepository.setDashboardCardTimeoutSeconds(any()) } answers {
            savedDashboardCardTimeoutSeconds = firstArg()
        }

        viewModel = DeveloperSettingsViewModel(
            mockPreferencesRepository,
            mockRuuviNetworkRepository,
            mockNetworkTokenRepository,
            mockRuntimeFeatureFlagProvider
        )
    }

    @Test
    fun `setDevServerEnabled reinitializes network repository`() {
        // When - user enables dev server
        viewModel.setDevServerEnabled(true)

        // Then
        verify { mockRuuviNetworkRepository.reinitialize() }
    }

    @Test
    fun `setDevServerEnabled updates preference`() {
        // When
        viewModel.setDevServerEnabled(true)

        // Then
        verify { mockPreferencesRepository.setDevServerEnabled(true) }
    }

    @Test
    fun `dashboard timeout starts with the saved preference`() {
        assertEquals(60L, viewModel.dashboardCardTimeoutSeconds.value)
    }

    @Test
    fun `dashboard timeout accepts the minimum and custom periods`() {
        for (seconds in listOf(1L, 137L, Long.MAX_VALUE)) {
            viewModel.setDashboardCardTimeoutSeconds(seconds)

            verify { mockPreferencesRepository.setDashboardCardTimeoutSeconds(seconds) }
            assertEquals(seconds, viewModel.dashboardCardTimeoutSeconds.value)
        }
    }

    @Test
    fun `dashboard timeout rejects periods below one second without saving`() {
        for (seconds in listOf(0L, -1L)) {
            assertThrows(IllegalArgumentException::class.java) {
                viewModel.setDashboardCardTimeoutSeconds(seconds)
            }
        }

        verify(exactly = 0) { mockPreferencesRepository.setDashboardCardTimeoutSeconds(any()) }
        assertEquals(60L, viewModel.dashboardCardTimeoutSeconds.value)
    }

    @Test
    fun `dashboard timeout input accepts custom whole seconds without preset limits`() {
        for (seconds in listOf(1L, 60L, 137L, Long.MAX_VALUE)) {
            assertEquals(seconds, parseDashboardCardTimeoutSeconds(seconds.toString()))
        }
    }

    @Test
    fun `dashboard timeout input rejects empty invalid and overflowing values`() {
        for (input in listOf("", "0", "-1", "1.5", "abc", "9223372036854775808")) {
            assertNull(input, parseDashboardCardTimeoutSeconds(input))
        }
    }
}
