package com.ruuvi.station.util.ui

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import org.robolectric.shadows.ShadowToast

/** The latest system [Toast] (used only when no screen is available), as Robolectric saw it. */
internal fun latestToast(): Toast? = ShadowToast.getLatestToast()

/** Text of the latest [RuuviToast], whether drawn in a screen or as a system toast. */
internal fun latestToastText(): String? = RuuviToast.lastShown?.first

/** Toast length (Toast.LENGTH_SHORT / LENGTH_LONG) of the latest [RuuviToast]. */
internal fun latestToastDuration(): Int? = RuuviToast.lastShown?.second?.toastLength

/** How many toasts [RuuviToast] has shown since the last [resetToasts]. */
internal fun toastShownCount(): Int = RuuviToast.shownCount

internal fun resetToasts() {
    ShadowToast.reset()
    RuuviToast.resetHistory()
}

internal fun View.findTextView(): TextView? = when (this) {
    is TextView -> this
    is ViewGroup -> (0 until childCount).firstNotNullOfOrNull { getChildAt(it).findTextView() }
    else -> null
}
