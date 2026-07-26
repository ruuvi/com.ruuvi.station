package com.ruuvi.station.widgets.ui.simpleWidget

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.ruuviTagPreview
import com.ruuvi.station.units.model.UnitType
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.widgets.data.WidgetType
import com.ruuvi.station.widgets.domain.WidgetPreferencesInteractor
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SimpleWidgetConfigureViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val tagRepository = mockk<TagRepository>()
    private val networkInteractor = mockk<RuuviNetworkInteractor>()
    private val widgetPreferencesInteractor = mockk<WidgetPreferencesInteractor>(relaxed = true)
    private val preferencesRepository = mockk<PreferencesRepository>()

    @Before
    fun setUp() {
        every { networkInteractor.signedIn } returns false
        every { preferencesRepository.getBackgroundScanInterval() } returns 5
        every { preferencesRepository.getBackgroundScanMode() } returns BackgroundScanModes.DISABLED
    }

    @Test
    fun `sensor change preserves a measurement supported by both sensors`() {
        val firstSensor = sensor("first", UnitType.HumidityUnit.Relative)
        val secondSensor = sensor("second", UnitType.HumidityUnit.Relative)
        val viewModel = createViewModel(firstSensor, secondSensor)

        viewModel.selectSensor(firstSensor.id)
        viewModel.selectWidgetType(WidgetType.HUMIDITY)
        viewModel.selectSensor(secondSensor.id)

        assertEquals(WidgetType.HUMIDITY, viewModel.widgetType.value)
        assertEquals(true, viewModel.canBeSaved.value)
    }

    @Test
    fun `sensor change replaces an unsupported measurement with temperature`() {
        val airSensor = sensor(
            "air",
            UnitType.AirQuality.AqiIndex,
            UnitType.TemperatureUnit.Celsius,
        )
        val regularSensor = sensor("regular", UnitType.TemperatureUnit.Celsius)
        val viewModel = createViewModel(airSensor, regularSensor)

        viewModel.selectSensor(airSensor.id)
        viewModel.selectWidgetType(WidgetType.AIR_QUALITY)
        viewModel.selectSensor(regularSensor.id)

        assertEquals(WidgetType.TEMPERATURE, viewModel.widgetType.value)
        assertEquals(true, viewModel.canBeSaved.value)
    }

    @Test
    fun `sensor without temperature selects its first supported measurement`() {
        val sensor = sensor(
            "sensor",
            UnitType.SignalStrengthUnit.SignalDbm,
            UnitType.MovementUnit.MovementsCount,
        )
        val viewModel = createViewModel(sensor)

        viewModel.selectSensor(sensor.id)

        assertEquals(WidgetType.MOVEMENT, viewModel.widgetType.value)
        assertEquals(true, viewModel.canBeSaved.value)
    }

    @Test
    fun `unsupported measurement cannot replace a valid selection`() {
        val regularSensor = sensor("regular", UnitType.TemperatureUnit.Celsius)
        val viewModel = createViewModel(regularSensor)

        viewModel.selectSensor(regularSensor.id)
        viewModel.selectWidgetType(WidgetType.AIR_QUALITY)

        assertEquals(WidgetType.TEMPERATURE, viewModel.widgetType.value)
        assertEquals(true, viewModel.canBeSaved.value)
    }

    @Test
    fun `sensor without supported measurements cannot be saved`() {
        val unsupportedSensor = sensor("unsupported")
        val viewModel = createViewModel(unsupportedSensor)

        viewModel.selectSensor(unsupportedSensor.id)
        viewModel.save()

        assertNull(viewModel.widgetType.value)
        assertEquals(false, viewModel.canBeSaved.value)
        verify(exactly = 0) {
            widgetPreferencesInteractor.saveSimpleWidgetSettings(any(), any(), any())
        }
    }

    @Test
    fun `save rejects a measurement that is no longer supported`() {
        val regularSensor = sensor("regular", UnitType.TemperatureUnit.Celsius)
        val viewModel = createViewModel(regularSensor)
        viewModel.selectSensor(regularSensor.id)
        regularSensor.displayOrder = emptyList()
        regularSensor.possibleDisplayOptions = emptyList()

        viewModel.save()

        assertEquals(false, viewModel.canBeSaved.value)
        verify(exactly = 0) {
            widgetPreferencesInteractor.saveSimpleWidgetSettings(any(), any(), any())
        }
    }

    @Test
    fun `restored unsupported measurement is replaced and the fallback is saved`() {
        val appWidgetId = 42
        val regularSensor = sensor("regular", UnitType.TemperatureUnit.Celsius)
        val viewModel = createViewModel(regularSensor)
        every {
            widgetPreferencesInteractor.getSimpleWidgetSensor(appWidgetId)
        } returns regularSensor.id
        every {
            widgetPreferencesInteractor.getSimpleWidgetType(appWidgetId)
        } returns WidgetType.AIR_QUALITY

        viewModel.setWidgetId(appWidgetId)
        viewModel.save()

        assertEquals(WidgetType.TEMPERATURE, viewModel.widgetType.value)
        verify(exactly = 1) {
            widgetPreferencesInteractor.saveSimpleWidgetSettings(
                appWidgetId,
                regularSensor.id,
                WidgetType.TEMPERATURE,
            )
        }
    }

    @Test
    fun `restored supported measurement remains selected`() {
        val appWidgetId = 42
        val airSensor = sensor(
            "air",
            UnitType.AirQuality.AqiIndex,
            UnitType.TemperatureUnit.Celsius,
        )
        val viewModel = createViewModel(airSensor)
        every {
            widgetPreferencesInteractor.getSimpleWidgetSensor(appWidgetId)
        } returns airSensor.id
        every {
            widgetPreferencesInteractor.getSimpleWidgetType(appWidgetId)
        } returns WidgetType.AIR_QUALITY

        viewModel.setWidgetId(appWidgetId)

        assertEquals(WidgetType.AIR_QUALITY, viewModel.widgetType.value)
        assertEquals(true, viewModel.canBeSaved.value)
    }

    @Test
    fun `stale restored sensor cannot be saved`() {
        val appWidgetId = 42
        val viewModel = createViewModel()
        every {
            widgetPreferencesInteractor.getSimpleWidgetSensor(appWidgetId)
        } returns "missing"
        every {
            widgetPreferencesInteractor.getSimpleWidgetType(appWidgetId)
        } returns WidgetType.TEMPERATURE

        viewModel.setWidgetId(appWidgetId)
        viewModel.save()

        assertNull(viewModel.sensorId.value)
        assertEquals(false, viewModel.canBeSaved.value)
        verify(exactly = 0) {
            widgetPreferencesInteractor.saveSimpleWidgetSettings(any(), any(), any())
        }
    }

    private fun createViewModel(vararg sensors: RuuviTag): SimpleWidgetConfigureViewModel {
        every { tagRepository.getFavoriteSensors() } returns sensors.toList()
        return SimpleWidgetConfigureViewModel(
            tagRepository = tagRepository,
            networkInteractor = networkInteractor,
            widgetPreferencesInteractor = widgetPreferencesInteractor,
            preferencesRepository = preferencesRepository,
        )
    }

    private fun sensor(id: String, vararg supportedUnits: UnitType): RuuviTag =
        ruuviTagPreview.copy(
            id = id,
            name = id,
            displayName = id,
            displayOrder = supportedUnits.toList(),
            possibleDisplayOptions = emptyList(),
        )
}
