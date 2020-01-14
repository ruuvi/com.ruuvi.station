package com.ruuvi.station.bluetooth.gateway

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.RemoteException
import android.util.Log
import com.ruuvi.station.bluetooth.BluetoothInteractor
import com.ruuvi.station.service.RuuviRangeNotifier
import com.ruuvi.station.util.Preferences
import org.altbeacon.beacon.BeaconConsumer
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.Region
import org.altbeacon.bluetooth.BluetoothMedic

class BluetoothForegroundServiceGateway(private val application: Application) : BeaconConsumer {

    private var beaconManager: BeaconManager? = null
    private var region: Region? = null
    private var ruuviRangeNotifier: RuuviRangeNotifier? = null
    private var medic: BluetoothMedic? = null

    fun onCreate() {
        Log.d(TAG, "Starting foreground service")
        beaconManager = BeaconManager.getInstanceForApplication(application)
        beaconManager?.let { beaconManager ->
            BluetoothInteractor.setAltBeaconParsers(beaconManager)
            beaconManager.backgroundScanPeriod = 5000
            ruuviRangeNotifier = RuuviRangeNotifier(application, "AltBeaconFGScannerService")
            region = Region("com.ruuvi.station.leRegion", null, null, null)
            beaconManager.bind(this)
            medic = setupMedic(application)
        }
    }

    private fun setupMedic(context: Context?): BluetoothMedic? {
        val medic = BluetoothMedic.getInstance()
        medic.enablePowerCycleOnFailures(context)
        medic.enablePeriodicTests(context, BluetoothMedic.SCAN_TEST)
        return medic
    }

    fun startFGGateway() {
//        setupNotification()
        //beaconManager.enableForegroundServiceScanning(notification.build(), 1337);
        beaconManager!!.setEnableScheduledScanJobs(false)
//        startForeground(1337, notification!!.build())
    }

    fun setBackgroundGateway() {
        val scanInterval = Preferences(application).backgroundScanInterval * 1000
        if (scanInterval.toLong() != beaconManager!!.backgroundBetweenScanPeriod) {
            beaconManager!!.backgroundBetweenScanPeriod = scanInterval.toLong()
            try {
                beaconManager!!.updateScanPeriods()
            } catch (e: Exception) {
                Log.e(TAG, "Could not update scan intervals")
            }
        }
        beaconManager!!.backgroundMode = true
    }

    fun onDestroy() {
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
    }

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

    fun onBecameForeground() {
        beaconManager!!.backgroundMode = false
    }

    fun shouldUpdateScanInterval(): Boolean {
        val scanInterval = Preferences(application).backgroundScanInterval * 1000
        return scanInterval.toLong() != beaconManager!!.backgroundBetweenScanPeriod
    }

    companion object {
        private const val TAG = "BtForegroundGateway"
    }
}