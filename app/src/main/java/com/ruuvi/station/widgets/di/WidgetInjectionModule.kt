package com.ruuvi.station.widgets.di

import com.ruuvi.station.widgets.complexWidget.ComplexWidgetConfigureViewModel
import com.ruuvi.station.widgets.complexWidget.ComplexWidgetConfigureViewModelArgs
import com.ruuvi.station.widgets.domain.ComplexWidgetPreferencesInteractor
import com.ruuvi.station.widgets.domain.WidgetInteractor
import com.ruuvi.station.widgets.domain.WidgetPreferencesInteractor
import com.ruuvi.station.widgets.ui.simpleWidget.SimpleWidgetConfigureViewModel
import org.kodein.di.Kodein
import org.kodein.di.generic.*

object WidgetInjectionModule {
    val module = Kodein.Module(WidgetInjectionModule.javaClass.name) {

        bind<SimpleWidgetConfigureViewModel>() with provider { SimpleWidgetConfigureViewModel(instance(), instance(), instance(), instance()) }

        bind<ComplexWidgetConfigureViewModel>() with factory { args: ComplexWidgetConfigureViewModelArgs -> ComplexWidgetConfigureViewModel(args.appWidgetId, instance(), instance(), instance(), instance()) }

        bind<WidgetPreferencesInteractor>() with singleton { WidgetPreferencesInteractor(instance()) }

        bind<ComplexWidgetPreferencesInteractor>() with singleton { ComplexWidgetPreferencesInteractor(instance()) }

        bind<WidgetInteractor>() with singleton { WidgetInteractor(instance(),instance(), instance(), instance(), instance()) }
    }
}