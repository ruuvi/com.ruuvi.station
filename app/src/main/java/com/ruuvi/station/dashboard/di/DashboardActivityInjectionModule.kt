package com.ruuvi.station.dashboard.di

import com.ruuvi.station.dashboard.domain.SensorsSortingInteractor
import com.ruuvi.station.dashboard.ui.DashboardActivityViewModel
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.provider
import org.kodein.di.singleton

object DashboardActivityInjectionModule {

    val module = DI.Module(DashboardActivityInjectionModule.javaClass.name) {

        bind<DashboardActivityViewModel>() with provider {
            DashboardActivityViewModel(instance(), instance(), instance(), instance(), instance(),
                instance(), instance(), instance(), instance(), instance(), instance())
        }

        bind<SensorsSortingInteractor>() with singleton {
            SensorsSortingInteractor(instance(), instance())
        }
    }
}