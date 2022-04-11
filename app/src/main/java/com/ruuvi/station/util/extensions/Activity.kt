package com.ruuvi.station.util.extensions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.ruuvi.station.BuildConfig
import com.ruuvi.station.R

fun AppCompatActivity.sendFeedback(){
    val intent = Intent(Intent.ACTION_SENDTO)
    intent.data = Uri.parse("mailto:")
    intent.putExtra(Intent.EXTRA_EMAIL, arrayOf( "contact@ruuvi.com" ))
    intent.putExtra(Intent.EXTRA_SUBJECT, "Ruuvi Station Android Feedback")
    val body = """
                       
Device: ${Build.MANUFACTURER} ${Build.MODEL}
Android version: ${Build.VERSION.RELEASE}
App: ${applicationInfo.loadLabel(packageManager)} ${BuildConfig.VERSION_NAME}"""
    intent.putExtra(Intent.EXTRA_TEXT, body)

    startActivity(Intent.createChooser(intent, getString(R.string.send_email)))
}

fun AppCompatActivity.openUrl(url: String){
    val webIntent = Intent(Intent.ACTION_VIEW)
    webIntent.data = Uri.parse(url)
    startActivity(webIntent)
}

fun Activity.hideKeyboard() {
    val imm: InputMethodManager = this.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
}

fun Activity.showKeyboard(view: View) {
    val inputManager = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
}