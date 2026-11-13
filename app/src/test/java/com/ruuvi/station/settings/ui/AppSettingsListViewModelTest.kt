package com.ruuvi.station.settings.ui

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.feature.data.FeatureFlag
import com.ruuvi.station.feature.domain.RuntimeBehavior
import com.ruuvi.station.settings.domain.AppSettingsInteractor
import com.ruuvi.station.units.model.UnitType.HumidityUnit
import com.ruuvi.station.units.model.UnitType.PressureUnit
import com.ruuvi.station.units.model.UnitType.TemperatureUnit
import com.ruuvi.station.util.BackgroundScanModes
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppSettingsListViewModelTest {

    private lateinit var interactor: AppSettingsInteractor
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var runtimeBehavior: RuntimeBehavior
    private lateinit var viewModel: AppSettingsListViewModel

    @Before
    fun setUp() {
        interactor = mockk(relaxed = true)
        preferencesRepository = mockk(relaxed = true)
        runtimeBehavior = mockk(relaxed = true)

        viewModel = AppSettingsListViewModel(
            interactor,
            preferencesRepository,
            runtimeBehavior
        )
    }

    @Test
    fun `getBackgroundScanMode returns interactor scan mode`() {
        every { interactor.getBackgroundScanMode() } returns BackgroundScanModes.BACKGROUND
        assertEquals(BackgroundScanModes.BACKGROUND, viewModel.getBackgroundScanMode())
    }

    @Test
    fun `getBackgroundScanInterval returns interactor interval`() {
        every { interactor.getBackgroundScanInterval() } returns 600
        assertEquals(600, viewModel.getBackgroundScanInterval())
    }

    @Test
    fun `getGatewayUrl returns data forwarding url`() {
        every { interactor.getDataForwardingUrl() } returns "https://example.com/api"
        assertEquals("https://example.com/api", viewModel.getGatewayUrl())
    }

    @Test
    fun `units methods return interactor units`() {
        every { interactor.getTemperatureUnit() } returns TemperatureUnit.Fahrenheit
        every { interactor.getHumidityUnit() } returns HumidityUnit.Absolute
        every { interactor.getPressureUnit() } returns PressureUnit.MmHg

        assertEquals(TemperatureUnit.Fahrenheit, viewModel.getTemperatureUnit())
        assertEquals(HumidityUnit.Absolute, viewModel.getHumidityUnit())
        assertEquals(PressureUnit.MmHg, viewModel.getPressureUnit())
    }

    @Test
    fun `cloud mode settings delegate to interactor`() {
        every { interactor.isCloudModeEnabled() } returns true
        every { interactor.shouldShowCloudMode() } returns true

        assertTrue(viewModel.isCloudModeEnabled())
        assertTrue(viewModel.shouldShowCloudMode())

        viewModel.setIsCloudModeEnabled(false)
        verify { interactor.setIsCloudModeEnabled(false) }
    }

    @Test
    fun `shouldShowGlobalUnitsSettings delegates to runtimeBehavior`() {
        every { runtimeBehavior.isFeatureEnabled(FeatureFlag.GLOBAL_UNITS_SETTINGS) } returns true
        assertTrue(viewModel.shouldShowGlobalUnitsSettings())
        verify { runtimeBehavior.isFeatureEnabled(FeatureFlag.GLOBAL_UNITS_SETTINGS) }
    }
}
