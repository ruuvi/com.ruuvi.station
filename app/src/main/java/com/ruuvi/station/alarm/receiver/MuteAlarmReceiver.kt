package com.ruuvi.station.alarm.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ruuvi.station.database.tables.Alarm
import java.util.Calendar

class MuteAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra(ALARM_ID, DEFAULT_ID)
        if (alarmId != DEFAULT_ID) {
            val alarm = Alarm.get(alarmId)
            if (alarm != null) {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.HOUR, HOUR)
                alarm.mutedTill = calendar.time
                alarm.update()
            }
        }
    }

    companion object {
        const val ALARM_ID = "alarmId"
        private const val DEFAULT_ID = -1
        private const val HOUR = 1

        fun createPendingIntent(context: Context, alarmId: Int): PendingIntent? {
            val muteIntent = Intent(context, MuteAlarmReceiver::class.java)
            muteIntent.putExtra(ALARM_ID, alarmId)
            return PendingIntent.getBroadcast(context, alarmId, muteIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }
}