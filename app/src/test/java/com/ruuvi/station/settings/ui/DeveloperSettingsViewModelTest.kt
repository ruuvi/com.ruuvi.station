package com.ruuvi.station.settings.ui

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.dfu.domain.FirmwareRepository
import com.ruuvi.station.feature.data.Feature
import com.ruuvi.station.feature.provider.RuntimeFeatureFlagProvider
import com.ruuvi.station.network.domain.NetworkTokenRepository
import com.ruuvi.station.network.domain.RuuviNetworkRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for DeveloperSettingsViewModel.
 *
 * Tests verify that:
 * - When dev server setting is changed, all repositories that depend on server URL are reinitialized
 * - This includes both RuuviNetworkRepository (for general network calls) and 
 *   FirmwareRepository (for Air firmware updates)
 */
class DeveloperSettingsViewModelTest {

    private lateinit var mockPreferencesRepository: PreferencesRepository
    private lateinit var mockRuuviNetworkRepository: RuuviNetworkRepository
    private lateinit var mockNetworkTokenRepository: NetworkTokenRepository
    private lateinit var mockRuntimeFeatureFlagProvider: RuntimeFeatureFlagProvider
    private lateinit var mockFirmwareRepository: FirmwareRepository
    private lateinit var viewModel: DeveloperSettingsViewModel

    @Before
    fun setUp() {
        mockPreferencesRepository = mockk(relaxed = true)
        mockRuuviNetworkRepository = mockk(relaxed = true)
        mockNetworkTokenRepository = mockk(relaxed = true)
        mockRuntimeFeatureFlagProvider = mockk(relaxed = true)
        mockFirmwareRepository = mockk(relaxed = true)

        every { mockPreferencesRepository.isDevServerEnabled() } returns false

        viewModel = DeveloperSettingsViewModel(
            mockPreferencesRepository,
            mockRuuviNetworkRepository,
            mockNetworkTokenRepository,
            mockRuntimeFeatureFlagProvider,
            mockFirmwareRepository
        )
    }

    @Test
    fun `setDevServerEnabled reinitializes all server-dependent repositories`() {
        // When - user enables dev server
        viewModel.setDevServerEnabled(true)

        // Then - both repositories should be reinitialized to use new server URL
        // RuuviNetworkRepository for general network calls
        // FirmwareRepository for Air firmware updates
        verify { mockRuuviNetworkRepository.reinitialize() }
        verify { mockFirmwareRepository.reinitialize() }
    }

    @Test
    fun `setDevServerEnabled updates preference`() {
        // When
        viewModel.setDevServerEnabled(true)

        // Then
        verify { mockPreferencesRepository.setDevServerEnabled(true) }
    }
}
