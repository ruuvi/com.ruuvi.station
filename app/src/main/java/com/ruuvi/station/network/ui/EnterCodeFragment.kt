package com.ruuvi.station.network.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.ruuvi.station.R
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.support.closestKodein
import com.ruuvi.station.util.extensions.viewModel
import kotlinx.android.synthetic.main.fragment_enter_code.*

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
        })

        viewModel.successfullyVerifiedObserve.observe(viewLifecycleOwner, Observer {
            if (it) {
                findNavController().navigate(EnterCodeFragmentDirections.actionEnterCodeFragmentToSignedInFragment())
            }
        })
    }

    private fun setupUI() {
        submitCodeButton.setOnClickListener {
            viewModel.verifyCode(enterCodeManuallyEditText.text.toString())
        }
    }

    companion object {
        fun newInstance() = EnterCodeFragment()
    }
}