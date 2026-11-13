package com.ruuvi.station.tagdetails.ui

import android.app.Application
import android.content.Intent
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.dashboard.DashboardTapAction
import com.ruuvi.station.feature.data.FeatureFlag
import com.ruuvi.station.feature.domain.RuntimeBehavior
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.util.Period
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.bind
import org.kodein.di.factory
import org.kodein.di.singleton
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    application = SensorCardActivityTestApplication::class,
    sdk = [35],
)
class SensorCardActivityComposeWiringTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var application: SensorCardActivityTestApplication
    private lateinit var viewModel: SensorCardViewModel
    private lateinit var preferences: PreferencesRepository
    private lateinit var runtimeBehavior: RuntimeBehavior

    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication() as SensorCardActivityTestApplication
        viewModel = mockk(relaxed = true)
        preferences = mockk()
        runtimeBehavior = mockk()

        every { viewModel.sensorsFlow } returns MutableStateFlow(emptyList())
        every { viewModel.selectedSensor } returns MutableStateFlow(null)
        every { viewModel.chartViewPeriod } returns MutableStateFlow(Period.Day1)
        every { viewModel.syncInProgress } returns MutableStateFlow(value = false)
        every { viewModel.showChartStats } returns MutableStateFlow(value = false)
        every { viewModel.chartSizeLevel } returns MutableStateFlow(0)
        every { preferences.getDashboardTapAction() } returns DashboardTapAction.SHOW_CHART
        every { runtimeBehavior.isFeatureEnabled(FeatureFlag.NEW_SENSOR_CARD) } returns true

        application.sensorCardViewModel = viewModel
        application.preferences = preferences
        application.runtimeBehavior = runtimeBehavior
        application.unitsConverter = mockk(relaxed = true)
        application.createdViewModelArguments = null
    }

    @Test
    fun `onCreate resolves dependencies and binds sensor id to the route view model`() {
        val activity = Robolectric.buildActivity(
            SensorCardActivity::class.java,
            Intent(application, SensorCardActivity::class.java)
                .putExtra(SensorCardActivity.ARGUMENT_SENSOR_ID, SENSOR_ID),
        ).setup().get()

        composeRule.waitForIdle()

        assertEquals(SENSOR_ID, application.createdViewModelArguments?.sensorId)
        assertFalse(activity.isFinishing)
        verify(exactly = 1) { preferences.getDashboardTapAction() }
        verify(exactly = 1) {
            runtimeBehavior.isFeatureEnabled(FeatureFlag.NEW_SENSOR_CARD)
        }
    }

    private companion object {
        const val SENSOR_ID = "AA:BB:CC:DD:EE:FF"
    }
}

class SensorCardActivityTestApplication : Application(), DIAware {
    lateinit var sensorCardViewModel: SensorCardViewModel
    lateinit var preferences: PreferencesRepository
    lateinit var runtimeBehavior: RuntimeBehavior
    lateinit var unitsConverter: UnitsConverter
    var createdViewModelArguments: SensorCardViewModelArguments? = null

    override val di = DI.lazy {
        bind<PreferencesRepository>() with singleton { preferences }
        bind<RuntimeBehavior>() with singleton { runtimeBehavior }
        bind<UnitsConverter>() with singleton { unitsConverter }
        bind<SensorCardViewModel>() with factory { arguments: SensorCardViewModelArguments ->
            createdViewModelArguments = arguments
            sensorCardViewModel
        }
    }
}
