package com.ruuvi.station.about.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT
import com.google.android.material.snackbar.Snackbar
import com.ruuvi.station.BuildConfig
import com.ruuvi.station.R
import com.ruuvi.station.about.model.AppStats
import com.ruuvi.station.database.domain.LocalDatabase
import com.ruuvi.station.databinding.ActivityAboutBinding
import com.ruuvi.station.util.extensions.makeWebLinks
import com.ruuvi.station.util.extensions.viewModel
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import java.io.File

class AboutActivity : AppCompatActivity(R.layout.activity_about), KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: AboutActivityViewModel by viewModel()

    lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            window.statusBarColor = Color.parseColor("#01000000")
        } else {
            window.statusBarColor = Color.TRANSPARENT
        }

        setupUI()
        setupViewModel()
    }

    private fun setupViewModel() {
        lifecycleScope.launchWhenStarted {
            viewModel.appStats.collect { appStats ->
                showAppStats(appStats)
            }
        }
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        with(binding.content) {
            infoText.movementMethod = LinkMovementMethod.getInstance()
            operationsText.movementMethod = LinkMovementMethod.getInstance()
            troubleshootingText.movementMethod = LinkMovementMethod.getInstance()
            openText.movementMethod = LinkMovementMethod.getInstance()
            moreText.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    private var counter: Int = 0

    private fun showAppStats(appStats: AppStats?) {
        var debugText = getString(R.string.version, BuildConfig.VERSION_NAME) + " " +
                getString(R.string.changelog)+ "\n"

        appStats?.let {
            debugText += getString(R.string.help_seen_tags, it.seenTags) + "\n"
            debugText += getString(R.string.help_added_tags, it.favouriteTags) + "\n"
            debugText += getString(R.string.help_db_data_points, it.measurementsCount) + "\n"
        }

        val dbPath = application.filesDir.path + "/../databases/" + LocalDatabase.NAME + ".db"
        val dbFile = File(dbPath)
        debugText += getString(R.string.help_db_size, dbFile.length() / 1024)
        binding.content.debugInfo.text = debugText
        binding.content.debugInfo.makeWebLinks(
            this,
            Pair(getString(R.string.changelog), getString(R.string.changelog_android_url))
        )
        binding.content.debugInfo.setOnClickListener {
            counter++
            val developerSettingsEnabled = viewModel.isDeveloperSettingsEnabled()
            if (counter == 15 && !developerSettingsEnabled) {
                Snackbar.make(binding.root, "You're almost there", LENGTH_SHORT).show()
            }
            if (counter == 20 && !developerSettingsEnabled) {
                Snackbar.make(binding.root, "You can see developer settings now", LENGTH_SHORT).show()
                viewModel.enableDeveloperSettings()
            }
            if (counter == 15 && developerSettingsEnabled) {
                Snackbar.make(binding.root, "You already can see settings options", LENGTH_SHORT).show()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return true
    }

    companion object {
        fun start(context: Context) {
            val aboutIntent = Intent(context, AboutActivity::class.java)
            context.startActivity(aboutIntent)
        }
    }
}
