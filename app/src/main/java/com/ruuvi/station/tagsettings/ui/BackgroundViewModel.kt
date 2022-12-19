package com.ruuvi.station.tagsettings.ui

import androidx.lifecycle.ViewModel
import com.ruuvi.station.R

class BackgroundViewModel(
    val sensorId: String
): ViewModel() {

    fun getDefaultImages(): List<Int> = listOf(
        R.drawable.bg2,
        R.drawable.bg3,
        R.drawable.bg4,
        R.drawable.bg5,
        R.drawable.bg6,
        R.drawable.bg7,
        R.drawable.bg8,
        R.drawable.bg9
    )
}