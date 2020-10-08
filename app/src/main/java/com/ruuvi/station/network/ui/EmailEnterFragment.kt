package com.ruuvi.station.network.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.ruuvi.station.util.extensions.viewModel
import com.ruuvi.station.R
import kotlinx.android.synthetic.main.fragment_email_enter.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.support.closestKodein

class EmailEnterFragment() : Fragment(R.layout.fragment_email_enter), KodeinAware {
    override val kodein: Kodein by closestKodein()

    private val viewModel: EmailEnterViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupUI()
    }

    private fun setupUI() {
        submitButton.setOnClickListener {
            viewModel.submitEmail(emailEditText.text.toString())
        }

        manualCodeButton.setOnClickListener {
            val action = EmailEnterFragmentDirections.actionEmailEnterFragmentToEnterCodeFragment()
            this.findNavController().navigate(action)
        }
    }

    private fun setupViewModel() {
        viewModel.errorTextObserve.observe(viewLifecycleOwner, Observer {
            errorTextView.text = it
        })

        viewModel.successfullyRegisteredObserve.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                val action = EmailEnterFragmentDirections.actionEmailEnterFragmentToEnterCodeFragment()
                this.findNavController().navigate(action)
            }
        })

        viewModel.alreadyLoggedInObserve.observe(viewLifecycleOwner, Observer {
            if (it) findNavController().navigate(R.id.signedInFragment)
        })
    }

    companion object {
        fun newInstance() = EmailEnterFragment()
    }
}