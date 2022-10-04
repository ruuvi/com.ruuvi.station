package com.ruuvi.station.util.extensions

import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams

fun View.setMarginTop(value: Int) = updateLayoutParams<ViewGroup.MarginLayoutParams> {
    topMargin = value
}

fun View.setMarginBottom(value: Int) = updateLayoutParams<ViewGroup.MarginLayoutParams> {
    bottomMargin = value
}