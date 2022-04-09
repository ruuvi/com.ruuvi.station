package com.ruuvi.station.network.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.ruuvi.station.R
import com.ruuvi.station.databinding.FragmentEnterCodeBinding
import com.ruuvi.station.startup.ui.StartupActivity
import com.ruuvi.station.util.extensions.hideKeyboard
import com.ruuvi.station.util.extensions.showKeyboard
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import com.ruuvi.station.util.extensions.viewModel
import org.kodein.di.android.x.closestKodein

class EnterCodeFragment : Fragment(R.layout.fragment_enter_code), KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: EnterCodeViewModel by viewModel()

    private lateinit var binding: FragmentEnterCodeBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentEnterCodeBinding.bind(view)
        setupViewModel()
        setupUI()
    }

    private fun setupViewModel() {
        viewModel.errorTextObserve.observe(viewLifecycleOwner) {
            binding.errorTextView.text = it
            if (it.isNotEmpty()) {
                binding.codeEdit.clear()
            }
        }

        viewModel.successfullyVerifiedObserve.observe(viewLifecycleOwner) {
            if (it) {
                StartupActivity.start(requireContext(), false)
                requireActivity().finish()
            }
        }

        viewModel.requestInProcessObserve.observe(viewLifecycleOwner) { inProcess ->
            binding.codeEdit.isEnabled = !inProcess
            binding.progressIndicator.isVisible = inProcess
            binding.syncingTextView.isVisible = inProcess
        }
    }

    private fun setupUI() {
        binding.skipTextView.setOnClickListener {
            requireActivity().finish()
        }

        binding.codeEdit.onCodeEntered = { code ->
            viewModel.verifyCode(code.trim())
            requireActivity().hideKeyboard()
        }

        val token = arguments?.getString("token")
        if (token.isNullOrEmpty() == false) {
            if (viewModel.isSignedIn()) {
                val messageDialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog).create()
                messageDialog.setMessage(getString(R.string.already_logged_in_message, viewModel.getUserEmail()))
                messageDialog.setButton(
                    AlertDialog.BUTTON_NEUTRAL, getString(R.string.ok)
                ) { dialog, _ -> dialog.dismiss() }
                messageDialog.setOnDismissListener {
                    activity?.onBackPressed()
                }
                messageDialog.show()
            } else {
                binding.codeEdit.handlePaste(token)
            }
        } else {
            binding.codeEdit.binding.code1EditText.requestFocus()
            requireActivity().showKeyboard(binding.codeEdit.binding.code1EditText)
        }
    }

    companion object {
        fun newInstance() = EnterCodeFragment()
    }
}