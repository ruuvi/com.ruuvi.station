package com.ruuvi.station.feature

import android.app.Fragment
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.ruuvi.station.R

import kotlinx.android.synthetic.main.activity_app_settings.*

class AppSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_settings)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        openFragment(0)
    }

    fun openFragment(type: Int) {
        var fragment: Fragment? = null
        when (type) {
            0 -> {
                fragment = AppSettingsListFragment()
            }
            1 -> {

            }
        }
        fragmentManager.beginTransaction()
                .replace(R.id.settings_frame, fragment)
                .commit()
        title = resources.getStringArray(R.array.navigation_items_titles)[type]
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            finish()
        }
        return true
    }
}
