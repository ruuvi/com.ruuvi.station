package com.ruuvi.station.alarm.domain

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ruuvi.station.R
import com.ruuvi.station.alarm.receiver.CancelAlarmReceiver
import com.ruuvi.station.alarm.receiver.MuteAlarmReceiver
import com.ruuvi.station.database.domain.AlarmRepository
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.tables.Alarm
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.TagConverter
import com.ruuvi.station.tagdetails.ui.TagDetailsActivity
import timber.log.Timber
import java.util.Calendar

class AlarmCheckInteractor(
    private val context: Context,
    private val tagConverter: TagConverter,
    private val sensorHistoryRepository: SensorHistoryRepository,
    private val alarmRepository: AlarmRepository
) {
    private val lastFiredNotification = mutableMapOf<Int, Long>()
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun getStatus(ruuviTag: RuuviTag): AlarmStatus {
        val alarms = getEnabledAlarms(ruuviTag)
        val hasEnabledAlarm = alarms.isNotEmpty()
        alarms
            .forEach {
                val resourceId = getResourceId(it, ruuviTag)
                if (resourceId != NOTIFICATION_RESOURCE_ID) {
                    return AlarmStatus.TRIGGERED
                }
            }
        if (hasEnabledAlarm) return AlarmStatus.NO_TRIGGERED
        return AlarmStatus.NO_ALARM
    }

    fun check(ruuviTagEntity: RuuviTagEntity, sensorSettings: SensorSettings) {
        val ruuviTag = tagConverter.fromDatabase(ruuviTagEntity, sensorSettings)
        getEnabledAlarms(ruuviTag)
            .forEach { alarm ->
                val resourceId = getResourceId(alarm, ruuviTag, true)
                if (resourceId != NOTIFICATION_RESOURCE_ID && canNotify(alarm)) {
                    sendAlert(alarm, ruuviTag.id, ruuviTag.displayName, resourceId)
                }
            }
    }

    private fun getEnabledAlarms(ruuviTag: RuuviTag): List<Alarm> =
        alarmRepository.getForSensor(ruuviTag.id).filter { it.enabled }

    private fun getResourceId(it: Alarm, ruuviTag: RuuviTag, shouldCompareMovementCounter: Boolean = false): Int {
        return when (it.type) {
            Alarm.TEMPERATURE,
            Alarm.HUMIDITY,
            Alarm.PRESSURE,
            Alarm.RSSI -> compareWithAlarmRange(it, ruuviTag)
            Alarm.MOVEMENT -> {
                val readings: List<TagSensorReading> =
                    sensorHistoryRepository.getLatestForSensor(ruuviTag.id, 2)
                if (readings.size == 2) {
                    when {
                        shouldCompareMovementCounter && ruuviTag.dataFormat == FORMAT5 && readings.first().movementCounter != readings.last().movementCounter -> R.string.alert_notification_movement
                        hasTagMoved(readings.first(), readings.last()) -> R.string.alert_notification_movement
                        else -> NOTIFICATION_RESOURCE_ID
                    }
                } else {
                    NOTIFICATION_RESOURCE_ID
                }
            }
            else -> NOTIFICATION_RESOURCE_ID
        }
    }

    private fun compareWithAlarmRange(alarm: Alarm, tag: RuuviTag): Int {
        return when (alarm.type) {
            Alarm.TEMPERATURE ->
                if (tag.temperature != null) {
                    getComparisonResourceId(
                    tag.temperature,
                    alarm.low to alarm.high,
                    R.string.alert_notification_temperature_low to
                        R.string.alert_notification_temperature_high
                    )
                } else {
                    NOTIFICATION_RESOURCE_ID
                }
            Alarm.HUMIDITY ->
                if (tag.humidity != null) {
                    getComparisonResourceId(
                        tag.humidity,
                        alarm.low to alarm.high,
                        R.string.alert_notification_humidity_low to
                            R.string.alert_notification_humidity_high
                    )
                } else {
                    NOTIFICATION_RESOURCE_ID
                }
            Alarm.PRESSURE ->
                if (tag.pressure != null) {
                    getComparisonResourceId(
                        tag.pressure,
                        alarm.low to alarm.high,
                        R.string.alert_notification_pressure_low to
                            R.string.alert_notification_pressure_high
                    )
                } else {
                    NOTIFICATION_RESOURCE_ID
                }

            Alarm.RSSI ->
                getComparisonResourceId(
                    tag.rssi,
                    alarm.low to alarm.high,
                    R.string.alert_notification_rssi_low to
                        R.string.alert_notification_rssi_high
                )
            else -> NOTIFICATION_RESOURCE_ID
        }
    }

    private fun getComparisonResourceId(
        comparedValue: Number,
        lowHigh: Pair<Int, Int>,
        resources: Pair<Int, Int>
    ): Int {
        val (low, high) = lowHigh
        val (lowResourceId, highResourceId) = resources
        return when {
            comparedValue.toDouble() < low -> lowResourceId
            comparedValue.toDouble() > high -> highResourceId
            else -> NOTIFICATION_RESOURCE_ID
        }
    }

    private fun hasTagMoved(one: TagSensorReading, two: TagSensorReading): Boolean {
        val threshold = 0.03
        return diff(one.accelZ, two.accelZ) > threshold ||
            diff(one.accelX, two.accelX) > threshold ||
            diff(one.accelY, two.accelY) > threshold
    }

    private fun diff(one: Double, two: Double): Double {
        return Math.abs(one - two)
    }

    private fun canNotify(alarm: Alarm): Boolean {
        val lastNotificationTime = lastFiredNotification[alarm.id]
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        calendar.add(Calendar.SECOND, -10)
        val notificationThreshold = calendar.timeInMillis
        val alarmMutedTill = alarm.mutedTill
        val muted = alarmMutedTill != null && alarmMutedTill.time > now
        return if (!muted && (lastNotificationTime == null || lastNotificationTime < notificationThreshold)) {
            lastFiredNotification[alarm.id] = now
            true
        } else {
            false
        }
    }

    private fun sendAlert(alarm: Alarm, tagId: String, tagName: String, notificationResourceId: Int) {
        Timber.d("sendAlert tag.tagName = $tagName; alarm.id = ${alarm.id}; notificationResourceId = $notificationResourceId")
        createNotificationChannel()
        val notification =
            createNotification(context, alarm, tagId, tagName, notificationResourceId)
        notificationManager.notify(alarm.id, notification)
    }

    private fun createNotification(context: Context, alarm: Alarm, tagId: String, tagName: String, notificationResourceId: Int): Notification? {
        val tagDetailsPendingIntent = TagDetailsActivity.createPendingIntent(context, tagId, alarm.id)
        val cancelPendingIntent = CancelAlarmReceiver.createPendingIntent(context, alarm.id)
        val mutePendingIntent = MuteAlarmReceiver.createPendingIntent(context, alarm.id)
        val action = NotificationCompat.Action(R.drawable.ic_ruuvi_app_notification_icon_v2, context.getString(R.string.alarm_notification_disable), cancelPendingIntent)
        val actionMute = NotificationCompat.Action(R.drawable.ic_ruuvi_app_notification_icon_v2, context.getString(R.string.alarm_mute_for_hour), mutePendingIntent)

        val bitmap = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
        return NotificationCompat
            .Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(notificationResourceId))
            .setTicker("$tagName ${context.getString(notificationResourceId)}")
            .setStyle(
                NotificationCompat
                    .BigTextStyle()
                    .setBigContentTitle(context.getString(notificationResourceId))
                    .setSummaryText(tagName).
                    bigText(alarm.customDescription)
            )
            .setContentText(alarm.customDescription)
            .setDefaults(Notification.DEFAULT_ALL)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(tagDetailsPendingIntent)
            .setLargeIcon(bitmap)
            .setSmallIcon(R.drawable.ic_ruuvi_app_notification_icon_v2)
            .addAction(action)
            .addAction(actionMute)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, importance)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun removeNotificationById(notificationId: Int) {
        Timber.d("dismissNotification with id = $notificationId")
        if (notificationId != -1) notificationManager.cancel(notificationId)
    }

    companion object {
        private const val NOTIFICATION_RESOURCE_ID = -9001
        private const val CHANNEL_ID = "notify_001"
        private const val NOTIFICATION_CHANNEL_NAME = "Alert notifications"
        private const val FORMAT5 = 5
    }
}

enum class AlarmStatus {
    TRIGGERED,
    NO_TRIGGERED,
    NO_ALARM
}