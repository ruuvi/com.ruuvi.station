package com.ruuvi.station.about.di

import com.ruuvi.station.about.ui.AboutActivityViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider

@ExperimentalCoroutinesApi
object AboutActivityInjectionModule {
    val module = Kodein.Module(AboutActivityInjectionModule.javaClass.name) {

        bind<AboutActivityViewModel>() with provider { AboutActivityViewModel(instance()) }
    }
}