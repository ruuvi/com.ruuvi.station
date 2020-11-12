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
            response(errorText)
        }
    }
}