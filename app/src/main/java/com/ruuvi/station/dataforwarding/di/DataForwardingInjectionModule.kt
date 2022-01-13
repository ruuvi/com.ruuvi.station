package com.ruuvi.station.dataforwarding.di

import com.ruuvi.station.dataforwarding.domain.EventFactory
import com.ruuvi.station.dataforwarding.domain.DataForwardingSender
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

object DataForwardingInjectionModule {
    val module = Kodein.Module(DataForwardingInjectionModule.javaClass.name) {
        bind<DataForwardingSender>() with singleton { DataForwardingSender(instance(), instance(), instance()) }

        bind<EventFactory>() with singleton { EventFactory(instance(), instance(), instance()) }
    }
}