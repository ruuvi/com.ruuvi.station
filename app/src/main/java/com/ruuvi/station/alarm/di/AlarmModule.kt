package com.ruuvi.station.alarm.di

import com.ruuvi.station.alarm.domain.AlarmCheckInteractor
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

object AlarmModule {

    val module = Kodein.Module(this.javaClass.name) {
        bind<AlarmCheckInteractor>() with singleton { AlarmCheckInteractor(instance(), instance(), instance(), instance(), instance()) }
    }
}