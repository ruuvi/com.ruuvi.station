package com.ruuvi.station.onboarding.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import com.ruuvi.station.app.ui.theme.RuuviTheme

class OnboardingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RuuviTheme {

            }
        }
    }
}