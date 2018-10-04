package com.ruuvi.station.feature

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.view.PagerAdapter
import android.view.View
import android.view.ViewGroup
import com.ruuvi.station.R
import com.ruuvi.station.util.Preferences
import kotlinx.android.synthetic.main.activity_welcome.*

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val welcomeAdapter = WelcomePager()
        welcome_pager.adapter = welcomeAdapter
        welcome_pager.offscreenPageLimit = 100 // just keep them all in memory

        tab_layout.setupWithViewPager(welcome_pager)

        start_button.setOnClickListener {
            Preferences(this).isFirstStart = false
            val intent = Intent(this, StartupActivity::class.java)
            intent.putExtra(TagDetails.FROM_WELCOME, true)
            startActivity(intent)
        }
    }
}


class WelcomePager: PagerAdapter() {
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        var resId = 0
        when (position) {
            0 -> resId = R.id.welcome_0
            1 -> resId = R.id.welcome_1
            2 -> resId = R.id.welcome_2
            3 -> resId = R.id.welcome_3
            4 -> resId = R.id.welcome_4
            5 -> resId = R.id.welcome_5
        }

        return container.findViewById(resId)
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun getCount(): Int {
        return 6
    }
}