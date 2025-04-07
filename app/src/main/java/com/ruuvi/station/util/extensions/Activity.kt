package com.ruuvi.station.util.extensions

import android.app.Activity
import android.content.Context
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.google.android.material.internal.NavigationMenuView
import com.google.android.material.navigation.NavigationView

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

fun Activity.gestureNavigationEnabled(): Boolean {
    return Settings.Secure.getInt(this.contentResolver, "navigation_mode", 0) == 2
}