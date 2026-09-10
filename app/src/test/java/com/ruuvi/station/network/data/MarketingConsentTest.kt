package com.ruuvi.station.network.data

import com.ruuvi.station.network.data.request.MarketingConsentRequest
import com.ruuvi.station.network.data.response.MarketingConsentStatus
import com.ruuvi.station.feature.data.FeatureFlag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketingConsentTest {
    @Test
    fun `marketing consent feature is disabled by default`() {
        assertFalse(FeatureFlag.MARKETING_CONSENT.defaultValue)
    }

    @Test
    fun `app language is normalized for Sendy`() {
        assertEquals("DE", MarketingConsentRequest.normalizeLanguage("de"))
        assertEquals("EN", MarketingConsentRequest.normalizeLanguage("en-US"))
        assertEquals("EN", MarketingConsentRequest.normalizeLanguage(""))
    }

    @Test
    fun `only actionable statuses enable the toggle`() {
        assertTrue(MarketingConsentStatus.SUBSCRIBED.isToggleEnabled)
        assertTrue(MarketingConsentStatus.UNSUBSCRIBED.isToggleEnabled)
        assertTrue(MarketingConsentStatus.NOT_FOUND.isToggleEnabled)
        assertFalse(MarketingConsentStatus.UNCONFIRMED.isToggleEnabled)
        assertFalse(MarketingConsentStatus.BOUNCED.isToggleEnabled)
        assertFalse(MarketingConsentStatus.SOFT_BOUNCED.isToggleEnabled)
        assertFalse(MarketingConsentStatus.COMPLAINED.isToggleEnabled)
        assertFalse(MarketingConsentStatus.UNKNOWN.isToggleEnabled)
    }

    @Test
    fun `unknown server statuses fail closed`() {
        assertEquals(
            MarketingConsentStatus.UNKNOWN,
            MarketingConsentStatus.fromValue("new_status")
        )
        assertEquals(
            MarketingConsentStatus.UNKNOWN,
            MarketingConsentStatus.fromValue(null)
        )
    }
}
