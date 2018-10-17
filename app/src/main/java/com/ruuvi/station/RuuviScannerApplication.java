package com.ruuvi.station;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.raizlabs.android.dbflow.config.FlowManager;
import com.ruuvi.station.service.AltBeaconScannerForegroundService;
import com.ruuvi.station.service.RuuviRangeNotifier;
import com.ruuvi.station.util.BackgroundScanModes;
import com.ruuvi.station.util.Constants;
import com.ruuvi.station.util.Foreground;
import com.ruuvi.station.util.Preferences;
import com.ruuvi.station.util.ServiceUtils;
import com.ruuvi.station.util.Utils;

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
    private BeaconManager beaconManager;
    private Region region;
    boolean running = false;
    private Preferences prefs;
    private RuuviRangeNotifier ruuviRangeNotifier;
    private boolean foreground = false;
    BluetoothMedic medic;

    public void stopScanning() {
        Log.d(TAG, "Stopping scanning");
        medic = null;
        if (beaconManager == null) return;
        running = false;
        beaconManager.removeRangeNotifier(ruuviRangeNotifier);
        try {
            beaconManager.stopRangingBeaconsInRegion(region);
        } catch (Exception e) {
            Log.d(TAG, "Could not remove ranging region");
        }
        beaconManager.unbind(this);
        beaconManager = null;
    }

    private boolean runForegroundIfEnabled() {
        if (prefs.getBackgroundScanMode() == BackgroundScanModes.FOREGROUND) {
            ServiceUtils su = new ServiceUtils(getApplicationContext());
            stopScanning();
            su.startForegroundService();
            return true;
        }
        return false;
    }

    public void startForegroundScanning() {
        foreground = true;
        Log.d(TAG, "Starting foreground scanning");
        if (runForegroundIfEnabled()) return;
        bindBeaconManager(this, getApplicationContext());
        beaconManager.setBackgroundMode(false);
        if (ruuviRangeNotifier != null) ruuviRangeNotifier.gatewayOn = false;
    }

    public void startBackgroundScanning() {
        Log.d(TAG, "Starting background scanning");
        if (runForegroundIfEnabled()) return;
        if (prefs.getBackgroundScanMode() != BackgroundScanModes.BACKGROUND) {
            Log.d(TAG, "Background scanning is not enabled, ignoring");
            return;
        }
        bindBeaconManager(this, getApplicationContext());
        int scanInterval = new Preferences(getApplicationContext()).getBackgroundScanInterval() * 1000;
        if (scanInterval != beaconManager.getBackgroundBetweenScanPeriod()) {
            beaconManager.setBackgroundBetweenScanPeriod(scanInterval);
            try {
                beaconManager.updateScanPeriods();
            } catch (Exception e) {
                Log.e(TAG, "Could not update scan intervals");
            }
        }
        beaconManager.setBackgroundMode(true);
        if (ruuviRangeNotifier != null) ruuviRangeNotifier.gatewayOn = true;
        if (medic == null) medic = setupMedic(getApplicationContext());
    }

    public static BluetoothMedic setupMedic(Context context) {
        BluetoothMedic medic = BluetoothMedic.getInstance();
        medic.enablePowerCycleOnFailures(context);
        medic.enablePeriodicTests(context, BluetoothMedic.SCAN_TEST);
        return medic;
    }

    private void bindBeaconManager(BeaconConsumer consumer, Context context) {
        if (beaconManager == null) {
            beaconManager = BeaconManager.getInstanceForApplication(context.getApplicationContext());
            beaconManager.getBeaconParsers().clear();
            beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(Constants.RuuviV2and4_LAYOUT));
            beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(Constants.RuuviV3_LAYOUT));
            beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(Constants.RuuviV5_LAYOUT));
            beaconManager.setBackgroundScanPeriod(5000);
            beaconManager.bind(consumer);
        } else {
            Log.d(TAG, "BeaconManager is already there");
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "App class onCreate");
        FlowManager.init(getApplicationContext());
        prefs = new Preferences(getApplicationContext());
        ruuviRangeNotifier = new RuuviRangeNotifier(getApplicationContext(), "RuuviScannerApplication");
        Foreground.init(this);
        Foreground.get().addListener(listener);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!foreground) {
                    if (prefs.getBackgroundScanMode() == BackgroundScanModes.FOREGROUND) {
                        new ServiceUtils(getApplicationContext()).startForegroundService();
                    } else if (prefs.getBackgroundScanMode() == BackgroundScanModes.BACKGROUND) {
                        startBackgroundScanning();
                    }
                }
            }
        }, 5000);
        region = new Region("com.ruuvi.station.leRegion", null, null, null);
    }

    Foreground.Listener listener = new Foreground.Listener() {
        public void onBecameForeground() {
            Utils.removeStateFile(getApplicationContext());
            foreground = true;
            if (beaconManager != null) {
                // if background scanning is turned on
                // beaconManager is already setup so it can just be set to foreground mode
                beaconManager.setBackgroundMode(false);
            } else {
                startForegroundScanning();
            }
            if (ruuviRangeNotifier != null) ruuviRangeNotifier.gatewayOn = false;
        }

        public void onBecameBackground() {
            foreground = false;
            ServiceUtils su = new ServiceUtils(getApplicationContext());
            if (prefs.getBackgroundScanMode() == BackgroundScanModes.DISABLED) {
                // background scanning is disabled so all scanning things will be killed
                stopScanning();
                su.stopForegroundService();
            } else if (prefs.getBackgroundScanMode() == BackgroundScanModes.BACKGROUND) {
                if (su.isRunning(AltBeaconScannerForegroundService.class)) {
                    su.stopForegroundService();
                } else {
                    startBackgroundScanning();
                }
            } else {
                stopScanning();
                su.startForegroundService();
            }
            if (ruuviRangeNotifier != null) ruuviRangeNotifier.gatewayOn = true;
            // wait a bit before killing the service so scanning is not started too often
            // when opening / closing the app quickly
            /*
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!foreground) {
                        new ServiceUtils(getApplicationContext()).stopService();
                    }
                }
            }, 5000);
            */
        }
    };

    @Override
    public void onBeaconServiceConnect() {
        Log.d(TAG, "onBeaconServiceConnect");
        Toast.makeText(getApplicationContext(), "Started scanning (Application)", Toast.LENGTH_SHORT).show();
        ruuviRangeNotifier.gatewayOn = !foreground;
        beaconManager.removeRangeNotifier(ruuviRangeNotifier);
        if (!beaconManager.getRangingNotifiers().contains(ruuviRangeNotifier)) {
            beaconManager.addRangeNotifier(ruuviRangeNotifier);
        }
        try {
            beaconManager.startRangingBeaconsInRegion(region);
        } catch (Exception e) {
            Log.e(TAG, "Could not start ranging");
        }

    }
}
