@file:Suppress("UsingMaterialAndMaterial3Libraries")

package com.ruuvi.station.tagdetails.ui

import android.app.Application
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.tag.domain.ruuviTagPreview
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.units.model.UnitType
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "en-rUS")
class SensorCardComposeTest {
    @get:Rule
    val compose = createComposeRule()

    private val context: Application
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `footer renders no data state without a measurement`() {
        compose.setContent {
            RuuviTheme {
                SensorCardBottom(
                    sensor = ruuviTagPreview.copy(latestMeasurement = null),
                    staleTimeoutSeconds = 60,
                    useDarkBackground = true,
                )
            }
        }

        compose.onNodeWithText(context.getString(R.string.no_data_10_days)).assertIsDisplayed()
        compose.onNodeWithContentDescription(
            context.getString(R.string.sensor_measurements_outdated),
        ).assertDoesNotExist()
    }

    @Test
    fun `stale footer shows snackbar only after warning icon click`() {
        var clickWarnings = 0
        compose.setContent {
            RuuviTheme {
                SensorCardBottom(
                    sensor = ruuviTagPreview.copy(
                        latestMeasurement = ruuviTagPreview.latestMeasurement?.copy(
                            updatedAt = Date(0),
                        ),
                    ),
                    staleTimeoutSeconds = 60,
                    useDarkBackground = true,
                    onShowStaleSnackbar = { clickWarnings++ },
                )
            }
        }

        compose.waitForIdle()
        compose.runOnIdle { assertEquals(0, clickWarnings) }
        compose.onAllNodes(hasClickAction()).assertCountEquals(1)

        compose.onNodeWithContentDescription(
            context.getString(R.string.sensor_measurements_outdated),
        ).assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, clickWarnings) }
    }

    @Test
    fun `fresh low battery footer shows battery warning and dismisses stale warning`() {
        var dismissals = 0
        val lowVoltage = environmentValue(
            value = 2.0,
            unitType = UnitType.BatteryVoltageUnit.Volt,
        )
        val sensor = ruuviTagPreview.copy(
            latestMeasurement = ruuviTagPreview.latestMeasurement?.copy(
                voltage = lowVoltage,
                updatedAt = Date(),
            ),
        )

        compose.setContent {
            RuuviTheme {
                SensorCardBottom(
                    sensor = sensor,
                    staleTimeoutSeconds = 60,
                    useDarkBackground = false,
                    onDismissStaleSnackbar = { dismissals++ },
                )
            }
        }

        compose.onNodeWithText(context.getString(R.string.low_battery)).assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, dismissals) }
    }

    @Test
    fun `sensor card opens details for the current primary measurement`() {
        val primary = environmentValue(21.0, UnitType.TemperatureUnit.Celsius)
        val sensor = ruuviTagPreview.copy(valuesToDisplay = listOf(primary))

        compose.setContent {
            RuuviTheme {
                SensorCard(
                    sensor = sensor,
                    getChartData = { _, _, _ -> emptyFlow() },
                    scrollToChart = {},
                )
            }
        }

        compose.onNode(
            hasText(context.getString(primary.unitType.measurementName)) and hasClickAction(),
        ).performClick()
        compose.onNodeWithText(context.getString(R.string.popup_no_data)).assertIsDisplayed()
    }

    @Test
    fun `air quality primary measurement without latest data is omitted`() {
        val aqi = environmentValue(42.0, UnitType.AirQuality.AqiIndex)
        compose.setContent {
            RuuviTheme {
                TopMeasurement(
                    sensor = ruuviTagPreview.copy(latestMeasurement = null),
                    value = aqi,
                )
            }
        }

        compose.onNodeWithText(aqi.valueWithoutUnit, substring = true).assertDoesNotExist()
    }

    @Test
    fun `secondary sensor values render and pass the selected value`() {
        val primary = environmentValue(21.0, UnitType.TemperatureUnit.Celsius)
        val humidity = environmentValue(44.0, UnitType.HumidityUnit.Relative)
        val pressure = environmentValue(1_013.0, UnitType.PressureUnit.HectoPascal)
        var selected: EnvironmentValue? = null
        val sensor = ruuviTagPreview.copy(
            valuesToDisplay = listOf(primary, humidity, pressure),
        )

        compose.setContent {
            RuuviTheme {
                SensorValues(
                    modifier = Modifier,
                    sensor = sensor,
                    columnCount = 2,
                    itemHeight = 48.dp,
                    horizontalPadding = 20.dp,
                    onValueClick = { selected = it },
                )
            }
        }

        compose.onNode(
            hasText(humidity.valueWithoutUnit, substring = true) and hasClickAction(),
        ).performClick()
        compose.runOnIdle { assertSame(humidity, selected) }
        compose.onNodeWithText(pressure.valueWithoutUnit, substring = true).assertIsDisplayed()
    }

    @Test
    fun `single sensor value does not render the secondary grid`() {
        val primary = environmentValue(21.0, UnitType.TemperatureUnit.Celsius)
        compose.setContent {
            RuuviTheme {
                SensorValues(
                    modifier = Modifier,
                    sensor = ruuviTagPreview.copy(valuesToDisplay = listOf(primary)),
                    columnCount = 2,
                    itemHeight = 48.dp,
                    horizontalPadding = 20.dp,
                    onValueClick = {},
                )
            }
        }

        compose.onNodeWithText(primary.valueWithoutUnit, substring = true).assertDoesNotExist()
    }

    private fun environmentValue(value: Double, unitType: UnitType) = EnvironmentValue(
        original = value,
        value = value,
        accuracy = Accuracy.Accuracy1,
        valueWithUnit = value.toString(),
        valueWithoutUnit = value.toString(),
        unitString = "unit",
        unitType = unitType,
    )
}
