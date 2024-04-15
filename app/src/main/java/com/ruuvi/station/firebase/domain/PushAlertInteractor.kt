package com.ruuvi.station.firebase.domain

import android.content.Context
import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlarmType
import com.ruuvi.station.alarm.domain.AlertNotificationInteractor
import com.ruuvi.station.database.domain.AlarmRepository
import com.ruuvi.station.database.tables.Alarm
import com.ruuvi.station.firebase.data.AlertMessage
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.HumidityUnit
import com.ruuvi.station.units.model.PressureUnit
import com.ruuvi.station.units.model.TemperatureUnit
import timber.log.Timber
import java.util.Date

class PushAlertInteractor(
    val alertNotificationInteractor: AlertNotificationInteractor,
    val unitsConverter: UnitsConverter,
    val alarmRepository: AlarmRepository
) {
    fun processAlertPush(message: AlertMessage, context: Context) {
        message.alarmType.let { alarmType ->
            val notificationMessage = getMessage(message, context)
            if (notificationMessage != null) {
                val localAlert = getLocalAlert(message.id, message.alarmType)
                if (isLocallyMuted(localAlert)) {
                    Timber.d("Alert is locally muted till $localAlert. Message: $message")
                    return
                }

                val notificationData = AlertNotificationInteractor.AlertNotificationData(
                    sensorId = message.id,
                    message = notificationMessage,
                    alarmId = localAlert?.id,
                    sensorName = message.name,
                    alertCustomDescription = message.alertData
                )

                alertNotificationInteractor.notify(localAlert?.id ?: -1, notificationData)
            }
        }
    }

    fun getMessage(message: AlertMessage, context: Context): String? {
        return when (message.alarmType) {
            AlarmType.HUMIDITY -> {
                getHumidityMessage(message, context)
            }
            AlarmType.PRESSURE -> {
                getPressureMessage(message, context)
            }
            AlarmType.TEMPERATURE -> {
                getTemperatureMessage(message, context)
            }
            AlarmType.RSSI -> {
                getRssiMessage(message, context)
            }
            AlarmType.MOVEMENT -> context.getString(R.string.alert_notification_movement)
            AlarmType.OFFLINE -> getOfflineMessage(message, context)
            else -> null
        }
    }

    fun getHumidityMessage(message: AlertMessage, context: Context): String {
        val resource = if (message.currentValue < message.thresholdValue) {
            R.string.alert_notification_humidity_low_threshold
        } else {
            R.string.alert_notification_humidity_high_threshold
        }

        val displayThreshold = unitsConverter.getDisplayValue(message.thresholdValue.toFloat())
        return context.getString(resource, "$displayThreshold ${unitsConverter.getHumidityUnitString(HumidityUnit.PERCENT)}")
    }

    fun getPressureMessage(message: AlertMessage, context: Context): String {
        val unit = PressureUnit.getByCode(message.alertUnit.toInt()) ?: PressureUnit.HPA

        val resource = if (message.currentValue < message.thresholdValue) {
            R.string.alert_notification_pressure_low_threshold
        } else {
            R.string.alert_notification_pressure_high_threshold
        }

        val displayThreshold = unitsConverter.getDisplayValue(message.thresholdValue.toFloat())
        return context.getString(resource, "$displayThreshold ${unitsConverter.getPressureUnitString(unit)}")
    }

    fun getTemperatureMessage(message: AlertMessage, context: Context): String {
        val unit = TemperatureUnit.getByCode(message.alertUnit) ?: TemperatureUnit.CELSIUS

        val resource = if (message.currentValue < message.thresholdValue) {
            R.string.alert_notification_temperature_low_threshold
        } else {
            R.string.alert_notification_temperature_high_threshold
        }

        val displayThreshold = unitsConverter.getDisplayValue(message.thresholdValue.toFloat())
        return context.getString(resource, "$displayThreshold ${unitsConverter.getTemperatureUnitString(unit)}")
    }

    fun getRssiMessage(message: AlertMessage, context: Context): String {
        val resource = if (message.currentValue < message.thresholdValue) {
            R.string.alert_notification_rssi_low_threshold
        } else {
            R.string.alert_notification_rssi_high_threshold
        }

        val displayThreshold = unitsConverter.getDisplayValue(message.thresholdValue.toFloat())
        return context.getString(resource, "$displayThreshold ${unitsConverter.getSignalUnit()}")
    }

    fun getOfflineMessage(message: AlertMessage, context: Context): String {
        return context.getString(R.string.alert_notification_offline_message, message.thresholdValue.toInt().toString())
    }


    private fun getLocalAlert(sensorId: String, alarmType: AlarmType?): Alarm? {
        return alarmRepository.getForSensor(sensorId).firstOrNull{it.alarmType == alarmType}
    }

    private fun isLocallyMuted(alert: Alarm?): Boolean {
        val alarmMutedTill = alert?.mutedTill
        return alarmMutedTill != null && alarmMutedTill.time > Date().time
    }
}