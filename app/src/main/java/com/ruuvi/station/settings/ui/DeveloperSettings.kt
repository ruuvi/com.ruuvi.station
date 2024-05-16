package com.ruuvi.station.settings.ui

import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebSettings.LOAD_DEFAULT
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.UiEvent
import com.ruuvi.station.app.ui.components.*
import timber.log.Timber

@Composable
fun DeveloperSettings(
    scaffoldState: ScaffoldState,
    onNavigate: (UiEvent.Navigate) -> Unit,
    viewModel: DeveloperSettingsViewModel
) {
    val devServerEnabled = viewModel.devServerEnabled.collectAsState()
    val newChartsUi = viewModel.newChartsUiEnabled.collectAsState()

    PageSurfaceWithPadding {
        Column() {
            SwitchIndicatorRuuvi(
                text = "New charts UI",
                checked = newChartsUi.value,
                onCheckedChange = viewModel::setNewChartsUi
            )

            SwitchIndicatorRuuvi(
                text = stringResource(id = R.string.use_dev_server),
                checked = devServerEnabled.value,
                onCheckedChange = viewModel::setDevServerEnabled
            )
            Paragraph(text = stringResource(id = R.string.use_dev_server_description))
            SettingsElement(
                name = "Web sharing",
                onClick = { onNavigate.invoke(UiEvent.Navigate(SettingsRoutes.SHARINGWEB)) }
            )
        }
    }
}

@Composable
fun SharingWebView(
    scaffoldState: ScaffoldState,
    viewModel: DeveloperSettingsViewModel
) {
    val userToken = viewModel.getWebViewToken()
    val script = "window.localStorage.setItem('user','$userToken')"
    var webView: WebView? = null
    PageSurface {
        Column(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        settings.javaScriptEnabled = true
                        settings.allowContentAccess = true
                        settings.allowFileAccess = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.cacheMode = LOAD_DEFAULT
//                        loadUrl("https://github.com/ruuvi")
                        setWebViewClient(object: WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                url: String?
                            ): Boolean {
                                //loadUrl("https://devstation.ruuvi.com/shares")
                                return false
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                return false
                            }

                            override fun onPageStarted(
                                view: WebView?,
                                url: String?,
                                favicon: Bitmap?
                            ) {
                                super.onPageStarted(view, url, favicon)
                                evaluateJavascript(script) {
                                    Timber.d("evaluateJavascript $script RESULT $it")
                                }

                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                evaluateJavascript(script) {
                                    Timber.d("evaluateJavascript $script RESULT $it")
                                }
                            }
                        })

                        loadUrl("https://devstation.ruuvi.com/shares")


//                        evaluateJavascript(script) {
//                            Timber.d("evaluateJavascript $script RESULT $it")
//                        }
                        webView = this
                    }
                }, update = {
                    webView = it
                })
        }
    }
}