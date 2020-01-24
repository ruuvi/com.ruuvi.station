package com.ruuvi.station.bluetooth

import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.RemoteException
import android.util.Log
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconConsumer
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.RangeNotifier
import org.altbeacon.beacon.Region
import org.altbeacon.bluetooth.BluetoothMedic
import java.util.ArrayList

class RuuviRangeNotifier(
    private val context: Context,
    private val from: String
) : IRuuviRangeNotifier {

    private var tagListener: IRuuviRangeNotifier.OnTagsFoundListener? = null

    private var last: Long = 0

    private val region = Region("com.ruuvi.station.leRegion", null, null, null)
    private var beaconManager: BeaconManager? = null
    private var medic: BluetoothMedic? = null
    private val rangeNotifier = object : RangeNotifier {

        override fun didRangeBeaconsInRegion(beacons: Collection<Beacon>, region: Region) {
            val now = System.currentTimeMillis()
            if (now <= last + 500) {
                Log.d(TAG, "Double range bug")
                return
            }
            last = now

            val tags: MutableList<FoundRuuviTag> = ArrayList()
            val allTags: MutableList<FoundRuuviTag> = ArrayList()
            Log.d(TAG, from + " " + " found " + beacons.size)
            for (beacon in beacons) { // the same tag can appear multiple times
                for (tag in tags) {
                    if (tag.id == beacon.bluetoothAddress) continue
                }
                val tag = LeScanResult.fromAltbeacon(beacon)
                if (tag != null) {
                    allTags.add(tag)
                }
            }

            tagListener?.onTagsFound(allTags = allTags)
        }
    }
    private val beaconConsumer = object : BeaconConsumer {

        override fun getApplicationContext(): Context = context

        override fun unbindService(serviceConnection: ServiceConnection?) {
            context.unbindService(serviceConnection)
        }

        override fun bindService(intent: Intent?, serviceConnection: ServiceConnection?, flags: Int): Boolean {
            return context.bindService(intent, serviceConnection, flags)
        }

        override fun onBeaconServiceConnect() {
            Log.d(TAG, "onBeaconServiceConnect")

            startRanging()
        }
    }

    init {
        Log.d(TAG, "Setting up range notifier from $from")
    }

    override fun startScanning(
        tagsFoundListener: IRuuviRangeNotifier.OnTagsFoundListener,
        shouldLaunchInBackground: Boolean
    ) {
        this.tagListener = tagsFoundListener

        if (medic == null) medic = setupMedic(context)


        if (beaconManager == null) {
            beaconManager = BeaconManager.getInstanceForApplication(context)

            beaconManager?.let { beaconManager ->

                setAltBeaconParsers(beaconManager)
                beaconManager.backgroundScanPeriod = 5000
                beaconManager.backgroundMode = shouldLaunchInBackground

                if (shouldLaunchInBackground) {

                    try {
                        beaconManager.updateScanPeriods()
                    } catch (e: Exception) {
                        Log.e(TAG, "Could not update scan intervals")
                    }
                }

                beaconManager.bind(beaconConsumer)
            }
        }
    }

    override fun stopScanning() {

        beaconManager?.removeRangeNotifier(rangeNotifier)

        try {
            beaconManager?.stopRangingBeaconsInRegion(region)
        } catch (e: Exception) {
            Log.d(TAG, "Could not stop ranging region")
        }
        beaconManager?.unbind(beaconConsumer)

        Log.d(TAG, "Stopping scanning")
        medic = null

        beaconManager = null
    }

    override fun enableBackgroundMode(isBackgroundModeEnabled: Boolean) {
        beaconManager?.backgroundMode = isBackgroundModeEnabled
    }

    override fun getBackgroundScanInterval(): Long? = beaconManager?.backgroundBetweenScanPeriod

    override fun enableScheduledScans(areScheduledScansEnabled: Boolean) {
        beaconManager?.setEnableScheduledScanJobs(areScheduledScansEnabled)
    }

    override fun setBackgroundScheduledScanInterval(scanInterval: Long) {
        beaconManager?.backgroundBetweenScanPeriod = scanInterval
        try {
            beaconManager?.updateScanPeriods()
        } catch (e: Exception) {
            Log.e(TAG, "Could not update scan intervals")
        }
    }

    private fun setAltBeaconParsers(beaconManager: BeaconManager) {
        beaconManager.beaconParsers.clear()
        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout(RuuviV2and4_LAYOUT))
        val v3Parser = BeaconParser().setBeaconLayout(RuuviV3_LAYOUT)
        v3Parser.setHardwareAssistManufacturerCodes(intArrayOf(1177))
        beaconManager.beaconParsers.add(v3Parser)
        val v5Parser = BeaconParser().setBeaconLayout(RuuviV5_LAYOUT)
        v5Parser.setHardwareAssistManufacturerCodes(intArrayOf(1177))
        beaconManager.beaconParsers.add(v5Parser)
    }

    private fun startRanging() {

        if (beaconManager?.rangingNotifiers?.contains(rangeNotifier) != true) {
            beaconManager?.addRangeNotifier(rangeNotifier)
        }
        try {
            beaconManager?.startRangingBeaconsInRegion(region)
        } catch (e: RemoteException) {
            Log.e(TAG, "Could not start ranging")
        }
    }

    private fun setupMedic(context: Context?): BluetoothMedic {
        val medic = BluetoothMedic.getInstance()
        medic.enablePowerCycleOnFailures(context)
        medic.enablePeriodicTests(context, BluetoothMedic.SCAN_TEST)
        return medic
    }

    companion object {
        private const val TAG = "RuuviRangeNotifier"

        const val RuuviV2and4_LAYOUT = "s:0-1=feaa,m:2-2=10,p:3-3:-41,i:4-21v"
        const val RuuviV3_LAYOUT = "x,m:0-2=990403,i:2-15,d:2-2,d:3-3,d:4-4,d:5-5,d:6-6,d:7-7,d:8-8,d:9-9,d:10-10,d:11-11,d:12-12,d:13-13,d:14-14,d:15-15"
        const val RuuviV5_LAYOUT = "x,m:0-2=990405,i:20-25,d:2-2,d:3-3,d:4-4,d:5-5,d:6-6,d:7-7,d:8-8,d:9-9,d:10-10,d:11-11,d:12-12,d:13-13,d:14-14,d:15-15,d:16-16,d:17-17,d:18-18,d:19-19,d:20-20,d:21-21,d:22-22,d:23-23,d:24-24,d:25-25"
    }

}