package com.ruuvi.station.network.ui

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.ruuvi.station.R
import com.ruuvi.station.databinding.FragmentEnterCodeBinding
import com.ruuvi.station.startup.ui.StartupActivity
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
        viewModel.errorTextObserve.observe(viewLifecycleOwner, Observer {
            binding.errorTextView.text = it
        })

        viewModel.successfullyVerifiedObserve.observe(viewLifecycleOwner, Observer {
            if (it) {
                StartupActivity.start(requireContext(), false)
            }
        })

        viewModel.requestInProcessObserve.observe(viewLifecycleOwner, Observer { inProcess ->
            binding.submitCodeButton.isEnabled = !inProcess
            binding.progressIndicator.isVisible = inProcess
            binding.syncingTextView.isVisible = inProcess
        })
    }

    private fun setupUI() {
        binding.submitCodeButton.setOnClickListener {
            val code = binding.enterCodeManuallyEditText.text.toString()
            viewModel.verifyCode(code.trim())
        }

        val token = arguments?.getString("token")
        if (token.isNullOrEmpty() == false) {
            if (viewModel.signedIn) {
                activity?.onBackPressed()
            } else {
                binding.enterCodeManuallyEditText.setText(token)
                binding.submitCodeButton.performClick()
            }
        }

        binding.skipTextView.setOnClickListener {
            requireActivity().finish()
        }
    }

    companion object {
        fun newInstance() = EnterCodeFragment()
    }
}