package com.ruuvi.station.tagsettings.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.RuuviTopAppBar
import com.ruuvi.station.app.ui.components.*
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.util.extensions.viewModel
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import timber.log.Timber
import java.io.File

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
                Body(
                    defaultImages = viewModel.getDefaultImages(),
                    setDefaultImage = { image ->
                        viewModel.setDefaultImage(image)
                        finish()
                    },
                    setImageFromGallery = { uri ->
                        viewModel.setImageFromGallery(uri)
                        finish()
                    },
                    getImageFileForCamera = viewModel::getImageFileForCamera,
                    setImageFromCamera = { file, uri ->
                        viewModel.setImageFromCamera(file, uri)
                        finish()
                    }
                )
            }
        }
    }

    @OptIn(ExperimentalGlideComposeApi::class)
    @Composable
    fun Body(
        defaultImages: List<Int>,
        setDefaultImage: (Int) -> Unit,
        setImageFromGallery: (Uri) -> Unit,
        getImageFileForCamera: () -> Pair<File, Uri>,
        setImageFromCamera: (File, Uri) -> Unit
    ) {
        val context = LocalContext.current
        val scaffoldState = rememberScaffoldState()
        val systemUiController = rememberSystemUiController()
        val systemBarsColor = RuuviStationTheme.colors.systemBars

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            backgroundColor = RuuviStationTheme.colors.background,
            topBar = { RuuviTopAppBar(title = stringResource(id = R.string.change_background)) },
            scaffoldState = scaffoldState
        ) { paddingValues ->

            LazyVerticalGrid(
                columns = GridCells.Fixed(3)
            ) {
                item(span = { GridItemSpan(3) }) {
                    PageHeader(
                        setImageFromGallery = setImageFromGallery,
                        getImageFileForCamera = getImageFileForCamera,
                        setImageFromCamera = setImageFromCamera
                    )
                }

                items(defaultImages) { defaultImage ->
                    GlideImage(
                        modifier = Modifier.height(RuuviStationTheme.dimensions.defaultImagePreviewHeight)
                            .padding(RuuviStationTheme.dimensions.small)
                            .clickable { setDefaultImage.invoke(defaultImage) },
                        model = rememberResourceUri(defaultImage),
                        contentScale = ContentScale.Crop,
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
    fun PageHeader(
        setImageFromGallery: (Uri) -> Unit,
        getImageFileForCamera: () -> Pair<File, Uri>,
        setImageFromCamera: (File, Uri) -> Unit
    ) {
        var cameraFile by remember {
            mutableStateOf<Pair<File, Uri>?>(null)
        }

        val imagePicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
            onResult = { uri ->
                Timber.d("Image selected ${uri?.path}")
                if (uri != null) {
                    setImageFromGallery.invoke(uri)
                }
            }
        )

        val cameraLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture(),
            onResult = { success ->
                Timber.d("Image taken $success")
                val cameraImage = cameraFile?.first
                if (success && cameraImage != null) {
                    setImageFromCamera(cameraImage, Uri.fromFile(cameraImage))
                }
            }
        )

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
                icon = painterResource(id = R.drawable.camera_24),
                tint = RuuviStationTheme.colors.accent
            ) {
                cameraFile = getImageFileForCamera.invoke()
                Timber.d("Image file uri $cameraFile")
                cameraFile?.let {
                    cameraLauncher.launch(it.second)
                }
            }
            DividerRuuvi()
            TextEditWithCaptionButton(
                title = stringResource(id = R.string.select_from_gallery),
                icon = painterResource(id = R.drawable.gallery_24),
                tint = RuuviStationTheme.colors.accent
            ) {
                imagePicker.launch("image/*")
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