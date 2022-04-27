package com.ruuvi.station.network.ui

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ruuvi.station.util.extensions.viewModel
import com.ruuvi.station.R
import com.ruuvi.station.databinding.FragmentEmailEnterBinding
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein

class EmailEnterFragment: Fragment(R.layout.fragment_email_enter), KodeinAware {
    override val kodein: Kodein by closestKodein()

    private val viewModel: EmailEnterViewModel by viewModel()

    private lateinit var binding: FragmentEmailEnterBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentEmailEnterBinding.bind(view)

        setupViewModel()
        setupUI()
    }

    private fun setupUI() {
        binding.submitButton.setOnClickListener {
            viewModel.submitEmail(binding.emailEditText.text.toString())
        }

        binding.skipTextView.setOnClickListener {
            requireActivity().finish()
        }
    }

    private fun setupViewModel() {
        viewModel.errorTextObserve.observe(viewLifecycleOwner) {
            binding.errorTextView.text = it
        }

        viewModel.successfullyRegisteredObserve.observe(viewLifecycleOwner) {
            if (it == true) {
                viewModel.successfullyRegisteredFinished()
                val action = EmailEnterFragmentDirections.actionEmailEnterFragmentToEnterCodeFragment(null)
                this.findNavController().navigate(action)
            }
        }

        viewModel.requestInProcessObserve.observe(viewLifecycleOwner) {inProcess ->
            binding.submitButton.isEnabled = !inProcess
            binding.progressIndicator.isVisible = inProcess
        }
    }

    companion object {
        fun newInstance() = EmailEnterFragment()
    }
}