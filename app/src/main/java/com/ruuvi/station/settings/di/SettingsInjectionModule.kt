package com.ruuvi.station.settings.di

import com.ruuvi.station.settings.domain.AppSettingsInteractor
import com.ruuvi.station.settings.ui.*
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.provider
import org.kodein.di.singleton

object SettingsInjectionModule {
    val module = DI.Module(SettingsInjectionModule.javaClass.name) {
        bind<BackgroundScanSettingsViewModel>() with provider { BackgroundScanSettingsViewModel(instance(), instance()) }

        bind<ChartSettingsViewModel>() with provider { ChartSettingsViewModel(instance()) }

        bind<AppSettingsListViewModel>() with provider { AppSettingsListViewModel(instance(), instance(), instance()) }

        bind<DataForwardingSettingsViewModel>() with provider { DataForwardingSettingsViewModel(instance()) }

        bind<AppSettingsPressureUnitViewModel>() with provider { AppSettingsPressureUnitViewModel(instance()) }

        bind<AppSettingsTemperatureUnitViewModel>() with provider { AppSettingsTemperatureUnitViewModel(instance()) }

        bind<AppSettingsHumidityViewModel>() with provider { AppSettingsHumidityViewModel(instance()) }

        bind<AppSettingsInteractor>() with singleton {
            AppSettingsInteractor(instance(), instance(), instance(), instance(), instance(), instance())
        }

        bind<AppearanceSettingsViewModel>() with provider { AppearanceSettingsViewModel(instance()) }

        bind<TemperatureSettingsViewModel>() with provider { TemperatureSettingsViewModel(instance()) }

        bind<HumiditySettingsViewModel>() with provider { HumiditySettingsViewModel(instance()) }

        bind<PressureSettingsViewModel>() with provider { PressureSettingsViewModel(instance()) }

        bind<GlobalUnitsAndResolutionViewModel>() with provider { GlobalUnitsAndResolutionViewModel(instance()) }

        bind<CloudSettingsViewModel>() with provider { CloudSettingsViewModel(instance()) }

        bind<AlertNotificationsSettingsViewModel>() with provider { AlertNotificationsSettingsViewModel(instance()) }

        bind<DeveloperSettingsViewModel>() with provider { DeveloperSettingsViewModel(instance(), instance(), instance(), instance()) }
    }
}
