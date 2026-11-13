package com.ruuvi.station.settings.ui

import com.ruuvi.station.settings.domain.AppSettingsInteractor
import com.ruuvi.station.settings.domain.ResolutionSettingsTarget
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.UnitType.HumidityUnit
import com.ruuvi.station.units.model.UnitType.PressureUnit
import com.ruuvi.station.units.model.UnitType.TemperatureUnit
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GlobalUnitsAndResolutionViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var appSettingsInteractor: AppSettingsInteractor
    private lateinit var viewModel: GlobalUnitsAndResolutionViewModel

    @Before
    fun setUp() {
        appSettingsInteractor = mockk(relaxed = true)
        every { appSettingsInteractor.getAccuracy(any()) } returns Accuracy.Accuracy2
        viewModel = GlobalUnitsAndResolutionViewModel(appSettingsInteractor)
    }

    @Test
    fun `getAllHumidityUnits filters out DewPoint`() {
        every { appSettingsInteractor.getAllHumidityUnits() } returns listOf(
            HumidityUnit.Relative,
            HumidityUnit.Absolute,
            HumidityUnit.DewPoint
        )

        val units = viewModel.getAllHumidityUnits()
        assertEquals(2, units.size)
        assertFalse(units.contains(HumidityUnit.DewPoint))
    }

    @Test
    fun `setTemperatureUnit delegates to interactor`() {
        viewModel.setTemperatureUnit(TemperatureUnit.Kelvin)
        verify { appSettingsInteractor.setTemperatureUnit(TemperatureUnit.Kelvin) }
    }

    @Test
    fun `setHumidityUnit delegates to interactor`() {
        viewModel.setHumidityUnit(HumidityUnit.Absolute)
        verify { appSettingsInteractor.setHumidityUnit(HumidityUnit.Absolute) }
    }

    @Test
    fun `setPressureUnit delegates to interactor`() {
        viewModel.setPressureUnit(PressureUnit.InchHg)
        verify { appSettingsInteractor.setPressureUnit(PressureUnit.InchHg) }
    }

    @Test
    fun `setAccuracy delegates to interactor`() {
        viewModel.setAccuracy(ResolutionSettingsTarget.Temperature, Accuracy.Accuracy1)
        verify { appSettingsInteractor.setAccuracy(ResolutionSettingsTarget.Temperature, Accuracy.Accuracy1) }
    }
}
