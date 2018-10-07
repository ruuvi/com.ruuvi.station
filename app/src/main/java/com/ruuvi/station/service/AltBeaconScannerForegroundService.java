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
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.ruuvi.station.R;
import com.ruuvi.station.RuuviScannerApplication;
import com.ruuvi.station.feature.StartupActivity;
import com.ruuvi.station.util.Constants;
import com.ruuvi.station.util.Preferences;

import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Region;


public class AltBeaconScannerForegroundService extends Service implements BeaconConsumer {
    private static final String TAG = "AScannerFgService";

    private BeaconManager beaconManager;
    private Region region;
    RuuviRangeNotifier ruuviRangeNotifier;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Starting foreground service");
        beaconManager = BeaconManager.getInstanceForApplication(this);
        if (beaconManager.isAnyConsumerBound()) beaconManager.unbind(this);
        beaconManager.getBeaconParsers().clear();
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(Constants.RuuviV2and4_LAYOUT));
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(Constants.RuuviV3_LAYOUT));
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(Constants.RuuviV5_LAYOUT));

        region = new Region("com.ruuvi.station.leRegion", null, null, null);
        startFG();
        beaconManager.bind(this);
        RuuviScannerApplication.setupMedic(this);
    }

    private void startFG() {
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

        beaconManager.enableForegroundServiceScanning(notification.build(), 1337);
        beaconManager.setEnableScheduledScanJobs(false);
        startForeground(1337, notification.build());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy =======");
        try {
            beaconManager.removeAllRangeNotifiers();
            beaconManager.stopRangingBeaconsInRegion(region);
        } catch (Exception e) {
            Log.d(TAG, "Could not remove ranging region");
        }
        beaconManager.unbind(this);
        beaconManager.disableForegroundServiceScanning();
        beaconManager.setEnableScheduledScanJobs(true);
        beaconManager = null;
        stopForeground(true);
        ((RuuviScannerApplication)(this.getApplication())).startForegroundScanning();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onBeaconServiceConnect() {
        if (ruuviRangeNotifier == null) ruuviRangeNotifier = new RuuviRangeNotifier(this, "AltBeaconScannerService");
        ruuviRangeNotifier.gatewayOn = true;
        beaconManager.removeAllRangeNotifiers();
        beaconManager.addRangeNotifier(ruuviRangeNotifier);
        try {
            beaconManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            Log.e(TAG, "Could not start ranging");
        }
    }
}
