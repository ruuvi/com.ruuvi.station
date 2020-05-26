package com.ruuvi.station.tagdetails.ui

import android.Manifest
import android.animation.IntEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.*
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.AppCompatImageView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import com.ruuvi.station.R
import com.ruuvi.station.app.preferences.Preferences
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
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import java.util.*

class TagDetailsActivity : AppCompatActivity(), KodeinAware {
    override val kodein: Kodein by closestKodein()
    private val viewModeFactory: ViewModelProvider.Factory by instance()
    private val viewModel: TagDetailsViewModel by lazy {
        ViewModelProviders.of(this, viewModeFactory).get(TagDetailsViewModel::class.java)
    }
    private val preferences: Preferences by instance()
    lateinit var adapter: TagsFragmentPagerAdapter
    var tag: RuuviTagEntity? = null
    var emptyList = true
    var alarmStatus: Int? = null
    private val backgrounds = HashMap<String, BitmapDrawable>()
    var backgroundFadeStarted: Long = 0
    lateinit var starter: Starter
    private var openAddView = false
    private var desiredTag : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag_details)
        starter = Starter(this)
        setupViewModel()
        setupUI()

        desiredTag = intent.getStringExtra("id")
        openAddView = intent.getBooleanExtra(FROM_WELCOME, false)
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

        noTags_textView.setOnClickListener {
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

        adapter = TagsFragmentPagerAdapter(supportFragmentManager)
        tag_pager.adapter = adapter
        tag_pager.offscreenPageLimit = 1
        tag_pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(p0: Int) {}

            override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {}

            override fun onPageSelected(p0: Int) {
                viewModel.pageSelected(p0)
            }
        })
    }

    private fun setupViewModel() {
        viewModel.observeTags().observe(this, Observer { tags ->
            val previousTagsSize = adapter.count
            var currentSize = 0
            emptyList = tags.isNullOrEmpty()
            tags?.let {
                adapter.setTags(tags)

                tags.forEach {
                    it.id?.let { tagId ->
                        Utils.getBackground(applicationContext, it).let { bitmap ->
                            backgrounds[tagId] = BitmapDrawable(applicationContext.resources, bitmap)
                        }
                    }
                }
                currentSize = tags.size
            }

            val sizeChanged = previousTagsSize > 0 && tags?.size != previousTagsSize
            setupVisibility(emptyList)

            if (tags?.size ?: 0 > 0) {
                if (!desiredTag.isNullOrEmpty()) {
                    var index = tags?.indexOfFirst { t -> t.id == desiredTag }
                    index?.let {
                        tag_pager.currentItem = index
                    }
                    desiredTag = null
                } else {
                    if (sizeChanged) {
                        tag_pager.currentItem = currentSize - 1
                    } else {
                        viewModel.pageSelected(tag_pager.currentItem)
                    }
                }
            }
        })

        viewModel.observeSelectedTag().observe(this, Observer { selectedTag ->
            val previousBitmapDrawable = backgrounds[tag?.id]
            if (previousBitmapDrawable != null) tag_background_view.setImageDrawable(previousBitmapDrawable)

            tag = selectedTag
            backgrounds[selectedTag?.id].let { bitmapDrawable ->
                if (bitmapDrawable != null) {
                    imageSwitcher.setImageDrawable(bitmapDrawable)
                    backgroundFadeStarted = Date().time
                }
            }
        })

        viewModel.observeAlarmStatus().observe(this, Observer { alarmStatus ->
            this.alarmStatus = alarmStatus
            invalidateOptionsMenu()
        })
    }

    fun setupDrawer() {
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
        if (!emptyList) {
            menuInflater.inflate(R.menu.menu_details, menu)
            val item = menu.findItem(R.id.action_alarm)
            if (tag != null) {
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
                invalidateOptionsMenu()
                viewModel.switchGraphVisibility()
                val bgScanEnabled = preferences.backgroundScanMode
                if (bgScanEnabled == BackgroundScanModes.DISABLED) {
                    if (preferences.isFirstGraphVisit) {
                        val simpleAlert = android.support.v7.app.AlertDialog.Builder(this).create()
                        simpleAlert.setTitle(resources.getText(R.string.bg_scan_for_graphs))
                        simpleAlert.setMessage(resources.getText(R.string.enable_background_scanning_question))

                        simpleAlert.setButton(android.support.v7.app.AlertDialog.BUTTON_POSITIVE, resources.getText(R.string.yes)) { _, _ ->
                            preferences.backgroundScanMode = BackgroundScanModes.BACKGROUND
                        }
                        simpleAlert.setButton(android.support.v7.app.AlertDialog.BUTTON_NEGATIVE, resources.getText(R.string.no)) { _, _ ->
                        }
                        simpleAlert.setOnDismissListener {
                            preferences.isFirstGraphVisit = false
                        }
                        simpleAlert.show()
                    }
                }
            }
            R.id.action_settings -> {
                val intent = Intent(this, TagSettings::class.java)
                intent.putExtra(TagSettings.TAG_ID, tag?.id)
                this.startActivity(intent)
            }
            android.R.id.home -> {
                finish()
            }
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshData()
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
            pager_title_strip.visibility = View.INVISIBLE
            noTags_textView.visibility = View.VISIBLE
        } else {
            pager_title_strip.visibility = View.VISIBLE
            noTags_textView.visibility = View.INVISIBLE
        }
    }

    class TagsFragmentPagerAdapter(fm: FragmentManager?) : FragmentStatePagerAdapter(fm) {
        private var tags = listOf<RuuviTagEntity>()
        var showGraph: Boolean  = false

        override fun getItem(position: Int): Fragment {
            return TagFragment.newInstance(tags[position])
        }

        override fun getCount(): Int {
            return tags.size
        }

        override fun getPageTitle(position: Int): String {
            return tags.get(position).displayName?.toUpperCase().orEmpty()
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
        val FROM_WELCOME = "FROM_WELCOME"
    }
}
