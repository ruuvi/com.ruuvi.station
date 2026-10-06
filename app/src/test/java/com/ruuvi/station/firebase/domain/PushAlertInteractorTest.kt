package com.ruuvi.station.firebase.domain

import android.content.Context
import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlertNotificationInteractor
import com.ruuvi.station.database.domain.AlarmRepository
import com.ruuvi.station.firebase.data.AlertMessage
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.UnitType.TemperatureUnit
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class PushAlertInteractorTest {
    private val notificationInteractor = mockk<AlertNotificationInteractor>()
    private val unitsConverter = mockk<UnitsConverter>()
    private val alarmRepository = mockk<AlarmRepository>()
    private val interactor = PushAlertInteractor(
        alertNotificationInteractor = notificationInteractor,
        unitsConverter = unitsConverter,
        alarmRepository = alarmRepository,
    )
    private val context = mockk<Context>()

    @Test
    fun `dew point notification preserves a Fahrenheit threshold from the push payload`() {
        val message = alertMessage(
            currentValue = 51.0,
            thresholdValue = 50.0,
            alertUnit = TemperatureUnit.Fahrenheit.unitCode,
        )
        every { unitsConverter.getDisplayValue(50f) } returns "50"
        every { unitsConverter.getTemperatureUnitString(TemperatureUnit.Fahrenheit) } returns "°F"
        every {
            context.getString(R.string.alert_notification_dew_point_high_threshold, "50 °F")
        } returns "Dew point is above 50 °F"

        val result = interactor.getDewPointMessage(message, context)

        assertEquals("Dew point is above 50 °F", result)
        verify(exactly = 0) { unitsConverter.getTemperatureValue(any()) }
    }

    private fun alertMessage(
        currentValue: Double,
        thresholdValue: Double,
        alertUnit: String,
    ) = AlertMessage(
        name = "Kitchen",
        id = "AA:BB:CC:DD:EE:FF",
        alertType = "dewpoint",
        triggerType = "high",
        currentValue = currentValue,
        thresholdValue = thresholdValue,
        alertUnit = alertUnit,
        alertData = "",
        showLocallyFormatted = true,
        title = "",
        body = "",
        subtitle = "",
    )
}
