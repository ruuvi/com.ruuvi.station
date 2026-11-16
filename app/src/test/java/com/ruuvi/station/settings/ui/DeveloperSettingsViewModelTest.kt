package com.ruuvi.station.settings.ui

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.feature.provider.RuntimeFeatureFlagProvider
import com.ruuvi.station.network.domain.NetworkTokenRepository
import com.ruuvi.station.network.domain.RuuviNetworkRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
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
    fun `dashboard timeout counter moves through readable preset periods`() {
        assertEquals(1L, previousDashboardCardTimeout(1L))
        assertEquals(10L, nextDashboardCardTimeout(1L))
        assertEquals(1L, previousDashboardCardTimeout(10L))
        assertEquals(30L, nextDashboardCardTimeout(10L))
        assertEquals(60L, previousDashboardCardTimeout(137L))
        assertEquals(300L, nextDashboardCardTimeout(137L))
        assertEquals(1_800L, previousDashboardCardTimeout(3_600L))
        assertEquals(3_600L, nextDashboardCardTimeout(3_600L))
    }

    @Test
    fun `dashboard timeout counter formats seconds and minutes`() {
        assertEquals("1s", formatDashboardCardTimeout(1L))
        assertEquals("10s", formatDashboardCardTimeout(10L))
        assertEquals("1 min", formatDashboardCardTimeout(60L))
        assertEquals("60 min", formatDashboardCardTimeout(3_600L))
    }
}
