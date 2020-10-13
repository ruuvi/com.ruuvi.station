package com.ruuvi.station.network.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ruuvi.station.database.TagRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.network.data.response.SensorDataResponse
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import java.util.*

class SignedInViewModel (
    val networkInteractor: RuuviNetworkInteractor,
    val tagRepository: TagRepository
) : ViewModel() {
    val emailObserve: LiveData<String> = MutableLiveData(networkInteractor.getEmail())

    val tagsList = MutableLiveData(listOf<SensorDataResponse>())
    val tagsObserve: LiveData<List<SensorDataResponse>> = tagsList

    private val operationStatus = MutableLiveData<String> ("")
    val operationStatusObserve: LiveData<String> = operationStatus

    init {
        networkInteractor.getUserInfo {
            it?.let {
                tagsList.value = it.data?.sensors
            }
        }
    }

    fun statusProcessed() { operationStatus.value = "" }

    fun addMissingTags() {
        networkInteractor.getUserInfo {
            it?.data?.let {
                it.sensors.forEach {sensor->
                    var tagDb = tagRepository.getTagById(sensor.sensor)
                    if (tagDb == null) {
                        tagDb = RuuviTagEntity()
                        tagDb.id = sensor.sensor
                        tagDb.name = sensor.name
                        tagDb.favorite = true
                        tagDb.updateAt = Date()
                        tagDb.insert()
                    } else {
                        tagDb.favorite = true
                        tagDb.updateAt = Date()
                        tagDb.name = sensor.name
                        tagDb.update()
                    }
                }
                operationStatus.value = "done"
            }
        }
    }
}