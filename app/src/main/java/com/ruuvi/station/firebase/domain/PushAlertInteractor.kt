package com.ruuvi.station.firebase.domain

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlarmType
import com.ruuvi.station.alarm.receiver.CancelAlarmReceiver
import com.ruuvi.station.alarm.receiver.MuteAlarmReceiver
import com.ruuvi.station.database.domain.AlarmRepository
import com.ruuvi.station.database.tables.Alarm
import com.ruuvi.station.firebase.data.AlertMessage
import com.ruuvi.station.tagdetails.ui.TagDetailsActivity
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.HumidityUnit
import com.ruuvi.station.units.model.PressureUnit
import com.ruuvi.station.units.model.TemperatureUnit
import timber.log.Timber
import java.util.Date

class PushAlertInteractor(
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

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                createNotificationChannel(notificationManager)

                val notificationData = AlertNotificationData (
                    sensorId = message.id,
                    message = notificationMessage,
                    alarmId = localAlert?.id,
                    sensorName = message.name,
                    alertCustomDescription = message.alertData
                        )
                notificationManager.notify(localAlert?.id ?: -1, createNotification(context, notificationData))
            }
        }
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, importance)
            notificationManager.createNotificationChannel(channel)
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
            else -> null
        }
    }

    fun getHumidityMessage(message: AlertMessage, context: Context): String {
        val unit = HumidityUnit.getByCode(message.alertUnit.toInt()) ?: HumidityUnit.PERCENT

        val resource = if (message.currentValue < message.thresholdValue) {
            R.string.alert_notification_humidity_low_threshold
        } else {
            R.string.alert_notification_humidity_high_threshold
        }

        val displayThreshold = unitsConverter.getDisplayValue(message.thresholdValue.toFloat())
        return context.getString(resource, "$displayThreshold ${unitsConverter.getHumidityUnitString(unit)}")
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

    data class AlertNotificationData(
        val sensorId: String,
        val message: String,
        val sensorName: String,
        val alarmId: Int?,
        val alertCustomDescription: String
    )

    private fun createNotification(context: Context, notificationData: AlertNotificationData): Notification? {
        val alertId = notificationData.alarmId ?: -1
        val tagDetailsPendingIntent =
            TagDetailsActivity.createPendingIntent(
                context,
                notificationData.sensorId,
                alertId
            )
        val cancelPendingIntent =
            CancelAlarmReceiver.createPendingIntent(context, alertId)
        val mutePendingIntent = MuteAlarmReceiver.createPendingIntent(context, alertId)
        val actionDisable = NotificationCompat.Action(
            R.drawable.ic_ruuvi_app_notification_icon_v2,
            context.getString(R.string.alarm_notification_disable),
            cancelPendingIntent
        )
        val actionMute = NotificationCompat.Action(
            R.drawable.ic_ruuvi_app_notification_icon_v2,
            context.getString(R.string.alarm_mute_for_hour),
            mutePendingIntent
        )

        val bitmap = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
        var notification =  NotificationCompat
            .Builder(context, "notify_001")//AlarmCheckInteractor.CHANNEL_ID)
            .setContentTitle(notificationData.message)
            .setTicker("${notificationData.sensorName} ${notificationData.message}")
            .setStyle(
                NotificationCompat
                    .BigTextStyle()
                    .setBigContentTitle(notificationData.message)
                    .setSummaryText(notificationData.sensorName)
                    .bigText(notificationData.alertCustomDescription)
            )
            .setContentText(notificationData.alertCustomDescription)
            .setDefaults(Notification.DEFAULT_ALL)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(tagDetailsPendingIntent)
            .setLargeIcon(bitmap)
            .setSmallIcon(R.drawable.ic_ruuvi_app_notification_icon_v2)

        if (alertId != -1) {
            notification = notification
                .addAction(actionDisable)
                .addAction(actionMute)
        }

        return notification.build()
    }

    private fun getLocalAlert(sensorId: String, alarmType: AlarmType?): Alarm? {
        return alarmRepository.getForSensor(sensorId).firstOrNull{it.alarmType == alarmType}
    }

    private fun isLocallyMuted(alert: Alarm?): Boolean {
        val alarmMutedTill = alert?.mutedTill
        return alarmMutedTill != null && alarmMutedTill.time > Date().time
    }

    companion object {
        private const val CHANNEL_ID = "notify_001"
        private const val NOTIFICATION_CHANNEL_NAME = "Alert notifications"
    }
}