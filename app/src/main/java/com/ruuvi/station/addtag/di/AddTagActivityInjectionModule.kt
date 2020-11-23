package com.ruuvi.station.addtag.di

import com.ruuvi.station.addtag.ui.AddTagActivityViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider

@ExperimentalCoroutinesApi
object AddTagActivityInjectionModule {

    val module = Kodein.Module(AddTagActivityInjectionModule.javaClass.name) {
        bind<AddTagActivityViewModel>() with provider {
            AddTagActivityViewModel(instance())
        }
    }
}