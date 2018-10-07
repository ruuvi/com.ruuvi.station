package com.ruuvi.station;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.raizlabs.android.dbflow.config.FlowManager;
import com.ruuvi.station.service.RuuviRangeNotifier;
import com.ruuvi.station.util.Constants;
import com.ruuvi.station.util.Foreground;
import com.ruuvi.station.util.Preferences;
import com.ruuvi.station.util.ServiceUtils;

import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Region;
import org.altbeacon.bluetooth.BluetoothMedic;

/**
 * Created by io53 on 10/09/17.
 */

public class RuuviScannerApplication extends Application implements BeaconConsumer {
    private static final String TAG = "RuuviScannerApplication";
    BeaconManager beaconManager;
    Region region;
    boolean running = false;
    Preferences prefs;
    RuuviRangeNotifier ruuviRangeNotifier;
    private boolean foreground = true;

    public void stopScanning() {
        Log.d(TAG, "Stopping scanning");
        if (beaconManager == null) return;
        running = false;
        beaconManager.setBackgroundMode(false);
        ruuviRangeNotifier = null;
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
        if (prefs.getBackgroundScanEnabled() && prefs.getForegroundServiceEnabled()) {
            if (beaconManager != null) {
                stopScanning();
            }
            su.startForegroundService();
            return true;
        }
        return false;
    }

    public void startForegroundScanning() {
        Log.d(TAG, "Starting foreground scanning");
        if (runForegroundIfEnabled()) return;
        bindBeaconManager(this, this);
        beaconManager.setBackgroundMode(false);
        ruuviRangeNotifier.gatewayOn = false;
    }

    public void startBackgroundScanning() {
        Log.d(TAG, "Starting background scanning");
        if (runForegroundIfEnabled()) return;
        bindBeaconManager(this, getApplicationContext());
        beaconManager.setBackgroundBetweenScanPeriod(prefs.getBackgroundScanInterval() * 1000);
        beaconManager.setBackgroundScanPeriod(5000);
        beaconManager.setBackgroundMode(true);
        setupMedic(this);
    }

    public static void setupMedic(Context context) {
        BluetoothMedic medic = BluetoothMedic.getInstance();
        medic.enablePowerCycleOnFailures(context);
        medic.enablePeriodicTests(context, BluetoothMedic.SCAN_TEST);
    }

    private void bindBeaconManager(BeaconConsumer consumer, Context context) {
        if (beaconManager == null) {
            beaconManager = BeaconManager.getInstanceForApplication(context);
            /*
            beaconManager.getBeaconParsers().clear();
            beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(Constants.RuuviV2and4_LAYOUT));
            beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(Constants.RuuviV3_LAYOUT));
            beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(Constants.RuuviV5_LAYOUT));
            */
            region = new Region("com.ruuvi.station.leRegion", null, null, null);
            beaconManager.bind(consumer);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        FlowManager.init(this);
        prefs = new Preferences(this);
        Foreground.init(this);
        Foreground.get().addListener(listener);
        BeaconManager bm = BeaconManager.getInstanceForApplication(this);
        bm.getBeaconParsers().clear();
        bm.getBeaconParsers().add(new BeaconParser().setBeaconLayout(Constants.RuuviV2and4_LAYOUT));
        bm.getBeaconParsers().add(new BeaconParser().setBeaconLayout(Constants.RuuviV3_LAYOUT));
        bm.getBeaconParsers().add(new BeaconParser().setBeaconLayout(Constants.RuuviV5_LAYOUT));
        new ServiceUtils(this).startService();
    }

    Foreground.Listener listener = new Foreground.Listener() {
        public void onBecameForeground() {
            //startForegroundScanning();
            foreground = true;
            stopScanning();
            new ServiceUtils(getApplicationContext()).startService();
        }

        public void onBecameBackground() {
            foreground = false;
            // wait a bit before killing the service so scanning is not started too often
            // when opening / closing the app quickly
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!foreground) {
                        new ServiceUtils(getApplicationContext()).stopService();
                    }
                }
            }, 5000);
        }
    };

    @Override
    public void onBeaconServiceConnect() {
        if (ruuviRangeNotifier == null) ruuviRangeNotifier = new RuuviRangeNotifier(this, "RuuviScannerApplication");
        ruuviRangeNotifier.gatewayOn = !foreground;
        beaconManager.addRangeNotifier(ruuviRangeNotifier);
        try {
            beaconManager.startRangingBeaconsInRegion(region);
        } catch (Exception e) {
            Log.e(TAG, "Could not start ranging");
        }
    }
}
