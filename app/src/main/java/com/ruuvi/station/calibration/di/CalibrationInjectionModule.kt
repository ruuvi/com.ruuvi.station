package com.ruuvi.station.calibration.di

import com.ruuvi.station.calibration.domain.CalibrationInteractor
import com.ruuvi.station.calibration.domain.CalibrationViewModelArgs
import com.ruuvi.station.calibration.ui.CalibrateHumidityViewModel
import com.ruuvi.station.calibration.ui.CalibratePressureViewModel
import com.ruuvi.station.calibration.ui.CalibrateTemperatureViewModel
import org.kodein.di.Kodein
import org.kodein.di.generic.*

object CalibrationInjectionModule {
    val module = Kodein.Module(CalibrationInjectionModule.javaClass.name) {
        bind<CalibrationInteractor>() with singleton {
            CalibrationInteractor(instance(), instance(), instance(), instance())
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