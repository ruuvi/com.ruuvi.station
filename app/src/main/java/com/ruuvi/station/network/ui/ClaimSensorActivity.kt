package com.ruuvi.station.network.ui

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.MyTopAppBar
import com.ruuvi.station.app.ui.components.Paragraph
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.util.extensions.viewModel
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein

class ClaimSensorActivity : AppCompatActivity(R.layout.activity_claim_sensor), KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: ClaimSensorViewModel by viewModel {
        intent.getStringExtra(SENSOR_ID)?.let {
            it
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            RuuviTheme {
                val scaffoldState = rememberScaffoldState()
                val systemUiController = rememberSystemUiController()
                val systemBarsColor = RuuviStationTheme.colors.systemBars

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    backgroundColor = RuuviStationTheme.colors.background,
                    topBar = { MyTopAppBar(title = "title") },
                    scaffoldState = scaffoldState
                ) { padding ->

                    Paragraph(text = "test")
                }

                SideEffect {
                    systemUiController.setSystemBarsColor(
                        color = systemBarsColor
                    )
                }
            }
        }
    }


    @Composable
    fun ClaimSensor() {
        
    }

    @Composable
    fun ForceClaim() {
        
    }
    
    private fun setupViewModel() {
        viewModel.claimResultObserve.observe(this) { claimResult ->
            if (claimResult != null) {
                if (claimResult.first) {
                    finish()
                } else {
                    //Snackbar.make(binding.root, claimResult.second, Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.claimInProgress.observe(this) {
            //binding.claimButton.isEnabled = !it
        }
    }

    private fun setupUI() {
//        binding.claimButton.setDebouncedOnClickListener {
//            viewModel.claimSensor()
//        }
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