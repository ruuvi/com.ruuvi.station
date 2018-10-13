package com.ruuvi.station.feature

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.SwitchCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.ruuvi.station.R
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.util.Preferences
import kotlinx.android.synthetic.main.fragment_app_settings_list.*

class AppSettingsListFragment : Fragment() {
    lateinit var prefs: Preferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = Preferences(context!!)

        //(activity as AppSettingsActivity).setScanSwitchLayout(view)
        scan_layout.setOnClickListener {
            (activity as AppSettingsActivity).openFragment(R.string.pref_bgscan)
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
        switch.isChecked = prefs.dashboardEnabled
        switch.setOnCheckedChangeListener{ _, isChecked ->
            prefs.dashboardEnabled = isChecked
        }

        updateSubs()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_app_settings_list, container, false)
    }

    fun updateSubs() {
        val bgScanInterval = prefs.backgroundScanInterval
        var min = bgScanInterval / 60
        var sec = bgScanInterval - min * 60
        if (prefs.backgroundScanMode == BackgroundScanModes.BACKGROUND) {
            if (min <= 15) {
                min = 15
                sec = 0
            }
        }
        var intervalText = ""
        if (min > 0) intervalText += min.toString() + " " + getString(R.string.minutes) + ", "
        intervalText += sec.toString() + " " + getString(R.string.seconds)
        background_scan_interval_sub.text = intervalText
        gateway_url_sub.text = prefs.gatewayUrl
        if (gateway_url_sub.text.isEmpty()) gateway_url_sub.text = "Disabled"
        //device_identifier_sub.text = pref.getString("pref_device_id", "")
        if (prefs.temperatureUnit == "C") {
            temperature_unit_sub.text = getString(R.string.celsius)
        } else {
            temperature_unit_sub.text = getString(R.string.fahrenheit )
        }
    }
}
