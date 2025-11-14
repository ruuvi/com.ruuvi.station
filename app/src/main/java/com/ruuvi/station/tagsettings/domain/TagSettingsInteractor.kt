package com.ruuvi.station.tagsettings.domain

import android.net.Uri
import androidx.annotation.DrawableRes
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.database.tables.isAir
import com.ruuvi.station.image.ImageInteractor
import com.ruuvi.station.image.ImageSource
import com.ruuvi.station.network.data.response.SensorSettings_defaultDisplayOrder
import com.ruuvi.station.network.data.response.SensorSettings_displayOrder
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.units.model.UnitType.*
import com.ruuvi.station.util.MacAddressUtils
import timber.log.Timber
import java.io.File

class TagSettingsInteractor(
    private val tagRepository: TagRepository,
    private val preferencesRepository: PreferencesRepository,
    private val sensorSettingsRepository: SensorSettingsRepository,
    private val networkInteractor: RuuviNetworkInteractor,
    private val imageInteractor: ImageInteractor
) {
    fun getDefaultImages() = imageInteractor.defaultImages

    fun getFavouriteSensorById(tagId: String): RuuviTag? =
        tagRepository
            .getFavoriteSensorById(tagId)

    fun getTagById(tagId: String): RuuviTagEntity? =
        tagRepository
            .getTagById(tagId)

    fun updateTag(tag: RuuviTagEntity) =
        tagRepository.updateTag(tag)

    fun getTemperatureUnit(): TemperatureUnit =
        preferencesRepository.getTemperatureUnit()

    fun deleteTagsAndRelatives(sensorId: String, deleteData: Boolean) {
        val sensorSettings = sensorSettingsRepository.getSensorSettings(sensorId)
        tagRepository.deleteSensorAndRelatives(sensorId)
        if (sensorSettings?.networkSensor == true && sensorSettings.owner?.isNotEmpty() == true) {
            if (sensorSettings.owner == networkInteractor.getEmail()) {
                networkInteractor.unclaimSensor(sensorId, deleteData)
            } else {
                networkInteractor.unshareSensor(networkInteractor.getEmail() ?: "", sensorId)
            }
        }
    }

    fun updateTagName(sensorId: String, sensorName: String?) {
        val tag = tagRepository.getTagById(sensorId)
        val name =
            if (sensorName.isNullOrEmpty()) MacAddressUtils.getDefaultName(sensorId, tag?.isAir()) else sensorName
        sensorSettingsRepository.updateSensorName(sensorId, name)
    }

    fun updateTagBackground(tagId: String, userBackground: String?, defaultBackground: Int?) =
        sensorSettingsRepository.updateSensorBackground(tagId, userBackground, defaultBackground, null)

    fun updateNetworkBackground(tagId: String, guid: String?) {
        sensorSettingsRepository.updateNetworkBackground(tagId, guid)
    }

    fun getSensorSettings(sensorId: String): SensorSettings? = sensorSettingsRepository.getSensorSettings(sensorId)

    fun setSensorFirmware(sensorId: String, firmware: String) = sensorSettingsRepository.setSensorFirmware(sensorId, firmware)

    fun setBackgroundImage(
        sensorId: String,
        userBackground: String,
        uploadNow: Boolean = false
    ) {
        Timber.d("setCustomBackgroundImage $sensorId $userBackground")
        val sensorSettings = getSensorSettings(sensorId)
        if (sensorSettings != null) {
            sensorSettings.userBackground?.let { oldFile ->
                imageInteractor.deleteFile(oldFile)
            }
            sensorSettingsRepository.updateSensorBackground(
                sensorId = sensorId,
                userBackground = userBackground,
                defaultBackground = 0,
                networkBackground = null
            )

            if (sensorSettings.networkSensor) {
                networkInteractor.uploadImage(
                    sensorId = sensorId,
                    filename = userBackground,
                    uploadNow = uploadNow
                )
            }
        }
    }

    fun setRandomDefaultBackgroundImage(
        sensorId: String,
        isAir: Boolean
    ) {
        setDefaultBackgroundImageByResource(
            sensorId = sensorId,
            defaultBackground = imageInteractor.getDefaultResource(isAir)
        )
    }

    fun setDefaultBackgroundImageByResource(
        sensorId: String,
        @DrawableRes defaultBackground: Int,
        uploadNow: Boolean = false
    ) {
        Timber.d("setDefaultBackgroundImage $sensorId $defaultBackground")
        val imageFile = imageInteractor.saveResourceAsFile(sensorId, defaultBackground)
        if (imageFile != null) {
            setBackgroundImage(
                sensorId = sensorId,
                userBackground = Uri.fromFile(imageFile).toString(),
                uploadNow = uploadNow
            )
        }
    }

    fun setImageFromGallery(sensorId: String, uri: Uri): Boolean {
        Timber.d("setImageFromGallery $sensorId $uri")
        val isImage = imageInteractor.isImage(uri)
        Timber.d("isImage $isImage")
        if (imageInteractor.isImage(uri)) {
            val image = imageInteractor.createFile(sensorId, ImageSource.GALLERY)
            Timber.d("output file ${image.absolutePath}")

            if (imageInteractor.copyFile(uri, image)) {
                val imageUri = Uri.fromFile(image)
                imageInteractor.resize(
                    filename = image.absolutePath,
                    uri = imageUri,
                    rotation = imageInteractor.getCameraPhotoOrientation(imageUri)
                )
                setBackgroundImage(
                    sensorId = sensorId,
                    userBackground = imageUri.toString()
                )
            }
            return true
        }
        return false
    }

    fun createFileForCamera(sensorId: String): Pair<File,Uri> = imageInteractor.createFileForCamera(sensorId)

    fun setImageFromCamera(sensorId: String, imageFile: File, uri: Uri) {
        imageInteractor.resize(
            filename = imageFile.absolutePath,
            uri = uri,
            rotation = imageInteractor.getCameraPhotoOrientation(uri)
        )
        setBackgroundImage(
            sensorId = sensorId,
            userBackground = uri.toString()
        )
    }

    fun setUseDefaultSensorsOrder(sensorId: String, useDefault: Boolean) {
        val sensorSettings = getSensorSettings(sensorId)
        sensorSettingsRepository.updateUseDefaultSensorOrder(sensorId, useDefault)
        if (sensorSettings?.networkSensor == true) {
            networkInteractor.updateSensorSetting(
                sensorId = sensorId,
                name = SensorSettings_defaultDisplayOrder,
                value = useDefault.toString()
            )
        }
    }

    fun newDisplayOrder(sensorId: String, displayOrder: String) {
        val sensorSettings = getSensorSettings(sensorId)
        sensorSettingsRepository.newDisplayOrder(sensorId, displayOrder)
        if (sensorSettings?.networkSensor == true) {
            networkInteractor.updateSensorSetting(
                sensorId = sensorId,
                name = SensorSettings_displayOrder,
                value = displayOrder
            )
        }
    }
}