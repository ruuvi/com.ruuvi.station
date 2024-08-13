package com.ruuvi.station.startup.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ruuvi.station.util.extensions.viewModel
import com.ruuvi.station.R
import com.ruuvi.station.dashboard.ui.DashboardActivity
import com.ruuvi.station.firebase.domain.FirebaseInteractor
import com.ruuvi.station.onboarding.ui.OnboardingActivity
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import timber.log.Timber

class StartupActivity : AppCompatActivity(), KodeinAware {

    override val kodein by closestKodein()

    private val viewModel: StartupActivityViewModel by viewModel()
    private val firebasePropertySaver: FirebaseInteractor by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_startup)

        firebasePropertySaver.saveUserProperties()

        viewModel.generateDeviceId()
        viewModel.startForegroundScanning()

        Timber.d("isFirstStart = ${viewModel.isFirstStart()} shouldAcceptTerms = ${viewModel.shouldAcceptTerms()}")
        when {
            viewModel.isFirstStart() || viewModel.shouldAcceptTerms() -> {
                OnboardingActivity.start(this)
            }
            else -> {
                DashboardActivity.start(this)
            }
        }
    }

    companion object {
        fun createIntentForNotification(context: Context): Intent =
            Intent(context, StartupActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        fun start(context: Context) {
            val intent = createIntentForNotification(context)
            context.startActivity(intent)
        }
    }
}
