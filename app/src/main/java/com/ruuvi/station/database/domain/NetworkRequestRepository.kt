package com.ruuvi.station.database.domain

import com.raizlabs.android.dbflow.kotlinextensions.delete
import com.raizlabs.android.dbflow.kotlinextensions.insert
import com.raizlabs.android.dbflow.kotlinextensions.save
import com.raizlabs.android.dbflow.sql.language.SQLite
import com.ruuvi.station.database.model.NetworkRequestStatus
import com.ruuvi.station.database.tables.NetworkRequest
import com.ruuvi.station.database.tables.NetworkRequest_Table
import java.util.*

class NetworkRequestRepository {
    fun delete(networkRequest: NetworkRequest) {
        if (networkRequest.id != 0) networkRequest.delete()
    }

    fun disableRequest(networkRequest: NetworkRequest, status: NetworkRequestStatus) {
        networkRequest.status = status
        networkRequest.attempts++
        if (networkRequest.status == NetworkRequestStatus.SUCCESS) {
            networkRequest.successDate = Date()
        }
        saveRequest(networkRequest)
    }

    private fun saveRequest(networkRequest: NetworkRequest) {
        if (networkRequest.id != 0) {
            networkRequest.save()
        } else {
            networkRequest.insert()
        }
    }

    fun registerFailedAttempt(networkRequest: NetworkRequest) {
        networkRequest.attempts++
        saveRequest(networkRequest)
    }

    fun disableSimilar(networkRequest: NetworkRequest) {
        SQLite.update(NetworkRequest::class.java)
            .set(NetworkRequest_Table.status.eq(NetworkRequestStatus.OVERRIDDEN))
            .where(NetworkRequest_Table.type.eq(networkRequest.type))
            .and(NetworkRequest_Table.key.eq(networkRequest.key))
            .and(NetworkRequest_Table.status.eq(NetworkRequestStatus.READY))
            .async()
            .execute()
    }

    fun getScheduledRequests(): List<NetworkRequest> {
        return SQLite.select()
            .from(NetworkRequest::class.java)
            .where(NetworkRequest_Table.status.eq(NetworkRequestStatus.READY))
            .orderBy(NetworkRequest_Table.requestDate, true)
            .queryList()
    }
}