package com.ruuvi.station.calibration.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isInvisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.observe
import com.ruuvi.station.R
import com.ruuvi.station.calibration.domain.CalibrationViewModelArgs
import com.ruuvi.station.databinding.FragmentCalibrateTemperatureBinding
import com.ruuvi.station.util.extensions.describingTimeSince
import com.ruuvi.station.util.extensions.setDebouncedOnClickListener
import com.ruuvi.station.util.extensions.viewModel
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.support.closestKodein
import timber.log.Timber
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

class CalibrateTemperatureFragment : Fragment(R.layout.fragment_calibrate_temperature), KodeinAware {

    override val kodein: Kodein by closestKodein()
    private val viewModel: CalibrateTemperatureViewModel by viewModel {
        arguments?.let {
            CalibrationViewModelArgs(it.getString(SENSOR_ID, ""))
        }
    }
    private lateinit var binding: FragmentCalibrateTemperatureBinding

    private var timer: Timer? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentCalibrateTemperatureBinding.bind(view)

        setupUI()
        setupViewModel()
    }

    private fun setupUI() {
        binding.calibrateButton.setDebouncedOnClickListener {
            val dialog = CalibrationEditDialog.newInstance(viewModel.getTemperatureUnit(), object : CalibrationEditListener {
                override fun onDialogPositiveClick(dialog: DialogFragment, value: Double) {
                    Timber.d("onDialogPositiveClick $value")
                    viewModel.calibrateTo(value)
                }

                override fun onDialogNegativeClick(dialog: DialogFragment) {
                    Timber.d("onDialogNegativeClick")
                }
            })
            dialog.show(requireActivity().supportFragmentManager, "calibrate")
        }

        binding.clearButton.setDebouncedOnClickListener {
            val alertDialog = AlertDialog.Builder(requireContext()).create()
            alertDialog.setTitle("Confirm")
            alertDialog.setMessage("Clear calibration settings?")
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, requireContext().getString(R.string.yes)
            ) { _, _ -> viewModel.clearTemperatureCalibration() }
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, requireContext().getString(R.string.no)
            ) { _, _ -> }
            alertDialog.show()
        }
    }

    private fun setupViewModel() {
        viewModel.calibrationInfoObserve.observe(viewLifecycleOwner) {info ->
            binding.originalValueTextView.text = viewModel.getTemperatureString(info.temperatureRaw)
            binding.originalUpdatedTextView.text = "(${info.updateAt?.describingTimeSince(requireContext())})"
            binding.correctedlValueTextView.text = viewModel.getTemperatureString(info.temperatureCalibrated)
            binding.correctedOffsetTextView.text = "(Offset ${info.currentTemperatureOffsetString})"

            binding.correctedTitleTextView.isInvisible = !info.isTemperatureCalibrated
            binding.correctedlValueTextView.isInvisible = !info.isTemperatureCalibrated
            binding.correctedOffsetTextView.isInvisible = !info.isTemperatureCalibrated
            binding.clearButton.isInvisible = !info.isTemperatureCalibrated
        }
    }

    override fun onResume() {
        super.onResume()
        timer = Timer("CalibrateTemperatureTimer", false)
        timer?.scheduleAtFixedRate(0, 500) {
            viewModel.refreshSensorData()
        }
    }

    override fun onPause() {
        super.onPause()
        timer?.cancel()
    }

    companion object {
        const val SENSOR_ID = "SENSOR_ID"
        fun newInstance(sensorId: String): CalibrateTemperatureFragment {
            val fragment = CalibrateTemperatureFragment()
            val arguments = Bundle()
            arguments.putString(SENSOR_ID, sensorId)
            fragment.arguments = arguments
            return fragment
        }
    }
}