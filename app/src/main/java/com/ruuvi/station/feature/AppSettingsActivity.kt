package com.ruuvi.station.feature

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import com.ruuvi.station.R
import com.ruuvi.station.feature.main.MainActivity
import android.support.v7.widget.SwitchCompat

import kotlinx.android.synthetic.main.activity_app_settings.*

class AppSettingsActivity : AppCompatActivity() {
    var showingFragmentTitle = -1
    lateinit var pref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_settings)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        pref = PreferenceManager.getDefaultSharedPreferences(this)

        openFragment(-1)
    }

    fun openFragment(res: Int) {
        showingFragmentTitle = res
        val transaction = supportFragmentManager.beginTransaction()
        var fragment: Fragment?
        if (res == -1 || res == R.string.title_activity_app_settings) {
            fragment = AppSettingsListFragment()
            showingFragmentTitle = R.string.title_activity_app_settings
            if (res == R.string.title_activity_app_settings) {
                transaction.setCustomAnimations(R.anim.enter_left, R.anim.exit_right)
            }
        } else {
            transaction.setCustomAnimations(R.anim.enter_right, R.anim.exit_left)
            fragment = AppSettingsDetailFragment.newInstance(res)
        }
        transaction.replace(R.id.settings_frame, fragment)
                .commit()
        title = getString(showingFragmentTitle)
    }

    override fun onBackPressed() {
        if (showingFragmentTitle != R.string.title_activity_app_settings) {
            openFragment(R.string.title_activity_app_settings)
        } else {
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            onBackPressed()
        }
        return true
    }

    fun setScanSwitchLayout(view: View) {
        val switch = view.findViewById<SwitchCompat>(R.id.bg_scan_switch)
        switch.isChecked = pref.getBoolean("pref_bgscan", false)
        switch.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            pref.edit().putBoolean("pref_bgscan", isChecked).apply()
            MainActivity.setBackgroundScanning(true, this, pref)
            //if (isChecked) MainActivity.checkAndAskForBatteryOptimization(this)
        })
    }

    fun setBatterySwitchLayout(view: View) {
        val switch = view.findViewById<SwitchCompat>(R.id.bg_scan_battery_switch)
        switch.isChecked = pref.getBoolean("pref_bgscan_battery_saving", false)
        switch.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            pref.edit().putBoolean("pref_bgscan_battery_saving", isChecked).apply()
            MainActivity.setBackgroundScanning(true, this, pref)
        })
    }

    fun getStringFromPref(prefTag: String, default: String): String {
        return pref.getString(prefTag, default)
    }

    fun getIntFromPref(prefTag: String, default: Int): Int {
        return pref.getInt(prefTag, default)
    }
}
