package com.ruuvi.station.model

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

class HumidityCalibration {
    companion object {
        var cache = HashMap<String, Float>()
        fun calibrate(context: Context, tag: RuuviTag): RuuviTag {
            val pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            val prevCalibration = this.get(context, tag)
            val calibration = tag.humidity.toFloat()+prevCalibration-75f
            pref.edit().putFloat("humidity_calibration:"+tag.id, calibration).apply()
            cache.put(tag.id, calibration)
            tag.humidity -= calibration
            return tag
        }
        fun clear(context: Context, tag: RuuviTag) {
            val pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            pref.edit().putFloat("humidity_calibration:"+tag.id, 0f).apply()
            cache.remove(tag.id)
        }
        fun apply(context: Context, tag: RuuviTag): RuuviTag {
            val calibration = this.get(context, tag)
            tag.humidity -= calibration
            return tag
        }
        fun get(context: Context, tag: RuuviTag): Float {
            if (cache.contains(tag.id)) {
                return cache.get(tag.id)!!
            }
            val pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            return pref.getFloat("humidity_calibration:"+tag.id, 0f)
        }
    }
}