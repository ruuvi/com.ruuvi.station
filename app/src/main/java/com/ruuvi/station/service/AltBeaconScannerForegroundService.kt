package com.ruuvi.station.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.ruuvi.station.R
import com.ruuvi.station.RuuviScannerApplication
import com.ruuvi.station.feature.StartupActivity
import com.ruuvi.station.util.Foreground
import com.ruuvi.station.util.Preferences
import com.ruuvi.station.util.Utils

class AltBeaconScannerForegroundService : Service() {

    private val bluetoothForegroundServiceGateway by lazy {
        (application as RuuviScannerApplication).bluetoothForegroundServiceGateway
    }

    var notification: NotificationCompat.Builder? = null

    private var listener: Foreground.Listener? = object : Foreground.Listener {
        override fun onBecameForeground() {
            Utils.removeStateFile(application)

            bluetoothForegroundServiceGateway.onBecameForeground()

        }

        override fun onBecameBackground() {
            setBackground()
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        updateNotification()
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        bluetoothForegroundServiceGateway.startScanning()
        Log.d(TAG, "Starting foreground service")
        Foreground.init(application)
        Foreground.get().addListener(listener)
        startFG()
        setBackground() // start in background mode
    }

    private fun setupNotification(): NotificationCompat.Builder? {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "foreground_scanner_channel"
        if (Build.VERSION.SDK_INT >= 26) {
            val channelName: CharSequence = "RuuviStation foreground scanner"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val notificationChannel = NotificationChannel(channelId, channelName, importance)
            try {
                notificationManager.createNotificationChannel(notificationChannel)
            } catch (e: Exception) {
                Log.e(TAG, "Could not create notification channel")
            }
        }
        val notificationIntent = Intent(this, StartupActivity::class.java)
        val bitmap = BitmapFactory.decodeResource(applicationContext.resources, R.mipmap.ic_launcher)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
        var notificationText = getString(R.string.scanner_notification_title)
        notificationText = notificationText.replace("..", " every ")
        val scanInterval = Preferences(applicationContext).backgroundScanInterval
        val min = scanInterval / 60
        val sec = scanInterval - min * 60
        if (min > 0) {
            var minutes = getString(R.string.minutes).toLowerCase()
            if (min == 1) {
                minutes = minutes.substring(0, minutes.length - 1)
            }
            notificationText += "$min $minutes, "
        }
        if (sec > 0) {
            var seconds = getString(R.string.seconds).toLowerCase()
            if (sec == 1) {
                seconds = seconds.substring(0, seconds.length - 1)
            }
            notificationText += "$sec $seconds"
        } else {
            notificationText = notificationText.replace(", ", "")
        }
        notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(notificationText)
            .setSmallIcon(R.mipmap.ic_launcher_small)
            .setTicker(this.getString(R.string.scanner_notification_ticker))
            .setStyle(NotificationCompat.BigTextStyle().bigText(this.getString(R.string.scanner_notification_message)))
            .setContentText(this.getString(R.string.scanner_notification_message))
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setLargeIcon(bitmap)
            .setContentIntent(pendingIntent)
        notification?.setSmallIcon(R.drawable.ic_ruuvi_bgscan_icon)
        return notification
    }

    private fun startFG() {
        setupNotification()

        bluetoothForegroundServiceGateway.enableForegroundMode()

        startForeground(1337, notification!!.build())
    }

    private fun updateNotification() {
        setupNotification()
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            mNotificationManager.notify(1337, notification!!.build())
        } catch (e: NullPointerException) {
            Log.d(TAG, "Could not update notification")
        }
    }

    private fun setBackground() {
        bluetoothForegroundServiceGateway.enableBackgroundMode()
        if (bluetoothForegroundServiceGateway.shouldUpdateScanInterval()) {
            updateNotification()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothForegroundServiceGateway.stopScanning()
        stopForeground(true)
        if (listener != null) Foreground.get().removeListener(listener)
        (application as RuuviScannerApplication).startBackgroundScanning()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        private const val TAG = "AScannerFgService"
    }
}