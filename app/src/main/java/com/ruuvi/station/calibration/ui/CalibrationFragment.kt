package com.ruuvi.station.calibration.ui

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isInvisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import com.ruuvi.station.R
import com.ruuvi.station.calibration.model.CalibrationType
import com.ruuvi.station.databinding.FragmentCalibrateBinding
import com.ruuvi.station.util.extensions.describingTimeSince
import com.ruuvi.station.util.extensions.makeWebLinks
import com.ruuvi.station.util.extensions.setDebouncedOnClickListener
import timber.log.Timber
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

abstract class CalibrationFragment(@LayoutRes contentLayoutId: Int ): Fragment(contentLayoutId) {
    abstract val viewModel: ICalibrationViewModel

    lateinit var binding: FragmentCalibrateBinding

    private var timer: Timer? = null

    abstract val calibrationType: CalibrationType

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentCalibrateBinding.bind(view)

        setupViewModel()
        setupUI()
        setupCalibrationMessage()
    }

    override fun onResume() {
        super.onResume()
        timer = Timer("CalibrationTimer", false)
        timer?.scheduleAtFixedRate(0, 500) {
            viewModel.refreshSensorData()
        }
    }

    fun setupCalibrationMessage() {
        binding.calibrationInstructionsTextView.text = getString(R.string.calibration_description)
        binding.calibrationInstructionsTextView.makeWebLinks(
            requireContext(),
            Pair(getString(R.string.calibration_description_link), getString(R.string.calibration_description_link_url))
        )
    }

    override fun onPause() {
        super.onPause()
        timer?.cancel()
    }

    fun setupUI() {
        binding.calibrateButton.setDebouncedOnClickListener {
            val dialog = CalibrationEditDialog.newInstance(calibrationType, viewModel.getUnit(), object : CalibrationEditListener {
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
            alertDialog.setTitle(getString(R.string.confirm))
            alertDialog.setMessage(getString(R.string.calibration_clear_confirm))
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes)
            ) { _, _ -> viewModel.clearCalibration() }
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.no)
            ) { _, _ -> }
            alertDialog.show()
        }
    }

    fun setupViewModel() {
        viewModel.calibrationInfoObserve.observe(viewLifecycleOwner) {info ->
            with(binding) {
                originalValueTextView.text = viewModel.getStringForValue(info.rawValue)
                originalUpdatedTextView.text = "(${info.updateAt?.describingTimeSince(requireContext())})"
                correctedlValueTextView.text = viewModel.getStringForValue(info.calibratedValue)
                correctedOffsetTextView.text = getString(R.string.calibration_offset, info.currentOffsetString)

                correctedTitleTextView.isInvisible = !info.isCalibrated
                correctedlValueTextView.isInvisible = !info.isCalibrated
                correctedOffsetTextView.isInvisible = !info.isCalibrated
                clearButton.isEnabled = info.isCalibrated
                separatorView.isInvisible = !info.isCalibrated
            }
        }
    }

    companion object {
        const val SENSOR_ID = "SENSOR_ID"
    }
}