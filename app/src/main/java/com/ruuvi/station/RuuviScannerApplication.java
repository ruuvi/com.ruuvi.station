package com.ruuvi.station;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.raizlabs.android.dbflow.config.FlowManager;
import com.ruuvi.station.service.AltBeaconScannerForegroundService;
import com.ruuvi.station.service.AltBeaconScannerService;
import com.ruuvi.station.service.RuuviRangeNotifier;
import com.ruuvi.station.util.Constants;
import com.ruuvi.station.util.Foreground;
import com.ruuvi.station.util.ServiceUtils;

import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Region;

/**
 * Created by io53 on 10/09/17.
 */

public class RuuviScannerApplication extends Application implements BeaconConsumer {
    private static final String TAG = "RuuviScannerApplication";
    BeaconManager beaconManager;
    Region region;
    boolean running = false;

    public void stopScanning() {
        if (beaconManager == null) return;
        running = false;
        Log.d(TAG, "DEBUG, Stopped background scanning");
        Log.d(TAG, "Stopped background scanning");
        beaconManager.setBackgroundMode(false);
        try {
            beaconManager.removeAllRangeNotifiers();
            beaconManager.stopRangingBeaconsInRegion(region);
        } catch (Exception e) {
            Log.d(TAG, "Could not remove ranging region");
        }
        beaconManager.unbind(this);
        beaconManager = null;
    }

    private boolean runForegroundIfEnabled() {
        ServiceUtils su = new ServiceUtils(getApplicationContext());
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        boolean bgScan = settings.getBoolean("pref_bgscan",false );
        if (bgScan && settings.getBoolean("foreground_service", false)) {
            if (beaconManager != null) {
                stopScanning();
            }
            su.startForegroundService();
            return true;
        }
        return false;
    }

    public void startForegroundScanning() {
        Log.d(TAG, "Started foreground scanning");
        if (runForegroundIfEnabled()) return;
        if (beaconManager == null) {
            beaconManager = BeaconManager.getInstanceForApplication(this);
            beaconManager.getBeaconParsers().clear();
            beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(Constants.RuuviV2and4_LAYOUT));
            beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(Constants.RuuviV3_LAYOUT));
            beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(Constants.RuuviV5_LAYOUT));

            region = new Region("com.ruuvi.station.leRegion", null, null, null);
            beaconManager.bind(this);
        }
        beaconManager.setBackgroundMode(false);
    }

    public void startBackgroundScanning() {
        Log.d(TAG, "Started background scanning");
        if (runForegroundIfEnabled()) return;
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        if (beaconManager == null) {
            beaconManager = BeaconManager.getInstanceForApplication(this);
            beaconManager.getBeaconParsers().clear();
            beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(Constants.RuuviV2and4_LAYOUT));
            beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(Constants.RuuviV3_LAYOUT));
            beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(Constants.RuuviV5_LAYOUT));

            region = new Region("com.ruuvi.station.leRegion", null, null, null);
            beaconManager.bind(this);
        }

        int backgroundScanInterval = settings.getInt("pref_background_scan_interval", Constants.DEFAULT_SCAN_INTERVAL);

        beaconManager.setBackgroundBetweenScanPeriod(backgroundScanInterval * 1000);
        beaconManager.setBackgroundScanPeriod(5000);
        beaconManager.setBackgroundMode(true);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        FlowManager.init(this);
        Foreground.init(this);
        Foreground.get().addListener(listener);
    }

    Foreground.Listener listener = new Foreground.Listener() {
        public void onBecameForeground() {
            startForegroundScanning();
        }

        public void onBecameBackground() {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            boolean bgScan = settings.getBoolean("pref_bgscan",false );
            if (bgScan) startBackgroundScanning();
            else {
                new ServiceUtils(getApplicationContext()).stopForegroundService();
                stopScanning();
            }
        }
    };

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.addRangeNotifier(new RuuviRangeNotifier(this, "RuuviScannerApplication"));
        try {
            beaconManager.startRangingBeaconsInRegion(region);
        } catch (Exception e) {
            Log.e(TAG, "Could not start ranging");
        }
    }
}
