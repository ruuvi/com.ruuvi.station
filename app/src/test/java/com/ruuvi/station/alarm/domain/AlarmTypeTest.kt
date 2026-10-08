package com.ruuvi.station.alarm.domain

import com.ruuvi.station.database.tables.Alarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AlarmTypeTest {
    @Test
    fun `VOC and NOx alert ranges span one through five hundred`() {
        listOf(AlarmType.VOC, AlarmType.NOX).forEach { alarmType ->
            assertEquals(1.0, alarmType.possibleRange.start, 0.0)
            assertEquals(500.0, alarmType.possibleRange.endInclusive, 0.0)
            assertFalse(alarmType.valueInRange(0.0))
            assertTrue(alarmType.valueInRange(1.0))
            assertTrue(alarmType.valueInRange(500.0))
            assertFalse(alarmType.valueInRange(501.0))
        }
    }

    @Test
    fun `new VOC and NOx alerts default to the complete range`() {
        val alarmsInteractor = mock<AlarmsInteractor>()

        listOf(AlarmType.VOC, AlarmType.NOX).forEach { alarmType ->
            whenever(alarmsInteractor.getRangeValue(alarmType, 1.0f)).thenReturn(1.0f)
            whenever(alarmsInteractor.getRangeValue(alarmType, 500.0f)).thenReturn(500.0f)
            whenever(alarmsInteractor.getDisplayValue(1.0f, 0)).thenReturn("1")
            whenever(alarmsInteractor.getDisplayValue(500.0f, 0)).thenReturn("500")

            val state = AlarmItemState.getDefaultState("sensor-id", alarmType, alarmsInteractor)

            assertEquals(1.0, state.min, 0.0)
            assertEquals(500.0, state.max, 0.0)
            assertEquals(1.0f, state.rangeLow)
            assertEquals(500.0f, state.rangeHigh)
            assertEquals("1", state.displayLow)
            assertEquals("500", state.displayHigh)
        }
    }

    @Test
    fun `legacy VOC alert with zero lower bound is displayed as one through five hundred`() {
        val alarmsInteractor = mock<AlarmsInteractor>()
        whenever(alarmsInteractor.getRangeValue(AlarmType.VOC, 1.0f)).thenReturn(1.0f)
        whenever(alarmsInteractor.getRangeValue(AlarmType.VOC, 500.0f)).thenReturn(500.0f)
        whenever(alarmsInteractor.getDisplayValue(1.0f, 0)).thenReturn("1")
        whenever(alarmsInteractor.getDisplayValue(500.0f, 0)).thenReturn("500")
        val legacyAlarm = Alarm(min = 0.0, max = 500.0, type = AlarmType.VOC.value)

        val state = AlarmItemState.getStateForDbAlarm(legacyAlarm, alarmsInteractor)

        assertEquals(1.0, state.min, 0.0)
        assertEquals(500.0, state.max, 0.0)
        assertEquals(1.0f, state.rangeLow)
        assertEquals(500.0f, state.rangeHigh)
    }

    @Test
    fun `getByNetworkCode matches camelCase, lowercase, and snake_case codes`() {
        assertEquals(AlarmType.DEW_POINT, AlarmType.getByNetworkCode("dewPoint"))
        assertEquals(AlarmType.DEW_POINT, AlarmType.getByNetworkCode("dewpoint"))
        assertEquals(AlarmType.DEW_POINT, AlarmType.getByNetworkCode("dew_point"))

        assertEquals(AlarmType.ABSOLUTE_HUMIDITY, AlarmType.getByNetworkCode("humidityAbsolute"))
        assertEquals(AlarmType.ABSOLUTE_HUMIDITY, AlarmType.getByNetworkCode("humidityabsolute"))
        assertEquals(AlarmType.ABSOLUTE_HUMIDITY, AlarmType.getByNetworkCode("humidity_absolute"))

        assertEquals(AlarmType.TEMPERATURE, AlarmType.getByNetworkCode("temperature"))
        assertEquals(AlarmType.HUMIDITY, AlarmType.getByNetworkCode("humidity"))
    }
}
