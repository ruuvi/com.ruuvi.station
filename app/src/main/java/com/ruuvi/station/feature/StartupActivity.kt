package com.ruuvi.station.feature

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.ruuvi.station.R
import com.ruuvi.station.feature.main.MainActivity
import com.ruuvi.station.util.DeviceIdentifier
import com.ruuvi.station.util.Preferences


class StartupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_startup)

        DeviceIdentifier.id(applicationContext)

        val prefs = Preferences(this)

        if (prefs.isFirstStart) {
            val intent = Intent(this, WelcomeActivity::class.java)
            startActivity(intent)
        }
        else if (prefs.dashboardEnabled) {
            val intent = Intent(applicationContext, DashboardActivity::class.java)
            startActivity(intent)
        } else {
            val intent = Intent(applicationContext, TagDetails::class.java)
            intent.putExtra(TagDetails.FROM_WELCOME,
                    getIntent().getBooleanExtra(TagDetails.FROM_WELCOME, false)
            )
            startActivity(intent)
        }

        Thread(Runnable {
            MainActivity.setBackgroundScanning(this)
        }).start()
    }
}
