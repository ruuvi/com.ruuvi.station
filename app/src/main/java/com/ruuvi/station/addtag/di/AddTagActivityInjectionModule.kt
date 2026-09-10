package com.ruuvi.station.addtag.di

import com.ruuvi.station.addtag.ui.AddTagActivityViewModel
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.provider

object AddTagActivityInjectionModule {

    val module = DI.Module(AddTagActivityInjectionModule.javaClass.name) {
        bind<AddTagActivityViewModel>() with provider {
            AddTagActivityViewModel(instance(), instance())
        }
    }
}