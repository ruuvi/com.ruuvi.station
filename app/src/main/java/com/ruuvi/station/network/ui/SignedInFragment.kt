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
import java.lang.StringBuilder

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
            activity?.onBackPressed()
        }

        addTagsButton.setOnClickListener {
            viewModel.addMissingTags()
        }
    }

    private fun setupViewModel() {
        viewModel.emailObserve.observe(viewLifecycleOwner, Observer {
            emailTextView.text = it
        })

        viewModel.tagsObserve.observe(viewLifecycleOwner, Observer { sensors ->
//            val owned = sensors.filter { it.owner }
//            val shared = sensors.filter { !it.owner }
//
//            val sb = StringBuilder()
//            owned.forEach { sb.appendln("${it.sensor} (${it.name})") }
//            if (shared.isNotEmpty()) {
//                sb.appendln("Tags shared with you: ")
//                shared.forEach { sb.appendln("${it.sensor} (${it.name})") }
//            }
//            sensorsTextView.text = sb.toString()
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