package com.ruuvi.station.app.di

import com.ruuvi.station.bluetooth.di.BluetoothScannerInjectionModule
import com.ruuvi.station.gateway.di.GatewayInjectionModule
import com.ruuvi.station.settings.di.SettingsInjectionModule
import org.kodein.di.Kodein

object AppInjectionModules {
    val module = Kodein.Module(AppInjectionModules.javaClass.name) {
        import(AppInjectionModule.module)
        import(PreferencesInjectionModule.module)
        import(BluetoothScannerInjectionModule.module)
        import(SettingsInjectionModule.module)
        import(GatewayInjectionModule.module)
    }
}