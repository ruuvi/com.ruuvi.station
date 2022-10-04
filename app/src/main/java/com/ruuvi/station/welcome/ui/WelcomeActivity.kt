package com.ruuvi.station.welcome.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.ruuvi.station.R
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.databinding.ActivityWelcomeBinding
import com.ruuvi.station.network.ui.SignInActivity
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
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()
    }

    private fun setupUI() {
        with(binding) {
            val welcomeAdapter = WelcomePager()
            welcomePager.adapter = welcomeAdapter
            welcomePager.offscreenPageLimit = 100 // just keep them all in memory
            welcomePager.addOnPageChangeListener(object: ViewPager.OnPageChangeListener {
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {}

                override fun onPageSelected(position: Int) {
                    if (position == 5 && !preferencesRepository.signedIn()) {
                        showGetBackDialog()
                    }
                }

                override fun onPageScrollStateChanged(state: Int) {}
            })

            tabLayout.setupWithViewPager(welcomePager)

            startButton.setDebouncedOnClickListener {
                preferencesRepository.setFirstStart(false)
                StartupActivity.start(this@WelcomeActivity, true)
            }

            signInButton.setDebouncedOnClickListener {
                openSignInActivity()
            }

            detailsButton.setDebouncedOnClickListener {
                showDetailsDialog()
            }

            if (preferencesRepository.signedIn()) {
                welcomePager.currentItem = 5
                welcome42TextView.setText(R.string.already_signed_in)
                signInButton.isVisible = false
            } else {
                welcome42TextView.setText(R.string.welcome_text_4_2)
                signInButton.isVisible = true
            }
        }
    }

    private fun showDetailsDialog() {
        val alertDialog = AlertDialog.Builder(this, R.style.CustomAlertDialog).create()
        alertDialog.setTitle(getString(R.string.ruuvi_cloud))
        alertDialog.setMessage(getString(R.string.sign_in_benefits_description))
        alertDialog.setButton(
            AlertDialog.BUTTON_NEUTRAL, getString(R.string.close)
        ) { _, _ ->  }
        alertDialog.show()
    }

    private fun showGetBackDialog() {
        val alertDialog = AlertDialog.Builder(this, R.style.CustomAlertDialog).create()
        alertDialog.setTitle(getString(R.string.sign_in_skip_confirm_title))
        alertDialog.setMessage(getString(R.string.sign_in_benefits_description))
        alertDialog.setButton(
            AlertDialog.BUTTON_POSITIVE, getString(R.string.sign_in_skip_confirm_yes)
        ) { _, _ -> }
        alertDialog.setButton(
            AlertDialog.BUTTON_NEGATIVE, getString(R.string.sign_in_skip_confirm_go_back)
        ) { _, _ ->  openSignInActivity() }
        alertDialog.show()
    }

    private fun openSignInActivity() {
        SignInActivity.start(this@WelcomeActivity)
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
            4 -> resId = R.id.welcomeLayout4
            5 -> resId = R.id.welcomeLayout5
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