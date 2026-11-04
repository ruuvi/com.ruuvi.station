package com.ruuvi.station.dashboard.ui

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.theme.RuuviTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = DashboardMarketingConsentTestApplication::class, sdk = [35])
class DashboardMarketingConsentDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `prompt accepts marketing consent`() {
        var accepted = false

        composeTestRule.setContent {
            RuuviTheme {
                MarketingConsentPromptDialog(
                    isSaving = false,
                    showSaveError = false,
                    onAccept = { accepted = true },
                    onDecline = {}
                )
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.ok)).performClick()

        assertTrue(accepted)
    }

    @Test
    fun `prompt declines marketing consent`() {
        var declined = false

        composeTestRule.setContent {
            RuuviTheme {
                MarketingConsentPromptDialog(
                    isSaving = false,
                    showSaveError = false,
                    onAccept = {},
                    onDecline = { declined = true }
                )
            }
        }

        class DashboardMarketingConsentTestApplication : Application()

        composeTestRule.onNodeWithText(
            context.getString(R.string.marketing_consent_prompt_decline)
        ).performClick()

        assertTrue(declined)
    }

    @Test
    fun `saving prompt disables consent actions`() {
        composeTestRule.setContent {
            RuuviTheme {
                MarketingConsentPromptDialog(
                    isSaving = true,
                    showSaveError = false,
                    onAccept = {},
                    onDecline = {}
                )
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.ok)).assertIsNotEnabled()
        composeTestRule.onNodeWithText(
            context.getString(R.string.marketing_consent_prompt_decline)
        ).assertIsNotEnabled()
    }

    @Test
    fun `error prompt displays retry action`() {
        composeTestRule.setContent {
            RuuviTheme {
                MarketingConsentPromptDialog(
                    isSaving = false,
                    showSaveError = true,
                    onAccept = {},
                    onDecline = {}
                )
            }
        }

        composeTestRule.onNodeWithText(
            context.getString(R.string.internet_connection_problem)
        ).assertExists()
        composeTestRule.onNodeWithText(context.getString(R.string.try_again)).assertExists()
    }

    @Test
    fun `saved dialog dismisses`() {
        var dismissed = false

        composeTestRule.setContent {
            RuuviTheme {
                MarketingConsentSavedDialog(onDismiss = { dismissed = true })
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.ok)).performClick()

        assertTrue(dismissed)
    }
}

class DashboardMarketingConsentTestApplication : Application()
