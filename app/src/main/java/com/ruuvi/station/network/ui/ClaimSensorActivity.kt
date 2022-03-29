package com.ruuvi.station.network.ui

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import com.google.android.material.snackbar.Snackbar
import com.ruuvi.station.R
import com.ruuvi.station.databinding.ActivityClaimSensorBinding
import com.ruuvi.station.util.extensions.setDebouncedOnClickListener
import com.ruuvi.station.util.extensions.viewModel
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein

class ClaimSensorActivity : AppCompatActivity(R.layout.activity_claim_sensor), KodeinAware {

    override val kodein: Kodein by closestKodein()

    lateinit var binding: ActivityClaimSensorBinding

    private val viewModel: ClaimSensorViewModel by viewModel {
        intent.getStringExtra(SENSOR_ID)?.let {
            it
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityClaimSensorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        setupUI()
        setupViewModel()
    }

    private fun setupViewModel() {
        viewModel.claimResultObserve.observe(this) { claimResult ->
            if (claimResult != null) {
                if (claimResult.first) {
                    finish()
                } else {
                    Snackbar.make(binding.root, claimResult.second, Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.claimInProgress.observe(this) {
            binding.claimButton.isEnabled = !it
        }
    }

    private fun setupUI() {
        binding.claimButton.setDebouncedOnClickListener {
            viewModel.claimSensor()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val SENSOR_ID = "SENSOR_ID"

        fun start(context: Context, sensorId: String?) {
            val intent = Intent(context, ClaimSensorActivity::class.java)
            intent.putExtra(SENSOR_ID, sensorId)
            context.startActivity(intent)
        }
    }
}