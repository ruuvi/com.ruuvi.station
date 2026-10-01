package com.ruuvi.station.dataforwarding.di

import com.ruuvi.station.dataforwarding.domain.EventFactory
import com.ruuvi.station.dataforwarding.domain.DataForwardingSender
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton

object DataForwardingInjectionModule {
    val module = DI.Module(DataForwardingInjectionModule.javaClass.name) {
        bind<DataForwardingSender>() with singleton { DataForwardingSender(instance(), instance(), instance()) }

        bind<EventFactory>() with singleton { EventFactory(instance(), instance()) }
    }
}