package com.ruuvi.station.image.di

import com.ruuvi.station.image.ImageInteractor
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton

object ImageInjectionModule {
    val module = DI.Module(ImageInjectionModule.javaClass.name) {
        bind<ImageInteractor>() with singleton { ImageInteractor(instance()) }
    }
}