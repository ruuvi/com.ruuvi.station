package com.ruuvi.station.units.model

import com.ruuvi.station.R

enum class Accuracy (val code: Int, val nameTemplateId: Int, val value: Float) {
    Accuracy0 (0, R.string.accuracy0_template, 1f),
    Accuracy1 (1, R.string.accuracy1_template, 0.1f),
    Accuracy2 (2, R.string.accuracy2_template, 0.01f)
}