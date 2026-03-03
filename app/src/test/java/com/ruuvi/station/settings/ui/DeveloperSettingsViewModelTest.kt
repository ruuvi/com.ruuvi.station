package com.ruuvi.station.settings.ui

import com.ruuvi.station.app.preferences.PreferencesRepository
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
 * - When dev server setting is changed, RuuviNetworkRepository is reinitialized
 * - Preference values are updated correctly
 */
class DeveloperSettingsViewModelTest {

    private lateinit var mockPreferencesRepository: PreferencesRepository
    private lateinit var mockRuuviNetworkRepository: RuuviNetworkRepository
    private lateinit var mockNetworkTokenRepository: NetworkTokenRepository
    private lateinit var mockRuntimeFeatureFlagProvider: RuntimeFeatureFlagProvider
    private lateinit var viewModel: DeveloperSettingsViewModel

    @Before
    fun setUp() {
        mockPreferencesRepository = mockk(relaxed = true)
        mockRuuviNetworkRepository = mockk(relaxed = true)
        mockNetworkTokenRepository = mockk(relaxed = true)
        mockRuntimeFeatureFlagProvider = mockk(relaxed = true)

        every { mockPreferencesRepository.isDevServerEnabled() } returns false

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
}
