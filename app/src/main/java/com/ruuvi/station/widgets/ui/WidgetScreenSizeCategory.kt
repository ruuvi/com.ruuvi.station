package com.ruuvi.station.widgets.ui

import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.annotation.RequiresApi

enum class WidgetScreenSizeCategory {
    SMALL,
    MEDIUM,
    BIG
}

@RequiresApi(Build.VERSION_CODES.N)
fun resolveWidgetScreenSizeCategory(context: Context): WidgetScreenSizeCategory {
    val windowManager = context.getSystemService(WindowManager::class.java)
    val screenHeightPx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && windowManager != null) {
        windowManager.currentWindowMetrics.bounds.height()
    } else {
        context.resources.displayMetrics.heightPixels
    }

    val stableDensity = android.util.DisplayMetrics.DENSITY_DEVICE_STABLE / 160f
    val screenHeightDp = screenHeightPx / stableDensity

    return when {
        screenHeightDp >= 750f -> WidgetScreenSizeCategory.BIG
        screenHeightDp >= 660f -> WidgetScreenSizeCategory.MEDIUM
        else -> WidgetScreenSizeCategory.SMALL
    }
}
