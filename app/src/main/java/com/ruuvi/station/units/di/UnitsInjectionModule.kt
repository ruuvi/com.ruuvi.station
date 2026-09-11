package com.ruuvi.station.units.di

import com.ruuvi.station.units.domain.AccelerationConverter
import com.ruuvi.station.units.domain.MovementConverter
import com.ruuvi.station.units.domain.UnitsConverter
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton

object UnitsInjectionModule {
    val module = DI.Module(UnitsInjectionModule.javaClass.name) {

        bind<UnitsConverter>() with singleton {
            UnitsConverter(instance(), instance())
        }

        bind<MovementConverter>() with singleton {
            MovementConverter(instance())
        }

        bind<AccelerationConverter>() with singleton {
            AccelerationConverter(instance())
        }
    }
}