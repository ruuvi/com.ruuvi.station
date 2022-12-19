package com.ruuvi.station.tagsettings.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.ruuvi.station.R
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor

class BackgroundViewModel(
    val sensorId: String,
    val tagSettingsInteractor: TagSettingsInteractor
): ViewModel() {

    fun getDefaultImages(): List<Int> = listOf(
        R.drawable.bg2,
        R.drawable.bg3,
        R.drawable.bg4,
        R.drawable.bg5,
        R.drawable.bg6,
        R.drawable.bg7,
        R.drawable.bg8,
        R.drawable.bg9
    )

    fun setDefaultImage(resource: Int) =
        tagSettingsInteractor.setDefaultBackgroundImageByResource(sensorId, resource)

    fun setImageFromGallery(uri: Uri) {
        tagSettingsInteractor.setImageFromGallery(sensorId, uri)
    }
}