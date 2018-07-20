package com.ruuvi.station.feature

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.TransitionDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
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
    private val FROM_WELCOME = 1447
    private val COARSE_LOCATION_PERMISSION = 1
    private val BACKGROUND_FADE_DURATION = 200

    var backgroundFadeStarted: Long = 0
    var tag: RuuviTag? = null
    lateinit var tags: MutableList<RuuviTag>

    lateinit var handler: Handler
    var openAddView = false
    lateinit var starter: Starter

    val backgrounds = HashMap<String, BitmapDrawable>()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag_details)
        setSupportActionBar(toolbar)

        supportActionBar?.title = null
        supportActionBar?.setIcon(R.drawable.logo_white)

        if (getBoolPref(PreferenceKeys.DASHBOARD_ENABLED_PREF)) {
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
        tag_pager.setOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                tag = tags[position]
                backgrounds[tag!!.id].let { bitmap ->
                    if (bitmap == null) return
                    val transitionDrawable = TransitionDrawable(arrayOf(tag_background_view.drawable, bitmap))
                    tag_background_view.setImageDrawable(transitionDrawable)
                    transitionDrawable.startTransition(BACKGROUND_FADE_DURATION)
                    backgroundFadeStarted = Date().time
                }
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
            COARSE_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // party
                    openAddView = true
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

    fun setBoolPref(pref: String) {
        val editor = PreferenceManager.getDefaultSharedPreferences(this).edit()
        editor.putBoolean(pref, true)
        editor.apply()
    }

    fun getBoolPref(pref: String): Boolean {
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        return settings.getBoolean(pref, false)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            finish()
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
                tag_background_view.setImageDrawable(bitmap)
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
            (tag_pager.adapter as TagPager).updateView(mTag)
            if (tag != null && mTag.id == tag!!.id) {
                tag = mTag
            }
        }
        if (tag == null && tags.isNotEmpty()) tag = tags[0]
        tag?.let {
            (tag_pager.adapter as TagPager).updateView(tag!!)
        }
        if (tags.isEmpty()) {
            pager_title_strip.visibility = View.INVISIBLE
            noTags_textView.visibility = View.VISIBLE
            this.invalidateOptionsMenu()
        } else {
            pager_title_strip.visibility = View.VISIBLE
            noTags_textView.visibility = View.INVISIBLE
            this.invalidateOptionsMenu()
        }
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
                item.isVisible = status > -1
                if (status == 0) {
                    item.setIcon(R.drawable.ic_notifications_off_24px)
                    val drawable = item.icon
                    if(drawable != null) {
                        drawable.mutate()
                        drawable.setColorFilter(resources.getColor(R.color.white), PorterDuff.Mode.SRC_ATOP)
                    }
                } else if (status == 1) {
                    item.setIcon(R.drawable.ic_notifications_active_24px)
                    val drawable = item.icon
                    if(drawable != null) {
                        drawable.mutate()
                        drawable.setColorFilter(resources.getColor(R.color.activeAlarm), PorterDuff.Mode.SRC_ATOP)
                        try {
                            val anim = ValueAnimator()
                            anim.setIntValues(Color.WHITE, Color.RED)
                            anim.setEvaluator(ArgbEvaluator());
                            anim.addUpdateListener {
                                drawable.setColorFilter(it.animatedValue as Int, PorterDuff.Mode.SRC_ATOP)
                            }
                            anim.duration = 300
                            anim.start()
                        } catch (e: Exception) {

                        }
                    }
                }
            }
        }
        return true
    }
}

class TagPager constructor(tags: List<RuuviTag>, context: Context, view: View) : PagerAdapter() {
    val VIEW_TAG = "DetailedTag"
    var tags = tags
    val context = context
    val view = view

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = LayoutInflater.from(context).inflate(R.layout.view_tag_detail, container, false)
        view.tag = VIEW_TAG + position
        (container as ViewPager).addView(view, 0)
        updateView(tags[position])
        return view
    }

    fun updateView(tag: RuuviTag) {
        var pos = -1
        for ((index, aTag) in tags.withIndex()) {
            if (tag.id.equals(aTag.id)) {
                pos = index
                break
            }
        }
        if (pos == -1) return

        val rootView = view.findViewWithTag<View>(VIEW_TAG + pos) ?: return

        val tag_temp = rootView.findViewById<TextView>(R.id.tag_temp)
        val tag_humidity = rootView.findViewById<TextView>(R.id.tag_humidity)
        val tag_pressure = rootView.findViewById<TextView>(R.id.tag_pressure)
        val tag_signal = rootView.findViewById<TextView>(R.id.tag_signal)
        val tag_updated = rootView.findViewById<TextView>(R.id.tag_updated)
        val tag_temp_unit = rootView.findViewById<TextView>(R.id.tag_temp_unit)

        var temperature = tag?.getTemperatureString(context)
        val unit = temperature.substring(temperature.length - 2, temperature.length)
        temperature = temperature.substring(0, temperature.length - 2)

        val unitSpan = SpannableString(unit)
        unitSpan.setSpan(SuperscriptSpan(), 0, unit.length, 0)

        tag_temp_unit.text = unitSpan
        tag_temp.text = temperature
        tag_humidity.text = String.format(context.getString(R.string.humidity_reading), tag?.humidity)
        tag_pressure.text = String.format(context.getString(R.string.pressure_reading), tag?.pressure)
        tag_signal.text = String.format(context.getString(R.string.signal_reading), tag?.rssi)
        var updatedAt = context.resources.getString(R.string.updated) + " " + Utils.strDescribingTimeSince(tag?.updateAt);
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
