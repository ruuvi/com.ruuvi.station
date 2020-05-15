package com.ruuvi.station.alarm.receiver

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import com.ruuvi.station.database.tables.Alarm
import com.ruuvi.station.alarm.AlarmChecker

class CancelAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("alarmId", -1)
        val notificationId = intent.getIntExtra("notificationId", -1)
        if (alarmId != -1) {
            val alarm = Alarm.get(alarmId)
            if (alarm != null) {
                alarm.enabled = false
                alarm.update()
            }
        }
        AlarmChecker.dismissNotification(notificationId, context)
    }
}