package com.ruuvi.station.alarm.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ruuvi.station.alarm.domain.AlarmCheckInteractor
import com.ruuvi.station.database.domain.AlarmRepository
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance

class CancelAlarmReceiver : BroadcastReceiver(), KodeinAware {

    override lateinit var kodein: Kodein
    private val alarmRepository: AlarmRepository by instance()
    private val networkInteractor: RuuviNetworkInteractor by instance()

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        kodein = (context.applicationContext as KodeinAware).kodein
        val alarmCheckInteractor: AlarmCheckInteractor by kodein.instance()

        val alarmId = intent.getIntExtra("alarmId", DEFAULT_ID)
        val notificationId = intent.getIntExtra("notificationId", DEFAULT_ID)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (alarmId != DEFAULT_ID) {
                    val alarm = alarmRepository.getById(alarmId)
                    if (alarm != null) {
                        alarmRepository.disableAlarm(alarm)
                        if (networkInteractor.signedIn) {
                            networkInteractor.setAlert(alarm)?.join()
                        }
                    }
                }
                alarmCheckInteractor.removeNotificationById(notificationId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val DEFAULT_ID = -1
        fun createPendingIntent(context: Context, alarmId: Int): PendingIntent? {

            val cancelIntent = Intent(context, CancelAlarmReceiver::class.java)
            cancelIntent.putExtra("alarmId", alarmId)
            cancelIntent.putExtra("notificationId", alarmId)
            return PendingIntent.getBroadcast(context, alarmId, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}