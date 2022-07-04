package com.ruuvi.station.calibration.ui

import android.os.Bundle
import com.ruuvi.station.R
import com.ruuvi.station.calibration.domain.CalibrationViewModelArgs
import com.ruuvi.station.calibration.model.CalibrationType
import com.ruuvi.station.util.extensions.viewModel
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein

class CalibrateHumidityFragment : CalibrationFragment(R.layout.fragment_calibrate), KodeinAware {

    override val kodein: Kodein by closestKodein()
    override val viewModel: CalibrateHumidityViewModel by viewModel {
        arguments?.let {
            CalibrationViewModelArgs(it.getString(SENSOR_ID, ""))
        }
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