package com.ruuvi.station.calibration.di

import com.ruuvi.station.calibration.domain.CalibrationInteractor
import com.ruuvi.station.calibration.domain.CalibrationViewModelArgs
import com.ruuvi.station.calibration.ui.CalibrateHumidityViewModel
import com.ruuvi.station.calibration.ui.CalibratePressureViewModel
import com.ruuvi.station.calibration.ui.CalibrateTemperatureViewModel
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.factory
import org.kodein.di.instance
import org.kodein.di.singleton

object CalibrationInjectionModule {
    val module = DI.Module(CalibrationInjectionModule.javaClass.name) {
        bind<CalibrationInteractor>() with singleton {
            CalibrationInteractor(instance(), instance(), instance(), instance(), instance())
        }

        bind<CalibrateTemperatureViewModel>() with factory { args: CalibrationViewModelArgs ->
            CalibrateTemperatureViewModel(args.sensorId, instance())
        }

        bind<CalibratePressureViewModel>() with factory { args: CalibrationViewModelArgs ->
            CalibratePressureViewModel(args.sensorId, instance())
        }

        bind<CalibrateHumidityViewModel>() with factory { args: CalibrationViewModelArgs ->
            CalibrateHumidityViewModel(args.sensorId, instance())
        }
    }
}