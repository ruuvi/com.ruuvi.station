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
import com.bumptech.glide.integration.compose.GlideImage
import com.google.accompanist.pager.*
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.rememberResourceUri
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

    HorizontalPager(
        modifier = Modifier
            .fillMaxSize(),
        count = OnboardingPages.values().size,
        state = pagerState
    ) { page ->

        val pageType = OnboardingPages.values().elementAt(page)

        val isTablet = booleanResource(id = R.bool.isTablet)
        val imageSizeFraction = if (isTablet) 0.5f else 0.8f

        GlideImage(
            modifier = Modifier.fillMaxSize(),
            model = rememberResourceUri(pageType.backgroundImageRes),
            contentScale = ContentScale.Crop,
            contentDescription = null
        )

        if (pageType == OnboardingPages.READ_SENSORS_DATA) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.4f),
                    painter = painterResource(id = R.drawable.onboarding_cloud),
                    contentScale = ContentScale.Crop,
                    contentDescription = null
                )
                Image(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    painter = painterResource(id = R.drawable.onboarding_read_data),
                    contentScale = ContentScale.FillHeight,
                    alignment = Alignment.Center,
                    contentDescription = null
                )
            }
        }

        Column(modifier = Modifier
            .statusBarsPadding()
            .padding(top = OnboardingActivity.topBarHeight)) {

            when (pageType) {
                OnboardingPages.MEASURE_YOUR_WORLD -> OnboardingTemplatePage(
                    textLines = listOf(
                        OnboardingElement.Title(stringResource(id = R.string.onboarding_measure_your_world)),
                        OnboardingElement.SubTitle(stringResource(id = R.string.onboarding_with_ruuvi_sensors)),
                        OnboardingElement.SubTitle(stringResource(id = R.string.onboarding_swype_to_continue)),
                        OnboardingElement.Image(
                            drawable = R.drawable.onboarding_beaver,
                            alignment = Alignment.BottomCenter,
                            contentScale = ContentScale.FillWidth
                        )
                    ),
                    imageRes = null
                )
                OnboardingPages.READ_SENSORS_DATA -> OnboardingTemplatePage(
                    textLines = listOf(
                        OnboardingElement.Title(stringResource(id = R.string.onboarding_read_sensors_data)),
                        OnboardingElement.SubTitle(stringResource(id = R.string.onboarding_via_bluetooth_or_cloud)),
                    ),
                    imageRes = null
                )
                OnboardingPages.DASHBOARD -> OnboardingTemplatePage(
                    listOf(
                        OnboardingElement.SubTitle(stringResource(id = R.string.onboarding_follow_measurement)),
                        OnboardingElement.Title(stringResource(id = R.string.onboarding_dashboard))
                    ),
                    imageRes = R.drawable.onboarding_dashboard
                )
                OnboardingPages.PERSONALISE -> OnboardingTemplatePage(
                    listOf(
                        OnboardingElement.Title(stringResource(id = R.string.onboarding_personalise)),
                        OnboardingElement.SubTitle(stringResource(id = R.string.onboarding_your_sensors))
                    ),
                    imageRes = R.drawable.onboarding_personalise
                )
                OnboardingPages.HISTORY -> OnboardingTemplatePage(
                    listOf(
                        OnboardingElement.SubTitle(stringResource(id = R.string.onboarding_explore_detailed)),
                        OnboardingElement.Title(stringResource(id = R.string.onboarding_history))
                    ),
                    imageRes = R.drawable.onboarding_history
                )
                OnboardingPages.ALERTS -> OnboardingTemplatePage(
                    listOf(
                        OnboardingElement.SubTitle(stringResource(id = R.string.onboarding_set_custom)),
                        OnboardingElement.Title(stringResource(id = R.string.onboarding_alerts))
                    ),
                    imageRes = R.drawable.onboarding_alerts
                )
                OnboardingPages.MESSAGES -> OnboardingTemplatePage(
                    listOf(
                        OnboardingElement.SubTitle(stringResource(id = R.string.onboarding_have_fun)),
                        OnboardingElement.Title(stringResource(id = R.string.onboarding_messages)),
                        OnboardingElement.Image(
                            drawable = R.drawable.onboarding_messages,
                            alignment = Alignment.TopCenter,
                            imageSizeFraction = imageSizeFraction,
                            contentScale = ContentScale.FillWidth)
                    ),
                    imageRes = null
                )
                OnboardingPages.SHARING ->
                    OnboardingSharingPage()
//                    OnboardingTemplatePage(
//                    listOf(
//                        OnboardingElement.Title(stringResource(id = R.string.onboarding_share_your_sensors)),
//                        OnboardingElement.SubTitle(stringResource(id = R.string.onboarding_sharees_can_use)),
//                        OnboardingElement.Image(
//                            drawable = R.drawable.onboarding_sharing,
//                            alignment = Alignment.TopCenter,
//                            contentScale = ContentScale.FillWidth,
//                            imageSizeFraction = imageSizeFraction
//                        ),
//                        OnboardingElement.GatewayBanner()
//                    ),
//                    imageRes = null
//                )
            }
        }

    }

    Box(modifier = Modifier.statusBarsPadding()) {
        OnboardingTopBar(height = OnboardingActivity.topBarHeight, pagerState = pagerState) { onSkipAction.invoke() }
    }
}


@OptIn(ExperimentalPagerApi::class)
@Composable
fun OnboardingTopBar(
    height: Dp,
    pagerState: PagerState,
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


sealed class OnboardingElement() {
    class Title(val text: String) : OnboardingElement() {
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

    class SubTitle(val text: String) : OnboardingElement() {
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

    class Image(
        val drawable: Int,
        val alignment: Alignment,
        val imageSizeFraction: Float = 1f,
        val contentScale: ContentScale = ContentScale.Fit
    ) : OnboardingElement() {
        @Composable
        override fun Display() {
            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))

            Image(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(imageSizeFraction),
                painter = painterResource(id = drawable),
                contentDescription = "",
                alignment = alignment,
                contentScale = contentScale
            )
        }
    }

    class GatewayBanner() : OnboardingElement() {
        @Composable
        override fun Display() {
            Box (
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeight(100.dp)
                    .navigationBarsPadding()
                    .background(RuuviStationTheme.colors.accent),
                contentAlignment = Alignment.Center
            ){
                Title(stringResource(id = R.string.onboarding_gateway_required))
            }
        }
    }

    @Composable
    open fun Display() {
        // should be overridden
    }
}

@Composable
fun OnboardingTemplatePage(
    textLines: List<OnboardingElement>,
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

            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))

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

@Composable
fun OnboardingSharingPage() {
//    OnboardingElement.Title(stringResource(id = R.string.onboarding_share_your_sensors)),
//    OnboardingElement.SubTitle(stringResource(id = R.string.onboarding_sharees_can_use)),
//    OnboardingElement.Image(
//        drawable = R.drawable.onboarding_sharing,
//        alignment = Alignment.TopCenter,
//        contentScale = ContentScale.FillWidth,
//        imageSizeFraction = imageSizeFraction
//    ),
//    OnboardingElement.GatewayBanner()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        OnboardingTemplatePage(
            listOf(
                OnboardingElement.Title(stringResource(id = R.string.onboarding_share_your_sensors)),
                OnboardingElement.SubTitle(stringResource(id = R.string.onboarding_sharees_can_use)),
                OnboardingElement.Image(
                    drawable = R.drawable.onboarding_sharing,
                    alignment = Alignment.TopCenter,
                    contentScale = ContentScale.FillWidth,
                    imageSizeFraction = 0.8f
                ),
                //OnboardingElement.GatewayBanner()
            ),
            imageRes = null
        )

//        Box (
//            modifier = Modifier
//                .fillMaxWidth()
//                .requiredHeight(100.dp)
//                .background(RuuviStationTheme.colors.accent),
//            contentAlignment = Alignment.Center
//        ){
//            OnboardingElement.Title(stringResource(id = R.string.onboarding_gateway_required))
//        }
//        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}
