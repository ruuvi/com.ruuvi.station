package com.ruuvi.station.units.domain

import android.content.Context
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.units.domain.aqi.AQI
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class UnitsConverterAqiTest {
    private lateinit var context: Context
    private lateinit var unitsConverter: UnitsConverter

    @Before
    fun setUp() {
        context = mock()
        unitsConverter = UnitsConverter(
            context = context,
            preferences = mock<PreferencesRepository>(),
        )
    }

    @Test
    fun `AQI score and scale suffix are exposed separately`() {
        whenever(context.getString(any(), any(), any())).thenReturn("73")

        val result = unitsConverter.getAqiEnvironmentValue(AQI.CalculatedAQI(73.0))

        assertEquals("73", result.valueWithoutUnit)
        assertEquals("/100", result.unitString)
        assertEquals("73/100", result.valueWithUnit)
    }

    @Test
    fun `unavailable AQI retains separate scale suffix`() {
        val result = unitsConverter.getAqiEnvironmentValue(AQI.UndefinedAQI)

        assertEquals("-", result.valueWithoutUnit)
        assertEquals("/100", result.unitString)
        assertEquals("-/100", result.valueWithUnit)
    }
}
