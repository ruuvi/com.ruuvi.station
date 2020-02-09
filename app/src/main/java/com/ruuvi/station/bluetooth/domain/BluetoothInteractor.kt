package com.ruuvi.station.bluetooth.domain

import android.app.Application
import android.os.Handler
import android.util.Log
import com.ruuvi.station.RuuviScannerApplication
import com.ruuvi.station.bluetooth.DefaultOnTagFoundListener
import com.ruuvi.station.bluetooth.IRuuviRangeNotifier
import com.ruuvi.station.bluetooth.RuuviRangeNotifier
import com.ruuvi.station.bluetooth.fake.FakeRuuviRangeNotifier
import com.ruuvi.station.service.AltBeaconScannerForegroundService
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.util.Foreground
import com.ruuvi.station.util.Preferences
import com.ruuvi.station.util.ServiceUtils
import com.ruuvi.station.util.Utils

class BluetoothInteractor(
    private val application: Application
) {

    private val TAG: String = BluetoothInteractor::class.java.simpleName

    private val prefs: Preferences = Preferences(application)

    private var isRunningInForeground = false
    private var ruuviRangeNotifier: IRuuviRangeNotifier? = null

    private val listener: Foreground.Listener = object : Foreground.Listener {

        override fun onBecameForeground() {

            Log.d(TAG, "ruuvi onBecameForeground start foreground scanning")

            startForegroundScanning()

            DefaultOnTagFoundListener.gatewayOn = false
        }

        override fun onBecameBackground() {

            isRunningInForeground = false

            val serviceUtils = ServiceUtils(application)

            if (prefs.backgroundScanMode === BackgroundScanModes.DISABLED) { // background scanning is disabled so all scanning things will be killed
                Log.d(TAG, "ruuvi onBecameBackground stop scanning, background scan mode DISABLED")

                stopScanning()
                serviceUtils.stopForegroundService()
            } else if (prefs.backgroundScanMode === BackgroundScanModes.BACKGROUND) {

                if (serviceUtils.isRunning(AltBeaconScannerForegroundService::class.java)) {

                    Log.d(TAG, "ruuvi onBecameBackground stop foreground scanning, background scan mode enabled(BACKGROUND)")

                    serviceUtils.stopForegroundService()
                }

                Log.d(TAG, "ruuvi onBecameBackground start background scanning, background scan mode enabled(BACKGROUND)")

                startBackgroundScanning()

            } else if (prefs.backgroundScanMode === BackgroundScanModes.FOREGROUND) {
                Log.d(TAG, "ruuvi onBecameBackground stop scanning and start foreground scanning, background scan mode FOREGROUND")

                stopScanning()
                serviceUtils.startForegroundService()
            }

            DefaultOnTagFoundListener.gatewayOn = true
        }
    }

    fun onAppCreated() {
        Log.d(TAG, "App class onCreate")

        DefaultOnTagFoundListener.gatewayOn = true

        ruuviRangeNotifier =
            if (RuuviScannerApplication.isBluetoothFakingEnabled) FakeRuuviRangeNotifier()
            else RuuviRangeNotifier(application, "RuuviScannerApplication")

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

        startScan(DefaultOnTagFoundListener(application), false)

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

        if (scanInterval < MIN_SCAN_INTERVAL_MILLISECONDS) scanInterval = MIN_SCAN_INTERVAL_MILLISECONDS

        Log.d(TAG, "ruuvi scanInterval $scanInterval")

        startScan(DefaultOnTagFoundListener(application), true, scanInterval)

        DefaultOnTagFoundListener.gatewayOn = true
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

    private fun startScan(
        tagsFoundListener: IRuuviRangeNotifier.OnTagsFoundListener,
        shouldLaunchInBackground: Boolean,
        backgroundScanIntervalMilliseconds: Long? = null
    ) {
        ruuviRangeNotifier?.startScanning(tagsFoundListener, shouldLaunchInBackground)

        backgroundScanIntervalMilliseconds?.let { backgroundScanIntervalMilliseconds ->
            ruuviRangeNotifier?.setBackgroundScheduledScanInterval(backgroundScanIntervalMilliseconds)
        }
    }

    private fun stopScanning() {
        Log.d(TAG, "Stopping scanning")

        ruuviRangeNotifier?.stopScanning()
    }

    companion object {

        private const val MIN_SCAN_INTERVAL_MILLISECONDS = 15 * 60 * 1000L

    }
}