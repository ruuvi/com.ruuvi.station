package com.ruuvi.station.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ruuvi.station.database.tables.Alarm
import java.util.*

class MuteAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra(ALARM_ID, -1)
        if (alarmId != -1) {
            val alarm = Alarm.get(alarmId)
            if (alarm != null) {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.HOUR, 1)
                alarm.mutedTill = calendar.time
                alarm.update()
            }
        }
    }

    companion object {
        const val ALARM_ID = "alarmId"
    }
}