package com.ruuvi.station.receivers

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import com.ruuvi.station.model.Alarm
import android.app.NotificationManager


class CancelAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("alarmId", -1)
        val notificationId = intent.getIntExtra("notificationId", -1)
        if (alarmId != -1) {
            Alarm.get(alarmId)?.delete()
        }
        if (notificationId != -1) {
            val ns = Context.NOTIFICATION_SERVICE
            val nMgr = context.getSystemService(ns) as NotificationManager
            nMgr.cancel(notificationId)
        }
    }
}