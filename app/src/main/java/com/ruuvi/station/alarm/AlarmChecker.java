package com.ruuvi.station.alarm;

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
import com.ruuvi.station.R;
import com.ruuvi.station.alarm.receiver.MuteAlarmReceiver;
import com.ruuvi.station.database.RuuviTagRepository;
import com.ruuvi.station.database.tables.Alarm;
import com.ruuvi.station.database.tables.RuuviTagEntity;
import com.ruuvi.station.database.tables.TagSensorReading;
import com.ruuvi.station.alarm.receiver.CancelAlarmReceiver;
import com.ruuvi.station.tagdetails.ui.TagDetailsActivity;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import timber.log.Timber;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by berg on 01/10/17.
 */

public class AlarmChecker {
    private static final String TAG = "AlarmChecker";

    // returns 1 for triggered alarm, 0 for non triggered alarm, -1 if tag has no alarm
    public static int getStatus(RuuviTagEntity tag) {
        List<Alarm> alarms = Alarm.getForTag(tag.getId());

        int notificationTextResourceId = -9001;
        boolean anyEnabled = false;
        for (Alarm alarm : alarms) {
            if (!alarm.enabled) continue;
            anyEnabled = true;
            switch (alarm.type) {
                case Alarm.TEMPERATURE:
                    if (tag.getTemperature() < alarm.low)
                        notificationTextResourceId = R.string.alert_notification_temperature_low;
                    if (tag.getTemperature() > alarm.high)
                        notificationTextResourceId = R.string.alert_notification_temperature_high;
                    break;
                case Alarm.HUMIDITY:
                    if (tag.getHumidity() < alarm.low)
                        notificationTextResourceId = R.string.alert_notification_humidity_low;
                    if (tag.getHumidity() > alarm.high)
                        notificationTextResourceId = R.string.alert_notification_humidity_high;
                    break;
                case Alarm.PERSSURE:
                    if (tag.getPressure() < alarm.low * 100)
                        notificationTextResourceId = R.string.alert_notification_pressure_low;
                    if (tag.getPressure() > alarm.high * 100)
                        notificationTextResourceId = R.string.alert_notification_pressure_high;
                    break;
                case Alarm.RSSI:
                    if (tag.getRssi() < alarm.low)
                        notificationTextResourceId = R.string.alert_notification_rssi_low;
                    if (tag.getRssi() > alarm.high)
                        notificationTextResourceId = R.string.alert_notification_rssi_high;
                    break;
                case Alarm.MOVEMENT:
                    List<TagSensorReading> readings = TagSensorReading.getLatestForTag(tag.getId(), 2);
                    if (readings.size() == 2) {
                        if (hasTagMoved(readings.get(0), readings.get(1))) {
                            notificationTextResourceId = R.string.alert_notification_movement;
                        }
                    }
                    break;
            }
            if (notificationTextResourceId != -9001) {
                return 1;
            }
        }
        if (anyEnabled) return 0;
        return -1;
    }

    public static void check(RuuviTagEntity tag, Context context) {
        Timber.d("check alarm tag.id = %1$s", tag.getId());
        List<Alarm> alarms = Alarm.getForTag(tag.getId());

        for (Alarm alarm : alarms) {
            int notificationTextResourceId = -9001;
            if (!alarm.enabled) continue;
            switch (alarm.type) {
                case Alarm.TEMPERATURE:
                    if (tag.getTemperature() < alarm.low)
                        notificationTextResourceId = R.string.alert_notification_temperature_low;
                    if (tag.getTemperature() > alarm.high)
                        notificationTextResourceId = R.string.alert_notification_temperature_high;
                    break;
                case Alarm.HUMIDITY:
                    if (tag.getHumidity() < alarm.low)
                        notificationTextResourceId = R.string.alert_notification_humidity_low;
                    if (tag.getHumidity() > alarm.high)
                        notificationTextResourceId = R.string.alert_notification_humidity_high;
                    break;
                case Alarm.PERSSURE:
                    if (tag.getPressure() < alarm.low * 100)
                        notificationTextResourceId = R.string.alert_notification_pressure_low;
                    if (tag.getPressure() > alarm.high * 100)
                        notificationTextResourceId = R.string.alert_notification_pressure_high;
                    break;
                case Alarm.RSSI:
                    if (tag.getRssi() < alarm.low)
                        notificationTextResourceId = R.string.alert_notification_rssi_low;
                    if (tag.getRssi() > alarm.high)
                        notificationTextResourceId = R.string.alert_notification_rssi_high;
                    break;
                case Alarm.MOVEMENT:
                    List<TagSensorReading> readings = TagSensorReading.getLatestForTag(tag.getId(), 2);
                    if (readings.size() == 2) {
                        if (tag.getDataFormat() == 5) {
                            if (readings.get(0).movementCounter != readings.get(1).movementCounter) {
                                notificationTextResourceId = R.string.alert_notification_movement;
                                break;
                            }
                        }
                        if (hasTagMoved(readings.get(0), readings.get(1))) {
                            notificationTextResourceId = R.string.alert_notification_movement;
                        }
                    }
                    break;
            }
            if (notificationTextResourceId != -9001 && canNotify(alarm)) {
                RuuviTagEntity fromDb = RuuviTagRepository.get(tag.getId());
                sendAlert(notificationTextResourceId, alarm.id, fromDb.getDisplayName(), fromDb.getId(), context);
            }
        }
    }

    public static void dismissNotification(Integer notificationId, Context context) {
        Timber.d("dismissNotification with id = %1$s", notificationId);
        if (notificationId != -1) {
            try {
                NotificationManager NotifyMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                NotifyMgr.cancel(notificationId);
            } catch (Exception e) {
                Timber.e(e,"Failed to dismiss notification with id = %1$s", notificationId);
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

    private static HashMap<Integer, Long> lastFiredNotification = new HashMap<Integer, Long>();

    private static Boolean canNotify(Alarm alarm) {
        Long lastNotificationTime = lastFiredNotification.get(alarm.id);
        Calendar calendar = Calendar.getInstance();
        Long now = calendar.getTimeInMillis();
        calendar.add(Calendar.SECOND, -10);
        Long notificationThreshold = calendar.getTimeInMillis();
        boolean muted = alarm.mutedTill != null && alarm.mutedTill.getTime() > now;
        if (!muted && (lastNotificationTime == null || lastNotificationTime < notificationThreshold)) {
            lastFiredNotification.put(alarm.id, now);
            return true;
        } else {
            return false;
        }
    }

    private static void sendAlert(int stringResId, int _id, String name, String mac, Context context) {
        Timber.d("sendAlert tag.name = %1$s; alarm.id = %2$s; stringResId = %3$s", name, _id, stringResId);

        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
        NotificationCompat.Builder notification;

        Intent intent = new Intent(context, TagDetailsActivity.class);
        intent.putExtra("id", mac);

        PendingIntent pendingIntent = TaskStackBuilder.create(context)
                .addNextIntent(intent)
                .getPendingIntent(_id, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent cancelIntent = new Intent(context, CancelAlarmReceiver.class);
        cancelIntent.putExtra("alarmId", _id);
        cancelIntent.putExtra("notificationId", _id);
        PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(context, _id, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action action = new NotificationCompat.Action(R.drawable.ic_ruuvi_app_notification_icon_v2, context.getString(R.string.disable_this_alarm), cancelPendingIntent);

        Intent muteIntent = new Intent(context, MuteAlarmReceiver.class);
        muteIntent.putExtra(MuteAlarmReceiver.ALARM_ID, _id);
        PendingIntent mutePendingIntent = PendingIntent.getBroadcast(context, _id, muteIntent, PendingIntent.FLAG_UPDATE_CURRENT);

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
                .setDeleteIntent(mutePendingIntent)
                .setLargeIcon(bitmap)
                .setSmallIcon(R.drawable.ic_ruuvi_app_notification_icon_v2)
                .addAction(action);

        try {
            NotificationManager NotifyMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel("notify_001",
                        "Alert notifications",
                        NotificationManager.IMPORTANCE_DEFAULT);
                NotifyMgr.createNotificationChannel(channel);
            }

            NotifyMgr.notify(_id, notification.build());
        } catch (Exception e) {
            Timber.e(e, "Failed to create notification");
        }
    }
}
