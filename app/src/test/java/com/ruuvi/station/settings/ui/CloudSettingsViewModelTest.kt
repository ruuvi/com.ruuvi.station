package com.ruuvi.station.settings.ui

import com.ruuvi.station.settings.domain.AppSettingsInteractor
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CloudSettingsViewModelTest {

    private lateinit var interactor: AppSettingsInteractor
    private lateinit var viewModel: CloudSettingsViewModel

    @Before
    fun setUp() {
        interactor = mockk(relaxed = true)
        every { interactor.isCloudModeEnabled() } returns false
        viewModel = CloudSettingsViewModel(interactor)
    }

    @Test
    fun `initial value reflects interactor state`() {
        assertFalse(viewModel.cloudModeEnabled.value)
    }

    @Test
    fun `setIsCloudModeEnabled updates interactor and state flow`() {
        every { interactor.isCloudModeEnabled() } returns true
        viewModel.setIsCloudModeEnabled(true)

        verify { interactor.setIsCloudModeEnabled(true) }
        assertTrue(viewModel.cloudModeEnabled.value)
    }
}
