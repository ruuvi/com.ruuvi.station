package com.ruuvi.station.network.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.RuuviTopAppBar
import com.ruuvi.station.app.ui.UiEvent
import com.ruuvi.station.app.ui.components.DividerRuuvi
import com.ruuvi.station.app.ui.components.Paragraph
import com.ruuvi.station.app.ui.components.ParagraphWithPadding
import com.ruuvi.station.app.ui.components.RuuviButton
import com.ruuvi.station.app.ui.components.dialog.CustomContentDialog
import com.ruuvi.station.app.ui.components.StatusBarFill
import com.ruuvi.station.app.ui.components.SubtitleWithPadding
import com.ruuvi.station.app.ui.components.TextFieldRuuvi
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.util.extensions.viewModel
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import timber.log.Timber

class ShareSensorActivity : AppCompatActivity() , KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: ShareSensorViewModel by viewModel {
        intent.getStringExtra(TAG_ID)?.let {
            it
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            RuuviTheme {
                val canShare by viewModel.canShareObserve.observeAsState(true)
                val emails by viewModel.emailsObserve.observeAsState(emptyList())
                val scaffoldState = rememberScaffoldState()

                StatusBarFill {
                    ShareBody(
                        scaffoldState = scaffoldState,
                        canShare = canShare,
                        emails = emails,
                        shareToUser = viewModel::shareTag,
                        unshare = viewModel::unshareTag
                    )
                }

                LaunchedEffect(null) {
                    viewModel.uiEvent.collect { uiEvent ->
                        Timber.d("uiEvent $uiEvent")
                        when (uiEvent) {
                            is UiEvent.ShowSnackbar -> {
                                scaffoldState.snackbarHostState.showSnackbar(
                                    message = uiEvent.message.asString(this@ShareSensorActivity),
                                    actionLabel = getString(R.string.ok),
                                    duration = SnackbarDuration.Long
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG_ID = "TAG_ID"

        fun start(context: Context, tagId: String?) {
            val intent = Intent(context, ShareSensorActivity::class.java)
            intent.putExtra(TAG_ID, tagId)
            context.startActivity(intent)
        }
    }
}

@Composable
fun ShareBody(
    scaffoldState: ScaffoldState,
    canShare: Boolean,
    emails: List<String>,
    shareToUser: (String) -> Unit,
    unshare: (String) -> Unit
) {
    val systemUiController = rememberSystemUiController()
    val systemBarsColor = RuuviStationTheme.colors.systemBars

    Scaffold(
        modifier = Modifier
            .systemBarsPadding()
            .fillMaxSize(),
        backgroundColor = RuuviStationTheme.colors.background,
        topBar = { RuuviTopAppBar(title = stringResource(id = R.string.share_sensor_title)) },
        scaffoldState = scaffoldState
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(RuuviStationTheme.dimensions.screenPadding)
        ) {
            if (canShare) {
                item {
                    AddFriend(shareToUser = shareToUser)
                }
            }
            if (emails.size > 0) {
                item {
                    SubtitleWithPadding(
                        text = stringResource(
                            id = R.string.share_sensor_already_shared,
                            emails.size,
                            10
                        )
                    )
                }
                items(emails) { email ->
                    SharedEmailItem(email = email, unshare = unshare)
                }
            }

            item {
                ParagraphWithPadding(text = stringResource(id = R.string.share_sensor_description))
            }
        }

        SideEffect {
            systemUiController.setSystemBarsColor(
                color = systemBarsColor
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AddFriend(
    shareToUser: (String) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    SubtitleWithPadding(text = stringResource(id = R.string.share_sensor_add_friend))
    var email by remember {
        mutableStateOf(TextFieldValue())
    }
    TextFieldRuuvi(
        value = email,
        hint = stringResource(id = R.string.email),
        onValueChange = {
            if (it.text.length <= 64) email = it
        },
        modifier = Modifier
            .fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            capitalization = KeyboardCapitalization.None
        )
    )
    Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
    Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center){
        RuuviButton(text = stringResource(id = R.string.share)) {
            shareToUser.invoke(email.text)
            email = TextFieldValue()
            keyboardController?.hide()
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SharedEmailItem(
    email: String,
    unshare: (String) -> Unit
) {
    var unshareDialog by remember {
        mutableStateOf(false)
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Paragraph(
                modifier = Modifier
                    .weight(1f),
                text = email
            )
            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                IconButton(
                    onClick = { unshareDialog = true }) {
                    Icon(
                        modifier = Modifier.height(RuuviStationTheme.dimensions.buttonHeight),
                        painter = painterResource(id = R.drawable.ic_baseline_clear_24),
                        contentDescription = "",
                        tint = RuuviStationTheme.colors.accent
                    )
                }
            }
        }
        DividerRuuvi()
    }

    if (unshareDialog) {
        CustomContentDialog(
            title = stringResource(id = R.string.confirm),
            onDismissRequest = { unshareDialog = false },
            onOkClickAction = {
                unshareDialog = false
                unshare(email)
            },
            positiveButtonText = stringResource(id = R.string.yes),
            negativeButtonText = stringResource(id = R.string.no)
        ) {
            Paragraph(text = stringResource(id = R.string.share_sensor_unshare_confirm, email))
        }
    }
}