package com.ruuvi.station.network.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.flexsentlabs.extensions.viewModel
import com.ruuvi.station.R
import kotlinx.android.synthetic.main.fragment_sign_out.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.support.closestKodein

class SignOutFragment : Fragment(R.layout.fragment_sign_out), KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: SignOutViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        okButton.setOnClickListener {
            viewModel.signOut()
            activity?.finish()
        }
    }

    companion object {
        fun newInstance() = SignOutFragment()
    }
}