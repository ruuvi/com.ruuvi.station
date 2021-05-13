package com.ruuvi.station.database.di

import com.raizlabs.android.dbflow.config.DatabaseDefinition
import com.raizlabs.android.dbflow.config.FlowManager
import com.ruuvi.station.database.domain.*
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

object DatabaseInjectionModule {
    val module = Kodein.Module(DatabaseInjectionModule.javaClass.name){
        bind<DatabaseDefinition>() with singleton {
            FlowManager.getDatabase(LocalDatabase::class.java)
        }

        bind<TagRepository>() with singleton {
            TagRepository(instance(), instance(), instance(), instance())
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

        bind<NetworkRequestRepository>() with singleton {
            NetworkRequestRepository()
        }
    }
}