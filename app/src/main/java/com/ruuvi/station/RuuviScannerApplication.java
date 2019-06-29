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
    RuuviScannerApplication me;

    public void stopScanning() {
        Log.d(TAG, "Stopping scanning");
        running = false;
        try {
            beaconManager.stopRangingBeaconsInRegion(region);
        } catch (Exception e) {
            Log.d(TAG, "Could not remove ranging region");
        }
    }

    public void disposeStuff() {
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
            disposeStuff();
            su.startForegroundService();
            return true;
        }
        return false;
    }

    public void startForegroundScanning() {
        if (runForegroundIfEnabled()) return;
        if (foreground) return;
        foreground = true;
        Utils.removeStateFile(getApplicationContext());
        Log.d(TAG, "Starting foreground scanning");
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
        bindBeaconManager(me, getApplicationContext());
        int scanInterval = new Preferences(getApplicationContext()).getBackgroundScanInterval() * 1000;
        int minInterval = 15 * 60 * 1000;
        if (scanInterval < minInterval) scanInterval = minInterval;
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
            Utils.setAltBeaconParsers(beaconManager);
            beaconManager.setBackgroundScanPeriod(5000);
            beaconManager.bind(consumer);
        } else if (!running) {
            running = true;
            try {
                beaconManager.startRangingBeaconsInRegion(region);
            } catch (Exception e) {
                Log.d(TAG, "Could not start ranging again");
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        me = this;
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
            Log.d(TAG, "onBecameForeground");
            startForegroundScanning();
            if (ruuviRangeNotifier != null) ruuviRangeNotifier.gatewayOn = false;
        }

        public void onBecameBackground() {
            Log.d(TAG, "onBecameBackground");
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
                disposeStuff();
                su.startForegroundService();
            }
            if (ruuviRangeNotifier != null) ruuviRangeNotifier.gatewayOn = true;
        }
    };

    @Override
    public void onBeaconServiceConnect() {
        Log.d(TAG, "onBeaconServiceConnect");
        //Toast.makeText(getApplicationContext(), "Started scanning (Application)", Toast.LENGTH_SHORT).show();
        ruuviRangeNotifier.gatewayOn = !foreground;
        if (!beaconManager.getRangingNotifiers().contains(ruuviRangeNotifier)) {
            beaconManager.addRangeNotifier(ruuviRangeNotifier);
        }
        running = true;
        try {
            beaconManager.startRangingBeaconsInRegion(region);
        } catch (Exception e) {
            Log.e(TAG, "Could not start ranging");
        }

    }
}
