package com.ruuvi.station.calibration.ui

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ruuvi.station.R

class CalibratePressureFragment : Fragment() {

    companion object {
        fun newInstance() = CalibratePressureFragment()
    }

    private lateinit var viewModel: CalibratePressureViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragmant_calibrate_pressure, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(CalibratePressureViewModel::class.java)
        // TODO: Use the ViewModel
    }

}