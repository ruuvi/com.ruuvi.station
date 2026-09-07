package com.ruuvi.station.widgets.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetTypeTest {
    @Test
    fun `persisted codes remain compatible with released widgets`() {
        val expectedTypesByCode = mapOf(
            1 to WidgetType.TEMPERATURE,
            2 to WidgetType.HUMIDITY,
            3 to WidgetType.PRESSURE,
            4 to WidgetType.MOVEMENT,
            5 to WidgetType.VOLTAGE,
            6 to WidgetType.SIGNAL_STRENGTH,
            7 to WidgetType.ACCELERATION_X,
            8 to WidgetType.ACCELERATION_Y,
            9 to WidgetType.ACCELERATION_Z,
            10 to WidgetType.SOUND_AVERAGE,
            11 to WidgetType.SOUND_PEAK,
            12 to WidgetType.MEASUREMENT_SEQUENCE_NUMBER,
            13 to WidgetType.TEMPERATURE_F,
            14 to WidgetType.TEMPERATURE_K,
            15 to WidgetType.HUMIDITY_ABSOLUTE,
            16 to WidgetType.DEW_POINT_C,
            17 to WidgetType.DEW_POINT_F,
            18 to WidgetType.DEW_POINT_K,
            19 to WidgetType.PRESSURE_PA,
            20 to WidgetType.PRESSURE_MMHG,
            21 to WidgetType.AIR_QUALITY,
            22 to WidgetType.LUMINOSITY,
            23 to WidgetType.CO2,
            24 to WidgetType.NOX,
            25 to WidgetType.PM10,
            26 to WidgetType.PM25,
            27 to WidgetType.PM40,
            28 to WidgetType.PM100,
            29 to WidgetType.VOC,
            30 to WidgetType.PRESSURE_INHG,
        )

        assertEquals(expectedTypesByCode, WidgetType.entries.associateBy(WidgetType::code))
        assertEquals(WidgetType.SOUND_AVERAGE, WidgetType.getByCode(31))
        assertTrue(WidgetType.entries.map(WidgetType::code).let { it.size == it.distinct().size })
    }
}
