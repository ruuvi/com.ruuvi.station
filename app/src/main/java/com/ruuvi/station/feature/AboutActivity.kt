package com.ruuvi.station.feature

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import android.widget.Toast
import com.ruuvi.station.BuildConfig
import com.ruuvi.station.R

import kotlinx.android.synthetic.main.activity_about.*
import kotlinx.android.synthetic.main.content_about.*
import java.io.File

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = null
        supportActionBar?.setIcon(R.drawable.logo_white)

        aboutInfoText.movementMethod = LinkMovementMethod.getInstance()
        versionInfo.text = getString(R.string.version) + ": " + BuildConfig.VERSION_NAME

        //todo: remove me
        val path = application.filesDir.path + "/android-beacon-library-scan-state"
        val file = File(path)
        val snack = Snackbar.make(about_root, "Size: " + file.length() / 1024 + "KB", Snackbar.LENGTH_INDEFINITE)
        snack.setAction("Remove") {
            if (file.delete()) {
               Toast.makeText(this, "Deleted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Could not delete", Toast.LENGTH_SHORT).show()
            }
        }
        snack.show()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            finish()
        }
        return true
    }
}
