package com.ruuvi.station.network.domain

import com.ruuvi.station.database.TagRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import java.util.*

class NetworkSignInInteractor (
    private val tagRepository: TagRepository,
    private val networkInteractor: RuuviNetworkInteractor,
    private val networkDataSyncInteractor: NetworkDataSyncInteractor
) {
    fun signIn(token: String, response: (String) -> Unit) {
        networkInteractor.verifyUser(token) {response->
            var  errorText = ""
            if (response == null) {
                errorText = "Unknown error"
            } else if (!response.error.isNullOrEmpty()) {
                errorText = response.error
            }
            networkInteractor.getUserInfo {
                it?.data?.let { body->
                    body.sensors.forEach {sensor->
                        var tagDb = tagRepository.getTagById(sensor.sensor)
                        if (tagDb == null) {
                            tagDb = RuuviTagEntity()
                            tagDb.id = sensor.sensor
                            tagDb.name = if (sensor.name.isEmpty()) sensor.sensor else sensor.name
                            tagDb.favorite = true
                            tagDb.updateAt = Date()
                            tagDb.insert()
                        } else {
                            tagDb.favorite = true
                            tagDb.updateAt = Date()
                            if (sensor.name.isNotEmpty()) tagDb.name = sensor.name
                            tagDb.update()
                        }
                    }
                    networkDataSyncInteractor.syncNetworkData()
                }
            }
            response(errorText)
        }
    }
}