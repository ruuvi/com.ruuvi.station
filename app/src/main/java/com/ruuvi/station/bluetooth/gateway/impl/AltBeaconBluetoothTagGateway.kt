package com.ruuvi.station.bluetooth.gateway.impl

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.util.Log
import com.ruuvi.station.bluetooth.gateway.BluetoothTagGateway
import com.ruuvi.station.service.RuuviRangeNotifier
import com.ruuvi.station.util.Preferences
import com.ruuvi.station.util.Utils
import org.altbeacon.beacon.BeaconConsumer
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.Region
import org.altbeacon.bluetooth.BluetoothMedic

class AltBeaconBluetoothTagGateway(private val application: Application) : BluetoothTagGateway {

    private val TAG = AltBeaconBluetoothTagGateway::class.java.simpleName

    private var onTagsFoundListener: BluetoothTagGateway.OnTagsFoundListener? = null

    private var medic: BluetoothMedic? = null

    private val region = Region("com.ruuvi.station.leRegion", null, null, null)

    private var beaconManager: BeaconManager? = null

    private var ruuviRangeNotifier: RuuviRangeNotifier? = null

    private var running = false

    private val beaconConsumer: BeaconConsumer = object : BeaconConsumer {

        override fun getApplicationContext(): Context = application.applicationContext

        override fun unbindService(serviceConnection: ServiceConnection?) {
            application.unbindService(serviceConnection)
        }

        override fun bindService(intent: Intent?, serviceConnection: ServiceConnection?, flags: Int): Boolean =
            application.bindService(intent, serviceConnection, flags)

        override fun onBeaconServiceConnect() {
            Log.d(TAG, "onBeaconServiceConnect")
            //Toast.makeText(getApplicationContext(), "Started scanning (Application)", Toast.LENGTH_SHORT).show();

            if (beaconManager?.rangingNotifiers?.contains(ruuviRangeNotifier) == false) {
                ruuviRangeNotifier?.let {
                    beaconManager?.addRangeNotifier(it)
                }
            }
            running = true
            try {
                beaconManager?.startRangingBeaconsInRegion(region)
            } catch (e: Throwable) {
                Log.e(TAG, "Could not start ranging")
            }
        }
    }

    override fun listenForTags(onTagsFoundListener: BluetoothTagGateway.OnTagsFoundListener) {

        this.onTagsFoundListener = onTagsFoundListener

        if (beaconManager == null) {
            beaconManager = BeaconManager.getInstanceForApplication(application)
            Utils.setAltBeaconParsers(beaconManager)
            beaconManager?.backgroundScanPeriod = 5000

            beaconManager?.bind(beaconConsumer)

            ruuviRangeNotifier = RuuviRangeNotifier(application, "AltBeaconFGScannerService", onTagsFoundListener)

            this.medic = setupMedic(application)
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

    override fun setBackgroundMode(isBackgroundModeEnabled: Boolean) {
        beaconManager?.backgroundMode = isBackgroundModeEnabled
    }

    override fun stopScanning() {
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

    override fun reset() {
        Log.d(TAG, "Stopping scanning")
        this.medic = null
        if (beaconManager == null) return

        setEnableScheduledScanJobs(false)

        running = false
        ruuviRangeNotifier?.let {
            beaconManager?.removeRangeNotifier(it)
        }

        region.let {
            try {
                beaconManager?.stopRangingBeaconsInRegion(it)
            } catch (e: Exception) {
                Log.d(TAG, "Could not remove ranging region", e)
            }
        }

        beaconManager?.unbind(beaconConsumer)

        medic = null
        beaconManager = null
        ruuviRangeNotifier = null

        // recreate everything
        beaconManager = BeaconManager.getInstanceForApplication(application)
        Utils.setAltBeaconParsers(beaconManager)
        beaconManager?.backgroundScanPeriod = 5000

        ruuviRangeNotifier = RuuviRangeNotifier(application, "AltBeaconFGScannerService", onTagsFoundListener)

        beaconManager?.bind(beaconConsumer)
        medic = setupMedic(application)
    }

    override fun getBackgroundBetweenScanInterval(): Long? =
        beaconManager?.backgroundBetweenScanPeriod

    override fun startBackgroundScanning(): Boolean {
        var hasStartedSuccessfully = false
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
            hasStartedSuccessfully = true
        }
        beaconManager?.backgroundMode = true

        if (this.medic == null) this.medic = setupMedic(application.applicationContext)

        return hasStartedSuccessfully;
    }

    override fun isForegroundScanningActive(): Boolean = beaconManager != null

    private fun setEnableScheduledScanJobs(areScheduledScanJobsEnabled: Boolean) {
        beaconManager?.setEnableScheduledScanJobs(areScheduledScanJobsEnabled)
    }

    private fun setupMedic(context: Context?): BluetoothMedic? {
        val medic = BluetoothMedic.getInstance()
        medic.enablePowerCycleOnFailures(context)
        medic.enablePeriodicTests(context, BluetoothMedic.SCAN_TEST)
        return medic
    }
}