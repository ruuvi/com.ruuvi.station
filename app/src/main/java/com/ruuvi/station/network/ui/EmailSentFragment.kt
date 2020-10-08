package com.ruuvi.station.network.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.flexsentlabs.extensions.viewModel
import com.ruuvi.station.R
import kotlinx.android.synthetic.main.fragment_email_sent.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.support.closestKodein

class EmailSentFragment : Fragment(R.layout.fragment_email_sent), KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: EmailSentViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        okButton.setOnClickListener {
//            val action = EmailSentFragmentDirections.actionEmailSentFragmentToEnterCodeFragment()
//            findNavController().navigate(action)
        }
    }

    companion object {
        fun newInstance() = EmailSentFragment()
    }
}