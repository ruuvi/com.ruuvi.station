package com.ruuvi.station.alarm.ui

import android.app.Dialog
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.ruuvi.station.R
import com.ruuvi.station.databinding.DialogCustomDescriptionEditBinding
import timber.log.Timber
import java.lang.IllegalStateException

class CustomDescriptionEditDialog (
    private val currentValue: String?,
    private var listener: CustomDescriptionEditListener? = null
): DialogFragment() {
    private lateinit var binding: DialogCustomDescriptionEditBinding
    private lateinit var alertDialog: AlertDialog

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            binding = DialogCustomDescriptionEditBinding.inflate(it.layoutInflater)
            binding.customDescriptionEditText.setText(currentValue)
            val builder = AlertDialog.Builder(it, R.style.CustomAlertDialog)
            builder
                .setView(binding.root)
                .setPositiveButton(R.string.ok) { _, _->
                    var value: String? = binding.customDescriptionEditText.text.toString()
                    if (value.isNullOrEmpty()) value = null
                    listener?.onDialogPositiveClick(this, value)
                }
                .setNegativeButton(R.string.cancel) { _, _->
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
        binding.customDescriptionEditText.requestFocus()
    }

    companion object {
        fun new_instance(currentValue: String?, listener: CustomDescriptionEditListener): CustomDescriptionEditDialog {
            return  CustomDescriptionEditDialog(currentValue, listener)
        }
    }

}

interface CustomDescriptionEditListener {
    fun onDialogPositiveClick(dialog: DialogFragment, value: String?)
    fun onDialogNegativeClick(dialog: DialogFragment)
}