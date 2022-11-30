package com.ruuvi.station.network.ui

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.MyTopAppBar
import com.ruuvi.station.app.ui.components.*
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
                val titleString = stringResource(id = R.string.claim_sensor)
                val scaffoldState = rememberScaffoldState()
                val systemUiController = rememberSystemUiController()
                val systemBarsColor = RuuviStationTheme.colors.systemBars
                var title by remember { mutableStateOf(titleString) }

                val claimState by viewModel.claimState.collectAsState()
                
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    backgroundColor = RuuviStationTheme.colors.background,
                    topBar = { MyTopAppBar(title = title) },
                    scaffoldState = scaffoldState
                ) { padding ->
                    
                    when (claimState) {
                        is ClaimSensorState.InProgress -> {
                            val state = claimState as ClaimSensorState.InProgress
                            title = stringResource(id = state.title)
                            LoadingScreen(status = stringResource(id = (claimState as ClaimSensorState.InProgress).status))
                        }
                        is ClaimSensorState.FreeToClaim -> {
                            title = stringResource(id = R.string.claim_sensor)
                            ClaimSensor()
                        }
                        is ClaimSensorState.ForceClaimInit -> {
                            title = stringResource(id = R.string.force_claim_sensor)
                            ForceClaimInit()
                        }
                        is ClaimSensorState.ForceClaimGettingId -> {
                            title = stringResource(id = R.string.force_claim_sensor)
                            ForceClaimGettingId()
                        }
                        is ClaimSensorState.ClaimFinished -> finish()
                        is ClaimSensorState.ErrorWhileChecking -> {
                            Paragraph(text = "error ${(claimState as ClaimSensorState.ErrorWhileChecking).error}")
                        }
                    }
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
        PageSurfaceWithPadding() {
            Column() {
                Paragraph(text = stringResource(id = R.string.claim_description))
                Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    RuuviButton(text = stringResource(id = R.string.claim_ownership)) {
                        viewModel.claimSensor()
                    }
                }
            }
        }
    }

    @Composable
    fun ForceClaimInit() {
        PageSurfaceWithPadding() {
            Column() {
                Paragraph(text = stringResource(id = R.string.force_claim_sensor_description1))
                Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    RuuviButton(text = stringResource(id = R.string.force_claim)) {
                        viewModel.getSensorId()
                    }
                }
            }
        }
    }

    @Composable
    fun ForceClaimGettingId() {
        PageSurfaceWithPadding() {
            Column() {
                Paragraph(text = stringResource(id = R.string.force_claim_sensor_description2))
            }
        }

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