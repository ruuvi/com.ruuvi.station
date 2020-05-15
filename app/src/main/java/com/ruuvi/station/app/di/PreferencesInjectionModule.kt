package com.ruuvi.station.app.di

import com.ruuvi.station.app.preferences.Preferences
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

object PreferencesInjectionModule {
    val module = Kodein.Module(PreferencesInjectionModule.javaClass.name) {
        bind<Preferences>() with singleton { Preferences(instance()) }
    }
}