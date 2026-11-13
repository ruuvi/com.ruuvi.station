package com.ruuvi.station.calibration.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Looper
import android.view.MenuItem
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.ruuvi.station.R
import com.google.android.material.shape.MaterialShapeDrawable
import com.ruuvi.station.calibration.model.CalibrationType
import com.ruuvi.station.util.ui.UiTestApplication
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
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
class CalibrationActivityUiTest {
    private val humidityViewModel = mockk<CalibrateHumidityViewModel>(relaxed = true)
    private val pressureViewModel = mockk<CalibratePressureViewModel>(relaxed = true)
    private val temperatureViewModel = mockk<CalibrateTemperatureViewModel>(relaxed = true)
    private var oldMode = AppCompatDelegate.MODE_NIGHT_UNSPECIFIED

    @Before
    fun setUp() {
        oldMode = AppCompatDelegate.getDefaultNightMode()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        every { humidityViewModel.calibrationInfoFlow } returns emptyFlow()
        every { pressureViewModel.calibrationInfoFlow } returns emptyFlow()
        every { temperatureViewModel.calibrationInfoFlow } returns emptyFlow()
        (RuntimeEnvironment.getApplication() as UiTestApplication).apply {
            calibrateHumidityViewModel = humidityViewModel
            calibratePressureViewModel = pressureViewModel
            calibrateTemperatureViewModel = temperatureViewModel
        }
    }

    @After
    fun tearDown() {
        AppCompatDelegate.setDefaultNightMode(oldMode)
    }

    @Test
    @Config(sdk = [25])
    fun `light theme applies legacy edge to edge fallback`() {
        withActivity(CalibrationType.HUMIDITY) { activity ->
            assertEquals(Color.WHITE, (activity.binding.root.background as ColorDrawable).color)
            assertEquals(
                Color.WHITE,
                (activity.binding.appBarLayout.background as MaterialShapeDrawable)
                    .fillColor
                    ?.defaultColor,
            )
            assertEquals(
                activity.getColor(R.color.titan),
                activity.binding.toolbar.children
                    .filterIsInstance<android.widget.TextView>()
                    .first()
                    .currentTextColor,
            )

            val icons = WindowCompat.getInsetsController(activity.window, activity.binding.root)
            assertTrue(icons.isAppearanceLightStatusBars)
            assertFalse(icons.isAppearanceLightNavigationBars)
            assertEquals(activity.getColor(R.color.titan), activity.window.navigationBarColor)
        }
    }

    @Test
    fun `system bar insets pad the root and scroll content`() {
        withActivity(CalibrationType.HUMIDITY) { activity ->
            val insets = WindowInsetsCompat.Builder()
                .setInsets(
                    WindowInsetsCompat.Type.systemBars(),
                    Insets.of(8, 24, 10, 48),
                )
                .setInsets(
                    WindowInsetsCompat.Type.displayCutout(),
                    Insets.of(12, 20, 14, 30),
                )
                .build()
            ViewCompat.dispatchApplyWindowInsets(activity.binding.root, insets)

            val fragment = activity.supportFragmentManager
                .findFragmentById(R.id.calibration_frame) as CalibrationFragment
            val calibrationScroll = fragment.binding.calibrationScroll

            assertEquals(activity.binding.root, activity.binding.calibrationFrame.parent)
            assertEquals(12, activity.binding.root.paddingLeft)
            assertEquals(24, activity.binding.root.paddingTop)
            assertEquals(14, activity.binding.root.paddingRight)
            assertEquals(0, activity.binding.root.paddingBottom)
            assertEquals(48, calibrationScroll.paddingBottom)
            assertFalse(calibrationScroll.clipToPadding)
        }
    }

    @Test
    @Config(qualifiers = "en-rUS-night-mdpi")
    fun `dark theme preserves dark surfaces and light system icons`() {
        withActivity(CalibrationType.PRESSURE) { activity ->
            assertEquals(
                activity.getColor(R.color.titan),
                (activity.binding.root.background as ColorDrawable).color,
            )
            val icons = WindowCompat.getInsetsController(activity.window, activity.binding.root)
            assertFalse(icons.isAppearanceLightStatusBars)
            assertFalse(icons.isAppearanceLightNavigationBars)
        }
    }

    @Test
    fun `each calibration type opens its matching fragment and title`() {
        val cases = listOf(
            CalibrationType.HUMIDITY to CalibrateHumidityFragment::class.java,
            CalibrationType.TEMPERATURE to CalibrateTemperatureFragment::class.java,
            CalibrationType.PRESSURE to CalibratePressureFragment::class.java,
        )

        cases.forEach { (type, fragmentClass) ->
            withActivity(type, SENSOR_ID) { activity ->
                val fragment = activity.supportFragmentManager
                    .findFragmentById(R.id.calibration_frame)
                assertTrue(fragmentClass.isInstance(fragment))
                assertEquals(SENSOR_ID, fragment?.arguments?.getString(CalibrationFragment.SENSOR_ID))
                assertEquals(activity.getString(type.titleId), activity.title)
                assertEquals(SENSOR_ID, activity.sensorId)
                assertEquals(type, activity.calibrationType)
            }
        }
    }

    @Test
    fun `missing sensor id defaults to empty and up finishes activity`() {
        withActivity(CalibrationType.TEMPERATURE, sensorId = null) { activity ->
            assertEquals("", activity.sensorId)
            val home = mockk<MenuItem>()
            every { home.itemId } returns android.R.id.home

            assertFalse(activity.onOptionsItemSelected(home))
            assertTrue(activity.isFinishing)
        }
    }

    @Test
    fun `start creates calibration intent with requested sensor and type`() {
        val hostController = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            CalibrationActivity.start(
                hostController.get(),
                SENSOR_ID,
                CalibrationType.PRESSURE,
            )

            val startedIntent = shadowOf(hostController.get()).nextStartedActivity
            assertEquals(
                CalibrationActivity::class.java.name,
                startedIntent.component?.className,
            )
            assertEquals(SENSOR_ID, startedIntent.getStringExtra(CalibrationActivity.SENSOR_ID))
            assertEquals(
                CalibrationType.PRESSURE,
                startedIntent.getSerializableExtra(CalibrationActivity.CALIBRATION_TYPE),
            )
        } finally {
            hostController.pause().stop().destroy()
        }
    }

    private fun withActivity(
        calibrationType: CalibrationType,
        sensorId: String? = SENSOR_ID,
        assertions: (CalibrationActivity) -> Unit,
    ) {
        val intent = Intent(
            RuntimeEnvironment.getApplication(),
            CalibrationActivity::class.java,
        ).putExtra(CalibrationActivity.CALIBRATION_TYPE, calibrationType)
        sensorId?.let { intent.putExtra(CalibrationActivity.SENSOR_ID, it) }

        val controller = Robolectric.buildActivity(CalibrationActivity::class.java, intent)
            .setup()
            .visible()
        try {
            shadowOf(Looper.getMainLooper()).idle()
            assertions(controller.get())
        } finally {
            controller.pause().stop().destroy()
        }
    }

    private companion object {
        const val SENSOR_ID = "AA:BB:CC:DD:EE:FF"
    }
}
