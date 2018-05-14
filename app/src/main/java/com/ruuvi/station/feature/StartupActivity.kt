package com.ruuvi.station.feature

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import com.ruuvi.station.R
import com.ruuvi.station.util.DeviceIdentifier
import com.ruuvi.station.util.PreferenceKeys
import com.ruuvi.station.util.PreferenceKeys.DASHBOARD_ENABLED_PREF


class StartupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_startup)

        DeviceIdentifier.id(applicationContext)

        if (!getBoolPref(PreferenceKeys.FIRST_START_PREF)) {
            val intent = Intent(this, WelcomeActivity::class.java)
            startActivity(intent)
            return
        }
        if (getBoolPref(DASHBOARD_ENABLED_PREF)) {
            val intent = Intent(applicationContext, DashboardActivity::class.java)
            startActivity(intent)
            return
        }
        val intent = Intent(applicationContext, TagDetails::class.java)
        startActivity(intent)
    }

    fun getBoolPref(pref: String): Boolean {
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        return settings.getBoolean(pref, false)
    }
}
