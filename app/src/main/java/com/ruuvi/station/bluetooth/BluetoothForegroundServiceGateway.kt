package com.ruuvi.station.bluetooth

import android.app.Application
import android.util.Log
import com.ruuvi.station.util.Preferences

class BluetoothForegroundServiceGateway(private val application: Application) {

    //    private var beaconManager: BeaconManager? = null
//    private var region: Region? = null
    private lateinit var ruuviRangeNotifier: RuuviRangeNotifier
//    private var medic: BluetoothMedic? = null

    fun onCreate() {
        Log.d(TAG, "Starting foreground service")
//        beaconManager = BeaconManager.getInstanceForApplication(application)
//        beaconManager?.let { beaconManager ->
//            BluetoothInteractor.setAltBeaconParsers(beaconManager)
//            beaconManager.backgroundScanPeriod = 5000
            ruuviRangeNotifier = RuuviRangeNotifier(application, "AltBeaconFGScannerService")
//            region = Region("com.ruuvi.station.leRegion", null, null, null)
//            beaconManager.bind(this)
//            medic = setupMedic(application)
//        }
        ruuviRangeNotifier.startScan()
    }
//
//    private fun setupMedic(context: Context?): BluetoothMedic? {
//        val medic = BluetoothMedic.getInstance()
//        medic.enablePowerCycleOnFailures(context)
//        medic.enablePeriodicTests(context, BluetoothMedic.SCAN_TEST)
//        return medic
//    }

    fun startFGGateway() {
        ruuviRangeNotifier.setEnableScheduledScanJobs(false)

    }

    fun setBackgroundGateway() {
       if(shouldUpdateScanInterval()){
           val scanInterval = Preferences(application).backgroundScanInterval * 1000
           ruuviRangeNotifier.setBackgroundScanInterval(scanInterval.toLong())
        }
        ruuviRangeNotifier.enableBackgroundMode(true)
    }

    fun onDestroy() {
        Log.d(TAG, "onDestroy =======")

        ruuviRangeNotifier.stopScanning()
//        beaconManager!!.removeRangeNotifier(ruuviRangeNotifier!!)
//        try {
//            beaconManager!!.stopRangingBeaconsInRegion(region!!)
//        } catch (e: Exception) {
//            Log.d(TAG, "Could not stop ranging region")
//        }
//        medic = null
//        beaconManager!!.unbind(this)
//
//        beaconManager = null
//        ruuviRangeNotifier = null
    }


    fun onBecameForeground() {
       ruuviRangeNotifier.enableBackgroundMode(false)
    }

    fun shouldUpdateScanInterval(): Boolean {
        val scanInterval = Preferences(application).backgroundScanInterval * 1000
        return scanInterval.toLong() != ruuviRangeNotifier.getBackgroundScanInterval()
    }

    companion object {
        private const val TAG = "BtForegroundGateway"
    }
}