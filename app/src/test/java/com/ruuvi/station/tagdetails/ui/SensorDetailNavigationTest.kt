package com.ruuvi.station.tagdetails.ui

import com.ruuvi.station.tagsettings.ui.SensorSettingsRoutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorDetailNavigationTest {
    @Test
    fun `default destination follows dashboard preference`() {
        assertEquals(
            SensorDetailDestination.CARD,
            SensorCardOpenType.DEFAULT.resolveStartDestination(false).root,
        )
        assertEquals(
            SensorDetailDestination.HISTORY,
            SensorCardOpenType.DEFAULT.resolveStartDestination(true).root,
        )
    }

    @Test
    fun `remove launches the nested removal screen under settings`() {
        val destination = SensorCardOpenType.REMOVE.resolveStartDestination(false)

        assertEquals(SensorDetailDestination.SETTINGS, destination.root)
        assertEquals(SensorSettingsRoutes.SENSOR_REMOVE, destination.settingsRoute)
    }

    @Test
    fun `explicit root destinations are preserved`() {
        mapOf(
            SensorCardOpenType.CARD to SensorDetailDestination.CARD,
            SensorCardOpenType.HISTORY to SensorDetailDestination.HISTORY,
            SensorCardOpenType.ALERTS to SensorDetailDestination.ALERTS,
            SensorCardOpenType.SETTINGS to SensorDetailDestination.SETTINGS,
        ).forEach { (openType, expectedDestination) ->
            assertEquals(expectedDestination, openType.resolveStartDestination(false).root)
        }
    }

    @Test
    fun `only full sensor card allows horizontal sensor swiping`() {
        assertTrue(SensorDetailDestination.CARD.allowsSensorSwipe)
        assertFalse(SensorDetailDestination.HISTORY.allowsSensorSwipe)
        assertFalse(SensorDetailDestination.ALERTS.allowsSensorSwipe)
        assertFalse(SensorDetailDestination.SETTINGS.allowsSensorSwipe)
    }
}
