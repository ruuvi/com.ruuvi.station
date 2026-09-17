package com.ruuvi.station.tag.di

import com.ruuvi.station.tag.domain.TagConverter
import com.ruuvi.station.tag.domain.TagInteractor
import com.ruuvi.station.tag.domain.VisibleMeasurementsOrderInteractor
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton

object RuuviTagInjectionModule {

    val module = DI.Module(RuuviTagInjectionModule.javaClass.name) {

        bind<TagInteractor>() with singleton {
            TagInteractor(instance(), instance(), instance(), instance(), instance(), instance(), instance())
        }

        bind<TagConverter>() with singleton {
            TagConverter(instance(), instance(), instance())
        }

        bind<VisibleMeasurementsOrderInteractor>() with singleton {
            VisibleMeasurementsOrderInteractor(instance(), instance())
        }
    }
}