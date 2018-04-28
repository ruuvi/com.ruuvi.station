package com.ruuvi.station.feature

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.graphics.drawable.TransitionDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.design.widget.BottomSheetDialog
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.app.AppCompatActivity
import com.ruuvi.station.R
import com.ruuvi.station.model.RuuviTag
import com.ruuvi.station.scanning.RuuviTagListener
import com.ruuvi.station.scanning.RuuviTagScanner
import com.ruuvi.station.util.Utils

import kotlinx.android.synthetic.main.activity_tag_details.*
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.view.*
import android.support.v4.view.ViewPager.OnPageChangeListener
import android.support.v7.app.ActionBarDrawerToggle
import android.widget.*
import kotlinx.android.synthetic.main.content_tag_details.*
import android.text.SpannableString
import android.text.style.SuperscriptSpan
import android.util.Log
import com.ruuvi.station.feature.main.MainActivity
import com.ruuvi.station.feature.main.MainActivity.isBluetoothEnabled
import com.ruuvi.station.feature.main.MainActivity.setBackgroundScanning
import com.ruuvi.station.service.ScannerService
import com.ruuvi.station.util.DeviceIdentifier
import com.ruuvi.station.util.PreferenceKeys.FIRST_START_PREF
import java.util.ArrayList


class TagDetails : AppCompatActivity() {
    private val TAG = "TagDetails"
    private val REQUEST_ENABLE_BT = 1337
    private val FROM_WELCOME = 1447
    private val COARSE_LOCATION_PERMISSION = 1

    var tag: RuuviTag? = null
    lateinit var tags: MutableList<RuuviTag>

    lateinit var handler: Handler
    var openAddView = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag_details)
        setSupportActionBar(toolbar)

        supportActionBar?.title = null
        supportActionBar?.setIcon(R.drawable.logo_white)

        DeviceIdentifier.id(this)

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

        noTags_textView.setOnClickListener {
            val addIntent = Intent(this, AddTagActivity::class.java)
            startActivity(addIntent)
        }

        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        //tag_pager.pageMargin = - (size.x / 2)
        tag_pager.setOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                tag = tags!!.get(position)
                Utils.getDefaultBackground(tag!!.defaultBackground, applicationContext).let { background ->
                    val transitionDrawable = TransitionDrawable(arrayOf(tag_background_view.drawable,background))
                    tag_background_view.setImageDrawable(transitionDrawable)
                    transitionDrawable.startTransition(500)
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

        if (tag != null) {
            Utils.getDefaultBackground(tag!!.defaultBackground, applicationContext).let { background ->
                tag_background_view.setImageDrawable(background)
            }
        }

        handler = Handler()

        if (!getBoolPref(FIRST_START_PREF)) {
            val intent = Intent(this, WelcomeActivity::class.java)
            startActivityForResult(intent, FROM_WELCOME)
        } else {
            getThingsStarted(false)
        }
    }

    private fun getThingsStarted(goToAddTags: Boolean) {
        if (isBluetoothEnabled()) {
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
        setBoolPref(FIRST_START_PREF)
        setBackgroundScanning(false, this, PreferenceManager.getDefaultSharedPreferences(this))
        requestPermissions()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            COARSE_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // party
                    openAddView = true
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                        requestPermissions()
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

    private fun getNeededPermissions(): List<String> {
        val permissionCoarseLocation = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)

        val listPermissionsNeeded = ArrayList<String>()

        if (permissionCoarseLocation != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        return listPermissionsNeeded
    }

    private fun showPermissionDialog(activity: AppCompatActivity): Boolean {
        val listPermissionsNeeded = getNeededPermissions()

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(activity, listPermissionsNeeded.toTypedArray(), COARSE_LOCATION_PERMISSION)
        }

        return !listPermissionsNeeded.isEmpty()
    }

    private fun requestPermissions() {
        if (getNeededPermissions().isNotEmpty()) {
            val activity = this
            val alertDialog = android.support.v7.app.AlertDialog.Builder(this@TagDetails).create()
            alertDialog.setTitle(getString(R.string.permission_dialog_title))
            alertDialog.setMessage(getString(R.string.permission_dialog_request_message))
            alertDialog.setButton(android.support.v7.app.AlertDialog.BUTTON_NEUTRAL, getString(R.string.ok)
            ) { dialog, which -> dialog.dismiss() }
            alertDialog.setOnDismissListener { showPermissionDialog(activity) }
            alertDialog.show()
        }
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
        (tag_pager.adapter as TagPager).tags = tags!!
        tag_pager.adapter?.notifyDataSetChanged()
        if (tags.isNotEmpty()) {
            Utils.getDefaultBackground(tags.get(tag_pager.currentItem).defaultBackground, applicationContext).let { background ->
                tag_background_view.setImageDrawable(background)
            }
        }

        if (getNeededPermissions().isEmpty()) {
            refrshTagLists()
            handler.post(object: Runnable {
                override fun run() {
                    updateUI()
                    handler.postDelayed(this, 1000)
                }
            })

            if (isBluetoothEnabled()) {
                val scannerService = Intent(this, ScannerService::class.java)
                startService(scannerService)
                if (!MainActivity.isLocationEnabled(this)) {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle(this.getString(R.string.locationServices))
                    builder.setMessage(this.getString(R.string.enableLocationServices))
                    builder.setPositiveButton(android.R.string.ok) { dialogInterface, i ->
                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        startActivity(intent)
                    }
                    builder.setNegativeButton(android.R.string.cancel) { dialogInterface, i -> }
                    builder.show()
                } else {
                    if (openAddView) {
                        openAddView = false
                        val addIntent = Intent(this, AddTagActivity::class.java)
                        startActivity(addIntent)
                    }
                }
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
        } else {
            if (requestCode == FROM_WELCOME) {
                getThingsStarted(true)
            }
        }
    }

    fun updateUI() {
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
        var listView = ListView(this)
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

        val rootView = view.findViewWithTag<View>(VIEW_TAG + pos)
        if (rootView == null) return;

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


