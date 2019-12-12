package com.ruuvi.station.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.ruuvi.station.R;
import com.ruuvi.station.RuuviScannerApplication;
import com.ruuvi.station.feature.StartupActivity;
import com.ruuvi.station.util.Foreground;
import com.ruuvi.station.util.Preferences;
import com.ruuvi.station.util.Utils;


public class AltBeaconScannerForegroundService extends Service {
    private static final String TAG = "AScannerFgService";

    NotificationCompat.Builder notification;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        if (((RuuviScannerApplication) getApplication()).bluetoothInteractor.isForegroundBeaconManagerActive()) {
            updateNotification();
        }
        return Service.START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Starting foreground service");
        ((RuuviScannerApplication) getApplication()).bluetoothInteractor.onCreateForegroundScanningService();

        Foreground.init(getApplication());
        Foreground.get().addListener(listener);

        startFG(); // start foreground notification

        startInBackgroundMode(); // start in background mode
    }


    private NotificationCompat.Builder setupNotification() {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        String channelId = "foreground_scanner_channel";
        if (Build.VERSION.SDK_INT >= 26) {
            CharSequence channelName = "RuuviStation foreground scanner";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, importance);
            try {
                notificationManager.createNotificationChannel(notificationChannel);
            } catch (Exception e) {
                Log.e(TAG, "Could not create notification channel");
            }
        }

        Intent notificationIntent = new Intent(this, StartupActivity.class);

        Bitmap bitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.mipmap.ic_launcher);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        String notificationText = getString(R.string.scanner_notification_title);
        notificationText = notificationText.replace("..", " every ");
        int scanInterval = new Preferences(getApplicationContext()).getBackgroundScanInterval();
        int min = scanInterval / 60;
        int sec = scanInterval - min * 60;
        if (min > 0) {
            String minutes = getString(R.string.minutes).toLowerCase();
            if (min == 1) {
                minutes = minutes.substring(0, minutes.length() - 1);
            }
            notificationText += min + " " + minutes + ", ";
        }
        if (sec > 0) {
            String seconds = getString(R.string.seconds).toLowerCase();
            if (sec == 1) {
                seconds = seconds.substring(0, seconds.length() - 1);
            }
            notificationText += sec + " " + seconds;
        }
        else {
            notificationText = notificationText.replace(", ", "");
        }

        notification
                = new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setContentTitle(notificationText)
                .setSmallIcon(R.mipmap.ic_launcher_small)
                .setTicker(this.getString(R.string.scanner_notification_ticker))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(this.getString(R.string.scanner_notification_message)))
                .setContentText(this.getString(R.string.scanner_notification_message))
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setLargeIcon(bitmap)
                .setContentIntent(pendingIntent);

        notification.setSmallIcon(R.drawable.ic_ruuvi_bgscan_icon);

        return notification;
    }

    private void startFG() {
        setupNotification();
        //beaconManager.enableForegroundServiceScanning(notification.build(), 1337);

        ((RuuviScannerApplication) getApplication()).bluetoothInteractor.setEnableScheduledScanJobs(false);

        startForeground(1337, notification.build());
    }

    private void updateNotification() {
        setupNotification();
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        try {
            mNotificationManager.notify(1337, notification.build());
        } catch (NullPointerException e) {
            Log.d(TAG, "Could not update notification");
        }
    }

    private void startInBackgroundMode() {
        final Long scanInterval = (long) (new Preferences(getApplicationContext()).getBackgroundScanInterval() * 1000);
        final Long backgroundBetweenScanPeriod = ((RuuviScannerApplication) getApplication())
                .bluetoothInteractor.getBackgroundBetweenScanPeriod();

        if (!scanInterval.equals(backgroundBetweenScanPeriod)) {
            updateNotification();

            ((RuuviScannerApplication) getApplication()).bluetoothInteractor.startInBackgroundMode(scanInterval);
        }
        ((RuuviScannerApplication) getApplication()).bluetoothInteractor.setBackgroundMode(true);
    }

    Foreground.Listener listener = new Foreground.Listener() {
        public void onBecameForeground() {
            Utils.removeStateFile(getApplicationContext());
            ((RuuviScannerApplication) getApplication()).bluetoothInteractor.setBackgroundMode(false);
        }

        public void onBecameBackground() {
            startInBackgroundMode();
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy =======");

        ((RuuviScannerApplication) getApplication()).bluetoothInteractor.onDestroyForegroundScannerService();

        stopForeground(true);
        if (listener != null) Foreground.get().removeListener(listener);
        ((RuuviScannerApplication) getApplication()).bluetoothInteractor.startBackgroundScanning();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
