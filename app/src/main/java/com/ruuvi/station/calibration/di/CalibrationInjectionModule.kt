package com.ruuvi.station.calibration.di

import com.ruuvi.station.calibration.domain.CalibrationInteractor
import com.ruuvi.station.calibration.domain.CalibrationViewModelArgs
import com.ruuvi.station.calibration.ui.CalibrateTemperatureViewModel
import org.kodein.di.Kodein
import org.kodein.di.generic.*

object CalibrationInjectionModule {
    val module = Kodein.Module(CalibrationInjectionModule.javaClass.name) {
        bind<CalibrationInteractor>() with singleton {
            CalibrationInteractor(instance(), instance(), instance())
        }

        bind<CalibrateTemperatureViewModel>() with factory { args: CalibrationViewModelArgs ->
            CalibrateTemperatureViewModel(args.sensorId, instance())
        }
    }
}