package com.ruuvi.station.firebase.domain

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.GsonBuilder
import com.ruuvi.station.firebase.data.PushBody
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import timber.log.Timber
import java.util.*

class RuuviFirebaseMessagingService: FirebaseMessagingService(), KodeinAware {

    override val kodein: Kodein by kodein()

    val pushAlertInteractor: PushAlertInteractor by instance()
    val pushRegisterInteractor: PushRegisterInteractor by instance()

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Timber.d("remoteMessage = ${remoteMessage.notification}")
        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Timber.d(
            """
            From: ${remoteMessage.from}
            Message type: ${remoteMessage.messageType}
            messageId: ${remoteMessage.messageId}
            sentTime: ${Date(remoteMessage.sentTime)}
            notification title: ${remoteMessage.notification?.title}
            notification body: ${remoteMessage.notification?.body}
            Message data payload: ${remoteMessage.data}
        """.trimIndent()
        )
        remoteMessage.messageType

        for ((key, value) in remoteMessage.data) {
            Timber.d("key = $key")
            Timber.d("value = $value")
        }

        remoteMessage.data.values.firstOrNull().let { message ->
            val parsedMessage = GsonBuilder().create().fromJson(message, PushBody::class.java)
            Timber.d("parsed $parsedMessage")

            if (parsedMessage.type == "alert") {
                pushAlertInteractor.processAlertPush(parsedMessage.data, this)
            }
        }
    }

    override fun onNewToken(p0: String) {
        Timber.d("onNewToken $p0")
        pushRegisterInteractor.checkAndRegisterDeviceToken()
        super.onNewToken(p0)
    }
}