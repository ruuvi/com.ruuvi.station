package com.ruuvi.station.dashboard.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.SuperscriptSpan
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import com.ruuvi.station.R
import com.ruuvi.station.about.ui.AboutActivity
import com.ruuvi.station.addtag.ui.AddTagActivity
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.feature.domain.RuntimeBehavior
import com.ruuvi.station.network.ui.SignInActivity
import com.ruuvi.station.settings.ui.AppSettingsActivity
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tagdetails.ui.TagDetailsActivity
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.bluetooth.domain.PermissionsInteractor
import com.ruuvi.station.databinding.ActivityDashboardBinding
import com.ruuvi.station.util.extensions.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import java.util.*
import kotlin.collections.MutableList
import kotlin.concurrent.scheduleAtFixedRate

class DashboardActivity : AppCompatActivity(R.layout.activity_dashboard), KodeinAware {

    override val kodein by closestKodein()

    private val viewModel: DashboardActivityViewModel by viewModel()

    private val runtimeBehavior: RuntimeBehavior by instance()
    private var tags: MutableList<RuuviTag> = arrayListOf()
    private lateinit var adapter: RuuviTagAdapter
    private var getTagsTimer :Timer? = null
    private var signedIn = false
    private lateinit var permissionsInteractor: PermissionsInteractor
    private val preferencesRepository: PreferencesRepository by instance()
    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        permissionsInteractor = PermissionsInteractor(this)

        supportActionBar?.title = null
        supportActionBar?.setIcon(R.drawable.logo_2021)

        setupViewModel()
        setupDrawer()
        setupListView()

        requestPermission()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PermissionsInteractor.REQUEST_CODE_BLUETOOTH || requestCode == PermissionsInteractor.REQUEST_CODE_LOCATION) {
            requestPermission()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PermissionsInteractor.REQUEST_CODE_PERMISSIONS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestPermission()
                } else {
                    permissionsInteractor.showPermissionSnackbar()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        getTagsTimer = Timer("DashboardActivityTimer", false)
        getTagsTimer?.scheduleAtFixedRate(0, 1000) {
            viewModel.updateTags()
            viewModel.updateNetworkStatus()
        }
    }

    override fun onPause() {
        super.onPause()
        getTagsTimer?.cancel()
    }

    private fun setupViewModel() {
        viewModel.observeTags.observe(this) {
            tags.clear()
            tags.addAll(it)
            binding.content.noTagsTextView.isVisible = tags.isEmpty()
            adapter.notifyDataSetChanged()
        }
    }

    private fun setupListView() {
        adapter = RuuviTagAdapter(this@DashboardActivity, tags, viewModel.converter)
        binding.content.dashboardListView.adapter = adapter
        binding.content.dashboardListView.onItemClickListener = tagClick
    }

    private fun setupDrawer() {
        val drawerToggle =
            ActionBarDrawerToggle(
                this, binding.mainDrawerLayout, binding.toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close
            )

        binding.mainDrawerLayout.addDrawerListener(drawerToggle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        drawerToggle.syncState()

        updateMenu(signedIn)

        binding.navigationContent.navigationView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.addNewSensorMenuItem -> AddTagActivity.start(this)
                R.id.appSettingsMenuItem -> AppSettingsActivity.start(this)
                R.id.aboutMenuItem -> AboutActivity.start(this)
                R.id.sendFeedbackMenuItem -> sendFeedback()
                R.id.getMoreSensorsMenuItem -> openUrl(WEB_URL)
                R.id.loginMenuItem -> login(signedIn)
            }
            binding.mainDrawerLayout.closeDrawer(GravityCompat.START)
            return@setNavigationItemSelectedListener true
        }

        viewModel.userEmail.observe(this) {
            var user = it
            if (user.isNullOrEmpty()) {
                user = getString(R.string.none)
                signedIn = false
            } else {
                signedIn = true
            }
            updateMenu(signedIn)
            binding.navigationContent.loggedUserTextView.text = user
        }
    }

    private fun login(signedIn: Boolean) {
        if (signedIn) {
            val builder = AlertDialog.Builder(this)
            with(builder)
            {
                setMessage(getString(R.string.sign_out_confirm))
                setPositiveButton(getString(R.string.yes)) { _, _ ->
                    viewModel.signOut()
                }
                setNegativeButton(getString(R.string.no)) { dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
                show()
            }
        } else {
            SignInActivity.start(this)

        }
    }

    fun showNetworkBenefitsDialog() {
        val alertDialog = AlertDialog.Builder(this, R.style.CustomAlertDialog).create()
        alertDialog.setTitle(getString(R.string.sign_in_benefits_title))
        alertDialog.setMessage(getString(R.string.sign_in_benefits_description))
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.ok)
        ) { dialog, _ -> dialog.dismiss() }
        alertDialog.setOnDismissListener {
            SignInActivity.start(this)
        }
        alertDialog.show()
    }

    private fun updateMenu(signed: Boolean) {
        binding.navigationContent.networkLayout.isVisible = viewModel.userEmail.value?.isNotEmpty() == true
        val loginMenuItem = binding.navigationContent.navigationView.menu.findItem(R.id.loginMenuItem)
        loginMenuItem?.let {
            it.title = if (signed) {
                getString(R.string.sign_out)
            } else {
                val signInText = getString(R.string.sign_in)
                val betaText = getString(R.string.beta)
                val spannable = SpannableString (signInText+betaText)
                spannable.setSpan(ForegroundColorSpan(Color.RED), signInText.length, signInText.length + betaText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(SuperscriptSpan(), signInText.length, signInText.length + betaText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(RelativeSizeSpan(0.75f), signInText.length, signInText.length + betaText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable
            }
        }
    }

    private val tagClick = AdapterView.OnItemClickListener { _, view, _, _ ->
        val tag = view.tag as RuuviTag
        TagDetailsActivity.start(this, tag.id)
    }

    private fun requestPermission() {
        permissionsInteractor.requestPermissions(preferencesRepository.getBackgroundScanMode() == BackgroundScanModes.BACKGROUND)
    }

    companion object {
        private const val WEB_URL = "https://ruuvi.com"

        fun start(context: Context) {
            val intent = Intent(context, DashboardActivity::class.java)
            context.startActivity(intent)
        }
    }
}
