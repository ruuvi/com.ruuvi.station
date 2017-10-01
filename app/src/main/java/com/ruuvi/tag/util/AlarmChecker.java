package com.ruuvi.tag.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;

import com.ruuvi.tag.R;
import com.ruuvi.tag.feature.main.MainActivity;
import com.ruuvi.tag.model.Alarm;
import com.ruuvi.tag.model.RuuviTag;

import java.util.List;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by berg on 01/10/17.
 */

public class AlarmChecker {
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
            }
            if (notificationTextResourceId != -9001)
                sendAlert(notificationTextResourceId, alarm.id, tag.name, context);
        }
    }

    private static void sendAlert(int stringResId, int _id, String name, Context context) {
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);

        int notificationid = _id + stringResId;

        boolean isShowing = isNotificationVisible(context, notificationid);

        NotificationCompat.Builder notification;

        if (!isShowing) {
            notification
                    = new NotificationCompat.Builder(context)
                    .setContentTitle(name)
                    .setSmallIcon(R.mipmap.ic_launcher_small)
                    .setTicker(name + " " + context.getString(stringResId))
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(context.getString(stringResId)))
                    .setContentText(context.getString(stringResId))
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setOnlyAlertOnce(true)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setLargeIcon(bitmap);

            NotificationManager NotifyMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            NotifyMgr.notify(notificationid, notification.build());
        }
    }


    private static boolean isNotificationVisible(Context context, int id) {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent test = PendingIntent.getActivity(context, id, notificationIntent, PendingIntent.FLAG_NO_CREATE);
        return test != null;
    }
}
