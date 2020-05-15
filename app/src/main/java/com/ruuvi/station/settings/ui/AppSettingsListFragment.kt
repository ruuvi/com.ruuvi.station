package com.ruuvi.station.settings.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.SwitchCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ruuvi.station.BuildConfig
import com.ruuvi.station.R
import com.ruuvi.station.model.HumidityUnit
import com.ruuvi.station.app.preferences.Preferences
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.util.test.StressTestGenerator
import kotlinx.android.synthetic.main.fragment_app_settings_list.*

class AppSettingsListFragment : Fragment() {
    lateinit var prefs: Preferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = Preferences(context!!)

        scan_layout.setOnClickListener {
            (activity as AppSettingsActivity).openFragment(R.string.pref_bgscan)
        }

        scan_interval.setOnClickListener {
            if (prefs.backgroundScanMode != BackgroundScanModes.DISABLED) {
                (activity as AppSettingsActivity).openFragment(R.string.background_scan_interval)
            }
        }

        gateway_url.setOnClickListener {
            (activity as AppSettingsActivity).openFragment(R.string.gateway_url)
        }

        temperature_unit.setOnClickListener {
            (activity as AppSettingsActivity).openFragment(R.string.temperature_unit)
        }

        humidity_unit.setOnClickListener {
            (activity as AppSettingsActivity).openFragment(R.string.humidity_unit)
        }

        debug_tools.visibility = if (BuildConfig.DEBUG) View.VISIBLE else View.GONE
        debug_tools.setOnClickListener {
            StressTestGenerator.generateData(8, 15000)
        }

        val switch = view.findViewById<SwitchCompat>(R.id.dashboard_switch)
        switch.isChecked = prefs.dashboardEnabled
        switch.setOnCheckedChangeListener{ _, isChecked ->
            prefs.dashboardEnabled = isChecked
        }

        updateView()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_app_settings_list, container, false)
    }

    private fun updateView() {
        var intervalText = ""
        if (prefs.backgroundScanMode != BackgroundScanModes.DISABLED) {
            val bgScanInterval = prefs.backgroundScanInterval
            val min = bgScanInterval / 60
            val sec = bgScanInterval - min * 60
            if (min > 0) intervalText += min.toString() + " " + getString(R.string.minutes) + ", "
            intervalText += sec.toString() + " " + getString(R.string.seconds)
        }
        background_scan_interval_sub.text = intervalText
        if (prefs.backgroundScanMode == BackgroundScanModes.BACKGROUND) {
            bg_scan_description.text = getString(R.string.continuous_background_scanning_enabled, intervalText)
        } else {
            bg_scan_description.text = getString(R.string.no_background_scanning_enabled)
        }

        gateway_url_sub.text = prefs.gatewayUrl
        if (gateway_url_sub.text.isEmpty()) gateway_url_sub.text = "Disabled"
        if (prefs.temperatureUnit == "K") {
            temperature_unit_sub.text = getString(R.string.kelvin)
        } else if (prefs.temperatureUnit == "F") {
            temperature_unit_sub.text = getString(R.string.fahrenheit )
        } else {
            temperature_unit_sub.text = getString(R.string.celsius)
        }
        when (prefs.humidityUnit) {
            HumidityUnit.PERCENT -> humidity_unit_sub.text = getString(R.string.relative_humidity_unit)
            HumidityUnit.GM3 -> humidity_unit_sub.text = getString(R.string.absolute_humidity_unit)
            HumidityUnit.DEW -> humidity_unit_sub.text = getString(R.string.dew_point_humidity_unit)
        }
    }
}
