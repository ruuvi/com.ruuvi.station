package com.ruuvi.station.widgets.di

import com.ruuvi.station.widgets.domain.WidgetInteractor
import com.ruuvi.station.widgets.domain.WidgetPreferencesInteractor
import com.ruuvi.station.widgets.ui.firstWidget.SensorWidgetConfigureViewModel
import com.ruuvi.station.widgets.ui.simpleWidget.SimpleWidgetConfigureViewModel
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton

object WidgetInjectionModule {
    val module = Kodein.Module(WidgetInjectionModule.javaClass.name) {
        bind<SensorWidgetConfigureViewModel>() with provider { SensorWidgetConfigureViewModel(instance(), instance(), instance()) }

        bind<SimpleWidgetConfigureViewModel>() with provider { SimpleWidgetConfigureViewModel(instance(), instance(), instance()) }

        bind<WidgetPreferencesInteractor>() with singleton { WidgetPreferencesInteractor(instance()) }

        bind<WidgetInteractor>() with singleton { WidgetInteractor(instance(),instance(), instance(), instance(), instance()) }
    }
}