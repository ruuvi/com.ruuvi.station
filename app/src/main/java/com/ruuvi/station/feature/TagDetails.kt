package com.ruuvi.station.feature

import android.Manifest
import android.animation.IntEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
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
import android.support.design.widget.BottomSheetDialog
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.app.AppCompatActivity
import com.ruuvi.station.R
import com.ruuvi.station.model.RuuviTag

import kotlinx.android.synthetic.main.activity_tag_details.*
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.view.*
import android.support.v4.view.ViewPager.OnPageChangeListener
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.AppCompatImageView
import android.widget.*
import kotlinx.android.synthetic.main.content_tag_details.*
import android.text.SpannableString
import android.text.style.SuperscriptSpan
import android.util.Log
import com.ruuvi.station.util.*
import java.util.*


class TagDetails : AppCompatActivity() {
    private val TAG = "TagDetails"
    private val REQUEST_ENABLE_BT = 1337
    private val BACKGROUND_FADE_DURATION = 200
    companion object {
        val FROM_WELCOME = "FROM_WELCOME"
    }

    var backgroundFadeStarted: Long = 0
    var tag: RuuviTag? = null
    lateinit var tags: MutableList<RuuviTag>
    var alarmStatus = HashMap<String, Int>()

    lateinit var handler: Handler
    private var openAddView = false
    lateinit var starter: Starter
    private var showGraph = false
    private var updateGraph = false

    val backgrounds = HashMap<String, BitmapDrawable>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag_details)
        setSupportActionBar(toolbar)

        supportActionBar?.title = null
        supportActionBar?.setIcon(R.drawable.logo_white)

        if (Preferences(this).dashboardEnabled) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            main_drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        } else {
            setupDrawer()
        }

        noTags_textView.setOnClickListener {
            val addIntent = Intent(this, AddTagActivity::class.java)
            startActivity(addIntent)
        }

        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        var prevTagId = ""
        tag_pager.setOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
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
                prevTagId = tag!!.id
            }
        })

        imageSwitcher.setFactory(object : ViewSwitcher.ViewFactory {
            override fun makeView(): View {
                val im = AppCompatImageView(applicationContext)
                im.scaleType = ImageView.ScaleType.CENTER_CROP
                im.layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT)
                return im
            }
        })

        val tagId = intent.getStringExtra("id");
        tags = RuuviTag.getAll(true)
        val pagerAdapter = TagPager(tags, applicationContext, tag_pager)
        tag_pager.adapter = pagerAdapter
        tag_pager.offscreenPageLimit = 100

        for (i in tags.indices) {
            if (tags[i].id == tagId) {
                tag = tags[i]
                tag_pager.currentItem = i
            }
        }

        if (tag == null && tags.isNotEmpty()) {
            tag = tags[0]
            tag_pager.currentItem = 0
        }

        try {
            for (i in 0..(pager_title_strip.childCount-1)) {
                val child = pager_title_strip.getChildAt(i)
                if (child is TextView) {
                    child.typeface = ResourcesCompat.getFont(applicationContext, R.font.montserrat_bold)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set pager font")
        }

        openAddView = intent.getBooleanExtra(FROM_WELCOME, false)

        handler = Handler()
        starter = Starter(this)
        starter.getThingsStarted()
    }

    fun setupDrawer() {
        val drawerToggle = ActionBarDrawerToggle(
                this, main_drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )

        main_drawerLayout.addDrawerListener(drawerToggle)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeButtonEnabled(true)
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
                1 -> {
                    val settingsIntent = Intent(this, AppSettingsActivity::class.java)
                    startActivity(settingsIntent)
                }
                2 -> {
                    val aboutIntent = Intent(this, AboutActivity::class.java)
                    startActivity(aboutIntent)
                }
                else -> {
                    val addIntent = Intent(this, AddTagActivity::class.java)
                    startActivity(addIntent)
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
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
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
        if (item?.itemId == android.R.id.home) {
            finish()
        } else if (item?.title == "graphs") {
            showGraph = !showGraph
            updateUI()
            invalidateOptionsMenu()
        } else {
            showOptionsMenu()
        }
        return true
    }

    private fun refrshTagLists() {
        tags.clear()
        tags.addAll(RuuviTag.getAll(true))
        updateUI()
    }


    override fun onResume() {
        super.onResume()
        updateGraph = true
        tags = RuuviTag.getAll(true)

        for (tag in tags) {
            Utils.getBackground(applicationContext, tag).let { bitmap ->
                backgrounds.put(tag.id, BitmapDrawable(applicationContext.resources, bitmap))
            }
        }
        (tag_pager.adapter as TagPager).tags = tags
        tag_pager.adapter?.notifyDataSetChanged()
        if (tags.isNotEmpty()) {
            backgrounds[tags.get(tag_pager.currentItem).id].let { bitmap ->
                imageSwitcher.setImageDrawable(bitmap)
            }
        }

        if (starter.getNeededPermissions().isEmpty()) {
            refrshTagLists()
            handler.post(object: Runnable {
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
            tag.update()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_ENABLE_BT) {
            }

        }
    }

    fun updateUI() {
        val now = Date().time
        if (backgroundFadeStarted + BACKGROUND_FADE_DURATION > now) {
            // do not update ui while the background is animating
            // maybe this would not be needed if the db call below was async
            return
        }
        tags = RuuviTag.getAll(true)
        for (mTag in tags) {
            (tag_pager.adapter as TagPager).updateView(mTag, showGraph, updateGraph)
            if (tag != null && mTag.id == tag!!.id) {
                tag = mTag
            }
        }
        if (tag == null && tags.isNotEmpty()) tag = tags[0]
        tag?.let {
            (tag_pager.adapter as TagPager).updateView(it, showGraph, updateGraph)
            if (alarmStatus.containsKey(it.id)) {
                val newStatus = AlarmChecker.getStatus(it)
                if (alarmStatus[it.id] != newStatus) {
                    alarmStatus[it.id] = AlarmChecker.getStatus(it)
                    this.invalidateOptionsMenu()
                }
            } else {
                alarmStatus[it.id] = AlarmChecker.getStatus(it)
            }
        }
        if (tags.isEmpty()) {
            pager_title_strip.visibility = View.INVISIBLE
            noTags_textView.visibility = View.VISIBLE
        } else {
            pager_title_strip.visibility = View.VISIBLE
            noTags_textView.visibility = View.INVISIBLE
        }
        updateGraph = false
    }

    private fun showOptionsMenu() {
        val sheetDialog = BottomSheetDialog(this)
        val listView = ListView(this)
        val menu: List<String> = this.resources.getStringArray(R.array.station_tag_menu).toList()

        listView.adapter = ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                menu
        )

        listView.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, i, l ->
            when (i) {
                0 -> {
                    val intent = Intent(this, GraphActivity::class.java)
                    intent.putExtra(GraphActivity.TAGID, tag?.id)
                    this.startActivity(intent)
                }
                1 -> {
                    val intent = Intent(this, TagSettings::class.java)
                    intent.putExtra(TagSettings.TAG_ID, tag?.id)
                    this.startActivity(intent)
                }
                2 -> {
                    delete()
                }
            }

            sheetDialog.dismiss()
        }

        sheetDialog.setContentView(listView)
        sheetDialog.show()
    }

    fun delete() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(this.getString(R.string.tag_delete_title))
        builder.setMessage(this.getString(R.string.tag_delete_message))
        builder.setPositiveButton(android.R.string.ok) { dialogInterface, i ->
            tag?.deleteTagAndRelatives()
            val intent = intent
            finish()
            startActivity(intent)
        }
        builder.setNegativeButton(android.R.string.cancel) { dialogInterface, i -> }

        builder.show()
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
                if (showGraph) {
                    graphItem.setIcon(R.drawable.ic_ruuvi_app_notification_icon_v2)
                } else {
                    graphItem.setIcon(R.drawable.ic_ruuvi_graphs_icon)
                }
            }
        }
        return true
    }
}

class TagPager constructor(var tags: List<RuuviTag>, val context: Context, val view: View) : PagerAdapter() {
    val VIEW_TAG = "DetailedTag"

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = LayoutInflater.from(context).inflate(R.layout.view_tag_detail, container, false)
        view.tag = VIEW_TAG + position
        (container as ViewPager).addView(view, 0)
        updateView(tags[position], false, false)
        return view
    }

    fun updateView(tag: RuuviTag, showGraph: Boolean, updateGraph: Boolean) {
        var pos = -1
        for ((index, aTag) in tags.withIndex()) {
            if (tag.id.equals(aTag.id)) {
                pos = index
                break
            }
        }
        if (pos == -1) return

        val rootView = view.findViewWithTag<View>(VIEW_TAG + pos) ?: return

        val graph = rootView.findViewById<View>(R.id.tag_graphs)
        val container = rootView.findViewById<View>(R.id.tag_container)
        if (showGraph && graph.visibility == View.INVISIBLE || showGraph && updateGraph) {
            graph.visibility = View.VISIBLE
            container.visibility = View.INVISIBLE
            GraphView(context).drawChart(tag.id, rootView)
        } else if (!showGraph && graph.visibility == View.VISIBLE) {
            graph.visibility = View.INVISIBLE
            container.visibility = View.VISIBLE
        }

        val tag_temp = rootView.findViewById<TextView>(R.id.tag_temp)
        val tag_humidity = rootView.findViewById<TextView>(R.id.tag_humidity)
        val tag_pressure = rootView.findViewById<TextView>(R.id.tag_pressure)
        val tag_signal = rootView.findViewById<TextView>(R.id.tag_signal)
        val tag_updated = rootView.findViewById<TextView>(R.id.tag_updated)
        val tag_temp_unit = rootView.findViewById<TextView>(R.id.tag_temp_unit)

        var temperature = tag.getTemperatureString(context)
        val unit = temperature.substring(temperature.length - 2, temperature.length)
        temperature = temperature.substring(0, temperature.length - 2)

        val unitSpan = SpannableString(unit)
        unitSpan.setSpan(SuperscriptSpan(), 0, unit.length, 0)

        tag_temp_unit.text = unitSpan
        tag_temp.text = temperature
        tag_humidity.text = String.format(context.getString(R.string.humidity_reading), tag.humidity)
        tag_pressure.text = String.format(context.getString(R.string.pressure_reading), tag.pressure)
        tag_signal.text = String.format(context.getString(R.string.signal_reading), tag.rssi)
        val updatedAt = context.resources.getString(R.string.updated) + " " + Utils.strDescribingTimeSince(tag.updateAt)
        tag_updated.text = updatedAt
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun getPageTitle(position: Int): CharSequence {
        return tags.get(position).dispayName.toUpperCase()
    }

    override fun getCount(): Int {
        return tags.size
    }
}
