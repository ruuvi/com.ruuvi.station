package com.ruuvi.station.tagdetails.ui

import android.Manifest
import android.animation.IntEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import com.google.android.material.snackbar.Snackbar
import androidx.core.app.*
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.lifecycleScope
import com.flexsentlabs.androidcommons.app.ui.setDebouncedOnClickListener
import com.flexsentlabs.extensions.viewModel
import com.ruuvi.station.R
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.feature.AboutActivity
import com.ruuvi.station.feature.AddTagActivity
import com.ruuvi.station.feature.TagSettings
import com.ruuvi.station.settings.ui.AppSettingsActivity
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.util.Starter
import com.ruuvi.station.util.Utils
import kotlinx.android.synthetic.main.activity_tag_details.*
import kotlinx.android.synthetic.main.content_tag_details.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import java.util.*

@ExperimentalCoroutinesApi
class TagDetailsActivity : AppCompatActivity(), KodeinAware {
    override val kodein: Kodein by closestKodein()

    private val viewModel: TagDetailsViewModel by viewModel()

    private val adapter: TagsFragmentPagerAdapter by lazy {
        TagsFragmentPagerAdapter(supportFragmentManager)
    }

    private var isEmptyList = true
    private var alarmStatus: Int? = null
    private val backgrounds = HashMap<String, BitmapDrawable>()
    private lateinit var starter: Starter
    private var openAddView = false
    private var desiredTag: String? = null
    private var tagPagerScrolling = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag_details)
        desiredTag = intent.getStringExtra("id")
        openAddView = intent.getBooleanExtra(FROM_WELCOME, false)

        starter = Starter(this)
        setupViewModel()
        setupUI()

        if (openAddView) {
            val addIntent = Intent(this, AddTagActivity::class.java)
            intent.putExtra(FROM_WELCOME, false)
            startActivity(addIntent)
            return
        }
        starter.getThingsStarted()
    }

    private fun setupUI() {
        setSupportActionBar(toolbar)

        supportActionBar?.title = null
        supportActionBar?.setIcon(R.drawable.logo_white)

        noTags_textView.setDebouncedOnClickListener {
            startActivity(Intent(this, AddTagActivity::class.java))
        }

        imageSwitcher.setFactory {
            val im = AppCompatImageView(applicationContext)
            im.scaleType = ImageView.ScaleType.CENTER_CROP
            im.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT)
            im
        }

        if (viewModel.dashboardEnabled) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            main_drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        } else {
            setupDrawer()
        }

        tag_pager.adapter = adapter
        tag_pager.offscreenPageLimit = 1
        tag_pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(p0: Int) {}

            override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {
                tagPagerScrolling = p2 != 0
            }

            override fun onPageSelected(p0: Int) {
                viewModel.pageSelected(p0)
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
        viewModel.tags.observe(this, androidx.lifecycle.Observer { tags ->
            val previousTagsSize = adapter.count
            isEmptyList = tags.isNullOrEmpty()
            tags?.let {
                adapter.setTags(tags)

                tags.forEach { tag ->
                    tag.id?.let { tagId ->
                        Utils.getBackground(applicationContext, tag).let { bitmap ->
                            backgrounds[tagId] = BitmapDrawable(applicationContext.resources, bitmap)
                        }
                    }
                }

                val isSizeChanged = previousTagsSize > 0 && tags.size != previousTagsSize
                setupVisibility(isEmptyList)

                if (tags.isNotEmpty()) {
                    if (!desiredTag.isNullOrEmpty()) {
                        val index = tags.indexOfFirst { t -> t.id == desiredTag }
                        desiredTag = null
                        intent.putExtra("id", null as String?)
                        index.let {
                            if (tag_pager.currentItem == it) viewModel.pageSelected(tag_pager.currentItem)
                            else tag_pager.setCurrentItem(it, false)
                        }
                    } else {
                        if (isSizeChanged) {
                            tag_pager.setCurrentItem(tags.size - 1, false)
                        } else {
                            viewModel.pageSelected(tag_pager.currentItem)
                        }
                    }
                }
            }
        })
    }

    private fun observeAlarmStatus() {
        lifecycleScope.launchWhenResumed {
            viewModel.alarmStatusFlow.collect {
                alarmStatus = it
                invalidateOptionsMenu()
            }
        }
    }

    private fun observeSelectedTag() {
        lifecycleScope.launchWhenResumed {
            viewModel.selectedTagFlow.collect { selectedTag ->
                val previousBitmapDrawable = backgrounds[viewModel.tag?.id]
                if (previousBitmapDrawable != null) {
                    tag_background_view.setImageDrawable(previousBitmapDrawable)
                }

                viewModel.tag = selectedTag
                backgrounds[selectedTag?.id].let { bitmapDrawable ->
                    if (bitmapDrawable != null) {
                        imageSwitcher.setImageDrawable(bitmapDrawable)
                    }
                }
            }
        }
    }

    private fun listenToShowGraph() {
        lifecycleScope.launch {
            viewModel.isShowGraphFlow.collect { isShowGraph ->
                if (isShowGraph) {
                    tag_pager.isSwipeEnabled = false
                    pager_title_strip.isTabSwitchEnabled = false
                    if (pager_title_strip.textSpacing != 1000) {
                        val animator = ValueAnimator.ofInt(0, 1000)
                        animator.addUpdateListener {
                            pager_title_strip.textSpacing = animator.animatedValue as Int
                        }
                        animator.start()
                    }
                } else {
                    tag_pager.isSwipeEnabled = true
                    pager_title_strip.isTabSwitchEnabled = true
                    if (pager_title_strip.textSpacing != 0) {
                        val animator = ValueAnimator.ofInt(1000, 0)
                        animator.addUpdateListener {
                            pager_title_strip.textSpacing = animator.animatedValue as Int
                        }
                        animator.start()
                    }
                }
            }
        }
    }

    private fun setupDrawer() {
        val drawerToggle = ActionBarDrawerToggle(
            this, main_drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )

        main_drawerLayout.addDrawerListener(drawerToggle)
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeButtonEnabled(true)
        }
        drawerToggle.syncState()

        val drawerListView = findViewById<ListView>(R.id.navigationDrawer_listView)

        drawerListView.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            resources.getStringArray(R.array.navigation_items_card_view)
        )

        drawerListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, i, _ ->
            main_drawerLayout.closeDrawers()
            when (i) {
                0 -> {
                    val addIntent = Intent(this, AddTagActivity::class.java)
                    startActivity(addIntent)
                }
                1 -> {
                    val settingsIntent = Intent(this, AppSettingsActivity::class.java)
                    startActivity(settingsIntent)
                }
                2 -> {
                    val aboutIntent = Intent(this, AboutActivity::class.java)
                    startActivity(aboutIntent)
                }
                3 -> {
                    val url = "https://ruuvi.com"
                    val webIntent = Intent(Intent.ACTION_VIEW)
                    webIntent.data = Uri.parse(url)
                    startActivity(webIntent)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (!isEmptyList) {
            menuInflater.inflate(R.menu.menu_details, menu)
            val item = menu.findItem(R.id.action_alarm)
            if (viewModel.tag != null) {
                when (alarmStatus ?: -1) {
                    -1 -> {
                        // off
                        item.setIcon(R.drawable.ic_notifications_off_24px)
                        item.icon?.alpha = 128
                    }
                    0 -> {
                        // on
                        item.setIcon(R.drawable.ic_notifications_on_24px)
                        item.icon?.alpha = 128
                    }
                    1 -> {
                        // triggered
                        item.setIcon(R.drawable.ic_notifications_active_24px)
                        val drawable = item.icon
                        if (drawable != null) {
                            drawable.mutate()
                            drawable.alpha = 128
                            val anim = ValueAnimator()
                            anim.setIntValues(1, 0)
                            anim.setEvaluator(IntEvaluator())
                            anim.addUpdateListener {
                                if (it.animatedFraction > 0.9) {
                                    drawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP)
                                } else if (it.animatedFraction < 0.1) {
                                    drawable.setColorFilter(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                                }
                            }

                            anim.repeatMode = ValueAnimator.REVERSE
                            anim.repeatCount = ValueAnimator.INFINITE
                            anim.duration = 500
                            anim.start()
                        }
                    }
                }
                val graphItem = menu.findItem(R.id.action_graph)
                if (adapter.showGraph) {
                    graphItem.setIcon(R.drawable.ic_ruuvi_app_notification_icon_v2)
                } else {
                    graphItem.setIcon(R.drawable.ic_ruuvi_graphs_icon)
                }
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_graph -> {
                if (!tagPagerScrolling) {
                    invalidateOptionsMenu()
                    viewModel.switchShowGraphChannel()
                    val bgScanEnabled = viewModel.preferences.backgroundScanMode
                    if (bgScanEnabled == BackgroundScanModes.DISABLED) {
                        if (viewModel.preferences.isFirstGraphVisit) {
                            val simpleAlert = androidx.appcompat.app.AlertDialog.Builder(this).create()
                            simpleAlert.setTitle(resources.getText(R.string.bg_scan_for_graphs))
                            simpleAlert.setMessage(resources.getText(R.string.enable_background_scanning_question))

                            simpleAlert.setButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE, resources.getText(R.string.yes)) { _, _ ->
                                viewModel.preferences.backgroundScanMode = BackgroundScanModes.BACKGROUND
                            }
                            simpleAlert.setButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE, resources.getText(R.string.no)) { _, _ ->
                            }
                            simpleAlert.setOnDismissListener {
                                viewModel.preferences.isFirstGraphVisit = false
                            }
                            simpleAlert.show()
                        }
                    }
                    adapter.showGraph = !adapter.showGraph
                }
            }
            R.id.action_settings -> {
                if (!tagPagerScrolling) {
                    val intent = Intent(this, TagSettings::class.java)
                    intent.putExtra(TagSettings.TAG_ID, viewModel.tag?.id)
                    this.startActivity(intent)
                }
            }
            android.R.id.home -> {
                finish()
            }
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshTags()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            10 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // party
                    if (openAddView) noTags_textView.callOnClick()
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        starter.requestPermissions()
                    } else {
                        showPermissionSnackbar(this)
                    }
                    Toast.makeText(applicationContext, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showPermissionSnackbar(activity: Activity) {
        val snackbar = Snackbar.make(main_drawerLayout, getString(R.string.location_permission_needed), Snackbar.LENGTH_LONG)
        snackbar.setAction(getString(R.string.settings)) {
            val intent = Intent()
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            val uri = Uri.fromParts("package", activity.packageName, null)
            intent.data = uri
            activity.startActivity(intent)
        }
        snackbar.show()
    }

    private fun setupVisibility(emptyList: Boolean) {
        if (emptyList) {
            pager_title_strip.isInvisible = true
            tag_pager.isInvisible = true
            noTags_textView.isVisible = true
        } else {
            pager_title_strip.isVisible = true
            tag_pager.isVisible = true
            noTags_textView.isInvisible = true
        }
        invalidateOptionsMenu()
    }

    class TagsFragmentPagerAdapter(manager: FragmentManager)
        : FragmentStatePagerAdapter(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        private var tags = listOf<RuuviTagEntity>()
        var showGraph: Boolean = false

        override fun getItem(position: Int): Fragment {
            return TagFragment.newInstance(tags[position])
        }

        override fun getCount(): Int {
            return tags.size
        }

        override fun getPageTitle(position: Int): String {
            return tags[position].displayName?.toUpperCase(Locale.getDefault()).orEmpty()
        }

        override fun getItemPosition(`object`: Any): Int {
            return PagerAdapter.POSITION_NONE
        }

        fun setTags(tags: List<RuuviTagEntity>) {
            this.tags = tags
            notifyDataSetChanged()
        }
    }

    companion object {
        const val FROM_WELCOME = "FROM_WELCOME"
    }
}
