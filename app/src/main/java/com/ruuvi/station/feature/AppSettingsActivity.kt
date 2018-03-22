package com.ruuvi.station.feature

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.widget.CompoundButton
import com.ruuvi.station.R

import kotlinx.android.synthetic.main.activity_app_settings.*
import kotlinx.android.synthetic.main.content_app_settings.*
import android.content.DialogInterface
import android.support.v7.app.AlertDialog
import android.support.v7.widget.AppCompatTextView
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import com.ruuvi.station.feature.main.MainActivity


class AppSettingsActivity : AppCompatActivity() {
    lateinit var pref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_settings)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        pref = PreferenceManager.getDefaultSharedPreferences(this)

        bg_scan_switch.isChecked = pref.getBoolean("pref_bgscan", false)
        bg_scan_switch.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            pref.edit().putBoolean("pref_bgscan", isChecked).apply()
            MainActivity.setBackgroundScanning(true, this, pref)
            MainActivity.checkAndAskForBatteryOptimization(this)
        })
        bg_scan_battery_switch.isChecked = pref.getBoolean("pref_bgscan_battery_saving", false)
        bg_scan_battery_switch.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
            pref.edit().putBoolean("pref_bgscan_battery_saving", isChecked).apply()
            MainActivity.setBackgroundScanning(true, this, pref)
        })

        scan_interval.setOnClickListener {
            val options = resources.getStringArray(R.array.pref_scaninterval_titles)
            val values = resources.getStringArray(R.array.pref_scaninterval_values)
            val builder = AlertDialog.Builder(this)
            builder.setTitle(resources.getString(R.string.background_scan_interval))
            builder.setItems(options) { dialog, which ->
                pref.edit().putString("pref_scaninterval", values[which]).apply()
                MainActivity.setBackgroundScanning(true, this, pref)
                updateSubs()
            }
            builder.show()
        }

        gateway_url.setOnClickListener {
            input("pref_backend", getString(R.string.gateway_url))
        }

        device_identifier.setOnClickListener {
            input("pref_device_id", getString(R.string.device_identifier))
        }

        temperature_unit.setOnClickListener {
            val options = resources.getStringArray(R.array.list_preference_temperature_unit_titles)
            val values = resources.getStringArray(R.array.list_preference_temperature_unit_values)
            val builder = AlertDialog.Builder(this)
            builder.setTitle(resources.getString(R.string.temperature_unit))
            builder.setItems(options) { dialog, which ->
                pref.edit().putString("pref_temperature_unit", values[which]).apply()
                MainActivity.setBackgroundScanning(true, this, pref)
                updateSubs()
            }
            builder.show()
        }
        updateSubs()
    }

    fun updateSubs() {
        setTextFromPref(background_scan_interval_sub,
                "pref_scaninterval",
                "300",
                R.array.pref_scaninterval_values,
                R.array.pref_scaninterval_titles)
        gateway_url_sub.text = pref.getString("pref_backend", "Disabled")
        if (gateway_url_sub.text.isEmpty()) gateway_url_sub.text = "Disabled"
        device_identifier_sub.text = pref.getString("pref_device_id", "")
        if (pref.getString("pref_temperature_unit", "C") == "C") {
            temperature_unit_sub.text = getString(R.string.celsius)
        } else {
            temperature_unit_sub.text = getString(R.string.fahrenheit )
        }
    }

    fun setTextFromPref(textView: AppCompatTextView, prefTag: String, default: String, resValues: Int, resTitles: Int) {
        textView.text =
                resources.getStringArray(resTitles)[
                        resources.getStringArray(resValues).indexOf(
                                pref.getString(prefTag, default)
                        )]
    }

    fun input(prefId: String, title: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setText(pref.getString(prefId, ""))
        val container = FrameLayout(applicationContext)
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.leftMargin = resources.getDimensionPixelSize(R.dimen.dialog_margin)
        params.rightMargin = resources.getDimensionPixelSize(R.dimen.dialog_margin)
        input.layoutParams = params
        container.addView(input)
        builder.setView(container)
        builder.setPositiveButton("Ok") { dialog, which ->
            pref.edit().putString(prefId, input.text.toString()).apply()
            MainActivity.setBackgroundScanning(true, this, pref)
            updateSubs()
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            finish()
        }
        return true
    }
}
