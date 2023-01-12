package com.ruuvi.station.firebase.domain

import android.app.Notification
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlarmCheckInteractor
import com.ruuvi.station.alarm.receiver.CancelAlarmReceiver
import com.ruuvi.station.alarm.receiver.MuteAlarmReceiver
import com.ruuvi.station.tagdetails.ui.TagDetailsActivity
import timber.log.Timber
import java.util.*

class RuuviFirebaseMessagingService: FirebaseMessagingService()  {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Timber.d("remoteMessage = ${remoteMessage.notification}")
        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Timber.d("""
            From: ${remoteMessage.from}
            Message type: ${remoteMessage.messageType}
            messageId: ${remoteMessage.messageId}
            sentTime: ${Date(remoteMessage.sentTime)}
            notification title: ${remoteMessage.notification?.title}
            notification body: ${remoteMessage.notification?.body}
            Message data payload: ${remoteMessage.data}
        """.trimIndent())
        remoteMessage.messageType
        // Check if message contains a data payload.
//        if (remoteMessage.data.isNotEmpty()) {
//            Timber.d("Message data payload: ${remoteMessage.data}")

//            if (/* Check if data needs to be processed by long running job */ true) {
//                // For long-running tasks (10 seconds or more) use WorkManager.
//                scheduleJob()
//            } else {
//                // Handle message within 10 seconds
//                handleNow()
//            }
//        }
//
        // Check if message contains a notification payload.
//        remoteMessage.notification?.let {
//            Timber.d("Message Notification Body: ${it.body}")
//        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }


    private fun createNotification(checker: AlarmCheckInteractor.AlarmChecker): Notification? {
        val message = checker.getMessage()
        if (!message.isNullOrEmpty()) {
            val tagDetailsPendingIntent =
                TagDetailsActivity.createPendingIntent(
                    this,
                    checker.ruuviTag.id,
                    checker.alarm.id
                )
            val cancelPendingIntent =
                CancelAlarmReceiver.createPendingIntent(this, checker.alarm.id)
            val mutePendingIntent = MuteAlarmReceiver.createPendingIntent(this, checker.alarm.id)
            val action = NotificationCompat.Action(
                R.drawable.ic_ruuvi_app_notification_icon_v2,
                this.getString(R.string.alarm_notification_disable),
                cancelPendingIntent
            )
            val actionMute = NotificationCompat.Action(
                R.drawable.ic_ruuvi_app_notification_icon_v2,
                this.getString(R.string.alarm_mute_for_hour),
                mutePendingIntent
            )

            val bitmap = BitmapFactory.decodeResource(this.resources, R.mipmap.ic_launcher)
            return NotificationCompat
                .Builder(this, "notify_001")//AlarmCheckInteractor.CHANNEL_ID)
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
}

/*
        val jsonString = """
            {default=
            	{
            	"token": "aadf4frwfiosjfosjflsafjlsafjlsknfdlnsvl",
            	"email": "denis@ruuvi.com",
                "type": "alert"
            	"data":
            		{
            			"name": "Fridge",
                        "id": "AA:BB:CC:DD:EE:FF",
                        "alertType": "Temperature",
                        "triggerType": "Over",
                        "currentValue": "14",
                        "thresholdValue": "10",
                        "alertUnit": "C",
                        "alertData": "Fridge is too warm!"
                    }
                }
            }
        """.trimIndent()
        val ob = GsonBuilder().create().fromJson(jsonString, AlertPush::class.java)

        Timber.d("parsed $ob")
 */