package com.ruuvi.station.network.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.core.view.WindowCompat
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.UiEvent
import com.ruuvi.station.app.ui.components.*
import com.ruuvi.station.app.ui.theme.Orange2
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.onboarding.ui.*
import com.ruuvi.station.startup.ui.StartupActivity
import com.ruuvi.station.util.extensions.navigate
import com.ruuvi.station.util.extensions.scaledSp
import com.ruuvi.station.util.extensions.viewModel
import kotlinx.coroutines.flow.SharedFlow
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import timber.log.Timber

class SignInActivity: AppCompatActivity(), KodeinAware {
    override val kodein: Kodein by closestKodein()

    private val viewModel: SignInViewModel by viewModel()

    @OptIn(ExperimentalGlideComposeApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            RuuviTheme {
                val navController = rememberNavController()
                val scaffoldState = rememberScaffoldState()
                val systemUiController = rememberSystemUiController()
                val activity = LocalContext.current as Activity
                var inProgress by remember { mutableStateOf(false) }
                val email by viewModel.email.collectAsState()

                BackHandler() {
                    if (navController.currentBackStackEntry?.destination?.route == SignInRoutes.CLOUD_BENEFITS) {
                        closeActivity()
                    }
                }

                GlideImage(
                    modifier = Modifier.fillMaxSize(),
                    model = rememberResourceUri(R.drawable.onboarding_background),
                    contentScale = ContentScale.Crop,
                    contentDescription = null
                )

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                    backgroundColor = Color.Transparent,
                    scaffoldState = scaffoldState
                ) { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = SignInRoutes.CLOUD_BENEFITS,
                        enterTransition = { slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(600)) },
                        exitTransition = { slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(600)) },
                        popEnterTransition = { slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(600)) },
                        popExitTransition = { slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(600)) }
                    ) {
                        composable(
                            route = SignInRoutes.CLOUD_BENEFITS
                        ) {
                            CloudBenefitsPage(
                                letsDoIt = {
                                    navController.navigate(SignInRoutes.ENTER_EMAIL)
                                }
                            )
                        }
                        composable(
                            SignInRoutes.ENTER_EMAIL
                        ) {
                            EnterEmailPage(
                                inProgress = inProgress,
                                requestCode = { email -> viewModel.submitEmail(email) },
                                useWithoutAccount = { closeActivity() }
                            )
                        }
                        composable(
                            SignInRoutes.ENTER_CODE
                        ) {
                            EnterCodePage(inProgress, email, viewModel.tokenProcessed) { token ->
                                viewModel.verifyCode(token)
                            }
                        }
                    }
                }

                SideEffect {
                    systemUiController.setSystemBarsColor(
                        color = Color.Transparent,
                        isNavigationBarContrastEnforced = false,
                        darkIcons = false
                    )
                }

                LaunchedEffect(null) {
                    viewModel.uiEvent.collect { uiEvent ->
                        Timber.d("uiEvent $uiEvent")
                        when (uiEvent) {
                            is UiEvent.Navigate -> {
                                navController.navigate(uiEvent)
                            }
                            is UiEvent.ShowSnackbar -> {
                                scaffoldState.snackbarHostState.showSnackbar(
                                    message = uiEvent.message.asString(this@SignInActivity),
                                    actionLabel = getString(R.string.ok)
                                )
                            }
                            is UiEvent.Finish -> closeActivity()
                            is UiEvent.NavigateUp -> navController.navigateUp()
                            is UiEvent.Progress -> inProgress = uiEvent.inProgress
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    private fun closeActivity() {
        finish()
        StartupActivity.start(this)
    }

    companion object {
        fun start(context: Context) {
            val settingsIntent = Intent(context, SignInActivity::class.java)
            context.startActivity(settingsIntent)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EnterEmailPage(
    inProgress: Boolean,
    requestCode: (String) -> Unit,
    useWithoutAccount: () -> Unit
) {
    var email by remember{ mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.huge))
        OnboardingTitle(
            text = stringResource(id = R.string.sign_in_or_create_free_account),
            modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.big))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
        OnboardingSubTitle(text = stringResource(id = R.string.to_use_all_app_features))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))
        SignInEmailTextField(value = email, hint = stringResource(id = R.string.type_your_email), onValueChange = { email = it })
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))
        RuuviButton(
            text = stringResource(id = R.string.request_code),
            enabled = !inProgress
        ) {
            requestCode.invoke(email)
            keyboardController?.hide()
        }
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))
        Text(
            modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.extended),
            style = RuuviStationTheme.typography.onboardingText,
            textAlign = TextAlign.Center,
            text = stringResource(id = R.string.no_password_needed),
            fontSize = 16.scaledSp,
            lineHeight = 20.scaledSp
        )
        if (inProgress) {
            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
            LoadingAnimation3dots()
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier
                    .padding(horizontal = RuuviStationTheme.dimensions.extended)
                    .clickable { useWithoutAccount.invoke() },
                style = RuuviStationTheme.typography.onboardingSubtitle,
                textAlign = TextAlign.Center,
                text = stringResource(id = R.string.use_without_account),
                textDecoration = TextDecoration.Underline,
                fontSize = 16.scaledSp,
                lineHeight = 20.scaledSp
            )
            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))
        }
    }
}

@Composable
fun CloudBenefitsPage(
    letsDoIt: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extraBig))
        OnboardingTitle(
            text = stringResource(id = R.string.why_should_sign_in),
            modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.big))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
        OnboardingSubTitle(text = stringResource(id = R.string.sensors_ownership_and_settings_stored_in_cloud))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
        BenefitsList()
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
        val annotatedString = buildAnnotatedString {
            withStyle(style = SpanStyle(color = Orange2, fontWeight = FontWeight.W900)) {
                append(stringResource(id = R.string.note))
            }
            append(" ")
            append(stringResource(id = R.string.claim_warning))
        }
        OnboardingText(text = annotatedString)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RuuviButton(text = stringResource(id = R.string.sign_in_continue)) {
                letsDoIt.invoke()
            }
            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))
            Text(
                modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.extended),
                style = RuuviStationTheme.typography.onboardingText,
                textAlign = TextAlign.Center,
                text = stringResource(id = R.string.signing_in_is_optional),
                fontSize = RuuviStationTheme.fontSizes.normal
            )
        }
    }
}

@Composable
fun EnterCodePage(
    inProgress: Boolean,
    email: String,
    tokenProcessed: SharedFlow<Boolean>,
    codeEntered: (String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    var code by remember { mutableStateOf("") }

    LaunchedEffect(key1 = null) {
        tokenProcessed.collect {
            if (it) code = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extraBig))
        OnboardingTitle(
            text = stringResource(id = R.string.enter_code)
        )
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
        OnboardingSubTitle(text = stringResource(id = R.string.sign_in_check_email, email))
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extraBig))
        OtpTextField(
            otpText = code,
            enabled = !inProgress,
            modifier = Modifier.focusRequester(focusRequester)
        ) { text, isDone ->
            code = text
            if (isDone) {
                keyboardController?.hide()
                codeEntered.invoke(text)
            }
        }
        if (inProgress) {
            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
            LoadingAnimation3dots()
        }
        Column(modifier = Modifier.weight(1f)) {
            BackgroundBeaver(R.drawable.signin_beaver_mail, 0.8f to 0.95f)
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
fun BenefitsList() {
    Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.Start) {
        OnboardingText(text = stringResource(id = R.string.cloud_stored_ownerships))
        OnboardingText(text = stringResource(id = R.string.cloud_stored_names))
        OnboardingText(text = stringResource(id = R.string.cloud_stored_alerts))
        OnboardingText(text = stringResource(id = R.string.cloud_stored_backgrounds))
        OnboardingText(text = stringResource(id = R.string.cloud_stored_calibration))
        OnboardingText(text = stringResource(id = R.string.cloud_stored_sharing))
    }
}

@Composable
fun SignInEmailTextField(
    modifier: Modifier = Modifier,
    value: String,
    label: String? = null,
    hint: String? = null,
    keyboardActions: KeyboardActions = KeyboardActions(),
    onValueChange: (String) -> Unit,
) {
    val labelFun: @Composable (() -> Unit)? = if (label != null) { { Paragraph(text = label) } } else null
    val hintFun:@Composable (() -> Unit)? = if (hint != null) { { Text(text = hint, style = RuuviStationTheme.typography.emailHintTextField) } } else null

    TextField(
        value = value,
        onValueChange = onValueChange,
        label = labelFun,
        placeholder = hintFun,
        textStyle = RuuviStationTheme.typography.emailTextField,
        colors = OnboardingTextFieldColors(),
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        keyboardActions = keyboardActions,
        singleLine = true
    )
}

@Composable
fun OnboardingTextFieldColors() = TextFieldDefaults.textFieldColors(
    textColor = Color.White,
    backgroundColor = Color.Transparent,
    cursorColor = RuuviStationTheme.colors.accent,
    trailingIconColor = RuuviStationTheme.colors.accent,
    focusedIndicatorColor = RuuviStationTheme.colors.accent,
    unfocusedIndicatorColor = RuuviStationTheme.colors.trackColor
)