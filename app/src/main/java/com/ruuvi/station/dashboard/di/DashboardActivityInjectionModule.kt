package com.ruuvi.station.dashboard.di

import com.ruuvi.station.dashboard.domain.SensorsSortingInteractor
import com.ruuvi.station.dashboard.ui.DashboardActivityViewModel
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton

object DashboardActivityInjectionModule {

    val module = Kodein.Module(DashboardActivityInjectionModule.javaClass.name) {

        bind<DashboardActivityViewModel>() with provider {
            DashboardActivityViewModel(instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance())
        }

        bind<SensorsSortingInteractor>() with singleton {
            SensorsSortingInteractor(instance())
        }
    }
}