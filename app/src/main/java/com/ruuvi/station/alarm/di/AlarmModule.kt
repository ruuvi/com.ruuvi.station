package com.ruuvi.station.alarm.di

import com.ruuvi.station.alarm.domain.AlarmCheckInteractor
import com.ruuvi.station.alarm.domain.AlarmsInteractor
import com.ruuvi.station.alarm.domain.AlertNotificationInteractor
import com.ruuvi.station.alarm.ui.AlarmItemsViewModel
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.factory
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

object AlarmModule {

    val module = Kodein.Module(this.javaClass.name) {
        bind<AlarmCheckInteractor>() with singleton { AlarmCheckInteractor(instance(), instance(), instance(), instance(), instance(), instance(), instance()) }

        bind<AlarmsInteractor>() with singleton { AlarmsInteractor(instance(), instance(), instance(), instance(), instance(), instance()) }

        bind<AlarmItemsViewModel>() with factory { sensorId: String ->
            AlarmItemsViewModel(sensorId, instance(), instance(), instance())
        }

        bind<AlertNotificationInteractor>() with singleton { AlertNotificationInteractor(instance()) }
    }
}