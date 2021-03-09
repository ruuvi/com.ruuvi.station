package com.ruuvi.station.tagdetails.ui

import android.Manifest
import android.animation.IntEvaluator
import android.animation.ValueAnimator
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.SuperscriptSpan
import android.view.Menu
import android.view.MenuItem
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.app.ActivityCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.Observer
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.snackbar.Snackbar
import com.ruuvi.station.R
import com.ruuvi.station.about.ui.AboutActivity
import com.ruuvi.station.addtag.ui.AddTagActivity
import com.ruuvi.station.alarm.domain.AlarmStatus
import com.ruuvi.station.alarm.domain.AlarmStatus.NO_ALARM
import com.ruuvi.station.alarm.domain.AlarmStatus.NO_TRIGGERED
import com.ruuvi.station.alarm.domain.AlarmStatus.TRIGGERED
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.feature.data.FeatureFlag
import com.ruuvi.station.feature.domain.RuntimeBehavior
import com.ruuvi.station.welcome.ui.WelcomeActivity.Companion.ARGUMENT_FROM_WELCOME
import com.ruuvi.station.network.data.NetworkSyncResultType
import com.ruuvi.station.network.ui.SignInActivity
import com.ruuvi.station.settings.ui.AppSettingsActivity
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tagdetails.domain.TagDetailsArguments
import com.ruuvi.station.tagsettings.ui.TagSettingsActivity
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.util.PermissionsHelper
import com.ruuvi.station.util.Utils
import com.ruuvi.station.util.extensions.*
import kotlinx.android.synthetic.main.activity_tag_details.imageSwitcher
import kotlinx.android.synthetic.main.activity_tag_details.mainDrawerLayout
import kotlinx.android.synthetic.main.activity_tag_details.tag_background_view
import kotlinx.android.synthetic.main.activity_tag_details.toolbar
import kotlinx.android.synthetic.main.content_tag_details.noTagsTextView
import kotlinx.android.synthetic.main.content_tag_details.pagerTitleStrip
import kotlinx.android.synthetic.main.content_tag_details.tagPager
import kotlinx.android.synthetic.main.navigation_drawer.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import timber.log.Timber
import org.kodein.di.generic.instance
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

class TagDetailsActivity : AppCompatActivity(), KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: TagDetailsViewModel by viewModel {
        TagDetailsArguments(
            intent.getStringExtra(ARGUMENT_TAG_ID),
            intent.getBooleanExtra(ARGUMENT_FROM_WELCOME, false)
        )
    }

    private val adapter: TagsFragmentPagerAdapter by lazy {
        TagsFragmentPagerAdapter(supportFragmentManager)
    }

    private val runtimeBehavior: RuntimeBehavior by instance()
    private val preferencesRepository: PreferencesRepository by instance()
    private var alarmStatus: AlarmStatus = NO_ALARM
    private val backgrounds = HashMap<String, BitmapDrawable>()
    private val permissionsHelper = PermissionsHelper(this)
    private var tagPagerScrolling = false
    private var timer: Timer? = null
    private var signedIn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag_details)

        setupViewModel()
        setupUI()

        if (viewModel.openAddView) {
            AddTagActivity.start(this)
            viewModel.openAddView = false
            return
        }
        if (permissionsHelper.arePermissionsGranted() && preferencesRepository.getBackgroundScanMode() == BackgroundScanModes.BACKGROUND) {
            permissionsHelper.requestBackgroundPermission()
        } else {
            permissionsHelper.requestPermissions()
        }
    }

    private fun setupUI() {
        setSupportActionBar(toolbar)

        supportActionBar?.title = null
        supportActionBar?.setIcon(R.drawable.logo_white)

        noTagsTextView.setDebouncedOnClickListener { AddTagActivity.start(this) }

        imageSwitcher.setFactory {
            val imageView = AppCompatImageView(applicationContext)
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            imageView.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT)
            imageView
        }

        if (viewModel.dashboardEnabled) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            mainDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        } else {
            setupDrawer()
        }

        tagPager.adapter = adapter
        tagPager.offscreenPageLimit = 1
        tagPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageScrolled(position: Int, offset: Float, offsetPixels: Int) {
                tagPagerScrolling = offsetPixels != 0
            }

            override fun onPageSelected(position: Int) {
                viewModel.pageSelected(position)
            }
        })

        Snackbar.make(window.decorView.rootView, "Network enabled = ${runtimeBehavior.isFeatureEnabled(FeatureFlag.RUUVI_NETWORK)}", Snackbar.LENGTH_LONG).show()
    }

    private fun setupViewModel() {
        observeTags()

        observeSelectedTag()

        observeAlarmStatus()

        listenToShowGraph()
    }

    private fun observeTags() {
        viewModel.tagsObserve.observe(this, Observer {
            setupTags(it)
        })
    }

    private fun observeAlarmStatus() {
        viewModel.alarmStatusObserve.observe(this, Observer {
            alarmStatus = it
            invalidateOptionsMenu()
        })
    }

    private fun observeSelectedTag() {
        viewModel.selectedTagObserve.observe(this, Observer {
            it?.let { setupSelectedTag(it) }
        })
    }

    private fun listenToShowGraph() {
        viewModel.isShowGraphObserve.observe(this, Observer {
            animateGraphTransition(it)
        })
    }

    private fun setupTags(tags: List<RuuviTag>) {
        val previousTagsSize = adapter.count
        adapter.setTags(tags)

        val isSizeChanged = previousTagsSize > 0 && tags.size != previousTagsSize
        setupVisibility(tags.isNullOrEmpty())
        if (tags.isNotEmpty()) {
            if (!viewModel.desiredTag.isNullOrEmpty() && !isSizeChanged) {
                val index = tags.indexOfFirst { tag -> tag.id == viewModel.desiredTag }
                scrollOrCacheCurrentPosition(tagPager.currentItem != index, index)
            } else {
                scrollOrCacheCurrentPosition(isSizeChanged, tags.size - 1)
            }
        }
    }

    private fun scrollOrCacheCurrentPosition(shouldScroll: Boolean, scrollToPosition: Int) {
        if (shouldScroll) {
            tagPager.setCurrentItem(scrollToPosition, false)
        }
        viewModel.pageSelected(tagPager.currentItem)
    }

    private fun setupSelectedTag(selectedTag: RuuviTag) {
        val previousBitmapDrawable = backgrounds[viewModel.getPrevTag()?.id]
        if (previousBitmapDrawable != null) {
            tag_background_view.setImageDrawable(previousBitmapDrawable)
        }
        val bitmap = Utils.getBackground(applicationContext, selectedTag)
        val bitmapDrawable = BitmapDrawable(applicationContext.resources, bitmap)
        backgrounds[selectedTag.id] = bitmapDrawable
        imageSwitcher.setImageDrawable(bitmapDrawable)
    }

    private fun animateGraphTransition(isShowGraph: Boolean) {
        tagPager.isSwipeEnabled = !isShowGraph
        pagerTitleStrip.isTabSwitchEnabled = !isShowGraph
        val textSpacing = if (isShowGraph) MAX_TEXT_SPACING else MIN_TEXT_SPACING
        val (animateFrom, animateTo) =
            if (isShowGraph) MIN_TEXT_SPACING to MAX_TEXT_SPACING else MAX_TEXT_SPACING to MIN_TEXT_SPACING

        if (pagerTitleStrip.textSpacing != textSpacing) {
            val animator = ValueAnimator.ofInt(animateFrom, animateTo)
            animator.addUpdateListener {
                pagerTitleStrip.textSpacing = animator.animatedValue as Int
            }
            animator.start()
        }
    }

    private fun setupDrawer() {
        val drawerToggle = ActionBarDrawerToggle(
            this, mainDrawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )

        mainDrawerLayout.addDrawerListener(drawerToggle)
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeButtonEnabled(true)
        }
        drawerToggle.syncState()

        updateMenu(signedIn)

        navigationView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.addNewSensorMenuItem -> AddTagActivity.start(this)
                R.id.appSettingsMenuItem -> AppSettingsActivity.start(this)
                R.id.aboutMenuItem -> AboutActivity.start(this)
                R.id.sendFeedbackMenuItem -> SendFeedback()
                R.id.getMoreSensorsMenuItem -> OpenUrl(WEB_URL)
                R.id.loginMenuItem -> login(signedIn)
            }
            mainDrawerLayout.closeDrawer(GravityCompat.START)
            return@setNavigationItemSelectedListener true
        }

        syncLayout.setOnClickListener {
            viewModel.networkDataSync()
        }

        viewModel.syncResultObserve.observe(this, Observer {syncResult ->
            val message = when (syncResult.type) {
                NetworkSyncResultType.NONE -> ""
                NetworkSyncResultType.SUCCESS -> getString(R.string.network_sync_result_success)
                NetworkSyncResultType.EXCEPTION -> getString(R.string.network_sync_result_exception, syncResult.errorMessage)
                NetworkSyncResultType.NOT_LOGGED -> getString(R.string.network_sync_result_not_logged)
            }
            if (message.isNotEmpty()) {
                Snackbar.make(mainDrawerLayout, message, Snackbar.LENGTH_SHORT).show()
                viewModel.syncResultShowed()
            }
        })

        viewModel.syncInProgressObserve.observe(this, Observer {
            if (it) {
                Timber.d("Sync in progress")
                syncNetworkButton.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate_indefinitely))
            } else {
                Timber.d("Sync not in progress")
                syncNetworkButton.clearAnimation()
            }
        })

        viewModel.userEmail.observe(this, Observer {
            var user = it
            if (user.isNullOrEmpty()) {
                user = getString(R.string.none)
                signedIn = false
            } else {
                signedIn = true
            }
            updateMenu(signedIn)
            loggedUserTextView.text = getString(R.string.network_user, user)
        })

        viewModel.syncStatus.observe(this, Observer {syncStatus ->
            if (syncStatus.syncInProgress) {
                syncStatusTextView.text = getString(R.string.connected_reading_info)
            } else {
                val lastSyncString =
                if (syncStatus.lastSync == Long.MIN_VALUE) {
                    getString(R.string.never)
                } else {
                    Date(syncStatus.lastSync).describingTimeSince(this)
                }
                syncStatusTextView.text = getString(R.string.network_synced, lastSyncString)
            }
        })
    }

    private fun login(signedIn: Boolean) {
        if (signedIn == false) {
            val alertDialog = AlertDialog.Builder(this, R.style.CustomAlertDialog).create()
            alertDialog.setTitle(getString(R.string.sign_in_benefits_title))
            alertDialog.setMessage(getString(R.string.sign_in_benefits_description))
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.ok)
            ) { dialog, _ -> dialog.dismiss() }
            alertDialog.setOnDismissListener {
                SignInActivity.start(this)
            }
            alertDialog.show()
        } else {
            val builder = AlertDialog.Builder(this)
            with(builder)
            {
                setMessage(getString(R.string.sign_out_confirm))
                setPositiveButton(getString(R.string.yes)) { dialogInterface, i ->
                    viewModel.signOut()
                }
                setNegativeButton(getString(R.string.no)) { dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
                show()
            }
        }
    }

    private fun updateMenu(signed: Boolean) {
        val loginMenuItem = navigationView.menu.findItem(R.id.loginMenuItem)
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (adapter.count > 0) {
            menuInflater.inflate(R.menu.menu_details, menu)
            val item = menu.findItem(R.id.action_alarm)
            if (viewModel.selectedTagObserve.value != null) {
                setupAlarmIcon(item)
                val graphItem = menu.findItem(R.id.action_graph)
                if (viewModel.isShowingGraph()) {
                    graphItem.setIcon(R.drawable.ic_ruuvi_app_notification_icon_v2)
                } else {
                    graphItem.setIcon(R.drawable.ic_ruuvi_graphs_icon)
                }
            }
        }
        return true
    }

    private fun setupAlarmIcon(item: MenuItem) {
        when (alarmStatus) {
            NO_ALARM -> {
                // off
                item.setIcon(R.drawable.ic_notifications_off_24px)
                item.icon?.alpha = ALARM_ICON_ALPHA
            }
            NO_TRIGGERED -> {
                // on
                item.setIcon(R.drawable.ic_notifications_on_24px)
                item.icon?.alpha = ALARM_ICON_ALPHA
            }
            TRIGGERED -> {
                // triggered
                item.setIcon(R.drawable.ic_notifications_active_24px)
                val drawable = item.icon
                if (drawable != null) {
                    drawable.mutate()
                    drawable.alpha = ALARM_ICON_ALPHA
                    val anim = ValueAnimator()
                    anim.setIntValues(1, 0)
                    anim.setEvaluator(IntEvaluator())
                    anim.addUpdateListener {
                        if (it.animatedFraction > 0.9) {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                drawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
                            } else {
                                drawable.colorFilter = BlendModeColorFilter(Color.WHITE, BlendMode.SRC_ATOP)
                            }
                        } else if (it.animatedFraction < 0.1) {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                drawable.setColorFilter(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                            } else {
                                drawable.colorFilter = BlendModeColorFilter(Color.TRANSPARENT, BlendMode.CLEAR)
                            }
                        }
                    }

                    anim.repeatMode = ValueAnimator.REVERSE
                    anim.repeatCount = ValueAnimator.INFINITE
                    anim.duration = ALARM_ICON_ANIMATION_DURATION
                    anim.start()
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_graph -> {
                if (!tagPagerScrolling) {
                    invalidateOptionsMenu()
                    viewModel.switchShowGraphChannel()
                    val bgScanEnabled = viewModel.getBackgroundScanMode()
                    if (bgScanEnabled == BackgroundScanModes.DISABLED) {
                        if (viewModel.isFirstGraphVisit()) {
                            val simpleAlert = androidx.appcompat.app.AlertDialog.Builder(this).create()
                            simpleAlert.setTitle(resources.getText(R.string.charts_background_dialog_title_question))
                            simpleAlert.setMessage(resources.getText(R.string.charts_background_dialog_description))

                            simpleAlert.setButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE, resources.getText(R.string.yes)) { _, _ ->
                                viewModel.setBackgroundScanMode(BackgroundScanModes.BACKGROUND)
                                permissionsHelper.requestBackgroundPermission()
                            }
                            simpleAlert.setButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE, resources.getText(R.string.no)) { _, _ ->
                            }
                            simpleAlert.setOnDismissListener {
                                viewModel.setIsFirstGraphVisit(false)
                            }
                            simpleAlert.show()
                        }
                    }
                }
            }
            R.id.action_settings -> {
                if (!tagPagerScrolling) TagSettingsActivity.start(this, viewModel.selectedTagObserve.value?.id)
            }
            android.R.id.home -> finish()
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshTags()
        timer = Timer("TagDetailsActivityTimer", true)
        timer?.scheduleAtFixedRate(0, 1000) {
            viewModel.checkForAlarm()
            viewModel.updateNetworkStatus()
        }
    }

    override fun onPause() {
        super.onPause()
        timer?.cancel()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            10 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (preferencesRepository.getBackgroundScanMode() == BackgroundScanModes.BACKGROUND) {
                        permissionsHelper.requestBackgroundPermission()
                    }
                    if (viewModel.openAddView) noTagsTextView.callOnClick()
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        permissionsHelper.requestPermissions()
                    } else {
                        showPermissionSnackbar()
                    }
                }
            }
        }
    }

    private fun showPermissionSnackbar() {
        val snackbar = Snackbar.make(mainDrawerLayout, getString(R.string.permission_location_needed), Snackbar.LENGTH_LONG)
        snackbar.setAction(getString(R.string.settings)) {
            val intent = Intent()
            val uri = Uri.fromParts("package", packageName, null)
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            intent.data = uri
            startActivity(intent)
        }
        snackbar.show()
    }

    private fun setupVisibility(isEmptyTags: Boolean) {
        pagerTitleStrip.isVisible = !isEmptyTags
        tagPager.isVisible = !isEmptyTags
        noTagsTextView.isVisible = isEmptyTags
        invalidateOptionsMenu()

        if (isEmptyTags) {
            val bitmap = Utils.getDefaultBackground(8, applicationContext)
            imageSwitcher.setImageDrawable(bitmap)
        }
    }

    class TagsFragmentPagerAdapter(manager: FragmentManager)
        : FragmentStatePagerAdapter(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        private var tags = listOf<RuuviTag>()

        override fun getItem(position: Int): Fragment {
            return TagFragment.newInstance(tags[position])
        }

        override fun getCount(): Int {
            return tags.size
        }

        override fun getPageTitle(position: Int): String {
            return tags[position].displayName.toUpperCase(Locale.getDefault())
        }

        override fun getItemPosition(`object`: Any): Int {
            return PagerAdapter.POSITION_NONE
        }

        fun setTags(tags: List<RuuviTag>) {
            this.tags = tags
            notifyDataSetChanged()
        }
    }

    companion object {
        private const val ARGUMENT_TAG_ID = "ARGUMENT_TAG_ID"
        private const val MIN_TEXT_SPACING = 0
        private const val MAX_TEXT_SPACING = 1000
        private const val WEB_URL = "https://ruuvi.com"
        private const val ALARM_ICON_ALPHA = 128
        private const val ALARM_ICON_ANIMATION_DURATION = 500L

        fun start(context: Context, isFromWelcome: Boolean) {
            val intent = Intent(context, TagDetailsActivity::class.java)
            intent.putExtra(ARGUMENT_FROM_WELCOME, isFromWelcome)
            context.startActivity(intent)
        }

        fun start(context: Context, tagId: String) {
            val intent = Intent(context, TagDetailsActivity::class.java)
            intent.putExtra(ARGUMENT_TAG_ID, tagId)
            context.startActivity(intent)
        }

        fun createPendingIntent(context: Context, tagId: String, alarmId: Int): PendingIntent? {
            val intent = Intent(context, TagDetailsActivity::class.java)
            intent.putExtra(ARGUMENT_TAG_ID, tagId)

            return TaskStackBuilder.create(context)
                .addNextIntent(intent)
                .getPendingIntent(alarmId, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }
}
