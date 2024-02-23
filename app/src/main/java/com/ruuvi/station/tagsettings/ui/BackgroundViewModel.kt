package com.ruuvi.station.tagsettings.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.ruuvi.station.network.domain.NetworkDataSyncInteractor
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor
import java.io.File

class BackgroundViewModel(
    val sensorId: String,
    val tagSettingsInteractor: TagSettingsInteractor,
    val networkDataSyncInteractor: NetworkDataSyncInteractor
): ViewModel() {

    lateinit var cameraFile: Pair<File, Uri>

    fun getDefaultImages(): List<Int> = tagSettingsInteractor.getDefaultImages()

    fun setDefaultImage(resource: Int) {
        tagSettingsInteractor.setDefaultBackgroundImageByResource(sensorId, resource)
        requestCloudSync()
    }

    fun setImageFromGallery(uri: Uri) {
        tagSettingsInteractor.setImageFromGallery(sensorId, uri)
        requestCloudSync()
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