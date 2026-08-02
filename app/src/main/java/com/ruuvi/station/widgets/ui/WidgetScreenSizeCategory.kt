package com.ruuvi.station.widgets.ui

import android.content.Context
import android.os.Build
import android.view.WindowManager

enum class WidgetScreenSizeCategory {
    SMALL,
    MEDIUM,
    BIG
}

fun resolveWidgetScreenSizeCategory(context: Context): WidgetScreenSizeCategory {
    val windowManager = context.getSystemService(WindowManager::class.java)
    val screenHeightPx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && windowManager != null) {
        windowManager.currentWindowMetrics.bounds.height()
    } else {
        context.resources.displayMetrics.heightPixels
    }
    val density = context.resources.displayMetrics.density
    val screenHeightDp = screenHeightPx / density

    return when {
        screenHeightDp >= 750f -> WidgetScreenSizeCategory.BIG
        screenHeightDp >= 660f -> WidgetScreenSizeCategory.MEDIUM
        else -> WidgetScreenSizeCategory.SMALL
    }
}
