package com.ruuvi.station.database.domain.converter

import com.raizlabs.android.dbflow.converter.TypeConverter
import com.ruuvi.station.database.model.NetworkRequestType

@com.raizlabs.android.dbflow.annotation.TypeConverter
class NetworkRequestTypeConverter(): TypeConverter<Int, NetworkRequestType>() {
    override fun getDBValue(model: NetworkRequestType): Int = model.code

    override fun getModelValue(data: Int): NetworkRequestType = NetworkRequestType.getById(data)
}