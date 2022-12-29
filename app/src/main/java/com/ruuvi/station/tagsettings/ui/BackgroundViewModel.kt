package com.ruuvi.station.tagsettings.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor
import java.io.File

class BackgroundViewModel(
    val sensorId: String,
    val tagSettingsInteractor: TagSettingsInteractor
): ViewModel() {

    fun getDefaultImages(): List<Int> = tagSettingsInteractor.getDefaultImages()

    fun setDefaultImage(resource: Int) =
        tagSettingsInteractor.setDefaultBackgroundImageByResource(sensorId, resource)

    fun setImageFromGallery(uri: Uri) {
        tagSettingsInteractor.setImageFromGallery(sensorId, uri)
    }

    fun setImageFromCamera(imageFile: File, uri: Uri) {
        tagSettingsInteractor.setImageFromCamera(sensorId, imageFile, uri)
    }

    fun getImageFileForCamera(): Pair<File, Uri> = tagSettingsInteractor.createFileForCamera(sensorId)
}