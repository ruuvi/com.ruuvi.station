package com.ruuvi.station.util.extensions

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.ruuvi.station.R
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.inputmethod.InputMethodManager
import com.google.android.material.internal.NavigationMenuView
import com.google.android.material.navigation.NavigationView

fun ComponentActivity.enableRuuviEdgeToEdge() {
    val darkTheme = isDarkMode()
    val legacyNavigationBarColor = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        ContextCompat.getColor(this, R.color.titan)
    } else {
        Color.TRANSPARENT
    }

    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
        navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, legacyNavigationBarColor) { darkTheme },
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = false
    }
}

/**
 * Protects an XML screen's header and sides while leaving its background edge to edge.
 * The optional scrolling content owns bottom padding so it can scroll behind navigation.
 */
fun View.applyRuuviEdgeToEdgeInsets(scrollingContent: ViewGroup? = null) {
    val contentBottomPadding = scrollingContent?.paddingBottom ?: 0
    scrollingContent?.clipToPadding = false
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val safeInsets = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        view.setPadding(safeInsets.left, safeInsets.top, safeInsets.right, 0)
        scrollingContent?.apply {
            setPadding(paddingLeft, paddingTop, paddingRight, contentBottomPadding + safeInsets.bottom)
        }
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

fun Activity.hideKeyboard() {
    val imm: InputMethodManager = this.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
}

fun Activity.showKeyboard(view: View) {
    val inputManager = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
}

fun Activity.disableNavigationViewScrollbars(navigationView: NavigationView) {
    val navigationMenuView = navigationView.getChildAt(0) as NavigationMenuView
    if (navigationMenuView != null) {
        navigationMenuView.isVerticalScrollBarEnabled = false
    }
}
