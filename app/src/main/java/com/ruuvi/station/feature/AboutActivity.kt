package com.ruuvi.station.feature

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import com.ruuvi.station.BuildConfig
import com.ruuvi.station.R
import com.ruuvi.station.database.LocalDatabase
import com.ruuvi.station.database.RuuviTagRepository
import com.ruuvi.station.database.tables.TagSensorReading

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

        operationsText.movementMethod = LinkMovementMethod.getInstance()
        troubleshootingText.movementMethod = LinkMovementMethod.getInstance()
        openText.movementMethod = LinkMovementMethod.getInstance()
        moreText.movementMethod = LinkMovementMethod.getInstance()
        drawDebugInfo()
    }

    private fun drawDebugInfo() {
        val readingCount = TagSensorReading.countAll()
        var debugText = getString(R.string.version, BuildConfig.VERSION_NAME) + "\n"
        val addedTags = RuuviTagRepository.getAll(true).size
        debugText += getString(R.string.seen_tags, addedTags + RuuviTagRepository.getAll(false).size) + "\n"
        debugText += getString(R.string.added_tags, addedTags) + "\n"
        debugText += getString(R.string.db_data_points, readingCount*9) + "\n"

        val dbPath = application.filesDir.path + "/../databases/" + LocalDatabase.NAME + ".db"
        val dbFile = File(dbPath)
        debugText += getString(R.string.db_size, dbFile.length() / 1024)
        debugInfo.text = debugText
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            finish()
        }
        return true
    }
}
