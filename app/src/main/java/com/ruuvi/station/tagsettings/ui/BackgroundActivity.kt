package com.ruuvi.station.tagsettings.ui

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.MyTopAppBar
import com.ruuvi.station.app.ui.components.DividerRuuvi
import com.ruuvi.station.app.ui.components.Paragraph
import com.ruuvi.station.app.ui.components.Subtitle
import com.ruuvi.station.app.ui.components.TextEditWithCaptionButton
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.util.extensions.viewModel
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein

class BackgroundActivity : AppCompatActivity(), KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: BackgroundViewModel by viewModel {
        intent.getStringExtra(SENSOR_ID)?.let {
            it
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            RuuviTheme {
                Body(defaultImages = viewModel.getDefaultImages())
            }
        }
    }

    @Composable
    fun Body(defaultImages: List<Int>) {
        val context = LocalContext.current
        val scaffoldState = rememberScaffoldState()
        val systemUiController = rememberSystemUiController()
        val systemBarsColor = RuuviStationTheme.colors.systemBars

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            backgroundColor = RuuviStationTheme.colors.background,
            topBar = { MyTopAppBar(title = stringResource(id = R.string.change_background)) },
            scaffoldState = scaffoldState
        ) { paddingValues ->

            LazyVerticalGrid(
                columns = GridCells.Fixed(3)
            ) {
                item(span = { GridItemSpan(3) }){
                    PageHeader()
                }

                items(defaultImages) { pic ->
                    Image(
                        modifier = Modifier.padding(RuuviStationTheme.dimensions.small),
                        painter = painterResource(id = pic),
                        contentDescription = ""
                    )
                }
            }
        }

        SideEffect {
            systemUiController.setSystemBarsColor(
                color = systemBarsColor
            )
        }
    }

    @Composable
    fun PageHeader() {
        Column(
        ) {
            Paragraph(
                modifier = Modifier.padding(
                    vertical = RuuviStationTheme.dimensions.extended,
                    horizontal = RuuviStationTheme.dimensions.medium
                ),
                text = stringResource(id = R.string.change_background_message)
            )
            DividerRuuvi()
            TextEditWithCaptionButton(
                title = stringResource(id = R.string.take_photo),
                value = null,
                icon = painterResource(id = R.drawable.camera_24),
                tint = RuuviStationTheme.colors.accent
            ) {

            }
            DividerRuuvi()
            TextEditWithCaptionButton(
                title = stringResource(id = R.string.select_from_gallery),
                value = null,
                icon = painterResource(id = R.drawable.gallery_24),
                tint = RuuviStationTheme.colors.accent
            ) {

            }
            DividerRuuvi()
            Subtitle(
                text = stringResource(id = R.string.select_default_image),
                modifier = Modifier.padding(
                    vertical = RuuviStationTheme.dimensions.extended,
                    horizontal = RuuviStationTheme.dimensions.medium
                ))
        }
    }

    companion object {
        private const val SENSOR_ID = "SENSOR_ID"

        fun start(context: Context, sensorId: String?) {
            val intent = Intent(context, BackgroundActivity::class.java)
            intent.putExtra(SENSOR_ID, sensorId)
            context.startActivity(intent)
        }
    }
}