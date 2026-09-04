package com.ruuvi.station.database.domain

import com.raizlabs.android.dbflow.kotlinextensions.and
import com.raizlabs.android.dbflow.sql.language.SQLite
import com.ruuvi.station.database.tables.SensorsShareList
import com.ruuvi.station.database.tables.SensorsShareList_Table
import timber.log.Timber
import java.util.Locale

class SensorShareListRepository {
    fun getUsedSharesTotal(): Int =
        SQLite.selectCountOf()
            .from(SensorsShareList::class.java)
            .longValue()
            .toInt()

    fun getShareListForSensor(sensorId: String): List<SensorsShareList> =
        SQLite.select()
            .from(SensorsShareList::class.java)
            .where(SensorsShareList_Table.sensorId.eq(sensorId))
            .queryList()

    fun clearShareList(sensorId: String) {
        SQLite
            .delete()
            .from(SensorsShareList::class.java)
            .where(SensorsShareList_Table.sensorId.eq(sensorId))
            .execute()
    }

    fun deleteFromShareList(sensorId: String, userEmail: String) {
        val normalizedEmail = userEmail.trim().lowercase(Locale.ROOT)
        SQLite
            .delete()
            .from(SensorsShareList::class.java)
            .where(SensorsShareList_Table.sensorId.eq(sensorId).and(SensorsShareList_Table.userEmail.eq(normalizedEmail)))
            .execute()
    }

    fun insertToShareList(sensorId: String, userEmail: String, pending: Boolean = false) {
        val normalizedSensorId = sensorId.trim()
        val normalizedEmail = userEmail.trim().lowercase(Locale.ROOT)

        if (normalizedSensorId.isNotEmpty() && normalizedEmail.isNotEmpty()) {
            val newElement = SensorsShareList(normalizedSensorId, normalizedEmail, pending)
            newElement.save()
        }
    }

    fun updateSharingList(sensorId: String, sharedTo: List<String>, sharedToPending: List<String>) {
        val savedList = getShareListForSensor(sensorId)
        val allNetworkEmails = (sharedTo + sharedToPending).map { it.trim().lowercase(Locale.ROOT) }

        for (element in savedList) {
            if (allNetworkEmails.none { it == element.userEmail }) {
                deleteFromShareList(sensorId, element.userEmail)
            }
        }

        for (userEmail in sharedTo) {
            val savedElement = savedList.firstOrNull { it.userEmail == userEmail.trim().lowercase(Locale.ROOT) }
            if (savedElement == null) {
                insertToShareList(sensorId, userEmail, false)
            } else if (savedElement.pending) {
                savedElement.pending = false
                savedElement.update()
            }
        }

        for (userEmail in sharedToPending) {
            val savedElement = savedList.firstOrNull { it.userEmail == userEmail.trim().lowercase(Locale.ROOT) }
            if (savedElement == null) {
                insertToShareList(sensorId, userEmail, true)
            } else if (!savedElement.pending) {
                savedElement.pending = true
                savedElement.update()
            }
        }
    }
}