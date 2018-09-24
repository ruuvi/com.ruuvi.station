package com.ruuvi.station.service;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.ruuvi.station.R;
import com.ruuvi.station.feature.StartupActivity;
import com.ruuvi.station.feature.main.MainActivity;
import com.ruuvi.station.gateway.Http;
import com.ruuvi.station.model.Alarm;
import com.ruuvi.station.model.LeScanResult;
import com.ruuvi.station.model.RuuviTag;
import com.ruuvi.station.model.RuuviTag_Table;
import com.ruuvi.station.model.ScanEvent;
import com.ruuvi.station.model.ScanEventSingle;
import com.ruuvi.station.model.TagSensorReading;
import com.ruuvi.station.scanning.BackgroundScanner;
import com.ruuvi.station.util.AlarmChecker;
import com.ruuvi.station.util.ComplexPreferences;
import com.ruuvi.station.util.Constants;
import com.ruuvi.station.util.DeviceIdentifier;
import com.ruuvi.station.util.Foreground;
import com.ruuvi.station.model.RuuviTagComplexList;
import com.ruuvi.station.util.Utils;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;

public class ScannerService extends Service {
    private static final String TAG = "ScannerService";

    private boolean scanning;

    private no.nordicsemi.android.support.v18.scanner.ScanSettings scanSettings;
    private BluetoothLeScannerCompat scanner;
    private Handler handler;
    private Handler bgScanHandler;
    private boolean isForegroundMode = false;
    private boolean foreground = false;
    private SharedPreferences settings;
    private int backgroundScanInterval = Constants.DEFAULT_SCAN_INTERVAL;
    private static List<RuuviTag> backgroundTags = new ArrayList<>();
    private static final int SCAN_TIME_MS = 5000;
    private Location tagLocation;
    private PowerManager.WakeLock wakeLock;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Foreground.init(getApplication());
        Foreground.get().addListener(listener);

        foreground = true;
        scanSettings = new no.nordicsemi.android.support.v18.scanner.ScanSettings.Builder()
                .setReportDelay(0)
                .setScanMode(no.nordicsemi.android.support.v18.scanner.ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setUseHardwareBatchingIfSupported(false).build();

        scanner = BluetoothLeScannerCompat.getScanner();

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
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        int getInterval = settings.getInt("pref_background_scan_interval", Constants.DEFAULT_SCAN_INTERVAL);
        return settings.getBoolean("pref_bgscan", false) && getInterval < 15 * 60;
        //return settings.getBoolean("pref_bgscan", false);
    }

    private Runnable reStarter = new Runnable() {
        @Override
        public void run() {
            stopScan();
            startScan();
            handler.postDelayed(reStarter, 5 * 60 * 1000);
        }
    };

    private Runnable bgLogger = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Started background scan");
            backgroundTags.clear();
            startScan();
            updateLocation();
            Log.d(TAG, "Scheduling next scan in " + backgroundScanInterval + "s");
            bgScanHandler.postDelayed(bgLogger, backgroundScanInterval * 1000);
            bgScanHandler.postDelayed(bgLoggerDone, SCAN_TIME_MS);
        }
    };

    private Runnable bgLoggerDone = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Stopping background scan, found " + backgroundTags.size() + " tags");
            stopScan();
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

        if (Build.VERSION.SDK_INT < 21) {
            notification.setSmallIcon(R.mipmap.ic_launcher_small);
        } else {
            notification.setSmallIcon(R.drawable.ic_ruuvi_notification_icon_v1);
        }

        startForeground(1337, notification.build());
    }

    private boolean canScan() {
        return scanner != null;
    }

    public void startScan() {
        if (scanning || !canScan()) return;
        scanning = true;
        try {
            scanner.startScan(null, scanSettings, nsCallback);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            scanning = false;
            Toast.makeText(getApplicationContext(), "Couldn't start scanning, is bluetooth disabled?", Toast.LENGTH_LONG).show();
        }
    }

    public void stopScan() {
        if (!canScan()) return;
        scanning = false;
        scanner.stopScan(nsCallback);
    }

    private no.nordicsemi.android.support.v18.scanner.ScanCallback nsCallback = new no.nordicsemi.android.support.v18.scanner.ScanCallback() {
        @Override
        public void onScanResult(int callbackType, no.nordicsemi.android.support.v18.scanner.ScanResult result) {
            super.onScanResult(callbackType, result);
            foundDevice(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());
        }
    };

    private void foundDevice(BluetoothDevice device, int rssi, byte[] data) {
        LeScanResult dev = new LeScanResult();
        dev.device = device;
        dev.rssi = rssi;
        dev.scanData = data;

        //Log.d(TAG, "found: " + device.getAddress());
        RuuviTag tag = dev.parse();
        if (tag != null) logTag(tag, getApplicationContext(), foreground);
    }

    @Override
    public void onDestroy() {
        stopScan();
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
            foreground = true;
            handler.postDelayed(reStarter, 5 * 60 * 1000);
            if (!isRunning(ScannerService.class))
                startService(new Intent(ScannerService.this, ScannerService.class));
            bgScanHandler.removeCallbacksAndMessages(null);
        }

        public void onBecameBackground() {
            foreground = false;
            handler.removeCallbacksAndMessages(null);
            if (!getForegroundMode()) {
                stopSelf();
                isForegroundMode = false;
            } else {
                if (settings.getBoolean("pref_wakelock", false)) {
                    PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(POWER_SERVICE);
                    try {
                        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                                "MyWakelockTag");
                        wakeLock.acquire();
                        Log.d(TAG, "Acquired wakelock");
                    } catch (Exception e) {
                        Log.e(TAG, "Could not acquire wakelock");
                    }
                } else {
                    wakeLock = null;
                }
                backgroundScanInterval = settings.getInt("pref_background_scan_interval", Constants.DEFAULT_SCAN_INTERVAL);
                if (!isForegroundMode) startFG();
                bgScanHandler.postDelayed(bgLogger, backgroundScanInterval * 1000);
            }
        }
    };

    public static Map<String, Long> lastLogged = null;
    public static int LOG_INTERVAL = 5; // seconds

    public static void logTag(RuuviTag ruuviTag, Context context, boolean foreground) {
        RuuviTag dbTag = RuuviTag.get(ruuviTag.id);
        if (dbTag != null) {
            ruuviTag = dbTag.preserveData(ruuviTag);
            ruuviTag.update();
            if (!dbTag.favorite) return;
        } else {
            ruuviTag.updateAt = new Date();
            ruuviTag.save();
            return;
        }

        if (!foreground) {
            if (ruuviTag.favorite && checkForSameTag(backgroundTags, ruuviTag) == -1) {
                backgroundTags.add(ruuviTag);
            }
            return;
        }

        if (lastLogged == null) lastLogged = new HashMap<>();

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, -LOG_INTERVAL);
        long loggingThreshold = calendar.getTime().getTime();
        for (Map.Entry<String, Long> entry : lastLogged.entrySet())
        {
            if (entry.getKey().equals(ruuviTag.id) && entry.getValue() > loggingThreshold) {
                return;
            }
        }

        List<RuuviTag> tags = new ArrayList<>();
        tags.add(ruuviTag);
        Http.post(tags, null, context);

        lastLogged.put(ruuviTag.id, new Date().getTime());
        TagSensorReading reading = new TagSensorReading(ruuviTag);
        reading.save();
        AlarmChecker.check(ruuviTag, context);
    }

    public static boolean Exists(String id) {
        long count = SQLite.selectCountOf()
                .from(RuuviTag.class)
                .where(RuuviTag_Table.id.eq(id))
                .count();
        return count > 0;
    }

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

    private static int checkForSameTag(List<RuuviTag> arr, RuuviTag ruuvi) {
        for (int i = 0; i < arr.size(); i++) {
            if (ruuvi.id.equals(arr.get(i).id)) {
                return i;
            }
        }
        return -1;
    }
}
