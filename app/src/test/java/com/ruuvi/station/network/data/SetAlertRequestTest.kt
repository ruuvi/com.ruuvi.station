package com.ruuvi.station.network.data

import com.ruuvi.station.alarm.domain.AlarmType
import com.ruuvi.station.database.tables.Alarm
import com.ruuvi.station.network.data.request.SetAlertRequest
import org.junit.Assert.assertEquals
import org.junit.Test

class SetAlertRequestTest {
    @Test
    fun `legacy VOC alert is normalized before cloud synchronization`() {
        val legacyAlarm = Alarm(
            min = 0.0,
            max = 500.0,
            type = AlarmType.VOC.value,
        )

        val request = SetAlertRequest.getAlarmRequest(legacyAlarm)

        assertEquals(1.0, request.min, 0.0)
        assertEquals(500.0, request.max, 0.0)
    }
}
