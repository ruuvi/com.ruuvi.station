package com.ruuvi.station.startup.di

import com.ruuvi.station.startup.ui.StartupActivityViewModel
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider

object StartupActivityInjectionModule {
    val module = Kodein.Module(StartupActivityInjectionModule.javaClass.name) {
        bind<StartupActivityViewModel>() with provider {
            StartupActivityViewModel(instance(), instance())
        }
    }
}