package com.ruuvi.station.calibration.ui

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ruuvi.station.R

class CalibrateTemperatureFragment : Fragment() {

    companion object {
        fun newInstance() = CalibrateTemperatureFragment()
    }

    private lateinit var viewModel: CalibrateTemperatureViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_calibrate_temperature, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(CalibrateTemperatureViewModel::class.java)
        // TODO: Use the ViewModel
    }

}