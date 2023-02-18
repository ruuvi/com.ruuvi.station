package com.ruuvi.station.database.domain

import com.raizlabs.android.dbflow.kotlinextensions.*
import com.raizlabs.android.dbflow.sql.language.SQLite
import com.ruuvi.station.database.model.NetworkRequestStatus
import com.ruuvi.station.database.model.NetworkRequestType
import com.ruuvi.station.database.tables.*
import java.util.*

class NetworkRequestRepository {
    fun getSimilar(networkRequest: NetworkRequest): List<NetworkRequest> {
        return SQLite.select()
            .from(NetworkRequest::class.java)
            .where(NetworkRequest_Table.type.eq(networkRequest.type))
            .and(NetworkRequest_Table.key.eq(networkRequest.key))
            .and(NetworkRequest_Table.status.eq(NetworkRequestStatus.READY).or(NetworkRequest_Table.status.eq(NetworkRequestStatus.EXECUTING)))
            .queryList()
    }

    fun getActiveRequestsForKeyType(key: String, type: NetworkRequestType): List<NetworkRequest> {
        return SQLite.select()
            .from(NetworkRequest::class.java)
            .where(NetworkRequest_Table.type.eq(type))
            .and(NetworkRequest_Table.key.eq(key))
            .and(NetworkRequest_Table.status.eq(NetworkRequestStatus.READY).or(NetworkRequest_Table.status.eq(NetworkRequestStatus.EXECUTING)))
            .queryList()
    }

    fun delete(networkRequest: NetworkRequest) {
        if (networkRequest.id != 0) networkRequest.delete()
    }

    fun disableRequest(networkRequest: NetworkRequest, status: NetworkRequestStatus) {
        val request = getById(networkRequest.id)
        if (request != null) {
            request.status = status
            request.attempts++
            if (request.status == NetworkRequestStatus.SUCCESS) {
                request.successDate = Date()
            }
            saveRequest(request)
        }
    }

    fun saveRequest(networkRequest: NetworkRequest) {
        if (networkRequest.id != 0) {
            networkRequest.save()
        } else {
            networkRequest.insert()
        }
    }

    fun getById(requestId: Int): NetworkRequest? {
        return SQLite
            .select()
            .from(NetworkRequest::class.java)
            .where(NetworkRequest_Table.id.eq(requestId))
            .querySingle()
    }

    fun registerFailedAttempt(networkRequest: NetworkRequest) {
        val request = getById(networkRequest.id)
        if (request != null && request.status == NetworkRequestStatus.EXECUTING) {
            request.attempts++
            if (request.attempts > MAX_ATTEMPTS) {
                request.status = NetworkRequestStatus.FAILED
            } else {
                request.status = NetworkRequestStatus.READY
            }
            saveRequest(request)
        }
    }

    fun disableSimilar(networkRequest: NetworkRequest) {
        SQLite.update(NetworkRequest::class.java)
            .set(NetworkRequest_Table.status.eq(NetworkRequestStatus.OVERRIDDEN))
            .where(NetworkRequest_Table.type.eq(networkRequest.type))
            .and(NetworkRequest_Table.key.eq(networkRequest.key))
            .and(NetworkRequest_Table.status.eq(NetworkRequestStatus.READY).or(NetworkRequest_Table.status.eq(NetworkRequestStatus.EXECUTING)))
            .execute()
    }

    fun getScheduledRequests(): List<NetworkRequest> {
        return SQLite.select()
            .from(NetworkRequest::class.java)
            .where(NetworkRequest_Table.status.eq(NetworkRequestStatus.READY))
            .orderBy(NetworkRequest_Table.requestDate, true)
            .queryList()
    }

    fun startExecuting(networkRequest: NetworkRequest): Boolean {
        val request = getById(networkRequest.id)
        if (request != null && request.status == NetworkRequestStatus.READY) {
            request.status = NetworkRequestStatus.EXECUTING
            saveRequest(request)
            return true
        } else {
            return false
        }
    }

    companion object {
        const val MAX_ATTEMPTS = 10
    }
}