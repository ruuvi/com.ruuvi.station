package com.ruuvi.station.util.extensions

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import com.ruuvi.station.R
import com.ruuvi.station.util.ui.UiTestApplication
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    application = UiTestApplication::class,
    sdk = [25, 35],
    qualifiers = "en-rUS-notnight-mdpi",
)
class ActivityEdgeToEdgeTest {
    private var oldMode = AppCompatDelegate.MODE_NIGHT_UNSPECIFIED

    @Before
    fun setUp() {
        oldMode = AppCompatDelegate.getDefaultNightMode()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    @After
    fun tearDown() {
        AppCompatDelegate.setDefaultNightMode(oldMode)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `edge to edge uses theme icons and legacy navigation fallback`() {
        val controller = Robolectric.buildActivity(EdgeToEdgeTestActivity::class.java).setup()

        try {
            val activity = controller.get()
            val insetsController = WindowCompat.getInsetsController(
                activity.window,
                activity.window.decorView,
            )

            assertEquals(Color.TRANSPARENT, activity.window.statusBarColor)
            assertTrue(insetsController.isAppearanceLightStatusBars)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                assertEquals(activity.getColor(R.color.titan), activity.window.navigationBarColor)
                assertFalse(insetsController.isAppearanceLightNavigationBars)
            } else {
                assertEquals(Color.TRANSPARENT, activity.window.navigationBarColor)
                assertTrue(insetsController.isAppearanceLightNavigationBars)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                assertFalse(activity.window.isNavigationBarContrastEnforced)
            }
        } finally {
            controller.pause().stop().destroy()
        }
    }
}

class EdgeToEdgeTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableRuuviEdgeToEdge()
    }
}
