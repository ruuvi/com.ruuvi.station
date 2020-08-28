package com.ruuvi.station.settings.di

import com.ruuvi.station.settings.domain.AppSettingsInteractor
import com.ruuvi.station.settings.ui.*
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton

object SettingsInjectionModule {
    val module = Kodein.Module(SettingsInjectionModule.javaClass.name) {
        bind<AppSettingsViewModel>() with provider { AppSettingsViewModel() }

        bind<AppSettingsBackgroundScanViewModel>() with provider { AppSettingsBackgroundScanViewModel(instance()) }

        bind<AppSettingsGraphViewModel>() with provider { AppSettingsGraphViewModel(instance()) }

        bind<AppSettingsDetailViewModel>() with provider { AppSettingsDetailViewModel(instance()) }

        bind<AppSettingsListViewModel>() with provider { AppSettingsListViewModel(instance()) }

        bind<AppSettingsGatewayViewModel>() with provider { AppSettingsGatewayViewModel(instance()) }

        bind<AppSettingsInteractor>() with singleton {
            AppSettingsInteractor(instance(), instance())
        }
    }
}