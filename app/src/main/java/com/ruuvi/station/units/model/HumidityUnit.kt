package com.ruuvi.station.units.model

import com.ruuvi.station.R

enum class HumidityUnit(val code: Int, val title: Int, val unit: Int) {
    PERCENT(0, R.string.humidity_relative_name, R.string.humidity_relative_unit),
    GM3(1, R.string.humidity_absolute_name, R.string.humidity_absolute_unit),
    DEW(2, R.string.humidity_dew_point_name, R.string.humidity_dew_point_unit);

    companion object {
        fun getByCode(code: Int) = values().firstOrNull{it.code == code}
    }
}