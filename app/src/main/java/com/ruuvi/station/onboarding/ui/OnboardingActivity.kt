package com.ruuvi.station.onboarding.ui

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.OnboardingTopAppBar
import com.ruuvi.station.app.ui.components.Paragraph
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.onboarding.domain.OnboardingPages
import com.ruuvi.station.util.extensions.scaledSp

class OnboardingActivity : AppCompatActivity() {
    @OptIn(ExperimentalPagerApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        setContent {
            RuuviTheme {
                val pagerState = rememberPagerState()
                val scaffoldState = rememberScaffoldState()
                val systemBarsColor = RuuviStationTheme.colors.dashboardBackground
                val systemUiController = rememberSystemUiController()
                val isDarkTheme = isSystemInDarkTheme()

                val backgroundImage = if (isDarkTheme) {
                    R.drawable.onboarding_bg_dark
                } else {
                    R.drawable.onboarding_bg_light
                }

                Image(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(id = backgroundImage),
                    contentScale = ContentScale.Crop,
                    contentDescription = null
                )

                if (pagerState.currentPage == OnboardingPages.values().indexOf(OnboardingPages.READ_SENSORS_DATA)) {
                    Image(
                        modifier = Modifier.fillMaxWidth(),
                        painter = painterResource(id = R.drawable.onboarding_cloud),
                        contentScale = ContentScale.Crop,
                        contentDescription = null
                    )
                }

                Scaffold(
                    scaffoldState = scaffoldState,
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    backgroundColor = Color.Transparent,
                    topBar = {
                        OnboardingTopAppBar(pagerState = pagerState) { finish() }
                    }
                ) { _ ->

                    HorizontalPager(
                        modifier = Modifier
                            .fillMaxSize(),
                        count = OnboardingPages.values().size,
                        state = pagerState
                    ) { page ->

                        val pageType = OnboardingPages.values().elementAt(page)

                        when (pageType) {
                            OnboardingPages.MEASURE_YOUR_WORLD -> OnboardingTemplate(
                                textLines = listOf(
                                    OnboardingText.OnboardingTitle(stringResource(id = R.string.measure_your_world)),
                                    OnboardingText.OnboardingSubTitle(stringResource(id = R.string.with_ruuvi_sensors)),
                                    OnboardingText.OnboardingSubTitle(stringResource(id = R.string.swype_to_continue)),
                                    OnboardingText.OnboardingSubTitle("(squirrel image here)"),
                                ),
                                imageRes = null
                            )
                            OnboardingPages.READ_SENSORS_DATA -> com.ruuvi.station.onboarding.ui.OnboardingTemplate(
                                textLines = listOf(
                                    OnboardingText.OnboardingTitle(stringResource(id = R.string.read_sensors_data)),
                                    OnboardingText.OnboardingSubTitle(stringResource(id = R.string.via_bluetooth_or_cloud)),
                                    OnboardingText.OnboardingImage(R.drawable.onboarding_read_data)
                                ),
                                imageRes = null
                            )
                            OnboardingPages.PAGE1 -> OnboardingTemplate(
                                listOf(
                                    OnboardingText.OnboardingSubTitle("Explore detailed 10 days"),
                                    OnboardingText.OnboardingTitle("History"),
                                ),
                                imageRes = R.drawable.onboarding1
                            )
                            OnboardingPages.PAGE2 -> OnboardingTemplate(
                                listOf(
                                    OnboardingText.OnboardingSubTitle("Follow measurements on a convenient"),
                                    OnboardingText.OnboardingTitle("Dashboard"),
                                ),
                                imageRes = R.drawable.onboarding2
                            )
                            OnboardingPages.PAGE3 -> OnboardingTemplate(
                                listOf(
                                    OnboardingText.OnboardingSubTitle("Set custom"),
                                    OnboardingText.OnboardingTitle("Alerts"),
                                ),
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

sealed class OnboardingText() {
    class OnboardingTitle(val text: String): OnboardingText() {
        @Composable
        override fun Display() {
            Text(
                modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.extended),
                style = RuuviStationTheme.typography.onboardingTitle,
                textAlign = TextAlign.Center,
                text = text,
                fontSize = 36.scaledSp
            )
        }
    }

    class OnboardingSubTitle(val text: String): OnboardingText() {
        @Composable
        override fun Display() {
            Text(
                modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.extended),
                style = RuuviStationTheme.typography.onboardingSubtitle,
                textAlign = TextAlign.Center,
                text = text,
                fontSize = 20.scaledSp
            )
        }
    }

    class OnboardingImage(val drawable: Int): OnboardingText() {
        @Composable
        override fun Display() {
            Image(
                modifier = Modifier.fillMaxSize(),
                painter = painterResource(id = drawable),
                contentDescription = "",
                alignment = Alignment.Center,
                contentScale = ContentScale.Fit
            )
        }
    }

    @Composable
    open fun Display() {
        // dd
    }
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
    textLines: List<OnboardingText>,
    imageRes: Int?
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (line in textLines) {
            line.Display()
            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
        }

        if (imageRes != null) {
            val isTablet = booleanResource(id = R.bool.isTablet)
            val imageSizeFraction = if (isTablet) 0.5f else 0.8f

            Card(
                modifier = Modifier
                    .fillMaxWidth(imageSizeFraction)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(10.dp),
                elevation = 1.dp,
                backgroundColor = Color.Transparent,
                border = BorderStroke(1.dp, Color.Black)
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = "",
                    alignment = Alignment.TopCenter,
                    contentScale = ContentScale.FillWidth
                )
            }
        }
    }

}