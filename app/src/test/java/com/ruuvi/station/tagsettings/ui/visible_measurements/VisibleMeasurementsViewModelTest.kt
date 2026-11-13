package com.ruuvi.station.tagsettings.ui.visible_measurements

import com.ruuvi.station.alarm.domain.AlarmsInteractor
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.dashboard.DashboardType
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.image.ImageInteractor
import com.ruuvi.station.tag.domain.VisibleMeasurementsOrderInteractor
import com.ruuvi.station.tag.domain.ruuviTagPreview
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.UnitType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VisibleMeasurementsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state uses cached sensor values without placeholder frame`() = runTest(dispatcher) {
        val possibleUnit = UnitType.TemperatureUnit.Celsius
        val sensor = ruuviTagPreview.copy(
            id = SENSOR_ID,
            defaultDisplayOrder = false,
            possibleDisplayOptions = listOf(possibleUnit),
        )
        val interactor = mockk<TagSettingsInteractor> {
            every { getFavouriteSensorById(SENSOR_ID) } returns sensor
        }
        val preferences = mockk<PreferencesRepository> {
            every { getDashboardType() } returns DashboardType.IMAGE_VIEW
        }
        val unitsConverter = mockk<UnitsConverter> {
            every { getTitleForUnitType(possibleUnit) } returns UNIT_TITLE
        }

        val viewModel = VisibleMeasurementsViewModel(
            sensorId = SENSOR_ID,
            interactor = interactor,
            preferencesRepository = preferences,
            unitsConverter = unitsConverter,
            alarmsInteractor = mockk<AlarmsInteractor>(relaxed = true),
            visibleMeasurementsOrderInteractor = mockk<VisibleMeasurementsOrderInteractor>(relaxed = true),
            sensorSettingsRepository = mockk<SensorSettingsRepository>(relaxed = true),
            imageInteractor = mockk<ImageInteractor>(relaxed = true),
        )

        backgroundScope.launch { viewModel.useDefaultOrder.collect {} }
        backgroundScope.launch { viewModel.possibleOptions.collect {} }
        advanceUntilIdle()

        assertFalse(viewModel.useDefaultOrder.value)
        assertEquals(
            listOf(ListOption(possibleUnit.getCode(), UNIT_TITLE, possibleUnit)),
            viewModel.possibleOptions.value,
        )
    }

    private companion object {
        const val SENSOR_ID = "AA:BB:CC:DD:EE:FF"
        const val UNIT_TITLE = "Temperature"
    }
}
