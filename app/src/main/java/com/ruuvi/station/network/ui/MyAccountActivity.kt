package com.ruuvi.station.network.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.*
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.dfu.ui.MyTopAppBar
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import com.ruuvi.station.util.extensions.viewModel

class MyAccountActivity : AppCompatActivity(), KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: MyAccountViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            val username by viewModel.userEmail.observeAsState("")
            var isLoading by mutableStateOf(false)
            var deleteAccountDialog by remember {
                mutableStateOf(false)
            }

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

                MyAccountBody(
                    user = username,
                    signOut = viewModel::signOut,
                    deleteAccount = viewModel::removeAccount
                )

                if (isLoading) {
                    LoadingStatusDialog()
                }

                if (deleteAccountDialog) {
                    RuuviMessageDialog(
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
    deleteAccount: () -> Unit
){
    var signOutDialog by remember {
        mutableStateOf(false)
    }

    Column() {
        MyTopAppBar(title = stringResource(id = R.string.my_ruuvi_account))
        PageSurfaceWithPadding() {
            Column() {
                SubtitleWithPadding(text = stringResource(id = R.string.logged_in))
                ParagraphWithPadding(text = user)
                
                Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    RuuviButton(text = stringResource(id = R.string.delete_account), isWarning = true) {
                        deleteAccount.invoke()
                    }

                    Spacer(modifier = Modifier.width(RuuviStationTheme.dimensions.big))
                    RuuviButton(text = stringResource(id = R.string.sign_out)) {
                        signOutDialog = true
                    }
                }
            }
        }

        if (signOutDialog) {
            RuuviConfirmDialog(
                title = stringResource(id = R.string.sign_out),
                message = stringResource(id = R.string.sign_out_confirm),
                onDismissRequest = { signOutDialog = false }
            ){
                signOut.invoke()
            }
        }
    }
}