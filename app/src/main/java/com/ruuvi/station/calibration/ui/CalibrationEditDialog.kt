package com.ruuvi.station.calibration.ui

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import com.ruuvi.station.R
import com.ruuvi.station.databinding.DialogCalibrationEditBinding
import java.lang.Exception
import java.lang.IllegalStateException

class CalibrationEditDialog(): DialogFragment() {
    private lateinit var binding: DialogCalibrationEditBinding
    private lateinit var alertDialog: AlertDialog
    private var unit: String = ""
    private var listener: CalibrationEditListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            binding = DialogCalibrationEditBinding.inflate(it.layoutInflater)
            val builder = AlertDialog.Builder(it, R.style.CustomAlertDialog)
            builder
                .setView(binding.root)
                .setTitle("Calibration setup")
                .setPositiveButton(R.string.ok) {_,_->
                    listener?.onDialogPositiveClick(this, binding.calibrationValueEditText.text.toString().toDouble())
                }
                .setNegativeButton(R.string.cancel) {_,_->
                    listener?.onDialogNegativeClick(this)
                }
            alertDialog = builder.create()
            alertDialog
        } ?: throw  IllegalStateException("Activity not found")
    }

    private fun setupUI() {
        val confirmButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        confirmButton.isEnabled = false

        binding.dialogTitle.text = "Enter desired value ($unit): "

        binding.calibrationValueEditText.addTextChangedListener {
            try {
                it?.let {
                    val value = it.toString().toDouble()
                    confirmButton.isEnabled = true
                }
            } catch (e: Exception) {
                confirmButton.isEnabled = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupUI()
    }

    companion object {
        fun newInstance(
            unit: String,
            listener: CalibrationEditListener
        ): CalibrationEditDialog {
            val dialog =  CalibrationEditDialog()
            dialog.unit = unit
            dialog.listener = listener
            return dialog
        }
    }
}

interface CalibrationEditListener{
    fun onDialogPositiveClick(dialog: DialogFragment, value: Double)
    fun onDialogNegativeClick(dialog: DialogFragment)
}