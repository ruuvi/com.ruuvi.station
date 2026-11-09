package com.ruuvi.station.util.ui

import android.app.Application
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = ComposeDpPixelsTestApplication::class, sdk = [35])
class ComposeDpPixelsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `dpToPx converts Dp to pixels correctly`() {
        var calculatedPx = 0f

        composeTestRule.setContent {
            calculatedPx = 16.dp.dpToPx()
        }

        assertEquals(16f, calculatedPx, 0.5f)
    }

    @Test
    fun `pxToDp converts int pixels to Dp correctly`() {
        var calculatedDp = 0.dp

        composeTestRule.setContent {
            calculatedDp = 16.pxToDp()
        }

        assertEquals(16.dp, calculatedDp)
    }

    @Test
    fun `pxToDp converts float pixels to Dp correctly`() {
        var calculatedDp = 0.dp

        composeTestRule.setContent {
            calculatedDp = 24.5f.pxToDp()
        }

        assertEquals(24.5.dp.value, calculatedDp.value, 0.1f)
    }

    @Test
    fun `mmToDp converts millimeters to Dp`() {
        var calculatedDp = 0.dp

        composeTestRule.setContent {
            calculatedDp = 10.mmToDp()
        }

        // 10mm should result in a positive Dp value
        assertEquals(true, calculatedDp.value > 0f)
    }
}

class ComposeDpPixelsTestApplication : Application()
