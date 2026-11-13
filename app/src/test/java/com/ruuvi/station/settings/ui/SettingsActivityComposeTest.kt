package com.ruuvi.station.settings.ui

import android.app.Activity
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Looper
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.view.WindowCompat
import androidx.lifecycle.MutableLiveData
import com.ruuvi.station.R
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.app.preferences.SharedPreferenceBooleanLiveData
import com.ruuvi.station.app.ui.DarkModeState
import com.ruuvi.station.feature.domain.RuntimeBehavior
import com.ruuvi.station.settings.domain.AppSettingsInteractor
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

        val interactor = mockk<AppSettingsInteractor>()
        val preferences = mockk<PreferencesRepository>()
        val runtimeBehavior = mockk<RuntimeBehavior>()
        val developerSettings = SharedPreferenceBooleanLiveData(
            sharedPrefs = mockk<SharedPreferences>(relaxed = true),
            key = "developer_settings",
            defValue = false,
        )

        every { preferences.getDeveloperSettingsLiveData() } returns developerSettings
        every { interactor.getTemperatureUnitLiveData() } returns
            MutableLiveData(TemperatureUnit.Celsius)
        every { interactor.getHumidityUnitLiveData() } returns
            MutableLiveData(HumidityUnit.Relative)
        every { interactor.getPressureUnitLiveData() } returns
            MutableLiveData(PressureUnit.HectoPascal)
        every { interactor.getTemperatureUnit() } returns TemperatureUnit.Celsius
        every { interactor.getHumidityUnit() } returns HumidityUnit.Relative
        every { interactor.getPressureUnit() } returns PressureUnit.HectoPascal
        every { interactor.getBackgroundScanMode() } returns BackgroundScanModes.DISABLED
        every { interactor.shouldShowCloudMode() } returns false
        every { interactor.getDarkMode() } returns DarkModeState.SYSTEM_THEME
        every { runtimeBehavior.isFeatureEnabled(any()) } returns true

        settingsListViewModel = AppSettingsListViewModel(
            interactor,
            preferences,
            runtimeBehavior,
        )
        appearanceViewModel = AppearanceSettingsViewModel(interactor)

        (RuntimeEnvironment.getApplication() as UiTestApplication).apply {
            appSettingsListViewModel = settingsListViewModel
            appearanceSettingsViewModel = appearanceViewModel
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
