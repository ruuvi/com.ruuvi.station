package com.ruuvi.station.graph.di

import com.ruuvi.station.graph.GraphView
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider

object GraphInjectionModule {
    val module = Kodein.Module(GraphInjectionModule.javaClass.name) {

        bind<GraphView>() with provider {
            GraphView(instance(), instance(), instance())
        }
    }
}
