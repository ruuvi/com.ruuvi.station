package com.ruuvi.tag.feature

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import com.ruuvi.tag.R
import com.ruuvi.tag.model.RuuviTag
import com.ruuvi.tag.scanning.RuuviTagListener
import com.ruuvi.tag.scanning.RuuviTagScanner
import com.ruuvi.tag.util.Utils

import kotlinx.android.synthetic.main.activity_tag_details.*
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.view.*
import android.support.v4.view.ViewPager.OnPageChangeListener




class TagDetails : AppCompatActivity(), RuuviTagListener {
    var tag: RuuviTag? = null
    var tags: List<RuuviTag>? = null

    var scanner: RuuviTagScanner? = null
    var tempText: TextView? = null
    var humidityText: TextView? = null
    var pressureText: TextView? = null
    var signalText: TextView? = null
    var updatedText: TextView? = null

    var pager: ViewPager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag_details)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = null
        supportActionBar?.setIcon(R.drawable.logo)

        tempText = findViewById(R.id.tag_temp)
        humidityText = findViewById(R.id.tag_humidity)
        pressureText = findViewById(R.id.tag_pressure)
        signalText = findViewById(R.id.tag_signal)
        updatedText = findViewById(R.id.tag_updated)
        pager = findViewById(R.id.tag_pager)

        pager!!.setOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                tag = tags!!.get(position)
                updateUI()
            }
        })

        val tagId = intent.getStringExtra("id");
        tag = RuuviTag.get(tagId)
        tags = RuuviTag.getAll()
        val pagerAdapter = TagPager(tags!!)
        pager?.adapter = pagerAdapter

        updateUI()

        scanner = RuuviTagScanner(this, this)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            finish()
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        scanner?.start()
    }

    override fun onPause() {
        super.onPause()
        scanner?.stop()
    }

    override fun tagFound(tag: RuuviTag) {
        if (tag.id == this.tag?.id) {
            this.tag?.updateDataFrom(tag);
            this.tag?.update()
            updateUI()
        }
    }

    fun updateUI() {
        tag?.let {
            tempText?.text = String.format(this.getString(R.string.temperature_reading), tag?.temperature)
            humidityText?.text = String.format(this.getString(R.string.humidity_reading), tag?.humidity)
            pressureText?.text = String.format(this.getString(R.string.pressure_reading), tag?.pressure)
            signalText?.text = String.format(this.getString(R.string.signal_reading), tag?.rssi)
            var updatedAt = this.resources.getString(R.string.updated) + " " + Utils.strDescribingTimeSince(tag?.updateAt);
            updatedText?.text = updatedAt
        }
    }
}

class TagPager constructor(tags: List<RuuviTag>) : PagerAdapter() {
    var tags = tags

    override fun instantiateItem(container: ViewGroup?, position: Int): Any {
        val context = container?.context!!

        val textView: TextView = TextView(context)
        textView.text = tags.get(position).dispayName
        textView.setTextColor(Color.WHITE)
        textView.gravity = Gravity.CENTER_HORIZONTAL
        textView.paintFlags = Paint.UNDERLINE_TEXT_FLAG
        textView.setTypeface(null, Typeface.BOLD)

        (container as ViewPager).addView(textView, 0)
        return textView
    }

    override fun isViewFromObject(view: View?, `object`: Any?): Boolean {
        return view == `object`
    }

    override fun destroyItem(container: ViewGroup?, position: Int, `object`: Any?) {
        // hmm
    }

    override fun getCount(): Int {
        return tags.size
    }

}
