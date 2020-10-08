package com.ruuvi.station.network.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.ruuvi.station.util.extensions.viewModel
import com.google.android.material.snackbar.Snackbar
import com.ruuvi.station.R
import kotlinx.android.synthetic.main.fragment_signed_in.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.support.closestKodein

class SignedInFragment : Fragment(R.layout.fragment_signed_in) , KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: SignedInViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupUI()
    }

    private fun setupUI() {
        signOutButton.setOnClickListener {
            findNavController().navigate(SignedInFragmentDirections.actionSignedInFragmentToSignOutFragment())
        }

        okButton.setOnClickListener {
            activity?.finish()
        }

        addTagsButton.setOnClickListener {
            viewModel.addMissingTags()
        }
    }

    private fun setupViewModel() {
        viewModel.emailObserve.observe(viewLifecycleOwner, Observer {
            emailTextView.text = it
        })

        viewModel.tagsObserve.observe(viewLifecycleOwner, Observer {
            sensorsTextView.text = it
        })

        viewModel.operationStatusObserve.observe(this, Observer {
            if (!it.isNullOrEmpty()) {
                Snackbar.make(rootLayout, it, Snackbar.LENGTH_SHORT).show()
                viewModel.statusProcessed()
            }
        })
    }

    companion object {
        fun newInstance() = SignedInFragment()
    }
}