package com.ruuvi.station.settings.di

import com.ruuvi.station.settings.domain.AppSettingsInteractor
import com.ruuvi.station.settings.ui.*
import org.kodein.di.Kodein
import org.kodein.di.generic.*

object SettingsInjectionModule {
    val module = Kodein.Module(SettingsInjectionModule.javaClass.name) {
        bind<AppSettingsViewModel>() with provider { AppSettingsViewModel() }

        bind<AppSettingsBackgroundScanViewModel>() with provider { AppSettingsBackgroundScanViewModel(instance()) }

        bind<AppSettingsGraphViewModel>() with provider { AppSettingsGraphViewModel(instance()) }

        bind<AppSettingsListViewModel>() with provider { AppSettingsListViewModel(instance(), instance()) }

        bind<AppSettingsGatewayViewModel>() with provider { AppSettingsGatewayViewModel(instance()) }

        bind<AppSettingsPressureUnitViewModel>() with provider { AppSettingsPressureUnitViewModel(instance()) }

        bind<AppSettingsTemperatureUnitViewModel>() with provider { AppSettingsTemperatureUnitViewModel(instance()) }

        bind<AppSettingsHumidityViewModel>() with provider { AppSettingsHumidityViewModel(instance()) }

        bind<AppSettingsLocaleViewModel>() with provider { AppSettingsLocaleViewModel(instance(), instance()) }

        bind<AppSettingsExperimentalViewModel>() with provider { AppSettingsExperimentalViewModel(instance()) }

        bind<AppSettingsInteractor>() with singleton {
            AppSettingsInteractor(instance(), instance(), instance(), instance(), instance())
        }
    }
}