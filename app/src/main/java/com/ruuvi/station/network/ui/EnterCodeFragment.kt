package com.ruuvi.station.network.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.ruuvi.station.R
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.support.closestKodein
import com.ruuvi.station.util.extensions.viewModel
import kotlinx.android.synthetic.main.fragment_enter_code.*
import kotlinx.android.synthetic.main.fragment_enter_code.errorTextView
import kotlinx.android.synthetic.main.fragment_enter_code.skipTextView

class EnterCodeFragment : Fragment(R.layout.fragment_enter_code), KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: EnterCodeViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupUI()
    }

    private fun setupViewModel() {
        viewModel.errorTextObserve.observe(viewLifecycleOwner, Observer {
            errorTextView.text = it
            submitCodeButton.isEnabled = true
        })

        viewModel.successfullyVerifiedObserve.observe(viewLifecycleOwner, Observer {
            if (it) {
                activity?.onBackPressed()
            }
        })
    }

    private fun setupUI() {
        submitCodeButton.setOnClickListener {
            submitCodeButton.isEnabled = false
            viewModel.verifyCode(enterCodeManuallyEditText.text.toString())
        }

        val token = arguments?.getString("token")
        if (token.isNullOrEmpty() == false) {
            if (viewModel.signedIn) {
                activity?.onBackPressed()
            } else {
                enterCodeManuallyEditText.setText(token)
                submitCodeButton.performClick()
            }
        }

        skipTextView.setOnClickListener {
            requireActivity().finish()
        }
    }

    companion object {
        fun newInstance() = EnterCodeFragment()
    }
}