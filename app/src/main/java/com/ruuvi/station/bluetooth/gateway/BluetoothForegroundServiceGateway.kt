package com.ruuvi.station.bluetooth.gateway

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.RemoteException
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.ruuvi.station.service.RuuviRangeNotifier
import org.altbeacon.beacon.BeaconConsumer
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.Region
import org.altbeacon.bluetooth.BluetoothMedic

class BluetoothForegroundServiceGateway(private val application: Application) : BeaconConsumer {

    private var beaconManager: BeaconManager? = null
    private var region: Region? = null
    var ruuviRangeNotifier: RuuviRangeNotifier? = null
    var medic: BluetoothMedic? = null
    var notification: NotificationCompat.Builder? = null

//    fun onCreate() {
//        Log.d(TAG, "Starting foreground service")
//        beaconManager = BeaconManager.getInstanceForApplication(application)
//        Utils.setAltBeaconParsers(beaconManager)
//        beaconManager!!.backgroundScanPeriod = 5000
//        Foreground.init(application)
//        Foreground.get().addListener(listener)
//        ruuviRangeNotifier = RuuviRangeNotifier(application, "AltBeaconFGScannerService")
//        region = Region("com.ruuvi.station.leRegion", null, null, null)
//        startFG()
//        beaconManager!!.bind(this)
//        medic = RuuviScannerApplication.setupMedic(application)
//        setBackground() // start in background mode
//    }
//
//    private fun startFGGateway() {
//        setupNotification()
//        //beaconManager.enableForegroundServiceScanning(notification.build(), 1337);
//        beaconManager!!.setEnableScheduledScanJobs(false)
//        startForeground(1337, notification!!.build())
//    }
//
//    private fun setBackgroundGateway() {
//        val scanInterval = Preferences(application).backgroundScanInterval * 1000
//        if (scanInterval.toLong() != beaconManager!!.backgroundBetweenScanPeriod) {
//            updateNotification()
//            beaconManager!!.backgroundBetweenScanPeriod = scanInterval.toLong()
//            try {
//                beaconManager!!.updateScanPeriods()
//            } catch (e: Exception) {
//                Log.e(AltBeaconScannerForegroundService.TAG, "Could not update scan intervals")
//            }
//        }
//        beaconManager!!.backgroundMode = true
//    }
//
//    var listener: Foreground.Listener? = object : Foreground.Listener {
//        override fun onBecameForeground() {
//            Utils.removeStateFile(application)
//            beaconManager!!.backgroundMode = false
//        }
//
//        override fun onBecameBackground() {
//            setBackground()
//        }
//    }
//
//    fun onDestroy() {
//        Log.d(AltBeaconScannerForegroundService.TAG, "onDestroy =======")
//        beaconManager!!.removeRangeNotifier(ruuviRangeNotifier!!)
//        try {
//            beaconManager!!.stopRangingBeaconsInRegion(region!!)
//        } catch (e: Exception) {
//            Log.d(AltBeaconScannerForegroundService.TAG, "Could not stop ranging region")
//        }
//        medic = null
//        beaconManager!!.unbind(this)
//        //beaconManager.setEnableScheduledScanJobs(true);
////beaconManager.disableForegroundServiceScanning();
//        beaconManager = null
//        ruuviRangeNotifier = null
//        stopForeground(true)
//        if (listener != null) Foreground.get().removeListener(listener)
//        (application as RuuviScannerApplication).startBackgroundScanning()
//    }

    override fun getApplicationContext(): Context = application

    override fun unbindService(p0: ServiceConnection?) {
        application.unbindService(p0)
    }

    override fun bindService(p0: Intent?, p1: ServiceConnection?, p2: Int): Boolean {
        return application.bindService(p0, p1, p2)
    }

    override fun onBeaconServiceConnect() {
        Log.d(TAG, "onBeaconServiceConnect")
        //Toast.makeText(getapplication(), "Started scanning (Service)", Toast.LENGTH_SHORT).show();
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
        private const val TAG = "BtForegroundGateway"
    }
}