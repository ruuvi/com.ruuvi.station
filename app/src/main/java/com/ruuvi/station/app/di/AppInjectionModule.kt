package com.ruuvi.station.app.di

import androidx.lifecycle.ViewModelProvider
import com.ruuvi.station.app.ui.ViewModelFactory
import com.ruuvi.station.util.Foreground
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

object AppInjectionModule {
    val module = Kodein.Module(AppInjectionModule.javaClass.name) {
        bind<ViewModelProvider.Factory>() with singleton { ViewModelFactory(dkodein) }

        bind<Foreground>() with singleton { Foreground.createInstance(instance()) }
    }
}