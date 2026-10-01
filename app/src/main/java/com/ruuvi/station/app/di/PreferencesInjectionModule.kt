package com.ruuvi.station.app.di

import com.ruuvi.station.app.preferences.Preferences
import com.ruuvi.station.app.preferences.PreferencesRepository
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton

object PreferencesInjectionModule {
    val module = DI.Module(PreferencesInjectionModule.javaClass.name) {
        bind<Preferences>() with singleton { Preferences(instance()) }

        bind<PreferencesRepository>() with singleton {
            PreferencesRepository(instance())
        }
    }
}