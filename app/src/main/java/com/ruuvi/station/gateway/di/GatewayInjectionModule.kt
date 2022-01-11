package com.ruuvi.station.gateway.di

import com.ruuvi.station.gateway.EventFactory
import com.ruuvi.station.gateway.DataForwardingSender
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

object GatewayInjectionModule {
    val module = Kodein.Module(GatewayInjectionModule.javaClass.name) {
        bind<DataForwardingSender>() with singleton { DataForwardingSender(instance(), instance(), instance()) }

        bind<EventFactory>() with singleton { EventFactory(instance(), instance(), instance()) }
    }
}