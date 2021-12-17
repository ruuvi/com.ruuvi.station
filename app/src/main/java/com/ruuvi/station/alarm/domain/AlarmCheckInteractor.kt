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
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.HumidityUnit
import com.ruuvi.station.util.extensions.diff
import timber.log.Timber
import java.util.Calendar

class AlarmCheckInteractor(
    private val context: Context,
    private val tagConverter: TagConverter,
    private val sensorHistoryRepository: SensorHistoryRepository,
    private val alarmRepository: AlarmRepository,
    private val unitsConverter: UnitsConverter
) {
    private val lastFiredNotification = mutableMapOf<Int, Long>()
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun getStatus(ruuviTag: RuuviTag): AlarmStatus {
        val alarms = getEnabledAlarms(ruuviTag)
        val hasEnabledAlarm = alarms.isNotEmpty()
        alarms
            .forEach { alarm ->
                if (AlarmChecker(ruuviTag, alarm).triggered) {
                    return AlarmStatus.TRIGGERED
                }
            }
        if (hasEnabledAlarm) return AlarmStatus.NO_TRIGGERED
        return AlarmStatus.NO_ALARM
    }

    fun checkAlarmsForSensor(sensor: RuuviTagEntity, sensorSettings: SensorSettings) {
        val ruuviTag = tagConverter.fromDatabase(sensor, sensorSettings)
        getEnabledAlarms(ruuviTag)
            .forEach { alarm ->
                val checker = AlarmChecker(ruuviTag, alarm)
                if (checker.triggered && canNotify(alarm)) {
                    sendAlert(checker)
                }
            }
    }

    private fun getEnabledAlarms(ruuviTag: RuuviTag): List<Alarm> =
        alarmRepository.getForSensor(ruuviTag.id).filter { it.enabled }

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

    private fun sendAlert(checker: AlarmChecker) {
        Timber.d("sendAlert tag.tagName = ${checker.ruuviTag}tagName; alarm.id = ${checker.alarm.id}; notificationResourceId = ${checker.alarmResource}")
        createNotificationChannel()
        val notification =
            createNotification(checker)
        notification?.let {
            notificationManager.notify(checker.alarm.id, notification)
        }
    }

    private fun createNotification(checker: AlarmChecker): Notification? {
        val message = checker.getMessage()
        if (!message.isNullOrEmpty()) {
            val tagDetailsPendingIntent =
                TagDetailsActivity.createPendingIntent(
                    context,
                    checker.ruuviTag.id,
                    checker.alarm.id
                )
            val cancelPendingIntent =
                CancelAlarmReceiver.createPendingIntent(context, checker.alarm.id)
            val mutePendingIntent = MuteAlarmReceiver.createPendingIntent(context, checker.alarm.id)
            val action = NotificationCompat.Action(
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
            return NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setContentTitle(message)
                .setTicker("${checker.ruuviTag.displayName} $message")
                .setStyle(
                    NotificationCompat
                        .BigTextStyle()
                        .setBigContentTitle(message)
                        .setSummaryText(checker.ruuviTag.displayName)
                        .bigText(checker.alarm.customDescription)
                )
                .setContentText(checker.alarm.customDescription)
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
        return null
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

    inner class AlarmChecker(
        val ruuviTag: RuuviTag,
        val alarm: Alarm
    ) {
        var alarmResource: Int? = null
        private var thresholdValue: Int = 0

        val triggered: Boolean
            get() = alarmResource != null

        init {
            checkAlarmStatus()
        }

        fun getMessage(): String? {
            alarmResource?.let { resource ->
                return when (alarm.alarmType) {
                    AlarmType.HUMIDITY ->
                        context.getString(resource, "$thresholdValue ${unitsConverter.getHumidityUnitString(HumidityUnit.PERCENT)}")
                    AlarmType.PRESSURE -> {
                        val thresholdString = "${unitsConverter.getPressureValue(thresholdValue.toDouble()).toInt()} ${unitsConverter.getPressureUnitString()}"
                        context.getString(resource, thresholdString)
                    }
                    AlarmType.TEMPERATURE -> {
                        val thresholdString = "${unitsConverter.getTemperatureValue(thresholdValue.toDouble()).toInt()}${unitsConverter.getTemperatureUnitString()}"
                        context.getString(resource, thresholdString)
                    }
                    AlarmType.RSSI -> {
                        context.getString(resource, unitsConverter.getSignalString(thresholdValue))
                    }
                    AlarmType.MOVEMENT -> context.getString(resource)
                    else -> null
                }
            }
            return null
        }

        private fun checkAlarmStatus() {
            when (alarm.alarmType) {
                AlarmType.HUMIDITY,
                AlarmType.PRESSURE,
                AlarmType.TEMPERATURE,
                AlarmType.RSSI -> compareWithAlarmRange()
                AlarmType.MOVEMENT -> checkMovementData()
            }
        }

        private fun compareWithAlarmRange() {
            when (alarm.type) {
                Alarm.TEMPERATURE ->
                    if (ruuviTag.temperature != null) {
                        compareValues(
                            ruuviTag.temperature,
                            R.string.alert_notification_temperature_low_threshold to
                                R.string.alert_notification_temperature_high_threshold
                        )
                    }
                Alarm.HUMIDITY ->
                    if (ruuviTag.humidity != null) {
                        compareValues(
                            ruuviTag.humidity,
                            R.string.alert_notification_humidity_low_threshold to
                                R.string.alert_notification_humidity_high_threshold
                        )
                    }
                Alarm.PRESSURE ->
                    if (ruuviTag.pressure != null) {
                        compareValues(
                            ruuviTag.pressure,
                            R.string.alert_notification_pressure_low_threshold to
                                R.string.alert_notification_pressure_high_threshold
                        )
                    }
                Alarm.RSSI ->
                    compareValues(
                        ruuviTag.rssi,
                        R.string.alert_notification_rssi_low_threshold to
                            R.string.alert_notification_rssi_high_threshold
                    )
            }
        }

        private fun checkMovementData() {
            val readings: List<TagSensorReading> =
                sensorHistoryRepository.getLatestForSensor(ruuviTag.id, 2)
            if (readings.size == 2) {
                alarmResource = when {
                    ruuviTag.dataFormat == FORMAT5 && readings.first().movementCounter != readings.last().movementCounter -> R.string.alert_notification_movement
                    ruuviTag.dataFormat != FORMAT5 && hasTagMoved(readings.first(), readings.last()) -> R.string.alert_notification_movement
                    else -> null
                }
            }
        }

        private fun compareValues(
            comparedValue: Number,
            resources: Pair<Int, Int>
        ) {
            val (lowResourceId, highResourceId) = resources
            when {
                comparedValue.toDouble() < alarm.low -> {
                    alarmResource = lowResourceId
                    thresholdValue = alarm.low
                }
                comparedValue.toDouble() > alarm.high -> {
                    alarmResource = highResourceId
                    thresholdValue = alarm.high
                }
            }
        }

        private fun hasTagMoved(one: TagSensorReading, two: TagSensorReading): Boolean {
            return one.accelZ.diff(two.accelZ) > MOVEMENT_THRESHOLD ||
                one.accelY.diff(two.accelY) > MOVEMENT_THRESHOLD ||
                one.accelX.diff(two.accelX) > MOVEMENT_THRESHOLD
        }
    }

    companion object {
        private const val CHANNEL_ID = "notify_001"
        private const val NOTIFICATION_CHANNEL_NAME = "Alert notifications"
        private const val FORMAT5 = 5
        private const val MOVEMENT_THRESHOLD = 0.03
    }
}

enum class AlarmStatus {
    TRIGGERED,
    NO_TRIGGERED,
    NO_ALARM
}