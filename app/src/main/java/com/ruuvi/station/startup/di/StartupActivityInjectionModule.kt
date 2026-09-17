package com.ruuvi.station.startup.di

import com.ruuvi.station.startup.domain.StartupActivityInteractor
import com.ruuvi.station.startup.ui.StartupActivityViewModel
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.provider
import org.kodein.di.singleton

object StartupActivityInjectionModule {
    val module = DI.Module(StartupActivityInjectionModule.javaClass.name) {
        bind<StartupActivityViewModel>() with provider {
            StartupActivityViewModel(instance(), instance(), instance())
        }

        bind<StartupActivityInteractor>() with singleton {
            StartupActivityInteractor(instance())
        }
    }
}