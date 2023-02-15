package com.ruuvi.station.onboarding.ui

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.google.accompanist.pager.*
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.RuuviButton
import com.ruuvi.station.app.ui.components.rememberResourceUri
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.app.ui.theme.ruuviStationFontsSizes
import com.ruuvi.station.onboarding.domain.OnboardingPages
import com.ruuvi.station.util.extensions.scaledSp
import com.ruuvi.station.util.extensions.viewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein

class OnboardingActivity : AppCompatActivity(), KodeinAware {

    override val kodein by closestKodein()

    private val onboardingViewModel: OnboardingViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            RuuviTheme {
                val systemUiController = rememberSystemUiController()
                val isDarkTheme = isSystemInDarkTheme()

                OnboardingBody(onboardingViewModel.signedIn) {
                    finish()
                }

                SideEffect {
                    systemUiController.setStatusBarColor(
                        color = Color.Transparent,
                        darkIcons = false
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
        val bannerHeight = 100.dp

        fun start(context: Context) {
            context.startActivity(Intent(context, OnboardingActivity::class.java))
        }
    }
}

@OptIn(ExperimentalPagerApi::class, ExperimentalGlideComposeApi::class)
@Composable
fun OnboardingBody(
    signedIn: Boolean,
    onFinishAction: () -> Unit
) {
    val pagerState = rememberPagerState()

    GlideImage(
        modifier = Modifier.fillMaxSize(),
        model = rememberResourceUri(R.drawable.onboarding_bg_dark),
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
            OnboardingPages.FINISH -> FinishPage(signedIn, onFinishAction)
        }
    }

    Box(modifier = Modifier.statusBarsPadding()) {
        OnboardingTopBar(
            height = OnboardingActivity.topBarHeight,
            pagerState = pagerState,
            skipVisible = pageType != OnboardingPages.FINISH

        ) {
            CoroutineScope(Dispatchers.Main).launch {
                pagerState.scrollToPage(OnboardingPages.values().indexOf(OnboardingPages.FINISH))
            }
        }
    }
}

@Composable
fun OnboardingTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.padding(horizontal = RuuviStationTheme.dimensions.extended),
        style = RuuviStationTheme.typography.onboardingTitle,
        textAlign = TextAlign.Center,
        text = text,
        fontSize = 36.scaledSp
    )
}

@Composable
fun OnboardingSubTitle(text: String) {
    Text(
        modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.extended),
        style = RuuviStationTheme.typography.onboardingSubtitle,
        textAlign = TextAlign.Center,
        text = text,
        fontSize = 20.scaledSp,
        lineHeight = 26.scaledSp
    )
}

@Composable
fun OnboardingText(text: String) {
    Text(
        modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.extended),
        style = RuuviStationTheme.typography.onboardingText,
        textAlign = TextAlign.Center,
        text = text,
        fontSize = 18.scaledSp,
        lineHeight = 22.scaledSp
    )
}

@Composable
fun OnboardingText(text: AnnotatedString) {
    Text(
        modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.extended),
        style = RuuviStationTheme.typography.onboardingText,
        textAlign = TextAlign.Center,
        text = text,
        fontSize = 18.scaledSp,
        lineHeight = 22.scaledSp
    )
}

@OptIn(ExperimentalGlideComposeApi::class)
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
            GlideImage(
                model = rememberResourceUri(resourceId = imageRes),
                contentDescription = "",
                alignment = Alignment.TopCenter,
                contentScale = ContentScale.FillWidth
            )
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun MeasureYourWorldPage() {
    BackgroundBeaver(R.drawable.onboarding_beaver_start)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = OnboardingActivity.topBarHeight),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))
        OnboardingTitle(stringResource(id = R.string.onboarding_measure_your_world))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
        OnboardingSubTitle(stringResource(id = R.string.onboarding_with_ruuvi_sensors))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))
        OnboardingSubTitle(stringResource(id = R.string.onboarding_swipe_to_continue))
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun BackgroundBeaver(imageRes: Int) {
    val isTablet = booleanResource(id = R.bool.isTablet)
    val imageSizeFraction = if (isTablet) 0.8f else 1f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        GlideImage(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(imageSizeFraction)
                .navigationBarsPadding(),
            model = rememberResourceUri(resourceId = imageRes),
            contentDescription = "",
            alignment = Alignment.BottomCenter,
            contentScale = ContentScale.FillWidth
        )
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ReadSensorsDataPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x9900CCBA))
                .padding(
                    top = OnboardingActivity.topBarHeight,
                    bottom = RuuviStationTheme.dimensions.extended
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Box(modifier = Modifier.statusBarsPadding()) { }
            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))
            OnboardingTitle(stringResource(id = R.string.onboarding_read_sensors_data))
            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
            OnboardingSubTitle(stringResource(id = R.string.onboarding_via_bluetooth_or_cloud))
        }

        GlideImage(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.1f),
            model = rememberResourceUri(resourceId = R.drawable.onboarding_cloud_bottom),
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            contentDescription = null
        )

        GlideImage(
            modifier = Modifier
                .fillMaxSize(),
            model = rememberResourceUri(resourceId = R.drawable.onboarding_read_data),
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center,
            contentDescription = null
        )
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
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))
        OnboardingSubTitle(stringResource(id = R.string.onboarding_follow_measurement))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
        OnboardingTitle(stringResource(id = R.string.onboarding_dashboard))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extraBig))
        Screenshot(R.drawable.onboarding_screenshot_dashboard)
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
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))
        OnboardingSubTitle(stringResource(id = R.string.onboarding_personalise))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
        OnboardingTitle(stringResource(id = R.string.onboarding_your_sensors))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extraBig))
        Screenshot(R.drawable.onboarding_screenshot_personalise)
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
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))
        OnboardingSubTitle(stringResource(id = R.string.onboarding_explore_detailed))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
        OnboardingTitle(stringResource(id = R.string.onboarding_history))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extraBig))
        Screenshot(R.drawable.onboarding_screenshot_history)
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
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))
        OnboardingSubTitle(stringResource(id = R.string.onboarding_set_custom))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
        OnboardingTitle(stringResource(id = R.string.onboarding_alerts))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extraBig))
        Screenshot(R.drawable.onboarding_screenshot_alerts)
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
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))
        OnboardingSubTitle(stringResource(id = R.string.onboarding_sharees_can_use))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
        OnboardingTitle(stringResource(id = R.string.onboarding_share_your_sensors))
        FitImageAboveBanner(imageSizeFraction, R.drawable.onboarding_sharing)
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
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
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))
        OnboardingSubTitle(stringResource(id = R.string.onboarding_access_widgets))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
        OnboardingTitle(stringResource(id = R.string.onboarding_handy_widgets))
        FitImageAboveBanner(imageSizeFraction, R.drawable.onboarding_widgets)
    }
}

@Composable
fun WebPage() {
    val imageSizeFraction = 0.8f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = OnboardingActivity.topBarHeight),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))
        OnboardingSubTitle(stringResource(id = R.string.onboarding_web_pros))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
        OnboardingTitle(stringResource(id = R.string.onboarding_station_web))
        FitImageAboveBanner(imageSizeFraction, R.drawable.onboarding_web)
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun FitImageAboveBanner(
    imageSizeFraction: Float,
    imageRes: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = RuuviStationTheme.dimensions.extended),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlideImage(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(imageSizeFraction),
            model = rememberResourceUri(resourceId = imageRes),
            contentDescription = "",
            alignment = Alignment.Center,
            contentScale = ContentScale.Fit
        )
        Box(Modifier.requiredHeight(OnboardingActivity.bannerHeight))
        Box(Modifier.navigationBarsPadding())
    }
}

@Composable
fun FinishPage(
    signedIn: Boolean,
    continueAction: ()-> Unit
) {
    BackgroundBeaver(R.drawable.onboarding_beaver_end)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = OnboardingActivity.topBarHeight),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))
        if (signedIn) {
            OnboardingTitle(stringResource(id = R.string.onboarding_lets_get_started ))
            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
            OnboardingSubTitle(stringResource(id = R.string.onboarding_lets_get_started_description))
        } else {
            OnboardingTitle(stringResource(id = R.string.onboarding_almost_there ))
            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
            OnboardingSubTitle(stringResource(id = R.string.onboarding_go_to_sign_in))
        }

        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extraBig))
        RuuviButton(text = stringResource(id = R.string.onboarding_continue)) {
            continueAction.invoke()
        }
    }
}


@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun GatewayBanner() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.Bottom
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(OnboardingActivity.bannerHeight)
                .background(RuuviStationTheme.colors.accent),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlideImage(
                modifier = Modifier.padding(start = RuuviStationTheme.dimensions.big),
                contentScale = ContentScale.Fit,
                model = rememberResourceUri(resourceId = R.drawable.onboarding_gateway),
                contentDescription = null
            )
            Text(
                modifier = Modifier.padding(
                    start = RuuviStationTheme.dimensions.extended,
                    end = RuuviStationTheme.dimensions.big),
                style = RuuviStationTheme.typography.onboardingSubtitle,
                textAlign = TextAlign.Start,
                text = stringResource(id = R.string.onboarding_gateway_required),
                fontSize = 16.scaledSp
            )
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