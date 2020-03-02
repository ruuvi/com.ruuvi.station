package com.ruuvi.station.bluetooth.di

import com.ruuvi.station.bluetooth.BluetoothInteractor
import com.ruuvi.station.bluetooth.BluetoothLibrary
import com.ruuvi.station.bluetooth.DefaultOnTagFoundListener
import com.ruuvi.station.bluetooth.IRuuviTagScanner
import com.ruuvi.station.bluetooth.util.ScannerSettings
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.util.Preferences
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

object BluetoothScannerInjectionModule {

    val module = Kodein.Module(BluetoothScannerInjectionModule.javaClass.name) {
        bind<BluetoothInteractor>() with singleton { BluetoothLibrary.getBluetoothInteractor(instance(), instance(), instance()) }
        bind<IRuuviTagScanner.OnTagFoundListener>() with singleton { DefaultOnTagFoundListener(instance()) }
        bind<ScannerSettings>() with singleton {
            object : ScannerSettings() {
                var prefs = Preferences(instance())
                override fun allowBackgroundScan(): Boolean {
                    return prefs.backgroundScanMode != BackgroundScanModes.DISABLED
                }

                override fun getBackgroundScanInterval(): Long {
                    return prefs.backgroundScanInterval * 1000L
                }
            }
        }
    }
}