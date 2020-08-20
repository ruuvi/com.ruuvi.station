package com.ruuvi.station.settings.di

import com.ruuvi.station.settings.domain.AppSettingsInteractor
import com.ruuvi.station.settings.ui.AppSettingsBackgroundScanViewModel
import com.ruuvi.station.settings.ui.AppSettingsDetailViewModel
import com.ruuvi.station.settings.ui.AppSettingsGraphViewModel
import com.ruuvi.station.settings.ui.AppSettingsListViewModel
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton

object SettingsInjectionModule {
    val module = Kodein.Module(SettingsInjectionModule.javaClass.name) {
        bind<AppSettingsBackgroundScanViewModel>() with provider { AppSettingsBackgroundScanViewModel(instance()) }

        bind<AppSettingsGraphViewModel>() with provider { AppSettingsGraphViewModel(instance()) }

        bind<AppSettingsDetailViewModel>() with provider { AppSettingsDetailViewModel(instance()) }

        bind<AppSettingsListViewModel>() with provider { AppSettingsListViewModel(instance()) }

        bind<AppSettingsInteractor>() with singleton {
            AppSettingsInteractor(instance())
        }
    }
}