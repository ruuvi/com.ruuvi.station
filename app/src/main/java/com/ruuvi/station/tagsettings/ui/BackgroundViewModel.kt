package com.ruuvi.station.tagsettings.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.UiEvent
import com.ruuvi.station.app.ui.UiText
import com.ruuvi.station.network.domain.NetworkDataSyncInteractor
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.io.File

class BackgroundViewModel(
    val sensorId: String,
    val tagSettingsInteractor: TagSettingsInteractor,
    val networkDataSyncInteractor: NetworkDataSyncInteractor
): ViewModel() {

    lateinit var cameraFile: Pair<File, Uri>

    private val _uiEvent = MutableSharedFlow<UiEvent> (1)
    val uiEvent: SharedFlow<UiEvent> = _uiEvent

    fun getDefaultImages(): List<Int> = tagSettingsInteractor.getDefaultImages()

    fun setDefaultImage(resource: Int) {
        tagSettingsInteractor.setDefaultBackgroundImageByResource(sensorId, resource)
        requestCloudSync()
    }

    fun setImageFromGallery(uri: Uri): Boolean {
        val result = tagSettingsInteractor.setImageFromGallery(sensorId, uri)
        if (result) {
            requestCloudSync()
        } else {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowSnackbar(UiText.StringResource(R.string.image_format_not_supported)))
            }
        }
        return result
    }

    fun setImageFromCamera() {
        if (this::cameraFile.isInitialized && cameraFile?.first != null) {
            tagSettingsInteractor.setImageFromCamera(sensorId, cameraFile.first, Uri.fromFile(cameraFile.first))
            requestCloudSync()
        }
    }

    private fun requestCloudSync() {
        networkDataSyncInteractor.syncNetworkData()
    }

    fun getImageFileForCamera(): Pair<File, Uri> {
        cameraFile = tagSettingsInteractor.createFileForCamera(sensorId)
        return cameraFile
    }
}