package com.ruuvi.station.feature

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.BottomSheetDialog
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
import android.widget.*
import kotlinx.android.synthetic.main.content_tag_details.*
import android.text.style.RelativeSizeSpan
import android.text.SpannableString
import android.text.style.SuperscriptSpan
import com.ruuvi.station.util.CustomTypefaceSpan


class TagDetails : AppCompatActivity(), RuuviTagListener {
    var tag: RuuviTag? = null
    var tags: List<RuuviTag>? = null

    var scanner: RuuviTagScanner? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag_details)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = null
        supportActionBar?.setIcon(R.drawable.logo_white)

        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        //tag_pager.pageMargin = - (size.x / 2)
        tag_pager.setOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                tag = tags!!.get(position)
                updateUI()
            }
        })

        val tagId = intent.getStringExtra("id");
        tags = RuuviTag.getAll()
        val pagerAdapter = TagPager(tags!!, applicationContext, tag_pager)
        tag_pager.adapter = pagerAdapter
        tag_pager.offscreenPageLimit = 100

        for (i in tags!!.indices) {
            if (tags!!.get(i).id == tagId) {
                tag = tags!!.get(i)
                tag_pager.currentItem = i
            }
        }

        if (tag == null) {
            Toast.makeText(this, "Something went wrong..", Toast.LENGTH_SHORT).show()
            finish()
        }

        val handler = Handler()
        handler.post(object: Runnable {
            override fun run() {
                updateUI()
                handler.postDelayed(this, 1000)
            }
        })

        scanner = RuuviTagScanner(this, this)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            finish()
        } else {
            showOptionsMenu()
        }
        /*else if (item?.itemId == R.id.action_share) {
            if (tag?.url != null) {
                if (!tag!!.url.isEmpty()) {
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "text/plain"
                    shareIntent.putExtra(Intent.EXTRA_TEXT, tag!!.url!!)
                    this.startActivity(shareIntent)
                    return true;
                }
            }
            Toast.makeText(this,
                    "You can only share tags in weather station mode right now"
                    , Toast.LENGTH_SHORT).show()
        } */
        return true
    }

    override fun onResume() {
        super.onResume()
        (tag_pager.adapter as TagPager).tags = RuuviTag.getAll()
        tag_pager.adapter.notifyDataSetChanged()
        scanner?.start()
    }

    override fun onPause() {
        super.onPause()
        scanner?.stop()
    }

    override fun tagFound(tag: RuuviTag) {
        if (tags == null) return;
        for (mTag in tags!!) {
            if (mTag.id == tag.id) {
                mTag.updateDataFrom(tag)
                mTag.update()
            }
        }
    }

    fun updateUI() {
        for (mTag in tags!!) {
            (tag_pager.adapter as TagPager).updateView(mTag)
            if (mTag.id == tag!!.id) {
                tag = mTag
            }
        }
        tag?.let {
            val temperature = SpannableString(String.format(this.getString(R.string.temperature_reading), tag?.temperature) + "C")
            temperature.setSpan(CustomTypefaceSpan(dummyTextView.typeface), temperature.length - 2, temperature.length, 0)
            temperature.setSpan(RelativeSizeSpan(0.6f), temperature.length - 2, temperature.length, 0)
            temperature.setSpan(SuperscriptSpan(), temperature.length - 2, temperature.length, 0)
            (tag_pager.adapter as TagPager).updateView(tag!!)
            /*
            tag_temp.text = temperature
       ¨åpo     tag_humidity.text = String.format(this.getString(R.string.humidity_reading), tag?.humidity)
            tag_pressure.text = String.format(this.getString(R.string.pressure_reading), tag?.pressure)
            tag_signal.text = String.format(this.getString(R.string.signal_reading), tag?.rssi)
            var updatedAt = this.resources.getString(R.string.updated) + " " + Utils.strDescribingTimeSince(tag?.updateAt);
            tag_updated.text = updatedAt
            */
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
            finish()
            tag?.deleteTagAndRelatives()
        }
        builder.setNegativeButton(android.R.string.cancel) { dialogInterface, i -> }

        builder.show()
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_details, menu)
        return true
    }
}

class TagPager constructor(tags: List<RuuviTag>, context: Context, view: View) : PagerAdapter() {
    val VIEW_TAG = "DetailedTag"
    var tags = tags
    val context = context
    val view = view

    override fun instantiateItem(container: ViewGroup?, position: Int): Any {
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

        val dummyTextView = rootView.findViewById<TextView>(R.id.dummyTextView)
        val tag_temp = rootView.findViewById<TextView>(R.id.tag_temp)
        val tag_humidity = rootView.findViewById<TextView>(R.id.tag_humidity)
        val tag_pressure = rootView.findViewById<TextView>(R.id.tag_pressure)
        val tag_signal = rootView.findViewById<TextView>(R.id.tag_signal)
        val tag_updated = rootView.findViewById<TextView>(R.id.tag_updated)

        val temperature = SpannableString(String.format(context.getString(R.string.temperature_reading), tag?.temperature) + "C")
        temperature.setSpan(CustomTypefaceSpan(dummyTextView.typeface), temperature.length - 2, temperature.length, 0)
        temperature.setSpan(RelativeSizeSpan(0.6f), temperature.length - 2, temperature.length, 0)
        temperature.setSpan(SuperscriptSpan(), temperature.length - 2, temperature.length, 0)

        tag_temp.text = temperature
        tag_humidity.text = String.format(context.getString(R.string.humidity_reading), tag?.humidity)
        tag_pressure.text = String.format(context.getString(R.string.pressure_reading), tag?.pressure)
        tag_signal.text = String.format(context.getString(R.string.signal_reading), tag?.rssi)
        var updatedAt = context.resources.getString(R.string.updated) + " " + Utils.strDescribingTimeSince(tag?.updateAt);
        tag_updated.text = updatedAt
    }

    override fun isViewFromObject(view: View?, `object`: Any?): Boolean {
        return view == `object`
    }

    override fun destroyItem(container: ViewGroup?, position: Int, `object`: Any?) {
        // hmm
    }

    override fun getPageTitle(position: Int): CharSequence {
        return tags.get(position).dispayName
    }

    override fun getCount(): Int {
        return tags.size
    }
}


