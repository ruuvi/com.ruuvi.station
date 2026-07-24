package com.ruuvi.station.widgets.data

import com.ruuvi.station.bluetooth.contract.FoundRuuviTag
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class DecodedSensorDataTest {
    @Test
    fun `decoded cloud data retains sound and measurement sequence values`() {
        val foundTag = mockk<FoundRuuviTag>(relaxed = true)
        every { foundTag.dBaAvg } returns 42.5
        every { foundTag.dBaPeak } returns 73.25
        every { foundTag.measurementSequenceNumber } returns 12_345
        val updatedAt = Date(1_234_567)

        val decoded = DecodedSensorData(foundTag, updatedAt)

        assertEquals(42.5, decoded.dBaAvg ?: Double.NaN, 0.0)
        assertEquals(73.25, decoded.dBaPeak ?: Double.NaN, 0.0)
        assertEquals(12_345, decoded.measurementSequenceNumber)
        assertEquals(updatedAt, decoded.updatedAt)
    }
}
