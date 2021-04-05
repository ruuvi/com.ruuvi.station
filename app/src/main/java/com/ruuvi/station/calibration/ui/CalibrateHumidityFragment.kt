package com.ruuvi.station.calibration.ui

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ruuvi.station.R

class CalibrateHumidityFragment : Fragment() {

    companion object {
        fun newInstance() = CalibrateHumidityFragment()
    }

    private lateinit var viewModel: CalibrateHumidityViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_calibrate_humidity, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(CalibrateHumidityViewModel::class.java)
        // TODO: Use the ViewModel
    }

}