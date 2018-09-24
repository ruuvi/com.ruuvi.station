package com.ruuvi.station.service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.ruuvi.station.gateway.Http;
import com.ruuvi.station.model.LeScanResult;
import com.ruuvi.station.model.RuuviTag;
import com.ruuvi.station.util.Constants;
import com.ruuvi.station.util.Foreground;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class AltBeaconScannerService extends Service implements BeaconConsumer {
    private static final String TAG = "AltBeaconScannerService";

    private int backgroundScanInterval = Constants.DEFAULT_SCAN_INTERVAL;
    private static List<RuuviTag> backgroundTags = new ArrayList<>();
    private Location tagLocation;
    private BeaconManager beaconManager;
    private Region region;
    public static final String RuuviV2and4 = "s:0-1=feaa,m:2-2=10,p:3-3:-41,i:4-21v";
    //public static final String RuuviV3 = "x,m:0-1=9904,m:2-2=03,i:3-15,d:3-3,d:4-4,d:5-5,d:6-7,d:8-9,d:10-11,d:12-13,d:14-15";
    //public static final String RuuviV5 = "x,m:0-1=9904,m:2-2=05,i:20-25,d:3-4,d:5-6,d:7-8,d:9-10,d:11-12,d:13-14,d:15-16,d:17-17,d:18-19,d:20-25";
    public static final String RuuviV3 = "x,m:0-1=9904,m:2-2=03,i:2-15,d:2-2,d:3-3,d:4-4,d:5-5,d:6-6,d:7-7,d:8-8,d:9-9,d:10-10,d:11-11,d:12-12,d:13-13,d:14-14,d:15-15";
    public static final String RuuviV5 = "x,m:0-1=9904,m:2-2=05,i:20-25,d:2-2,d:3-3,d:4-4,d:5-5,d:6-6,d:7-7,d:8-8,d:9-9,d:10-10,d:11-11,d:12-12,d:13-13,d:14-14,d:15-15,d:16-16,d:17-17,d:18-18,d:19-19,d:20-20,d:21-21,d:22-22,d:23-23,d:24-24,d:25-25";
    BackgroundPowerSaver backgroundPowerSaver;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().clear();
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(RuuviV2and4));
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(RuuviV5));
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(RuuviV3));

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        backgroundScanInterval = settings.getInt("pref_background_scan_interval", backgroundScanInterval);

        beaconManager.setForegroundScanPeriod(1100);
        beaconManager.setForegroundBetweenScanPeriod(0);
        beaconManager.setBackgroundBetweenScanPeriod(backgroundScanInterval * 1000);
        beaconManager.setBackgroundScanPeriod(5000);

        backgroundPowerSaver = new BackgroundPowerSaver(this);

        region = new Region("com.ruuvi.station.leRegion", null, null, null);
        beaconManager.bind(this);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            beaconManager.removeAllRangeNotifiers();
            beaconManager.stopRangingBeaconsInRegion(region);
        } catch (Exception e) {
            Log.d(TAG, "Could not remove ranging region");
        }
        beaconManager.unbind(this);
        beaconManager = null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    Foreground.Listener listener = new Foreground.Listener() {
        public void onBecameForeground() {
        }

        public void onBecameBackground() {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            backgroundScanInterval = settings.getInt("pref_background_scan_interval", backgroundScanInterval);

            beaconManager.setBackgroundBetweenScanPeriod(backgroundScanInterval * 1000);
            try {
                beaconManager.updateScanPeriods();
            } catch (Exception e) {
                Log.e(TAG, "Could not update scan periods");
            }
        }
    };

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopForeground(true);
        stopSelf();
        Foreground.get().removeListener(listener);
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                updateLocation();
                Log.d(TAG, "There are  "+beacons.size());
                List<RuuviTag> tags = new ArrayList<>();
                Log.d(TAG, "didRangeBeaconsInRegion " + beacons.size());
                foundBeacon: for (Beacon beacon : beacons) {
                    // the same tag can appear multiple times
                    for (RuuviTag tag : tags) {
                        if (tag.id.equals(beacon.getBluetoothAddress())) continue foundBeacon;
                    }
                    RuuviTag tag = LeScanResult.fromAltbeacon(beacon);
                    if (tag != null) {
                        tags.add(tag);
                        ScannerService.logTag(tag, getApplicationContext(), true);
                    }
                }
                if (tags.size() > 0) Http.post(tags, tagLocation, getApplicationContext());
            }
        });

        try {
            beaconManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            Log.e(TAG, "Could not start ranging");
        }
    }
}
