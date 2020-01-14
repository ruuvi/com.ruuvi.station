package com.ruuvi.station.service;

import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.ruuvi.station.R;
import com.ruuvi.station.bluetooth.BluetoothScannerInteractor;
import com.ruuvi.station.feature.StartupActivity;
import com.ruuvi.station.gateway.Http;
import com.ruuvi.station.model.RuuviTag;
import com.ruuvi.station.model.TagSensorReading;
import com.ruuvi.station.util.AlarmChecker;
import com.ruuvi.station.util.BackgroundScanModes;
import com.ruuvi.station.util.Constants;
import com.ruuvi.station.util.Foreground;
import com.ruuvi.station.util.Preferences;

import java.util.List;


public class ScannerService extends Service {
    private static final String TAG = "ScannerService";

    private Handler handler;
    private Handler bgScanHandler;
    private boolean isForegroundMode = false;
    private int backgroundScanInterval = Constants.DEFAULT_SCAN_INTERVAL;
    private static final int SCAN_TIME_MS = 5000;
    private Location tagLocation;
    private PowerManager.WakeLock wakeLock;

    private BluetoothScannerInteractor bluetoothScannerInteractor;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        bluetoothScannerInteractor = new BluetoothScannerInteractor(getApplication());

        Foreground.init(getApplication());
        Foreground.get().addListener(listener);

        if (getForegroundMode()) startFG();
        handler = new Handler();
        handler.post(reStarter);
        bgScanHandler = new Handler();
    }

    private void updateLocation() {
        FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    tagLocation = location;
                }
            });
        }
    }

    private boolean getForegroundMode() {
        Preferences prefs = new Preferences(this);
        int getInterval =  prefs.getBackgroundScanInterval();
        return prefs.getBackgroundScanMode() == BackgroundScanModes.DISABLED && getInterval < 15 * 60;
        //return settings.getBoolean("pref_bgscan", false);
    }

    private Runnable reStarter = new Runnable() {
        @Override
        public void run() {
            bluetoothScannerInteractor.stopScan();
            bluetoothScannerInteractor.startScan();
            handler.postDelayed(reStarter, 5 * 60 * 1000);
        }
    };

    private Runnable bgLogger = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Started background scan");
            bluetoothScannerInteractor.clearBackgroundTags();
            bluetoothScannerInteractor.startScan();
            updateLocation();
            Log.d(TAG, "Scheduling next scan in " + backgroundScanInterval + "s");
            bgScanHandler.postDelayed(bgLogger, backgroundScanInterval * 1000);
            bgScanHandler.postDelayed(bgLoggerDone, SCAN_TIME_MS);
        }
    };

    private Runnable bgLoggerDone = new Runnable() {
        @Override
        public void run() {
            List<RuuviTag> backgroundTags = bluetoothScannerInteractor.getBackgroundTags();

            Log.d(TAG, "Stopping background scan, found " + backgroundTags.size() + " tags");
            bluetoothScannerInteractor.stopScan();
            Http.post(backgroundTags, tagLocation, getApplicationContext());

            for (RuuviTag tag: backgroundTags) {
                TagSensorReading reading = new TagSensorReading(tag);
                reading.save();
                AlarmChecker.check(tag, getApplicationContext());
            }
        }
    };

    public void startFG() {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);


        String channelId = "foreground_scanner_channel";
        if (Build.VERSION.SDK_INT >= 26) {
            CharSequence channelName = "RuuviStation foreground scanner";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, importance);
            try {
                notificationManager.createNotificationChannel(notificationChannel);
            } catch (Exception e) {
                Log.e(TAG, "Could not create notification channel");
            }
        }

        isForegroundMode = true;
        Intent notificationIntent = new Intent(this, StartupActivity.class);

        Bitmap bitmap = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.mipmap.ic_launcher);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        NotificationCompat.Builder notification;
        notification
                = new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setContentTitle(this.getString(R.string.scanner_notification_title))
                .setSmallIcon(R.mipmap.ic_launcher_small)
                .setTicker(this.getString(R.string.scanner_notification_ticker))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(this.getString(R.string.scanner_notification_message)))
                .setContentText(this.getString(R.string.scanner_notification_message))
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                .setLargeIcon(bitmap)
                .setContentIntent(pendingIntent);

        notification.setSmallIcon(R.drawable.ic_ruuvi_notification_icon_v1);

        startForeground(1337, notification.build());
    }


    @Override
    public void onDestroy() {
        bluetoothScannerInteractor.stopScan();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    Foreground.Listener listener = new Foreground.Listener() {
        public void onBecameForeground() {
            if (wakeLock != null) {
                try {
                    wakeLock.release();
                    Log.d(TAG, "Released wakelock");
                } catch (Exception e) {
                    Log.e(TAG, "Could not release wakelock");
                }
            }
            handler.postDelayed(reStarter, 5 * 60 * 1000);
            if (!isRunning(ScannerService.class))
                startService(new Intent(ScannerService.this, ScannerService.class));
            bgScanHandler.removeCallbacksAndMessages(null);
        }

        public void onBecameBackground() {
            handler.removeCallbacksAndMessages(null);
            if (!getForegroundMode()) {
                stopSelf();
                isForegroundMode = false;
            } else {
                Preferences prefs = new Preferences(getApplicationContext());
                if (prefs.getServiceWakelock()) {
                    PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(POWER_SERVICE);
                    try {
                        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                                "ruuviStation:serviceWakelock");
                        wakeLock.acquire();
                        Log.d(TAG, "Acquired wakelock");
                    } catch (Exception e) {
                        Log.e(TAG, "Could not acquire wakelock");
                    }
                } else {
                    wakeLock = null;
                }
                backgroundScanInterval = prefs.getBackgroundScanInterval();
                if (!isForegroundMode) startFG();
                bgScanHandler.postDelayed(bgLogger, backgroundScanInterval * 1000);
            }
        }
    };

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopForeground(true);
        stopSelf();
        Foreground.get().removeListener(listener);
    }

    private boolean isRunning(Class<?> serviceClass) {
        ActivityManager mgr = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : mgr.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
