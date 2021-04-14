package com.ruuvi.station.database.di

import com.ruuvi.station.database.SensorSettingsRepository
import com.ruuvi.station.database.TagRepository
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

object DatabaseInjectionModule {
    val module = Kodein.Module(DatabaseInjectionModule.javaClass.name){
        bind<TagRepository>() with singleton {
            TagRepository(instance())
        }

        bind<SensorSettingsRepository>() with singleton {
            SensorSettingsRepository()
        }
    }
}