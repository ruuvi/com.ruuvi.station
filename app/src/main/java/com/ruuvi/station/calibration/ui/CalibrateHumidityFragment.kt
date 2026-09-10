package com.ruuvi.station.calibration.ui

import android.os.Bundle
import com.ruuvi.station.R
import com.ruuvi.station.calibration.domain.CalibrationViewModelArgs
import com.ruuvi.station.calibration.model.CalibrationType
import com.ruuvi.station.util.extensions.viewModel
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.android.x.closestDI

class CalibrateHumidityFragment : CalibrationFragment(R.layout.fragment_calibrate), DIAware {

    override val di: DI by closestDI()
    override val viewModel: CalibrateHumidityViewModel by viewModel {
        CalibrationViewModelArgs(arguments?.getString(SENSOR_ID, "") ?: "")
    }

    override val calibrationType: CalibrationType = CalibrationType.HUMIDITY

    companion object {
        fun newInstance(sensorId: String): CalibrateHumidityFragment {
            val fragment = CalibrateHumidityFragment()
            val arguments = Bundle()
            arguments.putString(SENSOR_ID, sensorId)
            fragment.arguments = arguments
            return fragment
        }
    }
}