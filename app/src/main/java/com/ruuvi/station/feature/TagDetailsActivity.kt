package com.ruuvi.station.feature

import android.Manifest
import android.animation.IntEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Point
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v4.view.ViewPager.OnPageChangeListener
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.AppCompatImageView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.ruuvi.station.R
import com.ruuvi.station.alarm.AlarmChecker
import com.ruuvi.station.database.RuuviTagRepository
import com.ruuvi.station.settings.ui.AppSettingsActivity
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.app.preferences.Preferences
import com.ruuvi.station.util.*
import kotlinx.android.synthetic.main.activity_tag_details.background_fader
import kotlinx.android.synthetic.main.activity_tag_details.imageSwitcher
import kotlinx.android.synthetic.main.activity_tag_details.main_drawerLayout
import kotlinx.android.synthetic.main.activity_tag_details.tag_background_view
import kotlinx.android.synthetic.main.activity_tag_details.toolbar
import kotlinx.android.synthetic.main.content_tag_details.noTags_textView
import kotlinx.android.synthetic.main.content_tag_details.pager_title_strip
import kotlinx.android.synthetic.main.content_tag_details.tag_pager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import timber.log.Timber
import java.util.Date
import java.util.HashMap
import kotlin.collections.set

class TagDetailsActivity : AppCompatActivity(), KodeinAware {
    override val kodein by closestKodein()
    private val preferences: Preferences by instance()
    private val BACKGROUND_FADE_DURATION = 200

    companion object {
        val FROM_WELCOME = "FROM_WELCOME"
    }

    var backgroundFadeStarted: Long = 0
    var tag: RuuviTagEntity? = null
    lateinit var tags: MutableList<RuuviTagEntity>
    var alarmStatus = HashMap<String, Int>()
    private val uiScope = CoroutineScope(Dispatchers.Main)
    lateinit var handler: Handler
    private var openAddView = false
    lateinit var starter: Starter
    private var lastSelectedTag = 0
    private lateinit var pagerAdapter: TagDetailsPagerAdapter

    val backgrounds = HashMap<String, BitmapDrawable>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag_details)
        setSupportActionBar(toolbar)

        supportActionBar?.title = null
        supportActionBar?.setIcon(R.drawable.logo_white)

        starter = Starter(this)

        if (preferences.dashboardEnabled) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            main_drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        } else {
            setupDrawer()
        }

        noTags_textView.setOnClickListener {
            startActivity(Intent(this, AddTagActivity::class.java))
        }

        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        var prevTagId = ""
        tag_pager.addOnPageChangeListener (object : OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                lastSelectedTag = position
                tag = tags[position]
                if (!prevTagId.isEmpty()) {
                    backgrounds[prevTagId].let { bitmapDrawable ->
                        if (bitmapDrawable == null) return
                        tag_background_view.setImageDrawable(bitmapDrawable)
                    }
                }
                backgrounds[tag!!.id].let { bitmapDrawable ->
                    if (bitmapDrawable == null) return
                    imageSwitcher.setImageDrawable(bitmapDrawable)
                    backgroundFadeStarted = Date().time
                }
                invalidateOptionsMenu()
                prevTagId = tag?.id.orEmpty()
            }
        })

        imageSwitcher.setFactory {
            val im = AppCompatImageView(applicationContext)
            im.scaleType = ImageView.ScaleType.CENTER_CROP
            im.layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT)
            im
        }

        val tagId = intent.getStringExtra("id")
        tags = ArrayList(RuuviTagRepository.getAll(true))
        pagerAdapter = TagDetailsPagerAdapter(tags, applicationContext, tag_pager)
        tag_pager.adapter = pagerAdapter
        tag_pager.offscreenPageLimit = 1

        for (i in tags.indices) {
            if (tags[i].id == tagId) {
                tag = tags[i]
                tag_pager.currentItem = i
                break
            }
        }

        if (tag == null && tags.isNotEmpty()) {
            tag = tags[0]
            tag_pager.currentItem = 0
        }

        try {
            for (i in 0..(pager_title_strip.childCount - 1)) {
                val child = pager_title_strip.getChildAt(i)
                if (child is TextView) {
                    child.typeface = ResourcesCompat.getFont(applicationContext, R.font.montserrat_bold)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to set pager font")
        }

        handler = Handler()
        openAddView = intent.getBooleanExtra(FROM_WELCOME, false)
        if (openAddView) {
            val addIntent = Intent(this, AddTagActivity::class.java)
            intent.putExtra(FROM_WELCOME, false)
            startActivity(addIntent)
            return
        }
        starter.getThingsStarted()
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

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_graph -> {
                pagerAdapter.showGraph = !pagerAdapter.showGraph
                updateUI()
                invalidateOptionsMenu()

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
                            Toast.makeText(applicationContext, resources.getText(R.string.bg_scan_for_graphs), Toast.LENGTH_LONG).show()
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

    private fun refreshTagLists() {
        tags.clear()
        tags.addAll(RuuviTagRepository.getAll(true))
        updateUI()
    }

    override fun onResume() {
        super.onResume()

        val newTagsList = RuuviTagRepository.getAll(true)
        if (newTagsList.size != tags.size) {
            lastSelectedTag = newTagsList.size - 1
        }
        tags = newTagsList

        var tagRemoved = true
        for (tag in tags) {
            tag.id?.let { tagId ->
                Utils.getBackground(applicationContext, tag).let { bitmap ->
                    backgrounds.put(tagId, BitmapDrawable(applicationContext.resources, bitmap))
                }
                if (this.tag?.id == tagId) tagRemoved = false
            }
        }
        if (tag != null && tagRemoved) {
            if (tags.size > 0) {
                intent.putExtra("id", tags[tags.size - 1].id)
            }
            finish()
            startActivity(intent)
            return
        }
        pagerAdapter.tags = tags
        pagerAdapter.notifyDataSetChanged()

        tag_pager.currentItem = lastSelectedTag

        if (tags.isNotEmpty()) {
            backgrounds[tags[tag_pager.currentItem].id].let { bitmap ->
                imageSwitcher.setImageDrawable(bitmap)
            }
        }

        if (starter.getNeededPermissions().isEmpty()) {
            refreshTagLists()
            handler.post(object : Runnable {
                override fun run() {
                    updateUI()
                    handler.postDelayed(this, 1000)
                }
            })

            if (starter.checkBluetooth()) {
                starter.startScanning()
            }
        } else {
            updateUI()
        }
        invalidateOptionsMenu()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacksAndMessages(null)
        for (tag in tags) {
            RuuviTagRepository.update(tag)
        }
    }

    fun updateUI() {
        val now = Date().time
        if (backgroundFadeStarted + BACKGROUND_FADE_DURATION > now) {
            // do not update ui while the background is animating
            // maybe this would not be needed if the db call below was async
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            tags = ArrayList(RuuviTagRepository.getAll(true))
            uiScope.launch {
                for (mTag in tags) {
                    if (tag != null && mTag.id == tag?.id) {
                        tag = mTag
                    } else {
                        pagerAdapter.updateView(mTag)
                    }
                }
                if (pagerAdapter.showGraph && background_fader.alpha == 0f) {
                    background_fader.animate().alpha(0.5f).start()
                } else if (!pagerAdapter.showGraph && background_fader.alpha != 0f) {
                    background_fader.animate().alpha(0f).start()
                }
                if (tag == null && tags.isNotEmpty()) tag = tags[0]
                tag?.let {
                    pagerAdapter.updateView(it)
                    it.id?.let { tagId ->
                        if (alarmStatus.containsKey(tagId)) {
                            val newStatus = AlarmChecker.getStatus(it)
                            if (alarmStatus[tagId] != newStatus) {
                                alarmStatus[tagId] = AlarmChecker.getStatus(it)
                                invalidateOptionsMenu()
                            }
                        } else {
                            alarmStatus[tagId] = AlarmChecker.getStatus(it)
                        }
                    }
                }
                if (tags.isEmpty()) {
                    pager_title_strip.visibility = View.INVISIBLE
                    noTags_textView.visibility = View.VISIBLE
                } else {
                    pager_title_strip.visibility = View.VISIBLE
                    noTags_textView.visibility = View.INVISIBLE
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (tags.isNotEmpty()) {
            menuInflater.inflate(R.menu.menu_details, menu)
            val item = menu.findItem(R.id.action_alarm)
            if (tag != null) {
                val status = AlarmChecker.getStatus(tag)
                when (status) {
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
                if (pagerAdapter.showGraph) {
                    graphItem.setIcon(R.drawable.ic_ruuvi_app_notification_icon_v2)
                } else {
                    graphItem.setIcon(R.drawable.ic_ruuvi_graphs_icon)
                }
            }
        }
        return true
    }
}
