package com.ruuvi.station.settings.ui

import android.app.Activity
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Looper
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.core.view.WindowCompat
import androidx.lifecycle.MutableLiveData
import com.ruuvi.station.R
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.app.domain.PowerManagerInterator
import com.ruuvi.station.app.preferences.SharedPreferenceBooleanLiveData
import com.ruuvi.station.app.ui.DarkModeState
import com.ruuvi.station.feature.domain.RuntimeBehavior
import com.ruuvi.station.feature.provider.RuntimeFeatureFlagProvider
import com.ruuvi.station.network.domain.NetworkTokenRepository
import com.ruuvi.station.network.domain.RuuviNetworkRepository
import com.ruuvi.station.settings.domain.AppSettingsInteractor
import com.ruuvi.station.settings.domain.ResolutionSettingsTarget
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.UnitType.HumidityUnit
import com.ruuvi.station.units.model.UnitType.PressureUnit
import com.ruuvi.station.units.model.UnitType.TemperatureUnit
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.util.ui.UiTestApplication
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Suppress("DEPRECATION")
@RunWith(RobolectricTestRunner::class)
@Config(application = UiTestApplication::class, sdk = [35], qualifiers = "en-rUS-notnight-mdpi")
class SettingsActivityComposeTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var settingsListViewModel: AppSettingsListViewModel
    private lateinit var appearanceViewModel: AppearanceSettingsViewModel
    private var oldMode = AppCompatDelegate.MODE_NIGHT_UNSPECIFIED

    @Before
    fun setUp() {
        oldMode = AppCompatDelegate.getDefaultNightMode()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        val interactor = mockk<AppSettingsInteractor>(relaxed = true)
        val preferences = mockk<PreferencesRepository>(relaxed = true)
        val runtimeBehavior = mockk<RuntimeBehavior>()
        val developerSettings = SharedPreferenceBooleanLiveData(
            sharedPrefs = mockk<SharedPreferences>(relaxed = true) {
                every { getBoolean("developer_settings", true) } returns true
            },
            key = "developer_settings",
            defValue = true,
        )

        every { preferences.getDeveloperSettingsLiveData() } returns developerSettings
        every { interactor.getTemperatureUnitLiveData() } returns
            MutableLiveData(TemperatureUnit.Celsius)
        every { interactor.getHumidityUnitLiveData() } returns
            MutableLiveData(HumidityUnit.Relative)
        every { interactor.getPressureUnitLiveData() } returns
            MutableLiveData(PressureUnit.HectoPascal)
        every { interactor.getTemperatureAccuracyLiveData() } returns
            MutableLiveData(Accuracy.Accuracy2)
        every { interactor.getHumidityAccuracyLiveData() } returns
            MutableLiveData(Accuracy.Accuracy2)
        every { interactor.getPressureAccuracyLiveData() } returns
            MutableLiveData(Accuracy.Accuracy2)
        every { interactor.getAccuracyLiveData(any()) } returnsMany
            ResolutionSettingsTarget.entries.map { MutableLiveData(Accuracy.Accuracy2) }
        every { interactor.getAccuracy(any()) } returns Accuracy.Accuracy2
        every { interactor.getAccuracyList() } returns Accuracy.entries.toTypedArray()
        every { interactor.getAllTemperatureUnits() } returns listOf(TemperatureUnit.Celsius)
        every { interactor.getAllHumidityUnits() } returns listOf(HumidityUnit.Relative)
        every { interactor.getAllPressureUnits() } returns listOf(PressureUnit.HectoPascal)
        every { interactor.getTemperatureUnit() } returns TemperatureUnit.Celsius
        every { interactor.getHumidityUnit() } returns HumidityUnit.Relative
        every { interactor.getPressureUnit() } returns PressureUnit.HectoPascal
        every { interactor.getBackgroundScanMode() } returns BackgroundScanModes.DISABLED
        every { interactor.shouldShowCloudMode() } returns true
        every { interactor.getDarkMode() } returns DarkModeState.SYSTEM_THEME
        every { runtimeBehavior.isFeatureEnabled(any()) } returns true

        settingsListViewModel = AppSettingsListViewModel(
            interactor,
            preferences,
            runtimeBehavior,
        )
        appearanceViewModel = AppearanceSettingsViewModel(interactor)

        (RuntimeEnvironment.getApplication() as UiTestApplication).apply {
            alertNotificationsSettingsViewModel = AlertNotificationsSettingsViewModel(interactor)
            appSettingsListViewModel = settingsListViewModel
            appearanceSettingsViewModel = this@SettingsActivityComposeTest.appearanceViewModel
            backgroundScanSettingsViewModel = BackgroundScanSettingsViewModel(
                interactor,
                mockk<PowerManagerInterator>(relaxed = true),
            )
            chartSettingsViewModel = ChartSettingsViewModel(interactor)
            cloudSettingsViewModel = CloudSettingsViewModel(interactor)
            dataForwardingSettingsViewModel = DataForwardingSettingsViewModel(interactor)
            developerSettingsViewModel = DeveloperSettingsViewModel(
                preferences,
                mockk<RuuviNetworkRepository>(relaxed = true),
                mockk<NetworkTokenRepository>(relaxed = true),
                mockk<RuntimeFeatureFlagProvider>(relaxed = true),
            )
            globalUnitsAndResolutionViewModel = GlobalUnitsAndResolutionViewModel(interactor)
        }
    }

    @After
    fun tearDown() {
        AppCompatDelegate.setDefaultNightMode(oldMode)
    }

    @Test
    fun `root renders edge to edge and appearance navigation updates title`() {
        withActivity { activity ->
            val rootTitle = activity.getString(R.string.menu_app_settings)
            val appearanceTitle = activity.getString(R.string.settings_appearance)

            compose.onNodeWithText(rootTitle).assertIsDisplayed()
            compose.onNode(
                hasText(appearanceTitle) and hasClickAction(),
            ).performClick()
            compose.waitForIdle()

            compose.onNodeWithText(activity.getString(R.string.app_theme)).assertIsDisplayed()
            compose.onNodeWithText(appearanceTitle).assertIsDisplayed()

            activity.onBackPressedDispatcher.onBackPressed()
            compose.waitForIdle()
            compose.onNodeWithText(rootTitle).assertIsDisplayed()

            val icons = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            assertTrue(icons.isAppearanceLightStatusBars)
            assertTrue(icons.isAppearanceLightNavigationBars)
            assertEquals(Color.TRANSPARENT, activity.window.statusBarColor)
            assertEquals(Color.TRANSPARENT, activity.window.navigationBarColor)
            assertFalse(activity.window.isNavigationBarContrastEnforced)
        }
    }

    @Test
    fun `all settings destinations update title and return to root`() {
        val destinations = listOf(
            R.string.settings_alert_notifications to 1,
            R.string.settings_background_scan to 3,
            R.string.settings_global_units to 4,
            R.string.settings_resolution to 5,
            R.string.ruuvi_cloud to 6,
            R.string.settings_chart to 7,
            R.string.settings_data_forwarding to 8,
            R.string.settings_developer to 9,
        )

        destinations.forEach { (titleRes, listIndex) ->
            withActivity { activity ->
                val rootTitle = activity.getString(R.string.menu_app_settings)
                val title = activity.getString(titleRes)
                try {
                    compose.onNode(hasScrollToIndexAction()).performScrollToIndex(listIndex)
                    compose.onNode(
                        hasText(title) and hasClickAction(),
                    ).performClick()
                    compose.waitForIdle()
                    compose.onNodeWithText(title).assertIsDisplayed()

                    activity.onBackPressedDispatcher.onBackPressed()
                    compose.waitForIdle()
                    compose.onNodeWithText(rootTitle).assertIsDisplayed()
                } catch (error: AssertionError) {
                    throw AssertionError("Failed to navigate to $title", error)
                }
            }
        }
    }

    @Test
    fun `dark theme configures dark status bar icons`() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        withActivity { activity ->
            val icons = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            assertFalse(icons.isAppearanceLightStatusBars)
            assertFalse(icons.isAppearanceLightNavigationBars)
        }
    }

    @Test
    fun `back from settings root finishes activity`() {
        withActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(activity.isFinishing)
        }
    }

    @Test
    fun `start launches settings activity`() {
        val hostController = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            SettingsActivity.start(hostController.get())

            assertEquals(
                SettingsActivity::class.java.name,
                shadowOf(hostController.get()).nextStartedActivity.component?.className,
            )
        } finally {
            hostController.pause().stop().destroy()
        }
    }

    private fun withActivity(assertions: (SettingsActivity) -> Unit) {
        val controller = Robolectric.buildActivity(SettingsActivity::class.java)
            .setup()
            .visible()
        try {
            shadowOf(Looper.getMainLooper()).idle()
            compose.waitForIdle()
            assertions(controller.get())
        } finally {
            controller.pause().stop().destroy()
        }
    }
}
