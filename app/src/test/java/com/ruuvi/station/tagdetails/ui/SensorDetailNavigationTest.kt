package com.ruuvi.station.tagdetails.ui

import com.ruuvi.station.tagsettings.ui.SensorSettingsRoutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorDetailNavigationTest {
    @Test
    fun `default destination follows dashboard preference`() {
        val cardDestination = SensorCardOpenType.DEFAULT.resolveStartDestination(false)
        val historyDestination = SensorCardOpenType.DEFAULT.resolveStartDestination(true)

        assertEquals(SensorDetailDestination.CARD, cardDestination.root)
        assertEquals(SensorDetailDestination.HISTORY, historyDestination.root)
        assertFalse(cardDestination.startsInNestedSettings)
        assertFalse(historyDestination.startsInNestedSettings)
    }

    @Test
    fun `remove launches the nested removal screen under settings`() {
        val destination = SensorCardOpenType.REMOVE.resolveStartDestination(false)

        assertEquals(SensorDetailDestination.SETTINGS, destination.root)
        assertEquals(SensorSettingsRoutes.SENSOR_REMOVE, destination.settingsRoute)
        assertTrue(destination.startsInNestedSettings)
    }

    @Test
    fun `explicit root destinations are preserved`() {
        mapOf(
            SensorCardOpenType.CARD to SensorDetailDestination.CARD,
            SensorCardOpenType.HISTORY to SensorDetailDestination.HISTORY,
            SensorCardOpenType.ALERTS to SensorDetailDestination.ALERTS,
            SensorCardOpenType.SETTINGS to SensorDetailDestination.SETTINGS,
        ).forEach { (openType, expectedDestination) ->
            val destination = openType.resolveStartDestination(false)
            assertEquals(expectedDestination, destination.root)
            assertFalse(destination.startsInNestedSettings)
        }
    }

    @Test
    fun `all root destinations allow horizontal sensor swiping`() {
        SensorDetailDestination.entries.forEach { destination ->
            assertTrue(destination.allowsTitleSensorSwipe)
        }
    }

    @Test
    fun `only full sensor card allows swiping directly on content`() {
        assertTrue(SensorDetailDestination.CARD.allowsBodySensorSwipe)
        assertFalse(SensorDetailDestination.HISTORY.allowsBodySensorSwipe)
        assertFalse(SensorDetailDestination.ALERTS.allowsBodySensorSwipe)
        assertFalse(SensorDetailDestination.SETTINGS.allowsBodySensorSwipe)
    }

    @Test
    fun `all root destinations retain the sensor background`() {
        SensorDetailDestination.entries.forEach { destination ->
            assertTrue(destination.usesSensorBackground)
        }
    }

    @Test
    fun `footer is limited to measurements and history`() {
        assertTrue(SensorDetailDestination.CARD.showsSensorFooter)
        assertTrue(SensorDetailDestination.HISTORY.showsSensorFooter)
        assertFalse(SensorDetailDestination.ALERTS.showsSensorFooter)
        assertFalse(SensorDetailDestination.SETTINGS.showsSensorFooter)
    }

    @Test
    fun `alerts and settings always use the dark theme`() {
        assertFalse(SensorDetailDestination.CARD.forcesDarkTheme)
        assertFalse(SensorDetailDestination.HISTORY.forcesDarkTheme)
        assertTrue(SensorDetailDestination.ALERTS.forcesDarkTheme)
        assertTrue(SensorDetailDestination.SETTINGS.forcesDarkTheme)
    }
}
