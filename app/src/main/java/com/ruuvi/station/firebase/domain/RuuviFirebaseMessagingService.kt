package com.ruuvi.station.firebase.domain

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
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
}