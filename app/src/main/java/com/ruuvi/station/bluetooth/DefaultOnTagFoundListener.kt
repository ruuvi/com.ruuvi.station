package com.ruuvi.station.bluetooth

import android.content.Context
import com.ruuvi.station.database.RuuviTagRepository
import com.ruuvi.station.gateway.Http
import com.ruuvi.station.model.RuuviTag
import com.ruuvi.station.model.TagSensorReading
import com.ruuvi.station.util.AlarmChecker
import com.ruuvi.station.util.Constants
import java.util.Calendar
import java.util.Date
import java.util.HashMap

class DefaultOnTagFoundListener(val context: Context) : RuuviRangeNotifier.OnTagsFoundListener {

    private var lastLogged: MutableMap<String, Long> = HashMap()

    override fun onFoundTags(allTags: List<RuuviTag>) {
        val favoriteTags = ArrayList<RuuviTag>()

        allTags.forEach {
            saveReading(it)

            if (it.favorite) {
                favoriteTags.add(it)
            }
        }

        if (favoriteTags.size > 0 && RuuviRangeNotifier.gatewayOn) Http.post(favoriteTags, RuuviRangeNotifier.tagLocation, context)

        TagSensorReading.removeOlderThan(24)
    }

    private fun saveReading(ruuviTag: RuuviTag) {
        var ruuviTag = ruuviTag
        val dbTag = RuuviTagRepository.get(ruuviTag.id)
        if (dbTag != null) {
            ruuviTag = dbTag.preserveData(ruuviTag)
            ruuviTag.update()
            if (!dbTag.favorite) return
        } else {
            ruuviTag.updateAt = Date()
            ruuviTag.save()
            return
        }
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.SECOND, -Constants.DATA_LOG_INTERVAL)
        val loggingThreshold = calendar.time.time
        for ((key, value) in lastLogged!!) {
            if (key == ruuviTag.id && value > loggingThreshold) {
                return
            }
        }
        ruuviTag.id?.let { id ->
            lastLogged[id] = Date().time
        }
        val reading = TagSensorReading(ruuviTag)
        reading.save()
        AlarmChecker.check(ruuviTag, context)
    }
}