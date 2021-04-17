package com.ruuvi.station.database.di

import com.ruuvi.station.database.domain.AlarmRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.database.domain.SensorHistoryRepository
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

        bind<SensorHistoryRepository>() with singleton {
            SensorHistoryRepository()
        }

        bind<AlarmRepository>() with singleton {
            AlarmRepository()
        }
    }
}