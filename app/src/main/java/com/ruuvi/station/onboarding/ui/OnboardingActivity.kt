package com.ruuvi.station.onboarding.ui

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.google.accompanist.pager.*
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.RuuviButton
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.app.ui.theme.ruuviStationFontsSizes
import com.ruuvi.station.onboarding.domain.OnboardingPages
import com.ruuvi.station.util.extensions.scaledSp

class OnboardingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        setContent {
            RuuviTheme {
                val systemBarsColor = RuuviStationTheme.colors.dashboardBackground
                val systemUiController = rememberSystemUiController()
                val isDarkTheme = isSystemInDarkTheme()

                OnboardingBody() {
                    finish()
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
        val topBarHeight = 60.dp
        fun start(context: Context) {
            context.startActivity(Intent(context, OnboardingActivity::class.java))
        }
    }
}

@OptIn(ExperimentalPagerApi::class, ExperimentalGlideComposeApi::class)
@Composable
fun OnboardingBody(
    onSkipAction: () -> Unit
) {
    val pagerState = rememberPagerState()
    val isDarkTheme = isSystemInDarkTheme()

    val backgroundRes = if (isDarkTheme) {
        R.drawable.onboarding_bg_dark
    } else {
        R.drawable.onboarding_bg_light
    }

    Image(
        modifier = Modifier.fillMaxSize(),
        painter = painterResource(id = backgroundRes),
        contentScale = ContentScale.Crop,
        contentDescription = null
    )

    val pageType = OnboardingPages.values().elementAt(pagerState.currentPage)

    if (pageType.gatewayRequired) {
        GatewayBanner()
    }

    HorizontalPager(
        modifier = Modifier
            .fillMaxSize(),
        count = OnboardingPages.values().size,
        state = pagerState
    ) { page ->
        val pageType = OnboardingPages.values().elementAt(page)

        when (pageType) {
            OnboardingPages.MEASURE_YOUR_WORLD -> MeasureYourWorldPage()
            OnboardingPages.READ_SENSORS_DATA -> ReadSensorsDataPage()
            OnboardingPages.DASHBOARD -> DashboardPage()
            OnboardingPages.PERSONALISE -> PersonalisePage()
            OnboardingPages.HISTORY -> HistoryPage()
            OnboardingPages.ALERTS -> AlertsPage()
            OnboardingPages.SHARING -> SharingPage()
            OnboardingPages.WIDGETS -> WidgetsPage()
            OnboardingPages.WEB -> WebPage()
            OnboardingPages.FINISH -> FinishPage(onSkipAction)
        }
    }

    Box(modifier = Modifier.statusBarsPadding()) {
        OnboardingTopBar(
            height = OnboardingActivity.topBarHeight,
            pagerState = pagerState,
            skipVisible = pageType != OnboardingPages.FINISH
        ) { onSkipAction.invoke() }
    }
}


@Composable
fun Title(text: String) {
    Text(
        modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.extended),
        style = RuuviStationTheme.typography.onboardingTitle,
        textAlign = TextAlign.Center,
        text = text,
        fontSize = 36.scaledSp
    )
}

@Composable
fun SubTitle(text: String) {
    Text(
        modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.extended),
        style = RuuviStationTheme.typography.onboardingSubtitle,
        textAlign = TextAlign.Center,
        text = text,
        fontSize = 20.scaledSp
    )
}

@Composable
fun Screenshot(imageRes: Int) {
    val isTablet = booleanResource(id = R.bool.isTablet)
    val imageSizeFraction = if (isTablet) 0.5f else 0.8f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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


@Composable
fun MeasureYourWorldPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = OnboardingActivity.topBarHeight),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Title(stringResource(id = R.string.onboarding_measure_your_world))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))
        SubTitle(stringResource(id = R.string.onboarding_with_ruuvi_sensors))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))
        SubTitle(stringResource(id = R.string.onboarding_swipe_to_continue))
        Image(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(),
            painter = painterResource(id = R.drawable.onboarding_beaver),
            contentDescription = "",
            alignment = Alignment.BottomCenter,
            contentScale = ContentScale.FillWidth
        )
    }
}

@Composable
fun ReadSensorsDataPage() {
    val isTablet = booleanResource(id = R.bool.isTablet)
    val cloudSizeFraction = if (isTablet) 0.3f else 0.45f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(cloudSizeFraction),
            painter = painterResource(id = R.drawable.onboarding_cloud),
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter,
            contentDescription = null
        )
        Image(
            modifier = Modifier
                .fillMaxSize(),
            painter = painterResource(id = R.drawable.onboarding_read_data),
            contentScale = ContentScale.FillHeight,
            alignment = Alignment.Center,
            contentDescription = null
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = OnboardingActivity.topBarHeight),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Title(stringResource(id = R.string.onboarding_read_sensors_data))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))
        SubTitle(stringResource(id = R.string.onboarding_via_bluetooth_or_cloud))
    }
}

@Composable
fun DashboardPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = OnboardingActivity.topBarHeight),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SubTitle(stringResource(id = R.string.onboarding_follow_measurement))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))
        Title(stringResource(id = R.string.onboarding_dashboard))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extraBig))
        Screenshot(R.drawable.onboarding_dashboard)
    }
}

@Composable
fun PersonalisePage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = OnboardingActivity.topBarHeight),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SubTitle(stringResource(id = R.string.onboarding_personalise))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))
        Title(stringResource(id = R.string.onboarding_your_sensors))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extraBig))
        Screenshot(R.drawable.onboarding_personalise)
    }
}

@Composable
fun HistoryPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = OnboardingActivity.topBarHeight),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SubTitle(stringResource(id = R.string.onboarding_explore_detailed))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))
        Title(stringResource(id = R.string.onboarding_history))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extraBig))
        Screenshot(R.drawable.onboarding_history)
    }
}

@Composable
fun AlertsPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = OnboardingActivity.topBarHeight),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SubTitle(stringResource(id = R.string.onboarding_set_custom))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))
        Title(stringResource(id = R.string.onboarding_alerts))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extraBig))
        Screenshot(R.drawable.onboarding_alerts)
    }
}

@Composable
fun SharingPage() {
    val isTablet = booleanResource(id = R.bool.isTablet)
    val imageSizeFraction = if (isTablet) 0.7f else 0.8f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = OnboardingActivity.topBarHeight),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Title(stringResource(id = R.string.onboarding_share_your_sensors))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))
        SubTitle(stringResource(id = R.string.onboarding_sharees_can_use))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extraBig))
        Image(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(imageSizeFraction),
            painter = painterResource(id = R.drawable.onboarding_sharing),
            contentDescription = "",
            alignment = Alignment.TopCenter,
            contentScale = ContentScale.FillWidth
        )
    }
}

@Composable
fun WidgetsPage() {
    val isTablet = booleanResource(id = R.bool.isTablet)
    val imageSizeFraction = if (isTablet) 0.7f else 0.8f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = OnboardingActivity.topBarHeight),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Title(stringResource(id = R.string.onboarding_handy_widgets))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))
        SubTitle(stringResource(id = R.string.onboarding_access_widgets))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extraBig))
        Image(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(imageSizeFraction),
            painter = painterResource(id = R.drawable.onboarding_widgets),
            contentDescription = "",
            alignment = Alignment.TopCenter,
            contentScale = ContentScale.FillWidth
        )
    }
}

@Composable
fun WebPage() {
    val isTablet = booleanResource(id = R.bool.isTablet)
    val imageSizeFraction = if (isTablet) 0.7f else 0.8f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = OnboardingActivity.topBarHeight),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Title(stringResource(id = R.string.onboarding_station_web))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))
        SubTitle(stringResource(id = R.string.onboarding_web_pros))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extraBig))
        Image(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(imageSizeFraction),
            painter = painterResource(id = R.drawable.onboarding_web),
            contentDescription = "",
            alignment = Alignment.TopCenter,
            contentScale = ContentScale.FillWidth
        )
    }
}

@Composable
fun FinishPage(continueAction: ()-> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = OnboardingActivity.topBarHeight),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Title("That's it.")
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))
        SubTitle("Let's go to sign in page. Write some text for this screen.")
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extraBig))
        RuuviButton(text = "Continue") {
            continueAction.invoke()
        }
    }
}


@Composable
fun GatewayBanner() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.Bottom
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(100.dp)
                .background(RuuviStationTheme.colors.accent),
            contentAlignment = Alignment.Center
        ) {
            SubTitle(stringResource(id = R.string.onboarding_gateway_required))
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun OnboardingTopBar(
    height: Dp,
    pagerState: PagerState,
    skipVisible: Boolean,
    actionCallBack: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .requiredHeight(height)
    ) {

        HorizontalPagerIndicator(
            activeColor = Color.White,
            pagerState = pagerState,
        )

        if (skipVisible) {
            Text(
                modifier = Modifier
                    .padding(horizontal = RuuviStationTheme.dimensions.extended)
                    .align(Alignment.CenterEnd)
                    .clickable { actionCallBack.invoke() },
                style = RuuviStationTheme.typography.topBarText,
                text = stringResource(id = R.string.onboarding_skip),
                fontSize = ruuviStationFontsSizes.normal,
                textDecoration = TextDecoration.Underline
            )
        }
    }
}