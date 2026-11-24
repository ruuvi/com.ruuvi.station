package com.ruuvi.station.util.ui

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import org.robolectric.shadows.ShadowToast

/** The latest [RuuviToast], as shown through Robolectric. */
internal fun latestToast(): Toast? = ShadowToast.getLatestToast()

/** Text of the latest [RuuviToast], read from its custom view. */
internal fun latestToastText(): String? =
    @Suppress("DEPRECATION")
    latestToast()?.view?.findTextView()?.text?.toString()

internal fun View.findTextView(): TextView? = when (this) {
    is TextView -> this
    is ViewGroup -> (0 until childCount).firstNotNullOfOrNull { getChildAt(it).findTextView() }
    else -> null
}
