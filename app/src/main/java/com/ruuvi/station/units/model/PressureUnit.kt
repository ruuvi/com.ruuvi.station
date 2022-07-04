package com.ruuvi.station.units.model

import com.ruuvi.station.R

enum class PressureUnit(val code: Int, val title: Int, val unit: Int) {
    PA(0, R.string.pressure_pa_name, R.string.pressure_pa_unit),
    HPA(1, R.string.pressure_hpa_name, R.string.pressure_hpa_unit),
    MMHG(2, R.string.pressure_mmhg_name, R.string.pressure_mmhg_unit),
    INHG(3, R.string.pressure_inhg_name, R.string.pressure_inhg_unit);

    companion object {
        fun getByCode(code: Int) = values().firstOrNull{it.code == code}
    }
}