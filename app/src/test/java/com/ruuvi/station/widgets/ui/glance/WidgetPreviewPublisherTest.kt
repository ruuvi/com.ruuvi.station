package com.ruuvi.station.widgets.ui.glance

import android.appwidget.AppWidgetProviderInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetPreviewPublisherTest {
    private val baseline = PreviewFingerprintInput(
        schemaVersion = 1,
        versionCode = 100,
        packageLastUpdateTime = 1_000L,
        fontScale = 1f,
        densityDpi = 420,
        nightMode = 0x10,
        localeTags = "en-US"
    )

    @Test
    fun `identical preview configurations have the same fingerprint`() {
        assertEquals(fingerprint(baseline), fingerprint(baseline.copy()))
    }

    @Test
    fun `every rendered preview input changes the fingerprint`() {
        val baselineFingerprint = fingerprint(baseline)

        assertNotEquals(
            baselineFingerprint,
            fingerprint(baseline.copy(schemaVersion = baseline.schemaVersion + 1))
        )
        assertNotEquals(
            baselineFingerprint,
            fingerprint(baseline.copy(versionCode = baseline.versionCode + 1))
        )
        assertNotEquals(
            baselineFingerprint,
            fingerprint(baseline.copy(packageLastUpdateTime = 2_000L))
        )
        assertNotEquals(
            baselineFingerprint,
            fingerprint(baseline.copy(fontScale = 1.3f))
        )
        assertNotEquals(
            baselineFingerprint,
            fingerprint(baseline.copy(densityDpi = 560))
        )
        assertNotEquals(
            baselineFingerprint,
            fingerprint(baseline.copy(nightMode = 0x20))
        )
        assertNotEquals(
            baselineFingerprint,
            fingerprint(baseline.copy(localeTags = "fi-FI"))
        )
    }

    @Test
    fun `home screen preview presence follows framework category flags`() {
        val homeScreen = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN
        val keyguard = AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD

        assertTrue(includesHomeScreenGeneratedPreview(homeScreen))
        assertTrue(includesHomeScreenGeneratedPreview(homeScreen or keyguard))
        assertFalse(includesHomeScreenGeneratedPreview(0))
        assertFalse(includesHomeScreenGeneratedPreview(keyguard))
    }

    @Test
    fun `matching fingerprint is skipped only while framework preview exists`() {
        assertFalse(
            generatedPreviewNeedsPublishing(
                appliedFingerprint = "current",
                currentFingerprint = "current",
                frameworkPreviewIsPresent = true
            )
        )
        assertTrue(
            generatedPreviewNeedsPublishing(
                appliedFingerprint = "current",
                currentFingerprint = "current",
                frameworkPreviewIsPresent = false
            )
        )
        assertTrue(
            generatedPreviewNeedsPublishing(
                appliedFingerprint = "old",
                currentFingerprint = "current",
                frameworkPreviewIsPresent = true
            )
        )
    }

    private fun fingerprint(input: PreviewFingerprintInput) = widgetPreviewFingerprint(
        schemaVersion = input.schemaVersion,
        versionCode = input.versionCode,
        packageLastUpdateTime = input.packageLastUpdateTime,
        fontScale = input.fontScale,
        densityDpi = input.densityDpi,
        nightMode = input.nightMode,
        localeTags = input.localeTags
    )

    private data class PreviewFingerprintInput(
        val schemaVersion: Int,
        val versionCode: Int,
        val packageLastUpdateTime: Long,
        val fontScale: Float,
        val densityDpi: Int,
        val nightMode: Int,
        val localeTags: String
    )
}
