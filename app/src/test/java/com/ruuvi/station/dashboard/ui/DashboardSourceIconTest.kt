package com.ruuvi.station.dashboard.ui

import androidx.compose.ui.graphics.Color
import com.ruuvi.station.R
import com.ruuvi.station.tag.domain.UpdateSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardSourceIconTest {
    @Test
    fun `dark card background selects dark source icons`() {
        assertTrue(isDarkDashboardCard(Color(0xFF001D1B)))
        assertEquals(
            R.drawable.ic_icon_gateway_dark,
            UpdateSource.Cloud.dashboardIconResource(
                isStale = false,
                backgroundColor = Color(0xFF001D1B),
            ),
        )
        assertEquals(
            R.drawable.bt_icon_warning_dark,
            UpdateSource.Advertisement.dashboardIconResource(
                isStale = true,
                backgroundColor = Color(0xFF001D1B),
            ),
        )
    }

    @Test
    fun `light card background selects light source icons`() {
        assertFalse(isDarkDashboardCard(Color.White))
        assertEquals(
            R.drawable.ic_icon_gateway_light,
            UpdateSource.Cloud.dashboardIconResource(
                isStale = false,
                backgroundColor = Color.White,
            ),
        )
        assertEquals(
            R.drawable.bt_icon_warning_light,
            UpdateSource.Advertisement.dashboardIconResource(
                isStale = true,
                backgroundColor = Color.White,
            ),
        )
    }
}
