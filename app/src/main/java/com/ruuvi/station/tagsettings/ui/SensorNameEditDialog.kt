package com.ruuvi.station.tagsettings.ui

import android.app.Dialog
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.ruuvi.station.R
import com.ruuvi.station.databinding.DialogSensorNameEditBinding
import timber.log.Timber
import java.lang.IllegalStateException

class SensorNameEditDialog(
    private val currentValue: String?,
    private var listener: SensorNameEditListener? = null
): DialogFragment() {
    private lateinit var binding: DialogSensorNameEditBinding
    private lateinit var alertDialog: AlertDialog


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            binding = DialogSensorNameEditBinding.inflate(it.layoutInflater)
            binding.sensorNameEditText.setText(currentValue)
            val builder = AlertDialog.Builder(it, R.style.CustomAlertDialog)
            builder
                .setView(binding.root)
                .setTitle(getString(R.string.tag_name))
                .setMessage(R.string.rename_sensor_message)
                .setPositiveButton(R.string.ok) {_,_->
                    var value: String? = binding.sensorNameEditText.text.toString()
                    if (value.isNullOrEmpty()) value = null
                    listener?.onDialogPositiveClick(this, value)
                }
                .setNegativeButton(R.string.cancel) {_,_->
                    listener?.onDialogNegativeClick(this)
                }
            alertDialog = builder.create()

            try {
                alertDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            } catch (e: Exception) {
                Timber.e(e, "Could not open keyboard")
            }

            alertDialog
        } ?: throw IllegalStateException("Activity not found")
    }

    override fun onResume() {
        super.onResume()
        binding.sensorNameEditText.requestFocus()
    }

    companion object {
        fun new_instance(currentValue: String?, listener: SensorNameEditListener): SensorNameEditDialog {
            return  SensorNameEditDialog(currentValue, listener)
        }
    }
}

interface SensorNameEditListener{
    fun onDialogPositiveClick(dialog: DialogFragment, value: String?)
    fun onDialogNegativeClick(dialog: DialogFragment)
}