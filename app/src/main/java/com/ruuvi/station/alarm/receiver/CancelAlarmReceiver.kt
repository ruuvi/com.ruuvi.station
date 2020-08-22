package com.ruuvi.station.alarm.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ruuvi.station.alarm.domain.AlarmCheckInteractor
import com.ruuvi.station.database.tables.Alarm
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance

class CancelAlarmReceiver : BroadcastReceiver(), KodeinAware {

    override lateinit var kodein: Kodein
    private val alarmCheckInteractor: AlarmCheckInteractor by kodein.instance()

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("alarmId", DEFAULT_ID)
        val notificationId = intent.getIntExtra("notificationId", DEFAULT_ID)
        if (alarmId != -1) {
            val alarm = Alarm.get(alarmId)
            if (alarm != null) {
                alarm.enabled = false
                alarm.update()
            }
        }
        alarmCheckInteractor.removeNotificationById(notificationId)
    }

    companion object {
        private const val DEFAULT_ID = -1
        fun createPendingIntent(context: Context, alarmId: Int): PendingIntent? {

            val cancelIntent = Intent(context, CancelAlarmReceiver::class.java)
            cancelIntent.putExtra("alarmId", alarmId)
            cancelIntent.putExtra("notificationId", alarmId)
            return PendingIntent.getBroadcast(context, alarmId, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }
}