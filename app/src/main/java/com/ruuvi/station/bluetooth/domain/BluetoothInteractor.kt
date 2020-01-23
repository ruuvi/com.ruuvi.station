package com.ruuvi.station.bluetooth.domain

import android.app.Application
import android.os.Handler
import android.util.Log
import com.ruuvi.station.bluetooth.DefaultOnTagFoundListener
import com.ruuvi.station.bluetooth.RuuviRangeNotifier
import com.ruuvi.station.bluetooth.interfaces.RuuviTagFactory
import com.ruuvi.station.service.AltBeaconScannerForegroundService
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.util.Foreground
import com.ruuvi.station.util.Preferences
import com.ruuvi.station.util.ServiceUtils
import com.ruuvi.station.util.Utils

class BluetoothInteractor(
    private val application: Application,
    private val ruuviTagFactory: RuuviTagFactory
) {

    private val TAG: String = BluetoothInteractor::class.java.simpleName

    private val prefs: Preferences = Preferences(application)

    private var isRunningInForeground = false
    private var ruuviRangeNotifier: RuuviRangeNotifier? = null

    private val listener: Foreground.Listener = object : Foreground.Listener {

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

    fun onAppCreated() {
        Log.d(TAG, "App class onCreate")

        DefaultOnTagFoundListener.gatewayOn = true

        ruuviRangeNotifier = RuuviRangeNotifier(application, ruuviTagFactory, "RuuviScannerApplication")

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
        tagsFoundListener: RuuviRangeNotifier.OnTagsFoundListener,
        shouldLaunchInBackground: Boolean,
        backgroundScanIntervalMilliseconds: Long? = null
    ) {
        ruuviRangeNotifier?.startScan(tagsFoundListener, shouldLaunchInBackground, backgroundScanIntervalMilliseconds)
    }

    private fun stopScanning() {
        Log.d(TAG, "Stopping scanning")

        ruuviRangeNotifier?.stopScanning()
    }

    companion object {

        private const val MIN_SCAN_INTERVAL_MILLISECONDS = 15 * 60 * 1000L

    }
}