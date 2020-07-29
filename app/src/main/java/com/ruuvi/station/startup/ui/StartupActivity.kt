package com.ruuvi.station.startup.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.flexsentlabs.extensions.viewModel
import com.ruuvi.station.R
import com.ruuvi.station.util.DeviceIdentifier
import com.ruuvi.station.dashboard.ui.DashboardActivity
import com.ruuvi.station.feature.WelcomeActivity
import com.ruuvi.station.tagdetails.ui.TagDetailsActivity
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein

class StartupActivity : AppCompatActivity(), KodeinAware {

    override val kodein by closestKodein()

    private val viewModel: StartupActivityViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_startup)
        DeviceIdentifier.id(applicationContext)

        viewModel
            .startForegroundScanning()

        when {
            viewModel.isFirstStart() -> {
                val intent = Intent(this, WelcomeActivity::class.java)
                startActivity(intent)
            }
            viewModel.isDashboardEnabled() -> {
                val intent = Intent(applicationContext, DashboardActivity::class.java)
                startActivity(intent)
            }
            else -> {
                val intent = Intent(applicationContext, TagDetailsActivity::class.java)
                intent.putExtra(TagDetailsActivity.FROM_WELCOME,
                    getIntent().getBooleanExtra(TagDetailsActivity.FROM_WELCOME, false)
                )
                startActivity(intent)
            }
        }
    }
}
