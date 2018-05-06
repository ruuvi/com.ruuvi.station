package com.ruuvi.station.feature

import android.os.Bundle
import android.support.v4.app.Fragment
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.support.v7.app.AlertDialog
import android.support.v7.widget.AppCompatTextView
import android.support.v7.widget.SwitchCompat
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.FrameLayout

import com.ruuvi.station.R
import com.ruuvi.station.feature.main.MainActivity
import com.ruuvi.station.util.PreferenceKeys
import kotlinx.android.synthetic.main.fragment_app_settings_list.*

class AppSettingsListFragment : Fragment() {
    lateinit var pref: SharedPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pref = PreferenceManager.getDefaultSharedPreferences(activity)

        (activity as AppSettingsActivity).setScanSwitchLayout(view)
        scan_layout.setOnClickListener {
            (activity as AppSettingsActivity).openFragment(R.string.pref_bgscan)
        }

        (activity as AppSettingsActivity).setBatterySwitchLayout(view)
        battery_layout.setOnClickListener {
            (activity as AppSettingsActivity).openFragment(R.string.pref_bgscan_battery_saving)
        }

        scan_interval.setOnClickListener {
            (activity as AppSettingsActivity).openFragment(R.string.background_scan_interval)
        }

        gateway_url.setOnClickListener {
            (activity as AppSettingsActivity).openFragment(R.string.gateway_url)
        }

        temperature_unit.setOnClickListener {
            (activity as AppSettingsActivity).openFragment(R.string.temperature_unit)
        }

        val switch = view.findViewById<SwitchCompat>(R.id.dashboard_switch)
        switch.isChecked = pref.getBoolean(PreferenceKeys.DASHBOARD_ENABLED_PREF, false)
        switch.setOnCheckedChangeListener({ _, isChecked ->
            pref.edit().putBoolean(PreferenceKeys.DASHBOARD_ENABLED_PREF, isChecked).apply()
        })

        updateSubs()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_app_settings_list, container, false)
    }

    fun updateSubs() {
        setTextFromPref(background_scan_interval_sub,
                "pref_scaninterval",
                "30",
                R.array.pref_scaninterval_values,
                R.array.pref_scaninterval_titles)
        gateway_url_sub.text = pref.getString("pref_backend", "Disabled")
        if (gateway_url_sub.text.isEmpty()) gateway_url_sub.text = "Disabled"
        //device_identifier_sub.text = pref.getString("pref_device_id", "")
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
        val builder = AlertDialog.Builder(context!!)
        builder.setTitle(title)
        val input = EditText(activity)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setText(pref.getString(prefId, ""))
        val container = FrameLayout(activity)
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.leftMargin = resources.getDimensionPixelSize(R.dimen.dialog_margin)
        params.rightMargin = resources.getDimensionPixelSize(R.dimen.dialog_margin)
        input.layoutParams = params
        container.addView(input)
        builder.setView(container)
        builder.setPositiveButton("Ok") { dialog, which ->
            pref.edit().putString(prefId, input.text.toString()).apply()
            MainActivity.setBackgroundScanning(true, activity, pref)
            updateSubs()
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

}
