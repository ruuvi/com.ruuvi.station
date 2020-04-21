package com.ruuvi.station.receivers

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import com.ruuvi.station.model.Alarm
import com.ruuvi.station.util.AlarmChecker

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