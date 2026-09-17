package com.ruuvi.station.tag.domain

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.raizlabs.android.dbflow.config.FlowManager
import com.ruuvi.station.R
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.database.tables.FavouriteSensorQuery
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.graph.model.mouldRiskHistory
import com.ruuvi.station.graph.model.preciseValue
import com.ruuvi.station.graph.model.startsSegment
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tagdetails.ui.SensorCardViewModel
import com.ruuvi.station.tagdetails.ui.SensorCardViewModelArguments
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor
import com.ruuvi.station.tagdetails.domain.TagDetailsInteractor
import com.ruuvi.station.units.domain.MovementConverter
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.domain.aqi.AQI
import com.ruuvi.station.units.domain.score.QualityCalculator
import com.ruuvi.station.units.domain.score.QualityRange
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.UnitType
import com.ruuvi.station.units.model.mouldRiskPresentation
import com.ruuvi.station.units.model.toIndexPresentation
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.mockito.kotlin.*
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.Date

class MouldRiskIntegrationTest {
    private val preferences: PreferencesRepository = mock()
    private val settings: SensorSettingsRepository = mock()
    private val context: Context = mock()
    private lateinit var units: UnitsConverter
    private lateinit var order: VisibleMeasurementsOrderInteractor
    private lateinit var converter: TagConverter
    private lateinit var flowManager: MockedStatic<FlowManager>

    @Before fun setUp() {
        // FavouriteSensorQuery builds its SQL column list during class initialization.
        flowManager = mockStatic(FlowManager::class.java)
        flowManager.`when`<String> { FlowManager.getTableName(SensorSettings::class.java) }.thenReturn("SensorSettings")
        flowManager.`when`<String> { FlowManager.getTableName(RuuviTagEntity::class.java) }.thenReturn("RuuviTagEntity")
        whenever(context.getString(any<Int>())).thenReturn("")
        whenever(context.getString(any<Int>(), any(), any())).thenReturn("formatted")
        whenever(context.getString(any<Int>(), any())).thenReturn("formatted")
        whenever(context.getText(any<Int>())).thenReturn("")
        whenever(preferences.getTemperatureUnit()).thenReturn(UnitType.TemperatureUnit.Celsius)
        whenever(preferences.getHumidityUnit()).thenReturn(UnitType.HumidityUnit.Relative)
        whenever(preferences.getPressureUnit()).thenReturn(UnitType.PressureUnit.HectoPascal)
        whenever(preferences.getTemperatureAccuracy()).thenReturn(Accuracy.Accuracy2)
        whenever(preferences.getRelativeHumidityAccuracy()).thenReturn(Accuracy.Accuracy2)
        whenever(preferences.getAbsoluteHumidityAccuracy()).thenReturn(Accuracy.Accuracy2)
        whenever(preferences.getDewPointAccuracy()).thenReturn(Accuracy.Accuracy2)
        whenever(preferences.getVoltageAccuracy()).thenReturn(Accuracy.Accuracy2)
        units = UnitsConverter(context, preferences)
        order = VisibleMeasurementsOrderInteractor(preferences, settings)
        converter = TagConverter(units, MovementConverter(context), order)
    }

    @After fun tearDown() { flowManager.close() }

    @Test fun `both sensor families require both input fields and remain opt in`() {
        for (format in listOf(5, 0xE1)) {
            for (temperature in listOf(null, 20.0)) for (humidity in listOf(null, 80.0)) {
                val entity = FavouriteSensorQuery(dataFormat = format, temperature = temperature, humidity = humidity)
                val possible = order.getPossibleDisplayOptions(entity)
                assertEquals(temperature != null && humidity != null, UnitType.MouldRisk.Index in possible)
                assertFalse(UnitType.MouldRisk.Index in order.getDefaultDisplayOrder(entity))
                if (UnitType.MouldRisk.Index in possible) {
                    assertEquals(possible.indexOf(UnitType.HumidityUnit.DewPoint) + 1, possible.indexOf(UnitType.MouldRisk.Index))
                }
                assertEquals(temperature != null && humidity != null, UnitType.MouldRisk.Index in
                    order.getPossibleDisplayOptions(RuuviTagEntity(dataFormat = format, temperature = temperature, humidity = humidity)))
            }
        }
    }

    @Test fun `calibrated live and historical values agree independently of visible input units`() {
        val entity = FavouriteSensorQuery(id = "sensor", latestId = "sensor", temperature = 18.0, humidity = 75.0,
            temperatureOffset = 2.0, humidityOffset = 5.0, defaultDisplayOrder = false, displayOrder = "[\"MOULD_INDEX\"]")
        val historyRepository: SensorHistoryRepository = mock()
        whenever(preferences.isShowAllGraphPoint()).thenReturn(true)
        whenever(settings.getSensorSettings("sensor")).thenReturn(SensorSettings(temperatureOffset = 2.0, humidityOffset = 5.0))
        whenever(historyRepository.getHistory("sensor", 48)).thenReturn(listOf(TagSensorReading(temperature = 18.0, humidity = 75.0)))
        val details = TagDetailsInteractor(mock(), historyRepository, settings, units, preferences)
        val historical = mouldRiskHistory(details.getTagReadings("sensor", 48)).segments.single().values.single()
        for (unit in UnitType.TemperatureUnit.getUnits()) {
            whenever(preferences.getTemperatureUnit()).thenReturn(unit)
            val sensor = converter.fromDatabase(entity)
            val value = sensor.valuesToDisplay.single()
            assertEquals(UnitType.MouldRisk.Index, value.unitType)
            assertEquals(50.0, value.value, 0.0)
            assertEquals(historical, value.value, 0.0)
            assertEquals("50/100", value.valueWithoutUnit)
            assertEquals(20.0, sensor.latestMeasurement!!.temperature!!.original, 0.0)
            assertEquals(80.0, sensor.latestMeasurement!!.humidity!!.original, 0.0)
        }
    }

    @Test fun `missing inputs preserve custom position and produce neutral unavailable presentation`() {
        val entity = FavouriteSensorQuery(temperature = null, humidity = 80.0, defaultDisplayOrder = false,
            displayOrder = "[\"MOULD_INDEX\",\"HUMIDITY_0\"]")
        val sensor = converter.fromDatabase(entity)
        assertEquals(UnitType.MouldRisk.Index, sensor.displayOrder.first())
        val value = sensor.valuesToDisplay.first()
        assertFalse(value.isAvailable)
        assertEquals("— /100", value.valueWithoutUnit)
        assertEquals(R.string.mould_risk_missing_input, value.unavailableReason)
        assertNull(QualityCalculator.calc(value))
        assertNull(value.mouldRiskPresentation().score)
        assertEquals(Color.Gray, value.mouldRiskPresentation().color)
        assertEquals(QualityRange.MouldVeryLow, QualityCalculator.calc(units.getMouldRiskEnvironmentValue(20.0, 40.0)))
    }

    @Test fun `saved and synced measurement codes round trip without introducing alarms or defaults`() {
        assertEquals("MOULD_INDEX", UnitType.MouldRisk.Index.getCode())
        val json = "[\"HUMIDITY_0\",\"MOULD_INDEX\",\"TEMPERATURE_F\"]"
        val expected = listOf(UnitType.HumidityUnit.Relative, UnitType.MouldRisk.Index, UnitType.TemperatureUnit.Fahrenheit)
        assertEquals(expected, order.getUserDefinedOrder(json, emptyList()))
        assertEquals(expected, VisibleMeasurementsOrderInteractor(preferences, settings).getUserDefinedOrder(json, emptyList()))
        assertNull(UnitType.MouldRisk.Index.alarmType)
        assertNull(UnitType.getByCode("MOULD_UNKNOWN"))
    }

    @Test fun `shared index presentation preserves air quality scores labels and colours`() {
        for (aqi in listOf(AQI.getAQI(null, null), AQI.getAQI(0.0, 420), AQI.getAQI(60.0, 2300))) {
            val presentation = aqi.toIndexPresentation()
            assertEquals(aqi.score, presentation.score)
            assertEquals(aqi.scoreString, presentation.scoreString)
            assertEquals(aqi.color, presentation.color)
            assertEquals(aqi.descriptionRes, presentation.description)
            assertEquals(100, presentation.maximum)
        }
    }

    @Test fun `mould presentation maps all five categories to the agreed colours and labels`() {
        val cases = listOf(
            Triple(60.0, Color(0xFF4BC8B9), R.string.mould_risk_very_low),
            Triple(65.0, Color(0xFF96CC48), R.string.mould_risk_low),
            Triple(70.0, Color(0xFFF7E13E), R.string.mould_risk_elevated),
            Triple(80.0, Color(0xFFF79C21), R.string.mould_risk_high),
            Triple(90.0, Color(0xFFED5021), R.string.mould_risk_very_high)
        )
        cases.forEach { (rh, color, label) ->
            val presentation = units.getMouldRiskEnvironmentValue(20.0, rh).mouldRiskPresentation()
            assertEquals(color, presentation.color)
            assertEquals(label, presentation.description)
        }
    }

    @Test fun `invalid live inputs keep the selected measurement unavailable`() {
        for ((temperature, humidity) in listOf(Double.NaN to 80.0, 20.0 to Double.NaN, 50.0 to 80.0, 20.0 to 101.0)) {
            val sensor = converter.fromDatabase(FavouriteSensorQuery(id = "sensor", latestId = "sensor",
                temperature = temperature, humidity = humidity, defaultDisplayOrder = false,
                displayOrder = "[\"MOULD_INDEX\"]"))
            assertFalse(sensor.valuesToDisplay.single().isAvailable)
            assertNull(sensor.valuesToDisplay.single().mouldRiskPresentation().score)
        }
    }

    @Test fun `both history renderers preserve calibrated scores and gaps for all sampling and display preferences`() = runBlocking<Unit> {
        val readings = listOf(
            TagSensorReading(createdAt = Date(0), temperature = 8.0, humidity = 78.0),
            TagSensorReading(createdAt = Date(1000), temperature = 8.0, humidity = null),
            TagSensorReading(createdAt = Date(2000), temperature = 8.0, humidity = 78.0),
            TagSensorReading(createdAt = Date(3000), temperature = 8.0, humidity = 83.0)
        )
        val historyRepository: SensorHistoryRepository = mock()
        val tagRepository: TagRepository = mock()
        whenever(preferences.getGraphViewPeriodHours()).thenReturn(48)
        whenever(preferences.getGraphPointInterval()).thenReturn(5)
        whenever(settings.getSensorSettings("sensor")).thenReturn(SensorSettings(temperatureOffset = 2.0, humidityOffset = 2.0))
        whenever(historyRepository.getHistory("sensor", 48)).thenReturn(readings)
        whenever(historyRepository.getCompositeHistory("sensor", 48, 5)).thenReturn(readings)
        val entity = FavouriteSensorQuery(id = "sensor", latestId = "sensor", temperature = 8.0, humidity = 83.0,
            temperatureOffset = 2.0, humidityOffset = 2.0, defaultDisplayOrder = false, displayOrder = "[\"MOULD_INDEX\"]")
        val details = TagDetailsInteractor(tagRepository, historyRepository, settings, units, preferences)
        val model = SensorCardViewModel(SensorCardViewModelArguments(), mock(), details, mock(), mock(),
            preferences, mock(), historyRepository, mock(), mock(), mock(), mock(), units)
        for (allPoints in listOf(true, false)) {
            whenever(preferences.isShowAllGraphPoint()).thenReturn(allPoints)
            for (unit in UnitType.TemperatureUnit.getUnits()) {
                whenever(preferences.getTemperatureUnit()).thenReturn(unit)
                val sensor = converter.fromDatabase(entity)
                whenever(tagRepository.getFavoriteSensorById("sensor")).thenReturn(sensor)
                val popup = withTimeout(5000) { model.getChartData("sensor", UnitType.MouldRisk.Index, 48).first() }
                val fullGraph = withTimeout(5000) { model.historyUpdater("sensor").first() }.single()
                val entries = fullGraph.data!!
                assertEquals(popup.segments.flatMap { it.values }, entries.map { it.preciseValue })
                assertEquals(listOf(true, true, false), entries.map { it.startsSegment })
                assertEquals(listOf(0f, 2000f, 3000f), entries.map { it.x })
                assertEquals(sensor.valuesToDisplay.single().value, entries.last().preciseValue, 0.0)
                assertEquals(0.0, popup.minValue, 0.0)
                assertEquals(100.0, popup.maxValue, 0.0)
                assertNull(fullGraph.limits)
                assertTrue(fullGraph.latestValueAvailable)
            }
        }
        verify(historyRepository, atLeastOnce()).getHistory("sensor", 48)
        verify(historyRepository, atLeastOnce()).getCompositeHistory("sensor", 48, 5)
    }

    @Test fun `display order sync sends the mould code unchanged through the existing settings field`() {
        val network: RuuviNetworkInteractor = mock()
        whenever(settings.getSensorSettings("sensor")).thenReturn(SensorSettings(networkSensor = true))
        val interactor = TagSettingsInteractor(mock(), preferences, settings, network, mock())
        val json = "[\"HUMIDITY_0\",\"MOULD_INDEX\",\"TEMPERATURE_F\"]"
        interactor.newDisplayOrder("sensor", json)
        val timestamp = argumentCaptor<Long>()
        verify(settings).newDisplayOrder(eq("sensor"), eq(json), timestamp.capture())
        verify(network).updateSensorSetting("sensor", "displayOrder", json, timestamp.firstValue)
    }

    @Test fun `unavailable latest history never reuses an old score or drops the selected graph`() = runBlocking<Unit> {
        val details: TagDetailsInteractor = mock()
        val sensor = converter.fromDatabase(FavouriteSensorQuery(id = "sensor", defaultDisplayOrder = false,
            displayOrder = "[\"MOULD_INDEX\"]"))
        whenever(details.getTagById("sensor")).thenReturn(sensor)
        val model = SensorCardViewModel(SensorCardViewModelArguments(), mock(), details, mock(), mock(),
            preferences, mock(), mock(), mock(), mock(), mock(), mock(), units)
        val unavailable = TagSensorReading(createdAt = Date(1000), temperature = 20.0, humidity = null)
        for (readings in listOf(
            listOf(TagSensorReading(createdAt = Date(0), temperature = 20.0, humidity = 60.0), unavailable),
            listOf(unavailable)
        )) {
            whenever(details.getTagReadings("sensor")).thenReturn(readings)
            val chart = withTimeout(5000) { model.historyUpdater("sensor").first() }.single()
            assertEquals(UnitType.MouldRisk.Index, chart.unitType)
            assertFalse(chart.latestValueAvailable)
            assertEquals(readings.size - 1, chart.data!!.size)
        }
    }
}
