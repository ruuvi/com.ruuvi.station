package com.ruuvi.station.settings.di

import com.ruuvi.station.settings.ui.AppSettingsBackgroundScanViewModel
import com.ruuvi.station.settings.ui.AppSettingsGraphViewModel
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider

object SettingsInjectionModule {
    val module = Kodein.Module(SettingsInjectionModule.javaClass.name) {
        bind<AppSettingsBackgroundScanViewModel>() with provider { AppSettingsBackgroundScanViewModel(instance()) }

        bind<AppSettingsGraphViewModel>() with provider { AppSettingsGraphViewModel(instance()) }
    }
}