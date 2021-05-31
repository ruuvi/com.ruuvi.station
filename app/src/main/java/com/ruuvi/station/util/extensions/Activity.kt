package com.ruuvi.station.util.extensions

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
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

fun AppCompatActivity.hideKeyboard() {
    val imm: InputMethodManager = this.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
}

fun AppCompatActivity.getMainMenuItems(signed: Boolean) =
    arrayOf(
        getString(R.string.menu_add_new_sensor),
        getString(R.string.menu_app_settings),
        getString(R.string.menu_about_help),
        getString(R.string.menu_send_feedback),
        getString(R.string.menu_get_more_sensors),
        if (signed) getString(R.string.sign_out) else getString(R.string.sign_in)
    )