package com.ruuvi.station.app.di

import com.ruuvi.station.bluetooth.di.BluetoothScannerInjectionModule
import org.kodein.di.Kodein

object AppInjectionModules {
    val module = Kodein.Module(AppInjectionModules.javaClass.name) {
        import(BluetoothScannerInjectionModule.module)
    }
}