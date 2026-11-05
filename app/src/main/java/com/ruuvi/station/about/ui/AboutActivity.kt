package com.ruuvi.station.about.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.ruuvi.station.BuildConfig
import com.ruuvi.station.R
import com.ruuvi.station.about.model.AppStats
import com.ruuvi.station.database.domain.LocalDatabase
import com.ruuvi.station.databinding.ActivityAboutBinding
import com.ruuvi.station.util.extensions.isDarkMode
import com.ruuvi.station.util.extensions.makeWebLinks
import com.ruuvi.station.util.extensions.viewModel
import com.ruuvi.station.util.ui.showBottomSnackbar
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.android.closestDI
import java.io.File

class AboutActivity : AppCompatActivity(R.layout.activity_about), DIAware {

    override val di: DI by closestDI()

    private val viewModel: AboutActivityViewModel by viewModel()

    lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val darkTheme = isDarkMode()
        val legacyNavigationBarColor = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            ContextCompat.getColor(this, R.color.titan)
        } else {
            Color.TRANSPARENT
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) {
                darkTheme
            },
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, legacyNavigationBarColor) {
                darkTheme
            },
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val safeInsets = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(safeInsets.left, safeInsets.top, safeInsets.right, 0)
            binding.content.root.apply {
                setPadding(paddingLeft, paddingTop, paddingRight, safeInsets.bottom)
                clipToPadding = false
            }
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)

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

        if (!isDarkMode()) {
            binding.root.setBackgroundColor(Color.WHITE)
            binding.appBarLayout.setBackgroundColor(Color.WHITE)

            val toolbarContentColor = ContextCompat.getColor(this, R.color.titan)
            binding.toolbar.setTitleTextColor(toolbarContentColor)
            binding.toolbar.navigationIcon?.setTint(toolbarContentColor)
        }

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
        val appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        var debugText = getString(R.string.version, appVersion) + " " +
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
                showBottomSnackbar("You're almost there")
            }
            if (counter == 20 && !developerSettingsEnabled) {
                showBottomSnackbar("You can see developer settings now")
                viewModel.enableDeveloperSettings()
            }
            if (counter == 15 && developerSettingsEnabled) {
                showBottomSnackbar("You already can see settings options")
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
