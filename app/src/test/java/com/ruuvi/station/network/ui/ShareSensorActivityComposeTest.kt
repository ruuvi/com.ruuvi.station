@file:Suppress("UsingMaterialAndMaterial3Libraries")

package com.ruuvi.station.network.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material.rememberScaffoldState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextReplacement
import androidx.core.view.WindowCompat
import com.ruuvi.station.R
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.domain.SensorShareListRepository
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.network.domain.NetworkShareListInteractor
import com.ruuvi.station.network.domain.NetworkTokenRepository
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
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
class ShareSensorActivityComposeTest {
    @get:Rule
    val compose = createComposeRule()

    private var oldMode = AppCompatDelegate.MODE_NIGHT_UNSPECIFIED

    @Before
    fun setUp() {
        oldMode = AppCompatDelegate.getDefaultNightMode()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        (RuntimeEnvironment.getApplication() as UiTestApplication).shareSensorViewModel =
            createViewModel()
    }

    @After
    fun tearDown() {
        AppCompatDelegate.setDefaultNightMode(oldMode)
    }

    @Test
    fun `valid sensor renders native sharing edge to edge`() {
        withActivity(SENSOR_ID) { activity ->
            compose.onNodeWithText(activity.getString(R.string.share_sensor_title))
                .assertIsDisplayed()
            compose.onNodeWithText(activity.getString(R.string.share_sensor_add_friend))
                .assertIsDisplayed()

            val icons = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            assertTrue(icons.isAppearanceLightStatusBars)
            assertTrue(icons.isAppearanceLightNavigationBars)
            assertEquals(Color.TRANSPARENT, activity.window.statusBarColor)
            assertEquals(Color.TRANSPARENT, activity.window.navigationBarColor)
            assertFalse(activity.window.isNavigationBarContrastEnforced)
        }
    }

    @Test
    fun `web sharing configures web view and page callbacks`() {
        (RuntimeEnvironment.getApplication() as UiTestApplication).shareSensorViewModel =
            createViewModel(useWebShare = true)

        withActivity(SENSOR_ID) { activity ->
            val webView = findWebView(activity.window.decorView)
            assertTrue(webView != null)
            webView ?: return@withActivity

            assertTrue(webView.settings.javaScriptEnabled)
            assertTrue(webView.settings.domStorageEnabled)
            assertTrue(webView.settings.allowContentAccess)
            assertTrue(webView.settings.allowFileAccess)

            val client = webView.webViewClient
            assertFalse(client.shouldOverrideUrlLoading(webView, webView.url))
            assertFalse(
                client.shouldOverrideUrlLoading(
                    webView,
                    mockk<WebResourceRequest>(relaxed = true),
                ),
            )
            client.onPageStarted(webView, webView.url, null)
            client.onPageFinished(webView, webView.url)
        }
    }

    @Test
    fun `missing sensor finishes before composing content`() {
        withActivity(sensorId = null) { activity ->
            assertTrue(activity.isFinishing)
            compose.onNodeWithText(activity.getString(R.string.share_sensor_title))
                .assertDoesNotExist()
        }
    }

    @Test
    fun `share body submits valid email and renders active and pending access`() {
        var sharedEmail: String? = null
        val unsharedEmails = mutableListOf<String>()

        compose.setContent {
            RuuviTheme {
                ShareBody(
                    scaffoldState = rememberScaffoldState(),
                    canShare = true,
                    emails = listOf("active@example.com"),
                    pendingEmails = listOf("pending@example.com"),
                    usedSharesTotal = 2,
                    maxSharesTotal = 10,
                    maxSharesPerSensor = 5,
                    shareToUser = { sharedEmail = it },
                    unshare = unsharedEmails::add,
                )
            }
        }

        compose.onNodeWithText("active@example.com").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performTextReplacement("friend@example.com")
        compose.onNodeWithText(
            (RuntimeEnvironment.getApplication() as UiTestApplication).getString(R.string.share),
        ).performClick()

        compose.runOnIdle {
            assertEquals("friend@example.com", sharedEmail)
            assertTrue(unsharedEmails.isEmpty())
        }
        compose.onNode(hasSetTextAction()).assertTextContains(
            (RuntimeEnvironment.getApplication() as UiTestApplication)
                .getString(R.string.type_your_email),
        )

        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(5)
        compose.onNodeWithText("pending@example.com").assertIsDisplayed()
        compose.onNodeWithText(
            (RuntimeEnvironment.getApplication() as UiTestApplication)
                .getString(R.string.share_pending_section_title),
        ).assertIsDisplayed()
    }

    @Test
    fun `share input rejects values longer than sixty four characters`() {
        compose.setContent {
            RuuviTheme {
                AddFriend {}
            }
        }

        compose.onNode(hasSetTextAction()).performTextReplacement("a".repeat(65))
        compose.onNode(hasSetTextAction()).assertTextContains(
            (RuntimeEnvironment.getApplication() as UiTestApplication)
                .getString(R.string.type_your_email),
        )
    }

    @Test
    fun `unshare requires confirmation and passes selected email`() {
        var unsharedEmail: String? = null
        compose.setContent {
            RuuviTheme {
                SharedEmailItem(email = "owner@example.com") {
                    unsharedEmail = it
                }
            }
        }

        compose.onNode(hasClickAction()).performClick()
        compose.onNodeWithText(
            (RuntimeEnvironment.getApplication() as UiTestApplication).getString(
                R.string.share_sensor_unshare_confirm,
                "owner@example.com",
            ),
        ).assertIsDisplayed()
        compose.onNodeWithText(
            (RuntimeEnvironment.getApplication() as UiTestApplication).getString(R.string.yes),
        ).performClick()

        compose.runOnIdle {
            assertEquals("owner@example.com", unsharedEmail)
        }
    }

    @Test
    fun `start preserves nullable sensor id in launched intent`() {
        val hostController = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            ShareSensorActivity.start(hostController.get(), SENSOR_ID)
            val populatedIntent = shadowOf(hostController.get()).nextStartedActivity
            assertEquals(
                ShareSensorActivity::class.java.name,
                populatedIntent.component?.className,
            )
            assertEquals(SENSOR_ID, populatedIntent.getStringExtra(TAG_ID))

            ShareSensorActivity.start(hostController.get(), null)
            assertEquals(
                null,
                shadowOf(hostController.get()).nextStartedActivity.getStringExtra(TAG_ID),
            )
        } finally {
            hostController.pause().stop().destroy()
        }
    }

    private fun withActivity(
        sensorId: String?,
        assertions: (ShareSensorActivity) -> Unit,
    ) {
        val intent = Intent(
            RuntimeEnvironment.getApplication(),
            ShareSensorActivity::class.java,
        )
        sensorId?.let { intent.putExtra(TAG_ID, it) }

        val controller = Robolectric.buildActivity(ShareSensorActivity::class.java, intent)
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

    private fun findWebView(view: View): WebView? = when (view) {
        is WebView -> view
        is ViewGroup -> (0 until view.childCount)
            .firstNotNullOfOrNull { index -> findWebView(view.getChildAt(index)) }
        else -> null
    }

    private fun createViewModel(useWebShare: Boolean = false): ShareSensorViewModel {
        val preferences = mockk<PreferencesRepository>()
        every { preferences.getSubscriptionMaxSharesPerSensor() } returns 5
        every { preferences.getSubscriptionMaxSharesTotal() } returns 10
        every { preferences.getSubscriptionUsedSharesTotal() } returns 2
        every { preferences.getUseWebShare() } returns useWebShare

        val sensorSettings = mockk<SensorSettings>()
        every { sensorSettings.canShare } returns true
        val sensorSettingsRepository = mockk<SensorSettingsRepository>()
        every { sensorSettingsRepository.getSensorSettings(SENSOR_ID) } returns sensorSettings

        val tokenRepository = mockk<NetworkTokenRepository>()
        every { tokenRepository.getTokenInfo() } returns null

        return ShareSensorViewModel(
            sensorId = SENSOR_ID,
            ruuviNetworkInteractor = mockk<RuuviNetworkInteractor>(relaxed = true),
            networkShareListInteractor = mockk<NetworkShareListInteractor>(relaxed = true),
            sensorShareListRepository = SensorShareListRepository(),
            sensorSettingsRepository = sensorSettingsRepository,
            preferencesRepository = preferences,
            networkTokenRepository = tokenRepository,
        )
    }

    private companion object {
        const val SENSOR_ID = "AA:BB:CC:DD:EE:FF"
        const val TAG_ID = "TAG_ID"
    }
}
