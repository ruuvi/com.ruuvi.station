package com.ruuvi.station.network.ui

import android.app.Application
import android.content.Context
import android.text.format.DateUtils
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.network.data.response.MarketingConsentStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "en-rUS")
class MyAccountActivityComposeTest {
    @get:Rule
    val compose = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        fcmToken = null
    }

    @Test
    fun `marketing consent visibility state and debug information are rendered`() {
        var consent by mutableStateOf(
            MarketingConsentUiState(
                checked = false,
                isVisible = false,
            ),
        )
        var changedTo: Boolean? = null
        fcmToken = "current-device-token"

        compose.setContent {
            RuuviTheme {
                MyAccountBody(
                    user = "sensor-owner@example.com",
                    marketingConsent = consent,
                    setMarketingConsent = { changedTo = it },
                    signOut = {},
                    deleteAccount = {},
                    subscription = null,
                    tokens = listOf(123L to "registered-token"),
                )
            }
        }

        compose.onNodeWithText("sensor-owner@example.com").assertExists()
        compose.onNodeWithText(context.getString(R.string.newsletter_subscription)).assertDoesNotExist()
        compose.onNodeWithText("current-device-token").assertExists()
        compose.onNodeWithText("123 - registered-token").assertExists()

        compose.runOnIdle {
            consent = MarketingConsentUiState(
                checked = false,
                status = MarketingConsentStatus.UNCONFIRMED,
                isToggleEnabled = true,
                isVisible = true,
            )
        }

        compose.onNode(
            hasText(context.getString(R.string.newsletter_subscription)) and hasClickAction(),
        ).performClick()
        compose.runOnIdle { assertEquals(true, changedTo) }
        compose.onNodeWithText(
            context.getString(R.string.newsletter_subscription_confirmation),
        ).assertExists()
    }

    @Test
    fun `delete and confirmed sign out invoke their actions while dismissal keeps account open`() {
        var deleteInvoked = false
        var signOutInvoked = false

        compose.setContent {
            RuuviTheme {
                MyAccountBody(
                    user = "owner@example.com",
                    marketingConsent = MarketingConsentUiState(checked = false),
                    setMarketingConsent = {},
                    signOut = { signOutInvoked = true },
                    deleteAccount = { deleteInvoked = true },
                    subscription = null,
                    tokens = null,
                )
            }
        }

        compose.onNodeWithText(context.getString(R.string.delete_account)).performClick()
        compose.runOnIdle { assertTrue(deleteInvoked) }

        compose.onNodeWithText(context.getString(R.string.sign_out)).performClick()
        compose.onNodeWithText(context.getString(R.string.sign_out_confirm)).assertExists()
        compose.onNodeWithText(context.getString(R.string.no)).performClick()
        compose.onNodeWithText(context.getString(R.string.sign_out_confirm)).assertDoesNotExist()
        compose.runOnIdle { assertFalse(signOutInvoked) }

        compose.onNodeWithText(context.getString(R.string.sign_out)).performClick()
        compose.onNodeWithText(context.getString(R.string.yes)).performClick()
        compose.runOnIdle { assertTrue(signOutInvoked) }
    }

    @Test
    fun `subscription information handles missing expiry and a dated plan`() {
        var subscription by mutableStateOf<Subscription?>(null)

        compose.setContent {
            RuuviTheme {
                SubscriptionInfo(subscription)
            }
        }

        compose.onNodeWithText(context.getString(R.string.current_plan)).assertDoesNotExist()

        compose.runOnIdle {
            subscription = Subscription(
                name = "Ruuvi Pro",
                endTime = null,
                maxClaims = 10,
                maxShares = 20,
                maxSharesPerSensor = 5,
            )
        }
        compose.onNodeWithText("Ruuvi Pro").assertExists()
        compose.onNodeWithText(context.getString(R.string.none)).assertExists()

        val expiry = Date(1_893_456_000_000L)
        val formattedExpiry = DateUtils.formatDateTime(
            context,
            expiry.time,
            DateUtils.FORMAT_SHOW_DATE or
                DateUtils.FORMAT_SHOW_YEAR or
                DateUtils.FORMAT_ABBREV_MONTH,
        )
        compose.runOnIdle {
            subscription = subscription?.copy(endTime = expiry)
        }

        compose.onNodeWithText(formattedExpiry).assertExists()
        compose.onNodeWithText(context.getString(R.string.subscription_disclaimer)).assertExists()
    }
}
