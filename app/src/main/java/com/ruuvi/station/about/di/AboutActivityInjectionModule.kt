package com.ruuvi.station.about.di

import com.ruuvi.station.about.ui.AboutActivityViewModel
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.provider

object AboutActivityInjectionModule {
    val module = DI.Module(AboutActivityInjectionModule.javaClass.name) {

        bind<AboutActivityViewModel>() with provider { AboutActivityViewModel(instance(), instance()) }
    }
}
