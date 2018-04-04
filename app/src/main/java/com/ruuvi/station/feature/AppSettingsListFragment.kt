package com.ruuvi.station.feature

import android.os.Bundle
import android.app.Fragment
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.support.v7.app.AlertDialog
import android.support.v7.widget.AppCompatTextView
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.FrameLayout

import com.ruuvi.station.R
import com.ruuvi.station.feature.main.MainActivity
import kotlinx.android.synthetic.main.fragment_app_settings_list.*

class AppSettingsListFragment : Fragment() {
    lateinit var pref: SharedPreferences

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (view == null)  return

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
            /*
            val options = resources.getStringArray(R.array.pref_scaninterval_titles)
            val values = resources.getStringArray(R.array.pref_scaninterval_values)
            val builder = AlertDialog.Builder(activity)
            builder.setTitle(resources.getString(R.string.background_scan_interval))
            builder.setItems(options) { dialog, which ->
                pref.edit().putString("pref_scaninterval", values[which]).apply()
                MainActivity.setBackgroundScanning(true, activity, pref)
                updateSubs()
            }
            builder.show()
            */
        }

        gateway_url.setOnClickListener {
            //input("pref_backend", getString(R.string.gateway_url))
            (activity as AppSettingsActivity).openFragment(R.string.gateway_url)
        }

        device_identifier.setOnClickListener {
            //input("pref_device_id", getString(R.string.device_identifier))
            (activity as AppSettingsActivity).openFragment(R.string.device_identifier)
        }

        temperature_unit.setOnClickListener {
            (activity as AppSettingsActivity).openFragment(R.string.temperature_unit)
            /*
            val options = resources.getStringArray(R.array.list_preference_temperature_unit_titles)
            val values = resources.getStringArray(R.array.list_preference_temperature_unit_values)
            val builder = AlertDialog.Builder(activity)
            builder.setTitle(resources.getString(R.string.temperature_unit))
            builder.setItems(options) { dialog, which ->
                pref.edit().putString("pref_temperature_unit", values[which]).apply()
                MainActivity.setBackgroundScanning(true, activity, pref)
                updateSubs()
            }
            builder.show()
            */
        }

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
        val builder = AlertDialog.Builder(activity)
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
