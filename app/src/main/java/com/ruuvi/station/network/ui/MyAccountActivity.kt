package com.ruuvi.station.network.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
 import android.text.format.DateUtils
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.ruuvi.station.BuildConfig
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.RuuviTopAppBar
import com.ruuvi.station.app.ui.components.*
import com.ruuvi.station.app.ui.components.dialog.RuuviConfirmDialog
import com.ruuvi.station.app.ui.components.dialog.MessageDialog
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.tagsettings.ui.MoreInfoItem
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import com.ruuvi.station.util.extensions.viewModel
import timber.log.Timber

var fcmToken by mutableStateOf<String?>(null)// String? = null

class MyAccountActivity : AppCompatActivity(), KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: MyAccountViewModel by viewModel()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Timber.e(task.exception, "Fetching FCM registration token failed")
                return@OnCompleteListener
            }

            // Get new FCM registration token
            fcmToken = task.result

            // Log and toast
            Timber.d("FCM token $fcmToken")
        })

        setContent {
            val username by viewModel.userEmail.observeAsState("")
            var isLoading by mutableStateOf(false)
            var deleteAccountDialog by remember {
                mutableStateOf(false)
            }
            val subscription by viewModel.subscription.collectAsState()
            val tokens by viewModel.tokens.collectAsState()

            LaunchedEffect(key1 = true) {
                viewModel.events.collect() { event ->
                    when (event) {
                        is MyAccountEvent.Loading -> { isLoading = event.isLoading }
                        is MyAccountEvent.CloseActivity -> finish()
                        is MyAccountEvent.RequestRegistered -> { deleteAccountDialog = true }
                        else -> {}
                    }
                }
            }

            RuuviTheme {
                val systemUiController = rememberSystemUiController()

                StatusBarFill {
                    MyAccountBody(
                        user = username,
                        signOut = viewModel::signOut,
                        deleteAccount = viewModel::removeAccount,
                        subscription = subscription,
                        tokens = tokens
                    )
                }

                if (isLoading) {
                    LoadingStatusDialog()
                }

                if (deleteAccountDialog) {
                    MessageDialog(
                        message = stringResource(id = R.string.account_delete_confirmation_description),
                        onDismissRequest = { deleteAccountDialog = false }
                    )
                }

                val systemBarsColor = RuuviStationTheme.colors.systemBars
                SideEffect {
                    systemUiController.setSystemBarsColor(
                        color = systemBarsColor,
                        darkIcons = false
                    )
                }
            }
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, MyAccountActivity::class.java)
            context.startActivity(intent)
        }
    }
}

@Composable
fun MyAccountBody(
    user: String,
    signOut: () -> Unit,
    deleteAccount: () -> Unit,
    subscription: Subscription?,
    tokens: List<Pair<Long, String>>?
){
    var signOutDialog by remember {
        mutableStateOf(false)
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .systemBarsPadding()
        .background(RuuviStationTheme.colors.background)
    ) {
        RuuviTopAppBar(title = stringResource(id = R.string.my_ruuvi_account))
        PageSurfaceWithPadding() {
            Column() {
                SubtitleWithPadding(text = stringResource(id = R.string.signed_in_user))
                ParagraphWithPadding(text = user)

                Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RuuviButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(id = R.string.delete_account),
                        isWarning = true
                    ) {
                        deleteAccount.invoke()
                    }

                    Spacer(modifier = Modifier.width(RuuviStationTheme.dimensions.big))
                    RuuviButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(id = R.string.sign_out)
                    ) {
                        signOutDialog = true
                    }
                }

                Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))

                ChangeEmailText()

                if (BuildConfig.DEBUG) {
                    Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))

                    if (fcmToken != null) {
                        MoreInfoItem(
                            title = "",
                            value = fcmToken.toString()
                        )
                        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))
                    }

                    SubscriptionInfo(subscription = subscription)

                    if (tokens != null) {
                        ParagraphWithPadding(text = "Registered FCM tokens")
                        for (token in tokens) {
                            Paragraph(text = "${token.first} - ${token.second}")
                        }
                        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))

                    }
                }
            }
        }

        if (signOutDialog) {
            RuuviConfirmDialog(
                title = stringResource(id = R.string.sign_out),
                message = stringResource(id = R.string.sign_out_confirm),
                onDismissRequest = { signOutDialog = false }
            ) {
                signOut.invoke()
            }
        }
    }
}

@Composable
fun ChangeEmailText(modifier: Modifier = Modifier) {
    HyperlinkText(
        modifier = modifier
            .fillMaxWidth(),
        fullText = stringResource(id = R.string.my_account_change_email),
        linkText = listOf(stringResource(id = R.string.my_account_change_email_link_markup)),
        hyperlinks = listOf(stringResource(id = R.string.my_account_change_email_link)),
        textStyle = RuuviStationTheme.typography.paragraph,
        linkTextColor = RuuviStationTheme.typography.paragraph.color
    )
}

@Composable
fun SubscriptionInfo(subscription: Subscription?) {
    if (subscription != null) {
        SubtitleWithPadding(text = stringResource(id = R.string.current_plan))
        ParagraphWithPadding(text = subscription.name)

        val dateText = subscription.endTime?.let {
            DateUtils.formatDateTime(
                LocalContext.current,
                subscription.endTime.time,
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH
            )
        } ?: stringResource(id = R.string.none)
        
        SubtitleWithPadding(text = stringResource(id = R.string.plan_expiry_date))
        ParagraphWithPadding(text = dateText)

        SubtitleWithPadding(text = stringResource(id = R.string.information))
        ParagraphWithPadding(text = stringResource(id = R.string.subscription_disclaimer))
    }
}