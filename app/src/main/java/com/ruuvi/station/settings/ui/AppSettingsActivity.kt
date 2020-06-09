package com.ruuvi.station.settings.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import com.ruuvi.station.R
import kotlinx.android.synthetic.main.activity_app_settings.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein

class AppSettingsActivity : AppCompatActivity(), KodeinAware {
    override val kodein by closestKodein()
    var showingFragmentTitle = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_settings)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        openFragment(-1)
    }

    fun openFragment(res: Int) {
        showingFragmentTitle = res
        val transaction = supportFragmentManager.beginTransaction()
        val fragment: Fragment?
        if (res == -1 || res == R.string.title_activity_app_settings) {
            fragment = AppSettingsListFragment()
            showingFragmentTitle = R.string.title_activity_app_settings
            if (res == R.string.title_activity_app_settings) {
                transaction.setCustomAnimations(R.anim.enter_left, R.anim.exit_right)
            }
        } else {
            transaction.setCustomAnimations(R.anim.enter_right, R.anim.exit_left)
            fragment = when (res) {
                R.string.pref_bgscan -> AppSettingsBackgroundScanFragment.newInstance()
                R.string.preferences_graph_settings -> AppSettingsGraphFragment.newInstance()
                else -> AppSettingsDetailFragment.newInstance(res)
            }
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
}
