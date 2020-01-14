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
import android.os.RemoteException
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.ruuvi.station.R
import com.ruuvi.station.RuuviScannerApplication
import com.ruuvi.station.feature.StartupActivity
import com.ruuvi.station.util.Foreground
import com.ruuvi.station.util.Preferences
import com.ruuvi.station.util.Utils
import org.altbeacon.beacon.BeaconConsumer
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.Region
import org.altbeacon.bluetooth.BluetoothMedic

class AltBeaconScannerForegroundService : Service(), BeaconConsumer {

    private val bluetoothForegroundServiceGateway by lazy {
        (application as RuuviScannerApplication).bluetoothForegroundServiceGateway
    }

    private var beaconManager: BeaconManager? = null
    private var region: Region? = null
    var ruuviRangeNotifier: RuuviRangeNotifier? = null
    var medic: BluetoothMedic? = null
    var notification: NotificationCompat.Builder? = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        updateNotification()
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Starting foreground service")
        beaconManager = BeaconManager.getInstanceForApplication(applicationContext)
        Utils.setAltBeaconParsers(beaconManager)
        beaconManager!!.backgroundScanPeriod = 5000
        Foreground.init(application)
        Foreground.get().addListener(listener)
        ruuviRangeNotifier = RuuviRangeNotifier(applicationContext, "AltBeaconFGScannerService")
        region = Region("com.ruuvi.station.leRegion", null, null, null)
        startFG()
        beaconManager!!.bind(this)
        medic = RuuviScannerApplication.setupMedic(applicationContext)
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
        //beaconManager.enableForegroundServiceScanning(notification.build(), 1337);
        beaconManager!!.setEnableScheduledScanJobs(false)
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
        val scanInterval = Preferences(applicationContext).backgroundScanInterval * 1000
        if (scanInterval.toLong() != beaconManager!!.backgroundBetweenScanPeriod) {
            updateNotification()
            beaconManager!!.backgroundBetweenScanPeriod = scanInterval.toLong()
            try {
                beaconManager!!.updateScanPeriods()
            } catch (e: Exception) {
                Log.e(TAG, "Could not update scan intervals")
            }
        }
        beaconManager!!.backgroundMode = true
    }

    var listener: Foreground.Listener? = object : Foreground.Listener {
        override fun onBecameForeground() {
            Utils.removeStateFile(applicationContext)
            beaconManager!!.backgroundMode = false
        }

        override fun onBecameBackground() {
            setBackground()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy =======")
        beaconManager!!.removeRangeNotifier(ruuviRangeNotifier!!)
        try {
            beaconManager!!.stopRangingBeaconsInRegion(region!!)
        } catch (e: Exception) {
            Log.d(TAG, "Could not stop ranging region")
        }
        medic = null
        beaconManager!!.unbind(this)
        //beaconManager.setEnableScheduledScanJobs(true);
//beaconManager.disableForegroundServiceScanning();
        beaconManager = null
        ruuviRangeNotifier = null
        stopForeground(true)
        if (listener != null) Foreground.get().removeListener(listener)
        (application as RuuviScannerApplication).startBackgroundScanning()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onBeaconServiceConnect() {
        Log.d(TAG, "onBeaconServiceConnect")
        //Toast.makeText(getApplicationContext(), "Started scanning (Service)", Toast.LENGTH_SHORT).show();
        ruuviRangeNotifier!!.gatewayOn = true
        if (!beaconManager!!.rangingNotifiers.contains(ruuviRangeNotifier)) {
            beaconManager!!.addRangeNotifier(ruuviRangeNotifier!!)
        }
        try {
            beaconManager!!.startRangingBeaconsInRegion(region!!)
        } catch (e: RemoteException) {
            Log.e(TAG, "Could not start ranging")
        }
    }

    companion object {
        private const val TAG = "AScannerFgService"
    }
}