package com.ruuvi.station.tagdetails.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorDetailNavigationTest {
    @Test
    fun `default destination follows dashboard preference`() {
        assertEquals(SensorCardOpenType.CARD, SensorCardOpenType.DEFAULT.resolveRoot(false))
        assertEquals(SensorCardOpenType.HISTORY, SensorCardOpenType.DEFAULT.resolveRoot(true))
    }

    @Test
    fun `remove launches settings root before opening nested route`() {
        assertEquals(SensorCardOpenType.SETTINGS, SensorCardOpenType.REMOVE.resolveRoot(false))
    }

    @Test
    fun `explicit root destinations are preserved`() {
        listOf(
            SensorCardOpenType.CARD,
            SensorCardOpenType.HISTORY,
            SensorCardOpenType.ALERTS,
            SensorCardOpenType.SETTINGS,
        ).forEach { destination ->
            assertEquals(destination, destination.resolveRoot(false))
        }
    }

    @Test
    fun `only full sensor card allows horizontal sensor swiping`() {
        assertTrue(SensorCardOpenType.CARD.allowsSensorSwipe())
        assertFalse(SensorCardOpenType.HISTORY.allowsSensorSwipe())
        assertFalse(SensorCardOpenType.ALERTS.allowsSensorSwipe())
        assertFalse(SensorCardOpenType.SETTINGS.allowsSensorSwipe())
    }
}
