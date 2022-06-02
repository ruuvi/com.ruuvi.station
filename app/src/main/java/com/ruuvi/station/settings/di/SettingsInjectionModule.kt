package com.ruuvi.station.settings.di

import com.ruuvi.station.settings.domain.AppSettingsInteractor
import com.ruuvi.station.settings.ui.*
import org.kodein.di.Kodein
import org.kodein.di.generic.*

object SettingsInjectionModule {
    val module = Kodein.Module(SettingsInjectionModule.javaClass.name) {
        bind<AppSettingsViewModel>() with provider { AppSettingsViewModel(instance()) }

        bind<AppSettingsBackgroundScanViewModel>() with provider { AppSettingsBackgroundScanViewModel(instance(), instance()) }

        bind<AppSettingsGraphViewModel>() with provider { AppSettingsGraphViewModel(instance()) }

        bind<AppSettingsListViewModel>() with provider { AppSettingsListViewModel(instance(), instance()) }

        bind<AppSettingsDataForwardingViewModel>() with provider { AppSettingsDataForwardingViewModel(instance()) }

        bind<AppSettingsPressureUnitViewModel>() with provider { AppSettingsPressureUnitViewModel(instance()) }

        bind<AppSettingsTemperatureUnitViewModel>() with provider { AppSettingsTemperatureUnitViewModel(instance()) }

        bind<AppSettingsHumidityViewModel>() with provider { AppSettingsHumidityViewModel(instance()) }

        bind<AppSettingsExperimentalViewModel>() with provider { AppSettingsExperimentalViewModel(instance()) }

        bind<AppSettingsInteractor>() with singleton {
            AppSettingsInteractor(instance(), instance(), instance(), instance(), instance())
        }
    }
}