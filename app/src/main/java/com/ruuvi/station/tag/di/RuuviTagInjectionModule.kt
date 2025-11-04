package com.ruuvi.station.tag.di

import com.ruuvi.station.tag.domain.TagConverter
import com.ruuvi.station.tag.domain.TagInteractor
import com.ruuvi.station.tag.domain.VisibleMeasurementsOrderInteractor
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

object RuuviTagInjectionModule {

    val module = Kodein.Module(RuuviTagInjectionModule.javaClass.name) {

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