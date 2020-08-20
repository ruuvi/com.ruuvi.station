package com.ruuvi.station.feature

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.viewpager.widget.PagerAdapter
import android.view.View
import android.view.ViewGroup
import com.flexsentlabs.androidcommons.app.ui.setDebouncedOnClickListener
import com.ruuvi.station.R
import com.ruuvi.station.app.preferences.Preferences
import com.ruuvi.station.startup.ui.StartupActivity
import com.ruuvi.station.tagdetails.ui.TagDetailsActivity
import kotlinx.android.synthetic.main.activity_welcome.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val welcomeAdapter = WelcomePager()
        welcome_pager.adapter = welcomeAdapter
        welcome_pager.offscreenPageLimit = 100 // just keep them all in memory

        tab_layout.setupWithViewPager(welcome_pager)

        start_button.setDebouncedOnClickListener {
            Preferences(this).isFirstStart = false
            val intent = Intent(this, StartupActivity::class.java)
            intent.putExtra(TagDetailsActivity.FROM_WELCOME, true)
            startActivity(intent)
        }
    }
}

class WelcomePager : PagerAdapter() {
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