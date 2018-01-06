package com.ruuvi.station.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.ruuvi.station.R;
import com.ruuvi.station.feature.main.MainActivity;
import com.ruuvi.station.model.Alarm;
import com.ruuvi.station.model.RuuviTag;
import com.ruuvi.station.model.TagSensorReading;

import org.apache.commons.lang3.ObjectUtils;

import java.util.List;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by berg on 01/10/17.
 */

public class AlarmChecker {
    private static final String TAG = "AlarmChecker";

    public static void check(RuuviTag tag, Context context) {
        List<Alarm> alarms = Alarm.getForTag(tag.id);

        int notificationTextResourceId = -9001;
        for (Alarm alarm : alarms) {
            switch (alarm.type) {
                case Alarm.TEMPERATURE:
                    if (tag.temperature < alarm.low)
                        notificationTextResourceId = R.string.alert_notification_temperature_low;
                    if (tag.temperature > alarm.high)
                        notificationTextResourceId = R.string.alert_notification_temperature_high;
                    break;
                case Alarm.HUMIDITY:
                    if (tag.humidity < alarm.low)
                        notificationTextResourceId = R.string.alert_notification_humidity_low;
                    if (tag.humidity > alarm.high)
                        notificationTextResourceId = R.string.alert_notification_humidity_high;
                    break;
                case Alarm.PERSSURE:
                    if (tag.pressure < alarm.low)
                        notificationTextResourceId = R.string.alert_notification_pressure_low;
                    if (tag.pressure > alarm.high)
                        notificationTextResourceId = R.string.alert_notification_pressure_high;
                    break;
                case Alarm.RSSI:
                    if (tag.rssi < alarm.low)
                        notificationTextResourceId = R.string.alert_notification_rssi_low;
                    if (tag.rssi > alarm.high)
                        notificationTextResourceId = R.string.alert_notification_rssi_high;
                    break;
                case Alarm.MOVEMENT:
                    List<TagSensorReading> readings = TagSensorReading.getLatestForTag(tag.id, 2);
                    if (readings.size() == 2) {
                        double prev = getSumOfAcc(readings.get(0));
                        double current = getSumOfAcc(readings.get(1));
                        if (Math.abs(current - prev) > 0.03) {
                            notificationTextResourceId = R.string.alert_notification_movement;
                        }
                    }
                    break;
            }
            if (notificationTextResourceId != -9001) {
                sendAlert(notificationTextResourceId, alarm.id, tag.name, context);
            }
        }
    }

    private static double getSumOfAcc(TagSensorReading reading) {
        return reading.accelX + reading.accelY + reading.accelZ;
    }

    private static void sendAlert(int stringResId, int _id, String name, Context context) {
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);

        int notificationid = _id + stringResId;

        boolean isShowing = isNotificationVisible(context, notificationid);

        NotificationCompat.Builder notification;

        if (!isShowing) {
            notification
                    = new NotificationCompat.Builder(context, "notify_001")
                    .setContentTitle(name)
                    .setSmallIcon(R.mipmap.ic_launcher_small)
                    .setTicker(name + " " + context.getString(stringResId))
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(context.getString(stringResId)))
                    .setContentText(context.getString(stringResId))
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setOnlyAlertOnce(true)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setLargeIcon(bitmap);

            try {
                NotificationManager NotifyMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel channel = new NotificationChannel("notify_001",
                            "Alert notifications",
                            NotificationManager.IMPORTANCE_DEFAULT);
                    NotifyMgr.createNotificationChannel(channel);
                }

                NotifyMgr.notify(notificationid, notification.build());
            } catch (Exception e) {
                Log.d(TAG, "Failed to create notification");
            }
        }
    }


    private static boolean isNotificationVisible(Context context, int id) {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent test = PendingIntent.getActivity(context, id, notificationIntent, PendingIntent.FLAG_NO_CREATE);
        return test != null;
    }
}
