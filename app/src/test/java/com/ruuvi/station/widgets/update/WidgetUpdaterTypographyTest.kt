package com.ruuvi.station.widgets.update

import android.content.res.Configuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetUpdaterTypographyTest {
    @Test
    fun `legacy tablet baseline is recaptured while currently 2 by 1`() {
        assertTrue(
            shouldMigrateSimpleWidgetTypographySize(
                hasValidCapturedSize = true,
                captureVersion = null,
                capturedOrientation = null,
                currentOrientation = Configuration.ORIENTATION_PORTRAIT,
                isTablet = true,
                observedSizeIsTwoByOne = true,
            )
        )
    }

    @Test
    fun `current tablet baseline is not recaptured`() {
        assertFalse(
            shouldMigrateSimpleWidgetTypographySize(
                hasValidCapturedSize = true,
                captureVersion = 2,
                capturedOrientation = Configuration.ORIENTATION_PORTRAIT,
                currentOrientation = Configuration.ORIENTATION_PORTRAIT,
                isTablet = true,
                observedSizeIsTwoByOne = true,
            )
        )
    }

    @Test
    fun `tablet 2 by 1 baseline is recaptured after orientation change`() {
        assertTrue(
            shouldMigrateSimpleWidgetTypographySize(
                hasValidCapturedSize = true,
                captureVersion = 2,
                capturedOrientation = Configuration.ORIENTATION_PORTRAIT,
                currentOrientation = Configuration.ORIENTATION_LANDSCAPE,
                isTablet = true,
                observedSizeIsTwoByOne = true,
            )
        )
    }

    @Test
    fun `tablet orientation change outside 2 by 1 keeps existing baseline`() {
        assertFalse(
            shouldMigrateSimpleWidgetTypographySize(
                hasValidCapturedSize = true,
                captureVersion = 2,
                capturedOrientation = Configuration.ORIENTATION_PORTRAIT,
                currentOrientation = Configuration.ORIENTATION_LANDSCAPE,
                isTablet = true,
                observedSizeIsTwoByOne = false,
            )
        )
    }

    @Test
    fun `portrait uses matching minimum width and maximum height`() {
        assertEquals(
            SimpleWidgetHostSize(width = 320, height = 180),
            resolveSimpleWidgetHostSize(
                minimumWidth = 320,
                minimumHeight = 140,
                maximumWidth = 420,
                maximumHeight = 180,
                isLandscape = false,
            )
        )
    }

    @Test
    fun `landscape uses matching maximum width and minimum height`() {
        assertEquals(
            SimpleWidgetHostSize(width = 420, height = 140),
            resolveSimpleWidgetHostSize(
                minimumWidth = 320,
                minimumHeight = 140,
                maximumWidth = 420,
                maximumHeight = 180,
                isLandscape = true,
            )
        )
    }

    @Test
    fun `previously captured 4 by 4 size is not a valid baseline`() {
        assertFalse(isSimpleWidgetTwoByOneSize(width = 360, height = 220))
    }

    @Test
    fun `captured 2 by 1 size remains a valid baseline`() {
        assertTrue(isSimpleWidgetTwoByOneSize(width = 180, height = 110))
    }

    @Test
    fun `wide tablet 2 by 1 size establishes typography baseline`() {
        assertTrue(
            shouldCaptureSimpleWidgetTypographySize(
                alreadyCaptured = false,
                width = 320,
                height = 140,
                isTablet = true,
            )
        )
    }

    @Test
    fun `landscape tablet 2 by 1 size establishes typography baseline`() {
        assertTrue(
            shouldCaptureSimpleWidgetTypographySize(
                alreadyCaptured = false,
                width = 420,
                height = 140,
                isTablet = true,
            )
        )
    }

    @Test
    fun `tablet 3 by 1 size does not establish typography baseline`() {
        assertFalse(
            shouldCaptureSimpleWidgetTypographySize(
                alreadyCaptured = false,
                width = 480,
                height = 140,
                isTablet = true,
            )
        )
    }

    @Test
    fun `tablet 2 by 2 size does not establish typography baseline`() {
        assertFalse(
            shouldCaptureSimpleWidgetTypographySize(
                alreadyCaptured = false,
                width = 320,
                height = 280,
                isTablet = true,
            )
        )
    }

    @Test
    fun `configured 2 by 1 size establishes typography baseline`() {
        assertTrue(
            shouldCaptureSimpleWidgetTypographySize(
                alreadyCaptured = false,
                width = 180,
                height = 110,
            )
        )
    }

    @Test
    fun `configured 4 by 4 size does not establish typography baseline`() {
        assertFalse(
            shouldCaptureSimpleWidgetTypographySize(
                alreadyCaptured = false,
                width = 360,
                height = 220,
            )
        )
    }

    @Test
    fun `one column size does not establish typography baseline`() {
        assertFalse(
            shouldCaptureSimpleWidgetTypographySize(
                alreadyCaptured = false,
                width = 82,
                height = 110,
            )
        )
    }

    @Test
    fun `two row size does not establish typography baseline`() {
        assertFalse(
            shouldCaptureSimpleWidgetTypographySize(
                alreadyCaptured = false,
                width = 180,
                height = 220,
            )
        )
    }

    @Test
    fun `captured typography baseline remains fixed for widget lifetime`() {
        assertFalse(
            shouldCaptureSimpleWidgetTypographySize(
                alreadyCaptured = true,
                width = 180,
                height = 110,
            )
        )
    }
}
