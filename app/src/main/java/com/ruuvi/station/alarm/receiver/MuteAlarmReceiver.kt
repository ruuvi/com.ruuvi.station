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
import java.util.Calendar

class MuteAlarmReceiver : BroadcastReceiver(), KodeinAware {

    override lateinit var kodein: Kodein

    override fun onReceive(context: Context, intent: Intent) {
        kodein = (context?.applicationContext as KodeinAware).kodein
        val alarmCheckInteractor: AlarmCheckInteractor by kodein.instance()

        val notificationId = intent.getIntExtra("notificationId", DEFAULT_ID)
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
        alarmCheckInteractor.removeNotificationById(notificationId)
    }

    companion object {
        const val ALARM_ID = "alarmId"
        const val NOTIFICATION_ID = "notificationId"
        private const val DEFAULT_ID = -1
        private const val HOUR = 1

        fun createPendingIntent(context: Context, alarmId: Int): PendingIntent? {
            val muteIntent = Intent(context, MuteAlarmReceiver::class.java)
            muteIntent.putExtra(ALARM_ID, alarmId)
            muteIntent.putExtra(NOTIFICATION_ID, alarmId)
            return PendingIntent.getBroadcast(context, alarmId, muteIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }
}