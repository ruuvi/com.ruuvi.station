package com.ruuvi.station

import android.app.Application
import com.facebook.stetho.Stetho
import com.raizlabs.android.dbflow.config.FlowManager
import com.ruuvi.station.bluetooth.BluetoothInteractor
import com.ruuvi.station.bluetooth.BluetoothLibrary
import com.ruuvi.station.bluetooth.DefaultOnTagFoundListener
import com.ruuvi.station.bluetooth.domain.BluetoothScannerInteractor
import com.ruuvi.station.bluetooth.util.ScannerSettings
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.util.Preferences
import timber.log.Timber

class RuuviScannerApplication : Application() {
    val bluetoothScannerInteractor = BluetoothScannerInteractor(this)
    var bluetoothInteractor: BluetoothInteractor? = null

    override fun onCreate() {
        super.onCreate()
        if(BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())

        FlowManager.init(this)

        if (BuildConfig.DEBUG) {
            Stetho.initializeWithDefaults(this)
        }

        BluetoothLibrary.initLibrary(this, DefaultOnTagFoundListener(this), object : ScannerSettings() {
            var prefs = Preferences(this@RuuviScannerApplication)

            override fun allowBackgroundScan(): Boolean {
                return prefs.backgroundScanMode != BackgroundScanModes.DISABLED
            }

            override fun getBackgroundScanInterval(): Long {
                return prefs.backgroundScanInterval * 1000L
            }
        })
        bluetoothInteractor = BluetoothLibrary.getBluetoothInteractor()
    }

    fun startForegroundScanning() {
        if (bluetoothInteractor?.canScan() == true) {
            bluetoothInteractor?.startForegroundScanning()
        }
        else {
            Timber.e("Bluetooth is not available!")
        }
    }

    fun startBackgroundScanning() {
    }

    companion object {
        private const val isBluetoothFakingEnabledInDebug = false

        val isBluetoothFakingEnabled =
            if (BuildConfig.DEBUG) isBluetoothFakingEnabledInDebug
            else /*if release */ false
    }
}