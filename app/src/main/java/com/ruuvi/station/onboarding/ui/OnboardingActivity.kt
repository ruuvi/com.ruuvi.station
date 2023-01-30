package com.ruuvi.station.onboarding.ui

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.GlideImage
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.OnboardingTopAppBar
import com.ruuvi.station.app.ui.components.Paragraph
import com.ruuvi.station.app.ui.components.Subtitle
import com.ruuvi.station.app.ui.components.Title
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme

class OnboardingActivity : AppCompatActivity() {
    @OptIn(ExperimentalPagerApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RuuviTheme {
                val pagerState = rememberPagerState()
                val scaffoldState = rememberScaffoldState()
                val systemBarsColor = RuuviStationTheme.colors.dashboardBackground
                val systemUiController = rememberSystemUiController()
                val isDarkTheme = isSystemInDarkTheme()

                Scaffold(
                    scaffoldState = scaffoldState,
                    modifier = Modifier.fillMaxSize(),
                    backgroundColor = RuuviStationTheme.colors.dashboardBackground,
                    topBar = {
                        OnboardingTopAppBar(pagerState = pagerState) {}
                    }
                ) { paddingValues ->

                    HorizontalPager(
                        modifier = Modifier.fillMaxSize(),
                        count = OnboardingPages.values().size,
                        state = pagerState
                    ) { page ->

                        val pageType = OnboardingPages.values().elementAt(page)

                        when (pageType) {
                            OnboardingPages.PAGE1 -> OnboardingTemplate(
                                title = "History",
                                subtitle = "Explore detailed 10 days",
                                imageRes = R.drawable.onboarding1
                            )
                            OnboardingPages.PAGE2 -> OnboardingTemplate(
                                title = "Dashboard",
                                subtitle = "Follow measurements on a convenient",
                                imageRes = R.drawable.onboarding2
                            )
                            OnboardingPages.PAGE3 -> OnboardingTemplate(
                                title = "Alerts",
                                subtitle = "Set custom",
                                imageRes = R.drawable.onboarding3
                            )
                        }
                    }
                }

                SideEffect {
                    systemUiController.setStatusBarColor(
                        color = systemBarsColor,
                        darkIcons = !isDarkTheme
                    )
                    systemUiController.setNavigationBarColor(
                        color = Color.Transparent,
                        darkIcons = !isDarkTheme
                    )
                }
            }
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, OnboardingActivity::class.java))
        }
    }
}

enum class OnboardingPages (val id: Int) {
    PAGE1(0),
    PAGE2(1),
    PAGE3(2)
}

@Composable
fun OnboardingScreen1() {
    Paragraph(text = "Page 1")
}

@Composable
fun OnboardingScreen2() {
    Paragraph(text = "Page 2")
}

@Composable
fun OnboardingScreen3() {
    Paragraph(text = "Page 3")
}

@Composable
fun OnboardingTemplate(
    title: String,
    subtitle: String,
    imageRes: Int
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Subtitle(text = subtitle)
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))
        Title(text = title)
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))
        //shape = RoundedCornerShape(10.dp)
        Card(shape = RoundedCornerShape(10.dp)) {
            Image(
                modifier = Modifier.fillMaxWidth(0.8f),
                painter = painterResource(id = imageRes),
                contentDescription = "",
                alignment = Alignment.TopCenter,
                contentScale = ContentScale.FillWidth
            )
        }
    }

}