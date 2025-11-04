package com.ruuvi.station.tagsettings.di

import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor
import com.ruuvi.station.tagsettings.ui.BackgroundViewModel
import com.ruuvi.station.tagsettings.ui.RemoveSensorViewModel
import com.ruuvi.station.tagsettings.ui.TagSettingsViewModel
import com.ruuvi.station.tagsettings.ui.visible_measurements.VisibleMeasurementsViewModel
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.factory
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

object TagSettingsInjectionModule {
    val module = Kodein.Module(TagSettingsInjectionModule.javaClass.name) {

        bind<TagSettingsInteractor>() with singleton {
            TagSettingsInteractor(instance(), instance(), instance(), instance(), instance())
        }

        bind<TagSettingsViewModel>() with factory { args: TagSettingsViewModelArgs ->
            TagSettingsViewModel(args.tagId, args.newSensor, instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance())
        }

        bind<BackgroundViewModel>() with factory { sensorId: String ->
            BackgroundViewModel(sensorId, instance(), instance())
        }

        bind<VisibleMeasurementsViewModel>() with factory { sensorId: String ->
            VisibleMeasurementsViewModel(sensorId, instance(), instance(), instance(), instance(), instance(), instance())
        }

        bind<RemoveSensorViewModel>() with factory { args: RemoveSensorViewModelArgs ->
            RemoveSensorViewModel(args.sensorId, instance(), instance())
        }
    }
}