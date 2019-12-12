package com.ruuvi.station.bluetooth

import android.app.Activity
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanFilter
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.ParcelUuid
import android.os.RemoteException
import android.util.Log
import com.ruuvi.station.service.AltBeaconScannerForegroundService
import com.ruuvi.station.service.RuuviRangeNotifier
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.util.Foreground
import com.ruuvi.station.util.Preferences
import com.ruuvi.station.util.ServiceUtils
import com.ruuvi.station.util.Utils
import org.altbeacon.beacon.BeaconConsumer
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.Region
import org.altbeacon.bluetooth.BluetoothMedic
import java.util.ArrayList

class BluetoothInteractor(private val application: Application) : BeaconConsumer {

    val isForegroundBeaconManagerActive: Boolean
        get() = foregroundBeaconManager != null

    var running = false

    var medic: BluetoothMedic? = null

    private val TAG: String = BluetoothInteractor::class.java.simpleName

    private var prefs: Preferences? = null

    private var beaconManager: BeaconManager? = null

    private var region = Region("com.ruuvi.station.leRegion", null, null, null)

    private var ruuviRangeNotifier: RuuviRangeNotifier? = null

    private var foreground = false

    override fun getApplicationContext(): Context = application.applicationContext

    override fun unbindService(serviceConnection: ServiceConnection?) {
        application.unbindService(serviceConnection)
    }

    override fun bindService(intent: Intent?, serviceConnection: ServiceConnection?, flags: Int): Boolean =
        application.bindService(intent, serviceConnection, flags)

    override fun onBeaconServiceConnect() {
        Log.d(TAG, "onBeaconServiceConnect")
        //Toast.makeText(getApplicationContext(), "Started scanning (Application)", Toast.LENGTH_SHORT).show();
        ruuviRangeNotifier?.gatewayOn = !foreground
        if (beaconManager?.rangingNotifiers?.contains(ruuviRangeNotifier) == false) {
            beaconManager?.addRangeNotifier(ruuviRangeNotifier!!)
        }
        running = true
        try {
            beaconManager!!.startRangingBeaconsInRegion(region)
        } catch (e: Throwable) {
            Log.e(TAG, "Could not start ranging")
        }
    }

    fun stopScanning() {
        Log.d(TAG, "Stopping scanning")
        running = false
        region.let {
            try {
                beaconManager?.stopRangingBeaconsInRegion(it)
            } catch (e: Exception) {
                Log.d(TAG, "Could not remove ranging region")
            }
        }
    }

    fun disposeStuff() {
        Log.d(TAG, "Stopping scanning")
        medic = null
        if (beaconManager == null) return
        running = false
        ruuviRangeNotifier?.let {
            beaconManager?.removeRangeNotifier(it)
        }

        region.let {
            try {
                beaconManager?.stopRangingBeaconsInRegion(it)
            } catch (e: Exception) {
                Log.d(TAG, "Could not remove ranging region")
            }
        }

        beaconManager?.unbind(this)
        beaconManager = null
    }

    private fun runForegroundIfEnabled(): Boolean {
        if (prefs?.backgroundScanMode == BackgroundScanModes.FOREGROUND) {
            val su = ServiceUtils(application.applicationContext)
            disposeStuff()
            su.startForegroundService()
            return true
        }
        return false
    }

    fun startForegroundScanning() {
        if (runForegroundIfEnabled()) return
        if (foreground) return
        foreground = true
        Utils.removeStateFile(application.applicationContext)
        Log.d(TAG, "Starting foreground scanning")
        bindBeaconManager(this, application.applicationContext)
        beaconManager?.backgroundMode = false
        if (ruuviRangeNotifier != null) ruuviRangeNotifier?.gatewayOn = false
    }

    fun startBackgroundScanning() {
        Log.d(TAG, "Starting background scanning")
        if (runForegroundIfEnabled()) return
        if (prefs?.backgroundScanMode != BackgroundScanModes.BACKGROUND) {
            Log.d(TAG, "Background scanning is not enabled, ignoring")
            return
        }
        bindBeaconManager(this, application.applicationContext)
        var scanInterval = Preferences(application.applicationContext).backgroundScanInterval * 1000
        val minInterval = 15 * 60 * 1000
        if (scanInterval < minInterval) scanInterval = minInterval
        if (scanInterval.toLong() != beaconManager?.backgroundBetweenScanPeriod) {
            beaconManager?.backgroundBetweenScanPeriod = scanInterval.toLong()
            try {
                beaconManager?.updateScanPeriods()
            } catch (e: Exception) {
                Log.e(TAG, "Could not update scan intervals")
            }
        }
        beaconManager?.backgroundMode = true
        if (ruuviRangeNotifier != null) ruuviRangeNotifier?.gatewayOn = true
        if (medic == null) medic = setupMedic(application.applicationContext)
    }

    fun setupMedic(context: Context?): BluetoothMedic? {
        val medic = BluetoothMedic.getInstance()
        medic.enablePowerCycleOnFailures(context)
        medic.enablePeriodicTests(context, BluetoothMedic.SCAN_TEST)
        return medic
    }

    private fun bindBeaconManager(consumer: BeaconConsumer?, context: Context) {
        if (beaconManager == null) {
            beaconManager = BeaconManager.getInstanceForApplication(context.applicationContext)
            Utils.setAltBeaconParsers(beaconManager)
            beaconManager?.backgroundScanPeriod = 5000
            consumer?.let {
                beaconManager?.bind(consumer)
            }
        } else if (!running) {
            running = true
            region.let {
                try {
                    beaconManager?.startRangingBeaconsInRegion(region)
                } catch (e: Exception) {
                    Log.d(TAG, "Could not start ranging again")
                }
            }
        }
    }

    fun onAppCreated() {
        ruuviRangeNotifier = RuuviRangeNotifier(application.applicationContext, "RuuviScannerApplication")

        prefs = Preferences(application.applicationContext)

        val listener: Foreground.Listener = object : Foreground.Listener {
            override fun onBecameForeground() {
                Log.d(TAG, "onBecameForeground")
                startForegroundScanning()
                if (ruuviRangeNotifier != null) ruuviRangeNotifier!!.gatewayOn = false
            }

            override fun onBecameBackground() {
                Log.d(TAG, "onBecameBackground")
                foreground = false
                val su = ServiceUtils(application.applicationContext)
                if (prefs?.backgroundScanMode === BackgroundScanModes.DISABLED) { // background scanning is disabled so all scanning things will be killed
                    stopScanning()
                    su.stopForegroundService()
                } else if (prefs?.backgroundScanMode === BackgroundScanModes.BACKGROUND) {
                    if (su.isRunning(AltBeaconScannerForegroundService::class.java)) {
                        su.stopForegroundService()
                    } else {
                        startBackgroundScanning()
                    }
                } else {
                    disposeStuff()
                    su.startForegroundService()
                }
                if (ruuviRangeNotifier != null) ruuviRangeNotifier!!.gatewayOn = true
            }
        }

        Foreground.init(application)
        Foreground.get().addListener(listener)
        Handler().postDelayed(
            {
                if (!foreground) {
                    if (prefs?.backgroundScanMode == BackgroundScanModes.FOREGROUND) {
                        ServiceUtils(application.applicationContext).startForegroundService()
                    } else if (prefs?.backgroundScanMode == BackgroundScanModes.BACKGROUND) {
                        startBackgroundScanning()
                    }
                }
            },
            5000
        )
    }

    private var foregroundBeaconManager: BeaconManager? = null
    private val foregroundRegion: Region = Region("com.ruuvi.station.leRegion", null, null, null)
    var foregroundRuuviRangeNotifier: RuuviRangeNotifier? = null
    var foregroundBluetoothMedic: BluetoothMedic? = null

    var foregroundBeaconConsumer = object : BeaconConsumer {

        override fun getApplicationContext(): Context = applicationContext

        override fun unbindService(serviceConnection: ServiceConnection?) {
            this@BluetoothInteractor.unbindService(serviceConnection)
        }

        override fun bindService(intent: Intent?, serviceConnection: ServiceConnection?, flags: Int): Boolean =
            this@BluetoothInteractor.bindService(intent, serviceConnection, flags)

        override fun onBeaconServiceConnect() {
            Log.d(TAG, "onBeaconServiceConnect")
            //Toast.makeText(getApplicationContext(), "Started scanning (Service)", Toast.LENGTH_SHORT).show();
            foregroundRuuviRangeNotifier?.gatewayOn = true
            if (foregroundBeaconManager?.rangingNotifiers?.contains(foregroundRuuviRangeNotifier) == false) {
                foregroundRuuviRangeNotifier?.let {
                    foregroundBeaconManager?.addRangeNotifier(it)
                }
            }
            try {
                foregroundBeaconManager?.startRangingBeaconsInRegion(region)
            } catch (e: RemoteException) {
                Log.e(TAG, "Could not start ranging")
            }
        }
    }

    fun onCreateForegroundScanningService() {
        foregroundBeaconManager = BeaconManager.getInstanceForApplication(applicationContext)
        Utils.setAltBeaconParsers(foregroundBeaconManager)
        foregroundBeaconManager?.backgroundScanPeriod = 5000


        foregroundRuuviRangeNotifier = RuuviRangeNotifier(applicationContext, "AltBeaconFGScannerService")

        foregroundBeaconManager?.bind(this)
        foregroundBluetoothMedic = setupMedic(applicationContext)
    }

    fun onDestroyForegroundScannerService() {

        foregroundRuuviRangeNotifier?.let {
            foregroundBeaconManager?.removeRangeNotifier(it)
        }
        try {
            foregroundBeaconManager?.stopRangingBeaconsInRegion(foregroundRegion)
        } catch (e: Throwable) {
            Log.d(TAG, "Could not stop ranging region")
        }
        foregroundBluetoothMedic = null
        foregroundBeaconManager?.unbind(this)
        //beaconManager.setEnableScheduledScanJobs(true);
        //beaconManager.disableForegroundServiceScanning();
        //beaconManager.setEnableScheduledScanJobs(true);
//beaconManager.disableForegroundServiceScanning();
        foregroundBeaconManager = null
        foregroundRuuviRangeNotifier = null
    }

    fun startInBackgroundMode(scanInterval: Long) {

        foregroundBeaconManager?.backgroundBetweenScanPeriod = scanInterval
        try {
            foregroundBeaconManager?.updateScanPeriods()
        } catch (e: Throwable) {
            Log.e(TAG, "Could not update scan intervals")
        }
    }

    /* Is called right after startInBackgroundMode */
    fun setBackgroundMode(isBackgroundModeEnabled: Boolean) {
        foregroundBeaconManager?.backgroundMode = isBackgroundModeEnabled
    }

    fun getBackgroundBetweenScanPeriod(): Long? =
        beaconManager?.backgroundBetweenScanPeriod

    fun setEnableScheduledScanJobs(areScheduledScanJobsEnabled: Boolean) {
        beaconManager?.setEnableScheduledScanJobs(areScheduledScanJobsEnabled)
    }

    fun isBluetoothEnabled(): Boolean {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
    }

    fun enableBluetooth(activity: Activity) {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        activity.startActivity(enableBtIntent)
    }

    fun checkAndEnableBluetooth(activity: Activity): Boolean {
        if (isBluetoothEnabled()) {
            return true
        }

        enableBluetooth(activity)
        return false
    }

    fun checkAndEnableBluetoothForStarter(that: Activity): Boolean {
        if (isBluetoothEnabled()) {
            return true
        }
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        that.startActivityForResult(enableBtIntent, 87)
        return false
    }

    fun getScanFilters(): List<ScanFilter>? {
        val filters: MutableList<ScanFilter> = ArrayList()
        val ruuviFilter = ScanFilter.Builder()
            .setManufacturerData(0x0499, byteArrayOf())
            .build()
        val eddystoneFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid.fromString("0000feaa-0000-1000-8000-00805f9b34fb"))
            .build()
        filters.add(ruuviFilter)
        filters.add(eddystoneFilter)
        return filters
    }
}