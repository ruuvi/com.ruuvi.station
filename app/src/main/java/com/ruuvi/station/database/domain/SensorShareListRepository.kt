package com.ruuvi.station.database.domain

import com.raizlabs.android.dbflow.kotlinextensions.and
import com.raizlabs.android.dbflow.sql.language.SQLite
import com.ruuvi.station.database.tables.SensorsShareList
import com.ruuvi.station.database.tables.SensorsShareList_Table

class SensorShareListRepository {
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
            .async()
            .execute()
    }

    fun deleteFromShareList(sensorId: String, userEmail: String) {
        SQLite
            .delete()
            .from(SensorsShareList::class.java)
            .where(SensorsShareList_Table.sensorId.eq(sensorId).and(SensorsShareList_Table.userEmail.eq(userEmail)))
            .execute()
    }

    fun insertToShareList(sensorId: String, userEmail: String) {
        if (sensorId.isNotEmpty() && userEmail.isNotEmpty()) {
            val newElement = SensorsShareList(sensorId, userEmail)
            newElement.insert()
        }
    }

    fun updateSharingList(sensorId: String, sharedTo: List<String>) {
        if (sharedTo.isEmpty()) {
            clearShareList(sensorId)
        } else {
            val savedList = getShareListForSensor(sensorId)
            for (element in savedList) {
                if (sharedTo.none { it == element.userEmail }) {
                    deleteFromShareList(sensorId, element.userEmail)
                }
            }

            for (userEmail in sharedTo) {
                if (savedList.none { it.userEmail == userEmail }) {
                    insertToShareList(sensorId, userEmail)
                }
            }
        }
    }
}