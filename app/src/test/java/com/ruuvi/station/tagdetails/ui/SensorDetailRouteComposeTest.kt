package com.ruuvi.station.tagdetails.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlarmSensorStatus
import com.ruuvi.station.alarm.ui.AlarmItemsViewModel
import com.ruuvi.station.app.ui.UiText
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.ruuviTagPreview
import com.ruuvi.station.tagsettings.ui.RemoveSensorViewModel
import com.ruuvi.station.tagsettings.ui.TagSettingsViewModel
import com.ruuvi.station.tagsettings.ui.led_control.LedControlViewModel
import com.ruuvi.station.tagsettings.ui.notes.NotesViewModel
import com.ruuvi.station.tagsettings.ui.visible_measurements.VisibleMeasurementsViewModel
import com.ruuvi.station.util.Period
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "en-rUS-mdpi")
class SensorDetailRouteComposeTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val sensors = MutableStateFlow<List<RuuviTag>>(emptyList())
    private val selectedSensorFlow = MutableStateFlow<String?>(null)
    private val viewModel = mockk<SensorCardViewModel>(relaxed = true) {
        every { sensorsFlow } returns sensors
        every { selectedSensor } returns selectedSensorFlow
        every { chartViewPeriod } returns MutableStateFlow(Period.Day1)
        every { syncInProgress } returns MutableStateFlow(false)
        every { showChartStats } returns MutableStateFlow(false)
        every { chartSizeLevel } returns MutableStateFlow(0)
        every { initialSensorId } returns null
        every { sensorDataTimeoutSeconds } returns 60
    }

    @Test
    fun `route follows sensor emissions external selection and title paging`() {
        compose.setContent {
            SensorDetailRoute(
                viewModel = viewModel,
                startDestination = SensorDetailStartDestination(SensorDetailDestination.CARD),
                viewModelProvider = viewModelProvider(),
                unitsConverter = mockk(relaxed = true),
                useNewSensorCard = true,
                onFinish = {},
            )
        }

        compose.onNodeWithContentDescription("Sensor One").assertDoesNotExist()

        compose.runOnIdle {
            sensors.value = listOf(sensor("one", "Sensor One"), sensor("two", "Sensor Two"))
            selectedSensorFlow.value = "one"
        }
        settleAnimations()
        compose.onNodeWithContentDescription("Sensor One").assertExists()

        compose.runOnIdle { selectedSensorFlow.value = "two" }
        settleAnimations()
        compose.onNodeWithContentDescription("Sensor Two").assertExists()
        verify(atLeast = 1) { viewModel.saveSelected("two") }

        compose.runOnIdle { selectedSensorFlow.value = "missing" }
        compose.waitForIdle()
        verify(atLeast = 1) { viewModel.saveSelected("two") }

        compose.runOnIdle { selectedSensorFlow.value = "one" }
        settleAnimations()
        compose.onNodeWithContentDescription("Sensor One")
            .performTouchInput { swipeLeft() }
        settleAnimations()
        compose.onNodeWithContentDescription("Sensor Two").assertExists()

        compose.onNodeWithContentDescription(compose.activity.getString(R.string.previous_sensor)).performClick()
        settleAnimations()
        compose.onNodeWithContentDescription("Sensor One").assertExists()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.next_sensor)).performClick()
        settleAnimations()

        compose.onNodeWithContentDescription("Sensor Two").assertExists()
        verify(atLeast = 2) { viewModel.saveSelected("two") }
    }

    @Test
    fun `destination selection resets nested route and back returns to settings root`() {
        val sensor = sensor("one", "Sensor One")
        val removeSensorViewModel = removeSensorViewModel(sensor)
        val settingsViewModel = tagSettingsViewModel(sensor)
        var finishCalls = 0
        sensors.value = listOf(sensor)
        selectedSensorFlow.value = sensor.id

        compose.setContent {
            SensorDetailRoute(
                viewModel = viewModel,
                startDestination = SensorDetailStartDestination(SensorDetailDestination.CARD),
                viewModelProvider = viewModelProvider(
                    settingsViewModel = settingsViewModel,
                    removeSensorViewModel = removeSensorViewModel,
                ),
                unitsConverter = mockk(relaxed = true),
                useNewSensorCard = true,
                onFinish = { finishCalls++ },
            )
        }

        compose.onNodeWithContentDescription(
            compose.activity.getString(R.string.full_image_view),
        ).performClick()
        compose.onNodeWithContentDescription(
            compose.activity.getString(R.string.sensor_settings),
        ).performClick()
        settleAnimations()

        compose.onNodeWithText(compose.activity.getString(R.string.remove))
            .performScrollTo()
            .performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.remove_this_sensor))
            .performScrollTo()
            .performClick()
        settleAnimations()
        compose.onNodeWithText(
            compose.activity.getString(R.string.remove_local_sensor_description),
        ).assertExists()

        compose.runOnIdle {
            compose.activity.onBackPressedDispatcher.onBackPressed()
        }
        settleAnimations()

        compose.onNodeWithText(compose.activity.getString(R.string.tag_name)).assertExists()
        compose.runOnIdle { assertEquals(0, finishCalls) }
    }

    @Test
    fun `refreshing a stale sensor dismisses only its active snackbar`() {
        val staleMessage = compose.activity.getString(R.string.sensor_measurements_outdated)
        val staleSensor = sensor("one", "Sensor One").copy(
            latestMeasurement = ruuviTagPreview.latestMeasurement?.copy(updatedAt = Date(0)),
        )
        sensors.value = listOf(staleSensor)
        selectedSensorFlow.value = staleSensor.id

        compose.setContent {
            SensorDetailRoute(
                viewModel = viewModel,
                startDestination = SensorDetailStartDestination(SensorDetailDestination.CARD),
                viewModelProvider = viewModelProvider(),
                unitsConverter = mockk(relaxed = true),
                useNewSensorCard = true,
                onFinish = {},
            )
        }

        compose.onNodeWithContentDescription(staleMessage).performClick()
        compose.onNodeWithText(staleMessage).assertExists()

        // A second tap exercises the active-job guard without creating another snackbar.
        compose.onNodeWithContentDescription(staleMessage).performClick()
        compose.onNodeWithText(staleMessage).assertExists()

        compose.runOnIdle {
            sensors.value = listOf(
                staleSensor.copy(
                    latestMeasurement = staleSensor.latestMeasurement?.copy(updatedAt = Date()),
                ),
            )
        }
        compose.waitForIdle()

        compose.onNodeWithText(staleMessage).assertDoesNotExist()
    }

    @Test
    fun `nested settings renders removal screen and back finishes launch flow`() {
        val sensor = sensor("one", "Sensor One")
        val removeSensorViewModel = removeSensorViewModel(sensor)
        var finishCalls = 0
        sensors.value = listOf(sensor)
        selectedSensorFlow.value = sensor.id

        compose.setContent {
            SensorDetailRoute(
                viewModel = viewModel,
                startDestination = SensorCardOpenType.REMOVE.resolveStartDestination(false),
                viewModelProvider = viewModelProvider(
                    removeSensorViewModel = removeSensorViewModel,
                ),
                unitsConverter = mockk(relaxed = true),
                useNewSensorCard = true,
                onFinish = { finishCalls++ },
            )
        }

        compose.onNodeWithText(compose.activity.getString(R.string.remove)).assertExists()
        compose.runOnIdle {
            compose.activity.onBackPressedDispatcher.onBackPressed()
        }
        compose.runOnIdle { assertEquals(1, finishCalls) }
    }

    private fun settleAnimations() {
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
    }

    private fun sensor(id: String, displayName: String) = RuuviTag(
        id = id,
        name = displayName,
        displayName = displayName,
        temperatureOffset = null,
        humidityOffset = null,
        pressureOffset = null,
        defaultBackground = R.drawable.new_bg2,
        userBackground = null,
        networkBackground = null,
        alarmSensorStatus = AlarmSensorStatus.NoAlarms,
        lastSync = null,
        networkLastSync = null,
        networkSensor = false,
        owner = null,
        subscriptionName = null,
        firmware = null,
        defaultDisplayOrder = true,
        displayOrder = emptyList(),
        possibleDisplayOptions = emptyList(),
        description = null,
        valuesToDisplay = emptyList(),
        latestMeasurement = null,
    )

    private fun removeSensorViewModel(sensor: RuuviTag) =
        mockk<RemoveSensorViewModel>(relaxed = true) {
            every { sensorState } returns MutableStateFlow(sensor)
            every { sensorOwnedByUser } returns MutableStateFlow(false)
            every { removeWithCloudData } returns MutableStateFlow(false)
        }

    private fun tagSettingsViewModel(sensor: RuuviTag) =
        mockk<TagSettingsViewModel>(relaxed = true) {
            every { sensorState } returns MutableStateFlow(sensor)
            every { userLoggedIn } returns MutableStateFlow(false)
            every { sensorShared } returns MutableStateFlow(UiText.EmptyString)
            every { sensorOwnedByUser } returns flowOf(false)
            every { sensorOwnedOrOffline } returns flowOf(false)
            every { isLowBattery } returns flowOf(false)
            every { firmware } returns flowOf(null)
            every { askToClaim } returns MutableSharedFlow()
            every { uiEvent } returns MutableSharedFlow()
        }

    private fun viewModelProvider(
        settingsViewModel: TagSettingsViewModel = mockk(relaxed = true),
        removeSensorViewModel: RemoveSensorViewModel = mockk(relaxed = true),
    ) = SensorDetailViewModelProvider(
        settings = { settingsViewModel },
        alerts = { mockk<AlarmItemsViewModel>(relaxed = true) },
        removeSensor = { removeSensorViewModel },
        visibleMeasurements = { mockk<VisibleMeasurementsViewModel>(relaxed = true) },
        ledControl = { mockk<LedControlViewModel>(relaxed = true) },
        notes = { mockk<NotesViewModel>(relaxed = true) },
    )
}
