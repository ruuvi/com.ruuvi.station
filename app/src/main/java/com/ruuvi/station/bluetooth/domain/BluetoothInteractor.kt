package com.ruuvi.station.bluetooth.domain

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.util.Log
import com.ruuvi.station.bluetooth.DefaultOnTagFoundListener
import com.ruuvi.station.bluetooth.RuuviRangeNotifier
import com.ruuvi.station.bluetooth.interfaces.RuuviTagFactory
import com.ruuvi.station.service.AltBeaconScannerForegroundService
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.util.Constants
import com.ruuvi.station.util.Foreground
import com.ruuvi.station.util.Preferences
import com.ruuvi.station.util.ServiceUtils
import com.ruuvi.station.util.Utils
import org.altbeacon.beacon.BeaconConsumer
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.Region
import org.altbeacon.bluetooth.BluetoothMedic

class BluetoothInteractor(
    private val application: Application,
    private val ruuviTagFactory: RuuviTagFactory
) {

    private val TAG: String = BluetoothInteractor::class.java.simpleName

    private val prefs: Preferences = Preferences(application)

    private var isRunningInForeground = false
    private var ruuviRangeNotifier: RuuviRangeNotifier? = null

    private var beaconManager: BeaconManager? = null
    private var region: Region = Region("com.ruuvi.station.leRegion", null, null, null)
    private var medic: BluetoothMedic? = null

    private val beaconConsumer = object : BeaconConsumer {

        override fun getApplicationContext(): Context = application

        override fun unbindService(serviceConnection: ServiceConnection) {
            application.unbindService(serviceConnection)
        }

        override fun bindService(intent: Intent?, serviceConnection: ServiceConnection, flags: Int): Boolean {
            return application.bindService(intent, serviceConnection, flags)
        }

        override fun onBeaconServiceConnect() {
            Log.d(TAG, "onBeaconServiceConnect")

            startRanging()
        }
    }

    private fun startRanging() {

        if (beaconManager?.rangingNotifiers?.contains(ruuviRangeNotifier) != true) {
            ruuviRangeNotifier?.let { ruuviRangeNotifier ->
                beaconManager?.addRangeNotifier(ruuviRangeNotifier)
            }
        }

        try {
            beaconManager?.startRangingBeaconsInRegion(region)
        } catch (e: Exception) {
            Log.e(TAG, "Could not start ranging")
        }
    }

    private fun setupMedic(context: Context?): BluetoothMedic {
        val medic = BluetoothMedic.getInstance()
        medic.enablePowerCycleOnFailures(context)
        medic.enablePeriodicTests(context, BluetoothMedic.SCAN_TEST)
        return medic
    }

    private fun startScan(shouldLaunchInBackground: Boolean, backgroundScanIntervalMilliseconds: Long? = null) {

        if (beaconManager == null) {
            beaconManager = BeaconManager.getInstanceForApplication(application)

            beaconManager?.let { beaconManager ->

                setAltBeaconParsers(beaconManager)
                beaconManager.backgroundScanPeriod = 5000
                beaconManager.backgroundMode = shouldLaunchInBackground

                if (shouldLaunchInBackground && backgroundScanIntervalMilliseconds != null) {

                    beaconManager.backgroundBetweenScanPeriod = backgroundScanIntervalMilliseconds

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

    private fun stopScanning() {
        Log.d(TAG, "Stopping scanning")
        medic = null
        if (beaconManager == null) return

        ruuviRangeNotifier?.let { ruuviRangeNotifier ->
            beaconManager?.removeRangeNotifier(ruuviRangeNotifier)
        }

        try {
            beaconManager?.stopRangingBeaconsInRegion(region)
        } catch (e: Exception) {
            Log.d(TAG, "Could not remove ranging region")
        }
        beaconManager?.unbind(beaconConsumer)
        beaconManager = null
    }

    fun onAppCreated() {
        Log.d(TAG, "App class onCreate")

        DefaultOnTagFoundListener.gatewayOn = true

        ruuviRangeNotifier = RuuviRangeNotifier(application, ruuviTagFactory, "RuuviScannerApplication")
            .apply { addTagListener(DefaultOnTagFoundListener(application)) }

        Foreground.init(application)
        Foreground.get().addListener(listener)

        Handler().postDelayed(
            {
                if (!isRunningInForeground) {
                    if (prefs.backgroundScanMode === BackgroundScanModes.FOREGROUND) {
                        ServiceUtils(application).startForegroundService()
                    } else if (prefs.backgroundScanMode === BackgroundScanModes.BACKGROUND) {
                        startBackgroundScanning()
                    }
                }
            },
            5000
        )
    }

    fun startForegroundScanning() {

        if (runForegroundIfEnabled()) return
        if (isRunningInForeground) return

        isRunningInForeground = true

        Utils.removeStateFile(application)
        Log.d(TAG, "Starting foreground scanning")

        startScan(false)

//        DefaultOnTagFoundListener.gatewayOn = !isRunningInForeground

        DefaultOnTagFoundListener.gatewayOn = false
    }

    fun startBackgroundScanning() {
        Log.d(TAG, "Starting background scanning")

        if (runForegroundIfEnabled()) return

        if (prefs.backgroundScanMode !== BackgroundScanModes.BACKGROUND) {
            Log.d(TAG, "Background scanning is not enabled, ignoring")
            return
        }

        var scanInterval = Preferences(application).backgroundScanInterval * 1000L

        if (scanInterval < MIN_SCAN_INTERVAL) scanInterval = MIN_SCAN_INTERVAL

        startScan(true, scanInterval)
//        DefaultOnTagFoundListener.gatewayOn = !isRunningInForeground

//        if (scanInterval.toLong() != beaconManager.backgroundBetweenScanPeriod) {
//            beaconManager.backgroundBetweenScanPeriod = scanInterval.toLong()
//            try {
//                beaconManager.updateScanPeriods()
//            } catch (e: Exception) {
//                Log.e(TAG, "Could not update scan intervals")
//            }
//        }

        DefaultOnTagFoundListener.gatewayOn = true

        if (medic == null) medic = setupMedic(application)
    }

    private fun runForegroundIfEnabled(): Boolean {
        if (prefs.backgroundScanMode === BackgroundScanModes.FOREGROUND) {
            val serviceUtils = ServiceUtils(application)
            stopScanning()
            serviceUtils.startForegroundService()
            return true
        }
        return false
    }

    private var listener: Foreground.Listener = object : Foreground.Listener {

        override fun onBecameForeground() {

            Log.d(TAG, "onBecameForeground")

            startForegroundScanning()

            DefaultOnTagFoundListener.gatewayOn = false
        }

        override fun onBecameBackground() {
            Log.d(TAG, "onBecameBackground")

            isRunningInForeground = false

            val serviceUtils = ServiceUtils(application)

            if (prefs.backgroundScanMode === BackgroundScanModes.DISABLED) { // background scanning is disabled so all scanning things will be killed

                stopScanning()
                serviceUtils.stopForegroundService()
            } else if (prefs.backgroundScanMode === BackgroundScanModes.BACKGROUND) {

                if (serviceUtils.isRunning(AltBeaconScannerForegroundService::class.java)) {
                    serviceUtils.stopForegroundService()
                } else {
                    startBackgroundScanning()
                }
            } else {

                stopScanning()
                serviceUtils.startForegroundService()
            }

            DefaultOnTagFoundListener.gatewayOn = true
        }
    }

    companion object {

        private const val MIN_SCAN_INTERVAL = 15 * 60 * 1000L

        private fun setAltBeaconParsers(beaconManager: BeaconManager) {
            beaconManager.beaconParsers.clear()
            beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout(Constants.RuuviV2and4_LAYOUT))
            val v3Parser = BeaconParser().setBeaconLayout(Constants.RuuviV3_LAYOUT)
            v3Parser.setHardwareAssistManufacturerCodes(intArrayOf(1177))
            beaconManager.beaconParsers.add(v3Parser)
            val v5Parser = BeaconParser().setBeaconLayout(Constants.RuuviV5_LAYOUT)
            v5Parser.setHardwareAssistManufacturerCodes(intArrayOf(1177))
            beaconManager.beaconParsers.add(v5Parser)
        }
    }
}