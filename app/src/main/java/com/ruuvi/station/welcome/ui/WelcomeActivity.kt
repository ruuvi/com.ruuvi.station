package com.ruuvi.station.welcome.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.PagerAdapter
import com.ruuvi.station.R
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.startup.ui.StartupActivity
import com.ruuvi.station.util.extensions.setDebouncedOnClickListener
import kotlinx.android.synthetic.main.activity_welcome.start_button
import kotlinx.android.synthetic.main.activity_welcome.tab_layout
import kotlinx.android.synthetic.main.activity_welcome.welcome_pager
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance

class WelcomeActivity : AppCompatActivity(), KodeinAware {

    override val kodein by closestKodein()

    private val preferencesRepository : PreferencesRepository by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val welcomeAdapter = WelcomePager()
        welcome_pager.adapter = welcomeAdapter
        welcome_pager.offscreenPageLimit = 100 // just keep them all in memory

        tab_layout.setupWithViewPager(welcome_pager)

        start_button.setDebouncedOnClickListener {
            preferencesRepository.setFirstStart(false)
            StartupActivity.start(this, true)
        }
    }

    companion object{
        const val ARGUMENT_FROM_WELCOME = "ARGUMENT_FROM_WELCOME"

        fun start(context: Context){
            val intent = Intent(context, WelcomeActivity::class.java)
            context.startActivity(intent)
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
            4 -> resId = R.id.welcome_5
        }

        return container.findViewById(resId)
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object`
    }

    override fun getCount(): Int {
        return 5
    }
}