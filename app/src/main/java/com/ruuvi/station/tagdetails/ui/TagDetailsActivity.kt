package com.ruuvi.station.tagdetails.ui

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
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.TaskStackBuilder
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.ruuvi.station.R
import com.ruuvi.station.about.ui.AboutActivity
import com.ruuvi.station.addtag.ui.AddTagActivity
import com.ruuvi.station.alarm.domain.AlarmStatus
import com.ruuvi.station.alarm.domain.AlarmStatus.*
import com.ruuvi.station.app.preferences.Preferences
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.app.review.ReviewManagerInteractor
import com.ruuvi.station.bluetooth.domain.PermissionsInteractor
import com.ruuvi.station.dashboard.ui.DashboardActivity
import com.ruuvi.station.databinding.ActivityTagDetailsBinding
import com.ruuvi.station.feature.domain.RuntimeBehavior
import com.ruuvi.station.network.ui.SignInActivity
import com.ruuvi.station.settings.ui.SettingsActivity
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tagdetails.domain.TagDetailsArguments
import com.ruuvi.station.tagsettings.ui.TagSettingsActivity
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.util.Utils
import com.ruuvi.station.util.extensions.*
import com.ruuvi.station.welcome.ui.WelcomeActivity.Companion.ARGUMENT_FROM_WELCOME
import com.ruuvi.station.widgets.ui.complexWidget.ComplexWidgetProvider
import com.ruuvi.station.widgets.ui.simpleWidget.SimpleWidget
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.scheduleAtFixedRate
import kotlin.math.abs

class TagDetailsActivity : AppCompatActivity(R.layout.activity_tag_details), KodeinAware {

    override val kodein: Kodein by closestKodein()

    private lateinit var binding: ActivityTagDetailsBinding

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
    private val reviewManagerInteractor: ReviewManagerInteractor by instance()

    private var alarmStatus: AlarmStatus = NO_ALARM
    private val backgrounds = HashMap<String, BitmapDrawable>()
    private lateinit var permissionsInteractor: PermissionsInteractor

    private var tagPagerScrolling = false
    private var timer: Timer? = null
    private var signedIn = false

    private val appUpdateManager: AppUpdateManager by lazy {
        AppUpdateManagerFactory.create(this)
    }

    private val listener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            showUpdateSnackbar()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTagDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        permissionsInteractor = PermissionsInteractor(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setupViewModel()
        setupUI()

        if (viewModel.openAddView) {
            AddTagActivity.start(this)
            viewModel.openAddView = false
            return
        } else {
            reviewManagerInteractor.requestReview(this)
        }

        requestPermission()
    }

    override fun onStart() {
        super.onStart()
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        val previousRequestDate = preferencesRepository.getRequestForAppUpdateDate()
        val daysSinceRequest = abs(TimeUnit.MILLISECONDS.toDays(Date().time - Date(previousRequestDate).time))

        appUpdateManager.registerListener(listener)

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && (appUpdateInfo.clientVersionStalenessDays() ?: -1) >= UPDATE_STALENESS_DAYS
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                && daysSinceRequest >= UPDATE_REQUEST_REPEAT_DAYS
            ) {
                preferencesRepository.updateRequestForAppUpdateDate()
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.FLEXIBLE,
                    this,
                    UPDATE_REQUEST_CODE)
            } else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                showUpdateSnackbar()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PermissionsInteractor.REQUEST_CODE_BLUETOOTH || requestCode == PermissionsInteractor.REQUEST_CODE_LOCATION) {
            requestPermission()
        }
    }

    override fun onStop() {
        super.onStop()
        appUpdateManager.unregisterListener(listener)
    }

    private fun showUpdateSnackbar() {
        Snackbar.make(
            findViewById(android.R.id.content),
            getText(R.string.update_downloaded_message),
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction(getText(R.string.update_action)) { appUpdateManager.completeUpdate() }
            show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PermissionsInteractor.REQUEST_CODE_PERMISSIONS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestPermission()
                    if (viewModel.openAddView) binding.content.noTagsTextView.callOnClick()
                } else {
                    permissionsInteractor.showPermissionSnackbar()
                }
            }
        }
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)

        supportActionBar?.title = null
        supportActionBar?.setIcon(R.drawable.logo_2021)

        binding.content.noTagsTextView.setDebouncedOnClickListener { AddTagActivity.start(this) }

        binding.imageSwitcher.setFactory {
            val imageView = ImageView(applicationContext)
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            imageView.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT)
            imageView
        }

        if (viewModel.dashboardEnabled) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            binding.mainDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        } else {
            setupDrawer()
        }

        binding.content.tagPager.adapter = adapter
        binding.content.tagPager.offscreenPageLimit = 1
        binding.content.tagPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageScrolled(position: Int, offset: Float, offsetPixels: Int) {
                tagPagerScrolling = offsetPixels != 0
            }

            override fun onPageSelected(position: Int) {
                viewModel.pageSelected(position)
            }
        })
    }

    private fun setupViewModel() {
        observeTags()

        observeSelectedTag()

        observeAlarmStatus()

        listenToShowGraph()
    }

    private fun observeTags() {
        viewModel.tagsObserve.observe(this) {
            setupTags(it)
        }
    }

    private fun observeAlarmStatus() {
        viewModel.alarmStatusObserve.observe(this) {
            alarmStatus = it
            invalidateOptionsMenu()
        }
    }

    private fun observeSelectedTag() {
        viewModel.selectedTagObserve.observe(this) {
            it?.let { setupSelectedTag(it) }
        }
    }

    private fun listenToShowGraph() {
        viewModel.isShowGraphObserve.observe(this) {
            animateGraphTransition(it)
            binding.darkerBackground.isVisible = it
        }
    }

    private fun setupTags(tags: List<RuuviTag>) {
        val newSensorId = getNewSensor(adapter.getTags(), tags)
        Timber.d("newSensorId = $newSensorId desiredTag = ${viewModel.desiredTag}")
        if (newSensorId != null) viewModel.desiredTag = newSensorId
        adapter.setTags(tags)

        setupVisibility(tags.isNullOrEmpty())
        if (tags.isNotEmpty()) {
            val index = tags.indexOfFirst { tag -> tag.id == viewModel.desiredTag }
            scrollOrCacheCurrentPosition(binding.content.tagPager.currentItem != index, index)
        }
    }

    private fun getNewSensor(previousSensors: List<RuuviTag>, newSensors: List<RuuviTag>): String? {
        if (previousSensors.isNullOrEmpty()) return null
        return newSensors.firstOrNull { newSens -> previousSensors.none { it.id == newSens.id } }?.id
    }

    private fun scrollOrCacheCurrentPosition(shouldScroll: Boolean, scrollToPosition: Int) {
        if (shouldScroll) {
            binding.content.tagPager.setCurrentItem(scrollToPosition, false)
        }
        viewModel.pageSelected(binding.content.tagPager.currentItem)
    }

    private fun setupSelectedTag(selectedTag: RuuviTag) {
        val previousBitmapDrawable = backgrounds[viewModel.getPrevTag()?.id]
        if (previousBitmapDrawable != null) {
            binding.tagBackgroundView.setImageDrawable(previousBitmapDrawable)
        }
        val bitmap = Utils.getBackground(applicationContext, selectedTag)
        val bitmapDrawable = BitmapDrawable(applicationContext.resources, bitmap)
        backgrounds[selectedTag.id] = bitmapDrawable
        binding.imageSwitcher.setImageDrawable(bitmapDrawable)
    }

    private fun animateGraphTransition(isShowGraph: Boolean) {
        binding.content.tagPager.isSwipeEnabled = !isShowGraph
        binding.content.pagerTitleStrip.isTabSwitchEnabled = !isShowGraph
        val textSpacing = if (isShowGraph) MAX_TEXT_SPACING else MIN_TEXT_SPACING
        val (animateFrom, animateTo) =
            if (isShowGraph) MIN_TEXT_SPACING to MAX_TEXT_SPACING else MAX_TEXT_SPACING to MIN_TEXT_SPACING

        if (binding.content.pagerTitleStrip.textSpacing != textSpacing) {
            val animator = ValueAnimator.ofInt(animateFrom, animateTo)
            animator.addUpdateListener {
                binding.content.pagerTitleStrip.textSpacing = animator.animatedValue as Int
            }
            animator.start()
        }
    }

    private fun setupDrawer() {
        val drawerToggle = ActionBarDrawerToggle(
            this, binding.mainDrawerLayout, binding.toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )

        binding.mainDrawerLayout.addDrawerListener(drawerToggle)
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeButtonEnabled(true)
        }
        drawerToggle.syncState()

        disableNavigationViewScrollbars(binding.navigationContent.navigationView)

        updateMenu(signedIn)

        binding.navigationContent.navigationView.setOnApplyWindowInsetsListener { view, insets ->
            val topInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(WindowInsets.Type.systemBars()).top
            } else {
                insets.systemWindowInsetTop
            }

            val bottomInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(WindowInsets.Type.systemBars()).bottom
            } else {
                insets.systemWindowInsetBottom
            }

            binding.navigationContent.navigationView.setMarginTop(topInset)
            binding.navigationContent.navigationView.setMarginBottom(bottomInset)
            Timber.d("insets $topInset $bottomInset $view $insets")
            return@setOnApplyWindowInsetsListener insets

        }

        binding.navigationContent.navigationView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.addNewSensorMenuItem -> AddTagActivity.start(this)
                R.id.appSettingsMenuItem -> SettingsActivity.start(this)
                R.id.aboutMenuItem -> AboutActivity.start(this)
                R.id.sendFeedbackMenuItem -> sendFeedback()
                R.id.whatTomeasureMenuItem -> openUrl(getString(R.string.what_to_measure_link))
                R.id.getMoreSensorsMenuItem -> openUrl(getString(R.string.buy_sensors_link))
                R.id.getGatewayMenuItem -> openUrl(getString(R.string.buy_gateway_link))
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
            val builder = AlertDialog.Builder(this, R.style.CustomAlertDialog)
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

    private fun updateMenu(signed: Boolean) {
        binding.navigationContent.networkLayout.isVisible = viewModel.userEmail.value?.isNotEmpty() == true
        val loginMenuItem = binding.navigationContent.navigationView.menu.findItem(R.id.loginMenuItem)
        loginMenuItem?.let {
            it.title = if (signed) {
                getString(R.string.sign_out)
            } else {
                getString(R.string.sign_in)
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
                item.icon?.alpha = ALARM_ICON_ALPHA_OPAQUE
            }
            TRIGGERED -> {
                // triggered
                item.setIcon(R.drawable.ic_notifications_active_24px)
                val drawable = item.icon
                if (drawable != null) {
                    drawable.mutate()
                    drawable.alpha = ALARM_ICON_ALPHA_OPAQUE
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
                            val simpleAlert = AlertDialog.Builder(this, R.style.CustomAlertDialog).create()
                            simpleAlert.setTitle(resources.getText(R.string.charts_background_dialog_title_question))
                            simpleAlert.setMessage(resources.getText(R.string.charts_background_dialog_description))

                            simpleAlert.setButton(AlertDialog.BUTTON_POSITIVE, resources.getText(R.string.yes)) { _, _ ->
                                viewModel.setBackgroundScanMode(BackgroundScanModes.BACKGROUND)
                                permissionsInteractor.requestBackgroundPermission()
                            }
                            simpleAlert.setButton(AlertDialog.BUTTON_NEGATIVE, resources.getText(R.string.no)) { _, _ ->
                            }
                            simpleAlert.setOnDismissListener {
                                viewModel.setIsFirstGraphVisit(false)
                            }
                            simpleAlert.show()
                        }
                    }
                }
            }
            R.id.action_alarm -> {
                if (!tagPagerScrolling) TagSettingsActivity.start(this, viewModel.selectedTagObserve.value?.id, true)
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

        //TODO REMOVE TESTING UPDATE?
        SimpleWidget.updateAll(this)
        ComplexWidgetProvider.updateAll(this)

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

    private fun setupVisibility(isEmptyTags: Boolean) {
        binding.content.pagerTitleStrip.isVisible = !isEmptyTags
        binding.content.tagPager.isVisible = !isEmptyTags
        binding.content.noTagsTextView.isVisible = isEmptyTags
        invalidateOptionsMenu()

        if (isEmptyTags) {
            binding.imageSwitcher.setImageDrawable(
                AppCompatResources.getDrawable(
                    this,
                    R.drawable.gradient_background
                )
            )
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
            return tags.elementAtOrNull(position)?.displayName?.toUpperCase(Locale.getDefault()) ?: ""
        }

        override fun getItemPosition(`object`: Any): Int {
            return PagerAdapter.POSITION_NONE
        }

        fun setTags(tags: List<RuuviTag>) {
            this.tags = tags
            notifyDataSetChanged()
        }

        fun getTags() = tags
    }

    private fun requestPermission() {
        permissionsInteractor.requestPermissions(
            needBackground = preferencesRepository.getBackgroundScanMode() == BackgroundScanModes.BACKGROUND,
            askForBluetooth = !preferencesRepository.isCloudModeEnabled() || !preferencesRepository.signedIn()
        )
    }

    companion object {
        private const val ARGUMENT_TAG_ID = "ARGUMENT_TAG_ID"
        private const val MIN_TEXT_SPACING = 0
        private const val MAX_TEXT_SPACING = 1000
        private const val ALARM_ICON_ALPHA = 128
        private const val ALARM_ICON_ALPHA_OPAQUE = 255
        private const val ALARM_ICON_ANIMATION_DURATION = 500L
        private const val UPDATE_REQUEST_CODE = 213
        private const val UPDATE_STALENESS_DAYS = 1
        private const val UPDATE_REQUEST_REPEAT_DAYS = 7

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

        fun createPendingIntent(context: Context, tagId: String, requestCode: Int): PendingIntent? {
            val intent = Intent(context, TagDetailsActivity::class.java)
            intent.putExtra(ARGUMENT_TAG_ID, tagId)

            val preferencesRepository = PreferencesRepository(Preferences(context))
            val stackBuilder = TaskStackBuilder.create(context)
            if (preferencesRepository.isDashboardEnabled()) {
                val intentDashboardActivity = Intent(context, DashboardActivity::class.java)
                stackBuilder.addNextIntent(intentDashboardActivity)
            }
            stackBuilder.addNextIntent(intent)

            return stackBuilder
                .getPendingIntent(requestCode, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}