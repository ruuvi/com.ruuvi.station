package com.ruuvi.station.util.extensions

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import com.ruuvi.station.BuildConfig

fun AppCompatActivity.SendFeedback(){
    val intent = Intent(Intent.ACTION_SENDTO)
    intent.data = Uri.parse("mailto:")
    intent.putExtra(Intent.EXTRA_EMAIL, arrayOf( "contact@ruuvi.com" ));
    intent.putExtra(Intent.EXTRA_SUBJECT, "Ruuvi Station Android Feedback")
    val body = """
                       
Device: ${Build.MANUFACTURER} ${Build.MODEL}
Android version: ${Build.VERSION.RELEASE}
App: ${applicationInfo.loadLabel(packageManager)} ${BuildConfig.VERSION_NAME}"""
    intent.putExtra(Intent.EXTRA_TEXT, body)

    startActivity(Intent.createChooser(intent, "Send Email"))
}

fun AppCompatActivity.OpenUrl(url: String){
    val webIntent = Intent(Intent.ACTION_VIEW)
    webIntent.data = Uri.parse(url)
    startActivity(webIntent)
}