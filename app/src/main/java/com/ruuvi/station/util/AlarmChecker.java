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
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.ruuvi.station.R;
import com.ruuvi.station.feature.TagDetails;
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
                        if (hasTagMoved(readings.get(0), readings.get(1))) {
                            notificationTextResourceId = R.string.alert_notification_movement;
                        }
                    }
                    break;
            }
            if (notificationTextResourceId != -9001) {
                RuuviTag fromDb = RuuviTag.get(tag.id);
                sendAlert(notificationTextResourceId, alarm.id, fromDb.getDispayName(), fromDb.id, context);
            }
        }
    }

    private static boolean hasTagMoved(TagSensorReading one, TagSensorReading two) {
        double threshold = 0.03;
        return diff(one.accelZ, two.accelZ) > threshold ||
                diff(one.accelX, two.accelX) > threshold ||
                diff(one.accelY, two.accelY) > threshold;
    }

    private static double diff(double one, double two) {
        return Math.abs(one - two);
    }

    private static void sendAlert(int stringResId, int _id, String name, String mac, Context context) {
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
        int notificationId = _id + stringResId;
        NotificationCompat.Builder notification;

        Intent intent = new Intent(context, TagDetails.class);
        intent.putExtra("id", mac);
        PendingIntent pendingIntent = TaskStackBuilder.create(context)
                .addNextIntent(intent)
                .getPendingIntent(notificationId, PendingIntent.FLAG_UPDATE_CURRENT);
        notification
                = new NotificationCompat.Builder(context, "notify_001")
                .setContentTitle(name)
                .setTicker(name + " " + context.getString(stringResId))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(context.getString(stringResId)))
                .setContentText(context.getString(stringResId))
                .setDefaults(Notification.DEFAULT_ALL)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(pendingIntent)
                .setLargeIcon(bitmap);

        if (Build.VERSION.SDK_INT < 21) {
            notification.setSmallIcon(R.mipmap.ic_launcher_small);
        } else {
            notification.setSmallIcon(R.drawable.ic_ruuvi_notification_icon_v1);
        }

        try {
            NotificationManager NotifyMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel("notify_001",
                        "Alert notifications",
                        NotificationManager.IMPORTANCE_DEFAULT);
                NotifyMgr.createNotificationChannel(channel);
            }

            NotifyMgr.notify(notificationId, notification.build());
        } catch (Exception e) {
            Log.d(TAG, "Failed to create notification");
        }
    }

    private static boolean isNotificationVisible(Context context, int id) {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent test = PendingIntent.getActivity(context, id, notificationIntent, PendingIntent.FLAG_NO_CREATE);
        return test != null;
    }
}
