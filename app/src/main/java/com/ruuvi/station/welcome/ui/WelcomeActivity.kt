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
import com.ruuvi.station.databinding.ActivityWelcomeBinding
import com.ruuvi.station.startup.ui.StartupActivity
import com.ruuvi.station.util.extensions.setDebouncedOnClickListener
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance

class WelcomeActivity : AppCompatActivity(R.layout.activity_welcome), KodeinAware {

    override val kodein by closestKodein()

    private val preferencesRepository : PreferencesRepository by instance()

    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        with(binding) {
            val welcomeAdapter = WelcomePager()
            welcomePager.adapter = welcomeAdapter
            welcomePager.offscreenPageLimit = 100 // just keep them all in memory

            tabLayout.setupWithViewPager(welcomePager)

            startButton.setDebouncedOnClickListener {
                preferencesRepository.setFirstStart(false)
                StartupActivity.start(this@WelcomeActivity, true)
            }
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
            0 -> resId = R.id.welcomeLayout0
            1 -> resId = R.id.welcomeLayout1
            2 -> resId = R.id.welcomeLayout2
            3 -> resId = R.id.welcomeLayout3
            4 -> resId = R.id.welcomeLayout5
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