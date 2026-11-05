package com.ruuvi.station.addtag.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.ruuvi.station.R
import com.ruuvi.station.app.permissions.PermissionsInteractor
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.tagdetails.ui.SensorCardActivity
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.util.ui.BottomFadingEdgeListView
import com.ruuvi.station.util.ui.UiTestApplication
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(application = UiTestApplication::class, sdk = [24, 25, 35], qualifiers = "en-rUS-notnight-mdpi")
class AddTagActivityUiTest {
    private val viewModel = mockk<AddTagActivityViewModel>(relaxed = true)
    private val preferences = mockk<PreferencesRepository>(relaxed = true)
    private val tags = MutableStateFlow<List<RuuviTagEntity>>(emptyList())
    private var oldMode = AppCompatDelegate.MODE_NIGHT_UNSPECIFIED

    @Before fun setUp() {
        oldMode = AppCompatDelegate.getDefaultNightMode()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        mockkConstructor(PermissionsInteractor::class)
        every { anyConstructed<PermissionsInteractor>().requestPermissions(any(), any()) } just Runs
        every { viewModel.sensorFlow } returns tags
        every { viewModel.nfcSupported } returns false
        every { preferences.getBackgroundScanMode() } returns BackgroundScanModes.DISABLED
        (RuntimeEnvironment.getApplication() as UiTestApplication).apply {
            addTagViewModel = viewModel
            this.preferences = this@AddTagActivityUiTest.preferences
        }
    }

    @After fun tearDown() {
        unmockkConstructor(PermissionsInteractor::class)
        AppCompatDelegate.setDefaultNightMode(oldMode)
    }

    @Test
    fun `light header and navigation fallback remain legible`() {
        withActivity { activity ->
            assertEquals(Color.WHITE, (activity.binding.root.background as ColorDrawable).color)
            assertEquals(activity.getColor(R.color.titan), activity.binding.toolbar.children.filterIsInstance<android.widget.TextView>().first().currentTextColor)
            assertTrue(WindowCompat.getInsetsController(activity.window, activity.binding.root).isAppearanceLightStatusBars)
            assertEquals(
                if (Build.VERSION.SDK_INT < 26) activity.getColor(R.color.titan) else Color.TRANSPARENT,
                activity.window.navigationBarColor,
            )
            assertEquals(View.GONE, activity.binding.content.nfcHintTextView.visibility)
        }
    }

    @Test
    @Config(qualifiers = "en-rUS-night-mdpi")
    fun `dark header and icons remain unchanged`() {
        every { viewModel.nfcSupported } returns true
        withActivity { activity ->
            assertEquals(activity.getColor(R.color.titan), (activity.binding.root.background as ColorDrawable).color)
            assertFalse(WindowCompat.getInsetsController(activity.window, activity.binding.root).isAppearanceLightStatusBars)
            assertEquals(View.VISIBLE, activity.binding.content.nfcHintTextView.visibility)
        }
    }

    @Test
    @Config(sdk = [35])
    fun `sensor updates replace empty state and apply padding inside fading list`() {
        withActivity { activity ->
            assertEquals(View.VISIBLE, activity.binding.content.noSensorsLayout.visibility)
            tags.value = List(12) { sensor(it + 1) }
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(View.GONE, activity.binding.content.noSensorsLayout.visibility)
            assertEquals(12, activity.binding.content.tagListView.adapter.count)
            assertTrue(activity.binding.content.tagListView is BottomFadingEdgeListView)
            ViewCompat.dispatchApplyWindowInsets(
                activity.binding.root,
                WindowInsetsCompat.Builder()
                    .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 24, 0, 48)).build(),
            )
            assertEquals(24, activity.binding.root.paddingTop)
            assertEquals(0, activity.binding.root.paddingBottom)
            assertEquals(48, activity.binding.content.tagListView.paddingBottom)
            assertFalse(activity.binding.content.tagListView.clipToPadding)
        }
    }

    @Test
    fun `real sensor can be added but existing favourite is rejected`() {
        val sensor = sensor(1)
        tags.value = listOf(sensor)
        every { viewModel.getTagById(sensor.id!!) } returns sensor.copy(favorite = true)
        withActivity { activity ->
            activity.binding.content.tagListView.performItemClick(View(activity), 0, 0)
            assertEquals(activity.getString(R.string.sensor_already_added), ShadowToast.getTextOfLatestToast())
            verify(exactly = 0) { viewModel.makeSensorFavorite(any()) }
            every { viewModel.getTagById(sensor.id!!) } returns sensor
            activity.binding.content.tagListView.performItemClick(View(activity), 0, 0)
            verify { viewModel.makeSensorFavorite(sensor) }
            assertEquals(
                SensorCardActivity::class.java.name,
                shadowOf(activity).nextStartedActivity.component?.className,
            )
        }
    }

    private fun sensor(index: Int) = RuuviTagEntity(
        id = "AA:BB:CC:DD:EE:%02X".format(index),
        rssi = -40 - index,
        dataFormat = 5,
    )

    private fun withActivity(assertions: (AddTagActivity) -> Unit) {
        val controller = Robolectric.buildActivity(AddTagActivity::class.java).setup().visible()
        try {
            shadowOf(Looper.getMainLooper()).idle()
            assertions(controller.get())
        } finally {
            controller.pause().stop().destroy()
        }
    }
}
