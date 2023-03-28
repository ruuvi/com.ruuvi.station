package com.ruuvi.station.alarm.domain

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ruuvi.station.R
import com.ruuvi.station.alarm.receiver.CancelAlarmReceiver
import com.ruuvi.station.alarm.receiver.MuteAlarmReceiver
import com.ruuvi.station.tagdetails.ui.TagDetailsActivity
import timber.log.Timber

class AlertNotificationInteractor(
    private val context: Context,
) {
    data class AlertNotificationData(
        val sensorId: String,
        val message: String,
        val sensorName: String,
        val alarmId: Int?,
        val alertCustomDescription: String
    )

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val alertSoundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.ruuvi_speak_16bit_stereo}")

    private val vibrationPattern = longArrayOf(0L, 200L, 100L, 200L)

    fun notify(notificationId: Int, notificationData: AlertNotificationData) {
        createNotificationChannel()
        notificationManager.notify(notificationId, createNotification(notificationData))
    }

    fun removeNotificationById(notificationId: Int) {
        Timber.d("dismissNotification with id = $notificationId")
        if (notificationId != -1) notificationManager.cancel(notificationId)
    }

    private fun createNotification(notificationData: AlertNotificationData): Notification? {
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
            .Builder(context, CHANNEL_ID)
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
            .setSound(alertSoundUri)
            .setVibrate(vibrationPattern)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attributes: AudioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )

            channel.setSound(alertSoundUri, attributes)
            channel.vibrationPattern = vibrationPattern

            notificationManager.deleteNotificationChannel(OLD_CHANNEL_ID)
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "ruuvi_notify_001"
        private const val OLD_CHANNEL_ID = "notify_001"
        private const val NOTIFICATION_CHANNEL_NAME = "Alert notifications"
    }
}